<div align="center">

# TransPilot ✈️

**AI-powered translation assistant — configurable LLM backend, encrypted API key storage, translation history with pagination & favorites, floating translate overlay, running on Android.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-BOM_2024.12-4285F4?logo=jetpackcompose)

</div>

---

## ✨ Features

- **LLM-Powered Translation** — Translate text using any OpenAI-compatible API (OpenAI, DeepSeek, local LLMs via Ollama, etc.)
- **Auto Language Detection** — Let the LLM figure out the source language automatically
- **Configurable Backend** — Custom API endpoint, API key, model name, and system prompt — all editable in-app
- **Encrypted API Key Storage** — API keys are encrypted via Android KeyStore (AES-256 GCM) before persisting to DataStore
- **Automatic Retry** — Exponential backoff retry (2 attempts) on network errors, 5xx, and rate limits
- **Translation History with Pagination** — History list uses Paging 3; loads 30 records at a time, smooth infinite scroll (no lag even with thousands of records)
- **Favorites** — Star important translations for quick access
- **Search** — Filter through history by keyword
- **Floating Translate Overlay** — Select text in any other app → tap "TransPilot 翻译" in the context menu → floating dialog shows translation
- **Persistent Settings** — Language preferences and API config survive app restarts

## 📸 Screenshots

| Translate | History | Settings | Floating Overlay |
|-----------|---------|----------|------------------|
| Language selection, text input, AI translation | Paginated list, expand/collapse, favorites, search, ripple fix | API config, encrypted key storage, theme mode | Text selection in any app → TransPilot |

## 🚀 Quick Start

### Prerequisites

- Android device or emulator (API 24+)
- An API key for your LLM provider

### Install

Download the latest APK from [Releases](https://github.com/exsatsukirin/TransPilot/releases) or build from source:

```bash
git clone https://github.com/exsatsukirin/TransPilot.git
cd TransPilot
export _JAVA_OPTIONS="-Dhttps.protocols=TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1.2 -Djdk.tls.disabledAlgorithms=SSLv3,TLSv1,TLSv1.1,DTLSv1.0,RC4,DES,MD5withRSA,3DES_EDE_CBC,anon,NULL"
export ANDROID_HOME=/path/to/your/android/sdk
gradle assembleDebug --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> **Note:** The `_JAVA_OPTIONS` workaround is needed for Java 21+ due to TLS cipher suite changes. See [Troubleshooting](#troubleshooting).

### Setup

1. Open TransPilot → tap **Settings** (gear icon)
2. Enter your API information:
   - **API Endpoint** — e.g. `https://api.deepseek.com/v1/chat/completions`
   - **API Key** — your secret key (automatically encrypted on save)
   - **Model** — e.g. `deepseek-chat`, `gpt-4o-mini`
   - **System Prompt** — customize the translation instruction (use `{source}` and `{target}` as placeholders)
3. Tap **Save**
4. Go back to **Translate** tab, select languages, enter text, and tap **Translate**!

### Quick Translate from Any App

1. Select text in any app (browser, notes, etc.)
2. In the context menu that appears, scroll and tap **TransPilot 翻译**
3. A floating dialog shows the translation with a **copy** button

## 🏗️ Architecture

```
TransPilot/
├── app/
│   └── src/main/java/com/exsatsukirin/transpilot/
│       ├── MainActivity.kt              # Single-activity entry, bottom nav
│       ├── data/
│       │   ├── ApiConfig.kt             # API config data class
│       │   ├── ApiConfigRepository.kt   # DataStore persistence (encrypted key)
│       │   ├── EncryptedKeyStore.kt     # Android KeyStore AES-256 GCM encryption
│       │   ├── TranslationRecord.kt     # Room entity
│       │   ├── TranslationDao.kt        # Room DAO (with PagingSource)
│       │   └── AppDatabase.kt           # Room database
│       ├── network/
│       │   └── LlmClient.kt            # OkHttp → OpenAI-compatible API (with retry)
│       └── ui/
│           ├── TranslatorViewModel.kt   # AndroidViewModel
│           ├── TranslateScreen.kt       # Translation UI
│           ├── HistoryScreen.kt         # Paginated history & favorites UI
│           ├── SettingsScreen.kt        # API settings & theme UI
│           └── TranslateOverlayActivity.kt  # Floating translate (PROCESS_TEXT)
├── build.gradle.kts
├── settings.gradle.kts
└── MAINTENANCE.md                       # Developer maintenance guide
```

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 (Dynamic Color) |
| State | AndroidViewModel + StateFlow (Eagerly / WhileSubscribed) |
| Navigation | Bottom navigation — 3 tabs (Translate / History / Settings) |
| Local Storage (translations) | Room + Paging 3 (page size: 30) |
| Local Storage (config) | DataStore Preferences |
| Key Encryption | Android KeyStore (AES-256 / GCM / NoPadding) |
| Network | OkHttp → OpenAI Chat Completions API (2x retry, exponential backoff) |
| Floating Translate | `Intent.ACTION_PROCESS_TEXT` + `Theme.Translucent` Activity |

## 🔧 Troubleshooting

### TLS Errors on Java 21+

Java 21+ disables `TLS_RSA_*` cipher suites by default, which can cause Gradle to fail downloading dependencies. Set this environment variable before building:

```bash
export _JAVA_OPTIONS="-Dhttps.protocols=TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1.2 -Djdk.tls.disabledAlgorithms=SSLv3,TLSv1,TLSv1.1,DTLSv1.0,RC4,DES,MD5withRSA,3DES_EDE_CBC,anon,NULL"
```

### Common API Errors

| Error | Likely Cause | Fix |
|-------|-------------|-----|
| 401 / `invalid_request_error` | Wrong API key or extra whitespace | Re-enter API key, ensure no leading/trailing spaces |
| 404 | Wrong endpoint URL | Check the API endpoint matches your provider |
| Model not found | Wrong model name | Verify the model name for your provider |
| Repeated failures | Network intermittent | 2x retry with backoff built in — check connection |

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
