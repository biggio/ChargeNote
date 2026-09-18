# ⚡ EV Charger Recorder (EV 智慧充電能耗記錄器)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Cloudflare D1](https://img.shields.io/badge/Cloud-Cloudflare%20D1-orange.svg)](https://developers.cloudflare.com/d1/)
[![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini-blueviolet.svg)](https://ai.google.dev/)
[![Version](https://img.shields.io/badge/Version-v1.5.0%20(Build%207)-brightgreen.svg)](https://github.com/)

一款專為電動車（EV）車主打造的現代化智慧充電與電耗記錄 Android 應用程式。採用 **Jetpack Compose + Material Design 3** 全新開發，結合 **Google Gemini 視覺 AI 拍照辨識**、**離線優先（Offline-First）架構** 以及 **Cloudflare D1 全球分散式雲端雙向同步**。

---

## ✨ 核心特色

### 📊 1. 即時電耗與能效儀表板 (Dashboard)
- **智慧能效分析**：即時計算動態電耗（`km/kWh`）、行駛總里程與累計電量。
- **費用精打細算**：即時統計充電總支出、平均每度電成本（`NT$/kWh`）及行駛每公里成本。
- **充電規格分佈**：視覺化圓餅圖呈現快充（DC）與慢充/家充（AC）比例。
- **自訂車輛參數**：可彈性設定可用電池容量（kWh）、官方標定里程與認證標準（WLTP / NEDC / EPA）。

### 🤖 2. Google Gemini 視覺 AI 自動填單
- **秒級拍照辨識**：支援相機即時拍照或從相簿選擇充電樁螢幕、發票或收據截圖。
- **自動提取關鍵資料**：AI 自動精準抓取充電度數（kWh）、總費用金額、充電營運商、站點名稱與 AC/DC 規格。
- **純個人安全金鑰架構 (BYOK)**：不內建任何開發者 API Key，完全由使用者自行至 Google AI Studio 免費申請並在 App 中配置，隱私與額度自主掌控。

### ☁️ 3. 離線優先 + Cloudflare D1 雲端雙向同步 (Offline-First)
- **離線可用無阻**：本地採用 Android 原生 **Room SQLite**，即使在地下停車場或無網路環境也能流暢記帳。
- **Cloudflare D1 雙向增量同步**：連網時自動在背景將異動非同步推送到免費全球邊緣資料庫 Cloudflare D1。
- **跨裝置與雲端還原**：支援多台手機/平板雙向同步，或換機時一鍵完整還原歷史充電紀錄。
- **一鍵式後端部署**：專案內含完整的 [`cloudflare/`](cloudflare/) 目錄（含 `schema.sql`、`worker.js`、`wrangler.toml` 與 3 分鐘教學）。

### 🏷️ 4. 充電營運商自訂與標籤管理
- **內建台灣主流品牌**：預設收錄家用充電、U-POWER、特爾電力、EVOASIS、Tesla 超充、華城 EVALUE、星舟快充等。
- **靈活維護**：在「設定」中可隨時新增自訂營運商標籤或移除不常用站點。
- **記帳自適應學習**：記帳時若輸入「其他」新品牌，系統會在儲存時自動收錄至營運商清單。

### 💾 5. 資料備份與本機儲存 (SAF)
- **原生 Storage Access Framework**：支援直接將 JSON 或 CSV 匯出檔案儲存至手機本機指定資料夾。
- **資料無損遷移**：支援 JSON 備份檔匯入與合併還原。

---

## 🏗️ 系統架構與技術棧

- **開發語言**：Kotlin 2.0+
- **UI 框架**：Jetpack Compose + Material Design 3
- **架構設計**：Modern Android Architecture (MVVM + Repository Pattern + Kotlin Coroutines & Flow)
- **本機資料庫**：Android Jetpack Room 2.6 (SQLite)
- **雲端資料庫**：Cloudflare D1 (Serverless Distributed SQLite)
- **邊緣後端**：Cloudflare Workers (JavaScript, REST API)
- **AI 整合**：Google Gemini 2.5 / 1.5 Flash Multimodal API (REST + OkHttp)
- **網路通訊**：OkHttp 4.12
- **最低支援版本**：Android 7.0 (API Level 24)+

---

## 🚀 快速開始

### 1. 取得原始碼
```bash
git clone git@github.com:biggio/EV-Charger-Recorder.git
cd EV-Charger-Recorder
```

### 2. 環境需求
- Android Studio Ladybug | Koala 或更新版本
- JDK 21 (推薦 Android Studio 內建 JBR)
- Android SDK 36 (Android 15+)

### 3. 編譯與安裝 Debug APK
```bash
./gradlew assembleDebug
```
編譯完成的 APK 位於：`app/build/outputs/apk/debug/app-debug.apk`

---

## ☁️ 部署專屬 Cloudflare D1 雲端同步

請參考專案內的 [`cloudflare/README.md`](cloudflare/README.md) 進行建置：
1. 登入 [Cloudflare Dashboard](https://dash.cloudflare.com/) 建立名為 `ev_charger_db` 的 D1 資料庫。
2. 執行 [`cloudflare/schema.sql`](cloudflare/schema.sql) 建立資料表。
3. 部署 [`cloudflare/worker.js`](cloudflare/worker.js) 並綁定 D1 資料庫與自訂金鑰 `SYNC_SECRET`。
4. 在 App「設定」頁面輸入 Worker 網址與金鑰，即可開始使用雲端自動同步！

---

## 📄 開源授權

本專案採用 [MIT License](LICENSE) 授權釋出。
