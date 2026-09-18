-- Cloudflare D1 Schema for EV Charger Recorder
-- Table: charging_records

CREATE TABLE IF NOT EXISTS charging_records (
    sync_id TEXT PRIMARY KEY,
    timestamp INTEGER NOT NULL,
    odometer_km REAL NOT NULL,
    energy_kwh REAL NOT NULL,
    cost REAL NOT NULL,
    charge_type TEXT NOT NULL,
    operator TEXT NOT NULL,
    soc_percent INTEGER NOT NULL,
    start_soc_percent INTEGER,
    location_name TEXT DEFAULT '',
    latitude REAL,
    longitude REAL,
    skip_efficiency_calc INTEGER DEFAULT 0,
    notes TEXT DEFAULT '',
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_records_updated_at ON charging_records (updated_at);
CREATE INDEX IF NOT EXISTS idx_records_timestamp ON charging_records (timestamp);
