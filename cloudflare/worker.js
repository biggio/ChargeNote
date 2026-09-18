/**
 * Cloudflare Worker for EV Charger Recorder
 * Synchronizes charging records between Android App and Cloudflare D1 SQLite Database.
 */

export default {
  async fetch(request, env, ctx) {
    // 1. CORS Preflight
    if (request.method === "OPTIONS") {
      return handleCors();
    }

    const url = new URL(request.url);
    const path = url.pathname;

    // 2. Verify Authorization
    const authHeader = request.headers.get("Authorization") || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : "";
    const expectedSecret = env.SYNC_SECRET ? env.SYNC_SECRET.trim() : "";

    // If server has SYNC_SECRET configured, strictly enforce it!
    if (expectedSecret) {
      if (!token || token !== expectedSecret) {
        return jsonResponse({
          success: false,
          error: "未授權 (HTTP 401)：同步金鑰（Sync Secret）不正確，請確認與 Cloudflare Worker 一致"
        }, 401);
      }
    }

    // 3. Health & Auth check
    if (path === "/" || path === "/api/health") {
      let dbStatus = "not_bound";
      let recordCount = 0;

      if (env.DB) {
        try {
          const res = await env.DB.prepare("SELECT count(*) as total FROM charging_records").first();
          dbStatus = "connected";
          recordCount = res ? res.total : 0;
        } catch (dbErr) {
          dbStatus = "error: " + (dbErr.message || String(dbErr));
        }
      }

      const hasSecret = Boolean(expectedSecret);
      const isWarn = !hasSecret && Boolean(token);

      return jsonResponse({
        status: dbStatus === "connected" ? "ok" : "warning",
        service: "EV Charger Recorder D1 Sync",
        version: "1.5",
        database: dbStatus,
        recordCount: recordCount,
        hasServerSecret: hasSecret,
        warning: isWarn ? "Cloudflare Worker 尚未設定 SYNC_SECRET 變數，目前金鑰尚未具備防護效果" : null,
        timestamp: Date.now()
      });
    }

    if (!env.DB) {
      return jsonResponse({ success: false, error: "伺服器錯誤：未綁定 D1 資料庫 (env.DB is undefined)" }, 500);
    }

    try {
      // 4. API Routes
      if (path === "/api/sync" && request.method === "GET") {
        const since = parseInt(url.searchParams.get("since") || "0", 10);
        return await handleGetSync(env.DB, since);
      }

      if (path === "/api/sync" && request.method === "POST") {
        const body = await request.json();
        return await handlePostSync(env.DB, body);
      }

      if (path === "/api/backup" && request.method === "GET") {
        return await handleGetAllRecords(env.DB);
      }

      if (path === "/api/backup" && request.method === "POST") {
        const body = await request.json();
        return await handleRestoreBackup(env.DB, body);
      }

      return jsonResponse({ success: false, error: "Not Found: " + path }, 404);
    } catch (err) {
      return jsonResponse({ success: false, error: err.message || String(err) }, 500);
    }
  }
};

/**
 * Handle GET /api/sync?since=timestamp
 */
async function handleGetSync(db, since) {
  const query = since > 0
    ? "SELECT * FROM charging_records WHERE updated_at > ? ORDER BY updated_at ASC"
    : "SELECT * FROM charging_records ORDER BY timestamp DESC";
  
  const stmt = since > 0 ? db.prepare(query).bind(since) : db.prepare(query);
  const { results } = await stmt.all();

  return jsonResponse({
    success: true,
    records: results || [],
    serverTimestamp: Date.now()
  });
}

/**
 * Handle POST /api/sync
 * Upserts incoming records using Last-Write-Wins based on updated_at,
 * and returns any records updated on the server since the client's last sync time.
 */
