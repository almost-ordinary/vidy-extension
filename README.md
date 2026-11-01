# FrameTune TV — Vidy Extension (`:extension-vidy`)

Standalone on-device extension providing direct streaming integration with **Vidy Embed Player** (`https://vidy.st/`) for **FrameTune TV**.

---

## 🔒 Isolation & Safety Rules

- **Git Isolation**: This directory is an independent local Git repository (`extensions/vidy/.git`). It is never pushed to the FrameTune TV remote repo.
- **Zero Middle Server**: Runs 100% on-device on Android TV via localhost loopback (`127.0.0.1:8910`). No Cloudflare or external proxy required.
- **Decoupled Architecture**: FrameTune TV (`:app`) has no scrapers built-in; it communicates with this extension via the Stremio Addon v3 protocol.

---

## ⚙️ Architecture & Technical Specifications

| Parameter | Value |
|---|---|
| **Package ID** | `com.frametune.extension.vidy` |
| **Min SDK / Compile SDK** | 21 / 34 |
| **Local Loopback Port** | `8910` (`http://127.0.0.1:8910/manifest.json`) |
| **Stream Endpoint** | `http://127.0.0.1:8910/stream/{type}/{id}.json` |
| **Intent Scheme** | `vidy://play?type={type}&tmdb={tmdbId}&season={s}&episode={e}` |
| **Player Activity** | `com.frametune.extension.vidy.player.VidyPlayerActivity` |
| **Setup Activity** | `com.frametune.extension.vidy.ui.ExtensionSetupActivity` |
| **Background Service** | `com.frametune.extension.vidy.service.LocalAddonService` |
| **Resolver** | `com.frametune.extension.vidy.resolver.ImdbToTmdbResolver` (Cinemeta on-device API) |

---

## 🎮 Navigation & Remote Controls

Controlled by TV D-Pad or companion mobile remote (`com.frametune.remote`):
- **D-Pad Left / Right**: ±10 seconds seek in Vidy player.
- **D-Pad Center (OK)**: Toggles play/pause or activates focused menu item (server, quality, audio track, subtitles).
- **D-Pad Up / Down**: Focuses through Vidy player controls with red highlight (`#E50914`).
- **Media Play / Pause**: Instant toggle via HTML5 video element bridge.
- **Back Button**: Dismisses popups and exits cleanly back to FrameTune TV launcher.

---

## 🛠️ Build & Verification Commands

```powershell
# From extensions/vidy directory:
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug

# Install on TV:
adb -s <tv_ip>:5555 install -r build/outputs/apk/debug/frametune-extension-vidy-debug.apk

# Launch:
adb -s <tv_ip>:5555 shell am start -n com.frametune.extension.vidy/.ui.ExtensionSetupActivity
```

