# Vidy TV Extension

An on-device **Stremio-compatible Addon and TV Player** for Android TV devices that streams via the [Vidy Embed Player](https://vidy.st/).

---

## 🌟 Features

- **Inbuilt Server Selection**: Instant server failover with multiple worldwide streaming nodes (Miami, Boise, Nashville, Chicago, Vegas, Reno, Oakland, Seattle).
- **Multi-Quality Support**: Adaptive streaming in 4K UHD, 1080p Full HD, 720p, and 480p.
- **Audio & Subtitles**: Multi-language audio tracks and OpenSubtitles selection directly on-screen.
- **100% On-Device / Zero Middle Server**: Runs an embedded lightweight HTTP server locally (`127.0.0.1:8910`) on the TV. No Cloudflare proxy or external server hosting required.
- **Full TV Remote & D-Pad Support**: Injected D-Pad navigation bridge provides smooth focus highlights, play/pause toggle, and ±10s seek with TV remote keys.
- **Automatic IMDb to TMDB Resolution**: Built-in on-device metadata resolver translating IMDb IDs (`tt...`) into TMDB IDs.

---

## 🚀 How It Works

1. **Local Addon Server**: Listens on `http://127.0.0.1:8910` and serves the standard Stremio Addon v3 protocol:
   - Manifest: `GET /manifest.json`
   - Stream Resolver: `GET /stream/{type}/{id}.json`
2. **Leanback Player**: Launches `vidy://play?type={type}&tmdb={tmdbId}` into a hardware-accelerated TV `WebView` optimized for Android TV displays.
3. **D-Pad Bridge**: Bridges TV remote key events (Up/Down/Left/Right/OK/Back) into the player interface for remote control.

---

## 🎮 TV Remote Navigation

| Remote Button | Action in Player |
|---|---|
| **D-Pad Left / Right** | Seek backward (-10s) / forward (+10s) |
| **D-Pad Center (OK)** | Toggle Play / Pause or click focused menu item |
| **D-Pad Up / Down** | Navigate player controls, servers, quality, audio & subtitles |
| **Media Play / Pause** | Toggle playback |
| **Back Button** | Dismiss open menus / exit player |

---

## 📦 Stremio Addon Manifest

Once the application is running on your Android TV device:
```
http://127.0.0.1:8910/manifest.json
```
Add this URL to any compatible TV player or addon manager on your device.

---

## 🛠️ Building from Source

### Prerequisites
- Android SDK 34
- JDK 17+
- Gradle (wrapper included)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be at:
```
build/outputs/apk/debug/frametune-extension-vidy-debug.apk
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Install to Android TV Device via ADB
```bash
adb connect <tv-ip>:5555
adb install -r build/outputs/apk/debug/frametune-extension-vidy-debug.apk
adb shell am start -n com.frametune.extension.vidy/.ui.ExtensionSetupActivity
```

---

## 📄 License
MIT License
