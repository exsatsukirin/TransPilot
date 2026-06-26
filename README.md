<div align="center">

# TransPilot ✈️

**AI-powered translation assistant — configurable LLM backend, translation history with favorites, running on Android.**

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
- **Translation History** — Every translation is saved with source/target language and timestamp
- **Favorites** — Star important translations for quick access
- **Search** — Filter through history by keyword
- **Persistent Settings** — Language preferences and API config survive app restarts

## 📸 Screenshots

| Translate | History | Settings |
|-----------|---------|----------|
| Language selection, text input, AI translation | Search, filter favorites, delete entries | API config, custom prompt editing |

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
   - **API Endpoint** — e.g. `https://api.openai.com/v1/chat/completions`
   - **API Key** — your secret key
   - **Model** — e.g. `gpt-4o-mini`, `deepseek-chat`
   - **System Prompt** — customize the translation instruction (use `{source}` and `{target}` as placeholders)
3. Tap **Save**
4. Go back to **Translate** tab, select languages, enter text, and tap **Translate**!

## 🏗️ Architecture

```
TransPilot/
├── app/
│   └── src/main/java/com/example/llmtranslator/
│       ├── MainActivity.kt          # Single-activity entry, bottom nav
│       ├── data/
│       │   ├── ApiConfig.kt          # API config data class
│       │   ├── ApiConfigRepository.kt # DataStore persistence
│       │   ├── TranslationRecord.kt   # Room entity
│       │   ├── TranslationDao.kt      # Room DAO
│       │   └── AppDatabase.kt         # Room database
│       ├── network/
│       │   └── LlmClient.kt          # OkHttp → OpenAI-compatible API
│       └── ui/
│           ├── TranslatorViewModel.kt # AndroidViewModel
│           ├── TranslateScreen.kt     # Translation UI
│           ├── HistoryScreen.kt       # History & favorites UI
│           └── SettingsScreen.kt      # API settings UI
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/wrapper/gradle-wrapper.properties
```

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| State | AndroidViewModel + StateFlow |
| Local Storage | Room (translations) + DataStore (config/preferences) |
| Network | OkHttp → OpenAI Chat Completions API |

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

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
