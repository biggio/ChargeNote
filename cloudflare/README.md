# Cloudflare D1 雲端同步設定指南 (3 分鐘速成)

本指南引導您如何在 Cloudflare 免費建立 **D1 資料庫** 與 **Worker 服務**，讓您的 EV Charger Recorder 享有終身免費的雲端雙向同步與備份。

---

## 方案 A：使用 Cloudflare 網頁控制台（推薦，完全不需安裝指令）

### 步驟 1：建立 D1 資料庫
1. 登入 [Cloudflare Dashboard](https://dash.cloudflare.com/)。
2. 左側選單點選 **Storage & Databases** ➜ **D1 SQL Database**。
3. 點擊 **Create Database**，名稱輸入 `ev-charging-db`，點擊 **Create**。
4. 進入剛建好的資料庫，切換到 **Console** 分頁。
5. 將專案中的 [`schema.sql`](schema.sql) 內容複製，貼入 Console 中並點擊 **Execute** 執行建表。

### 步驟 2：建立 Cloudflare Worker
1. 左側選單點選 **Compute (Workers)** ➜ **Workers & Pages**。
2. 點擊 **Create Application** ➜ **Create Worker**。
3. 名稱輸入 `ev-charger-sync`，點擊 **Deploy**。
4. 部署完成後點擊 **Edit code**：
   - 清空編輯器內的預設代碼。
   - 將專案中的 [`worker.js`](worker.js) 內容全選貼入。
   - 點擊右上角 **Deploy** 儲存。

### 步驟 3：綁定 D1 資料庫與設定同步密碼
1. 回到該 Worker 的 **Settings** 分頁：
   - 點選 **Bindings** ➜ **Add** ➜ 選擇 **D1 database**。
   - **Variable name** 輸入：`DB`（⚠️ 大寫 DB）。
   - **D1 database** 選擇剛剛建立的 `ev-charging-db`。
   - 點擊 **Save**。
2. 點選 **Variables and Secrets** ➜ **Add**：
   - **Variable name**：`SYNC_SECRET`
   - **Value**：自訂一組密鑰（例如 `ev_pass_2026` 或任意自訂密碼）。
   - 點擊 **Save**。

### 步驟 4：在 Android App 填入設定
1. 複製您的 Worker 網址（例如 `https://ev-charger-sync.your-subdomain.workers.dev`）。
2. 打開手機上的 **EV Charger Recorder** ➜ 前往 **「設定」** ➜ **「☁️ Cloudflare D1 雲端同步」**：
   - 填入 **Worker 網址**。
   - 填入剛剛設定的 **同步金鑰 (Sync Secret)**。
   - 點擊 **「測試連線」**，顯示綠色「已連線」即可開始同步！

---

## 方案 B：使用 Wrangler CLI 終端指令部署

若您熟悉終端機操作：

```bash
cd cloudflare

# 1. 建立 D1 資料庫
npx wrangler d1 create ev-charging-db

# (將終端輸出的 database_id 填入 wrangler.toml 的 database_id)

# 2. 執行建表
npx wrangler d1 execute ev-charging-db --file=./schema.sql --remote

# 3. 部署 Worker
npx wrangler deploy
```

部署完成後，即可將終端顯示的 Worker URL 與 `SYNC_SECRET` 輸入至 App 設定中。