async function handlePostSync(db, body) {
  const clientRecords = Array.isArray(body.records) ? body.records : [];
  const clientSince = parseInt(body.since || "0", 10);
  const now = Date.now();

  if (clientRecords.length > 0) {
    const upsertSql = `
      INSERT INTO charging_records (
        sync_id, timestamp, odometer_km, energy_kwh, cost, charge_type, operator,
        soc_percent, start_soc_percent, location_name, latitude, longitude,
        skip_efficiency_calc, notes, updated_at, is_deleted
      ) VALUES (
        ?, ?, ?, ?, ?, ?, ?,
        ?, ?, ?, ?, ?,
        ?, ?, ?, ?
      )
      ON CONFLICT(sync_id) DO UPDATE SET
        timestamp = excluded.timestamp,
        odometer_km = excluded.odometer_km,
        energy_kwh = excluded.energy_kwh,
        cost = excluded.cost,
        charge_type = excluded.charge_type,
        operator = excluded.operator,
        soc_percent = excluded.soc_percent,
        start_soc_percent = excluded.start_soc_percent,
        location_name = excluded.location_name,
        latitude = excluded.latitude,
        longitude = excluded.longitude,
        skip_efficiency_calc = excluded.skip_efficiency_calc,
        notes = excluded.notes,
        updated_at = excluded.updated_at,
        is_deleted = excluded.is_deleted
      WHERE excluded.updated_at >= charging_records.updated_at;
    `;

    // Process in batches of 50 for D1 limits
    const BATCH_SIZE = 50;
    for (let i = 0; i < clientRecords.length; i += BATCH_SIZE) {
      const slice = clientRecords.slice(i, i + BATCH_SIZE);
      const statements = slice.map(r => {
        return db.prepare(upsertSql).bind(
          r.sync_id || r.syncId,
          r.timestamp || 0,
          r.odometer_km ?? r.odometerKm ?? 0.0,
          r.energy_kwh ?? r.energyKwh ?? 0.0,
          r.cost ?? 0.0,
          r.charge_type || r.chargeType || "DC",
          r.operator || "其他",
          r.soc_percent ?? r.socPercent ?? 80,
          r.start_soc_percent ?? r.startSocPercent ?? null,
          r.location_name ?? r.locationName ?? "",
          r.latitude ?? null,
          r.longitude ?? null,
          (r.skip_efficiency_calc || r.skipEfficiencyCalc) ? 1 : 0,
          r.notes || "",
          r.updated_at || r.updatedAt || now,
          r.is_deleted ? 1 : 0
        );
      });
      await db.batch(statements);
    }
  }

  // Fetch updates for the client
  const fetchQuery = clientSince > 0
    ? "SELECT * FROM charging_records WHERE updated_at > ? ORDER BY updated_at ASC"
    : "SELECT * FROM charging_records ORDER BY timestamp DESC";
  
  const stmt = clientSince > 0 ? db.prepare(fetchQuery).bind(clientSince) : db.prepare(fetchQuery);
  const { results } = await stmt.all();

  return jsonResponse({
    success: true,
    syncedCount: clientRecords.length,
    serverRecords: results || [],
    serverTimestamp: now
  });
}

/**
 * Handle GET /api/backup
 */
async function handleGetAllRecords(db) {
  const { results } = await db.prepare("SELECT * FROM charging_records ORDER BY timestamp ASC").all();
  return jsonResponse({
    success: true,
    records: results || [],
    count: results ? results.length : 0,
    serverTimestamp: Date.now()
  });
}

/**
 * Handle POST /api/backup (Restore full backup)
 */
async function handleRestoreBackup(db, body) {
  const records = Array.isArray(body.records) ? body.records : [];
  if (records.length === 0) {
    return jsonResponse({ success: false, error: "無備份資料可供還原" }, 400);
  }

  // Clear existing records and replace
  await db.prepare("DELETE FROM charging_records").run();

  const insertSql = `
    INSERT INTO charging_records (
      sync_id, timestamp, odometer_km, energy_kwh, cost, charge_type, operator,
      soc_percent, start_soc_percent, location_name, latitude, longitude,
      skip_efficiency_calc, notes, updated_at, is_deleted
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
  `;

  const now = Date.now();
  const BATCH_SIZE = 50;
  for (let i = 0; i < records.length; i += BATCH_SIZE) {
    const slice = records.slice(i, i + BATCH_SIZE);
    const statements = slice.map(r => {
      return db.prepare(insertSql).bind(
        r.sync_id || r.syncId,
        r.timestamp || 0,
        r.odometer_km ?? r.odometerKm ?? 0.0,
        r.energy_kwh ?? r.energyKwh ?? 0.0,
        r.cost ?? 0.0,
        r.charge_type || r.chargeType || "DC",
        r.operator || "其他",
        r.soc_percent ?? r.socPercent ?? 80,
        r.start_soc_percent ?? r.startSocPercent ?? null,
        r.location_name ?? r.locationName ?? "",
        r.latitude ?? null,
        r.longitude ?? null,
        (r.skip_efficiency_calc || r.skipEfficiencyCalc) ? 1 : 0,
        r.notes || "",
        r.updated_at || r.updatedAt || now,
        r.is_deleted ? 1 : 0
      );
    });
    await db.batch(statements);
  }

  return jsonResponse({
    success: true,
    restoredCount: records.length,
    serverTimestamp: now
  });
}

function handleCors() {
  return new Response(null, {
    status: 204,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
      "Access-Control-Max-Age": "86400"
    }
  });
}

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*"
    }
  });
}
