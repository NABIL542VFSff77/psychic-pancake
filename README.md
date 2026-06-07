# Dynamic Island for Android — Spotify Edition

A floating overlay that mimics Apple's Dynamic Island, showing Spotify's currently playing track with a smooth wave animation — on top of any app, all the time.

---

## How to Build & Install

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android device / emulator running **Android 8.0 (API 26)+**
- Spotify installed on the same device

### Steps

1. **Open in Android Studio**
   - File → Open → select the `android-dynamic-island` folder

2. **Sync Gradle**
   - Android Studio will prompt you — click "Sync Now"

3. **Run the app**
   - Connect your device (USB debugging on) or use an emulator
   - Click the ▶ Run button

4. **Grant permissions (the app walks you through these)**

   | Permission | Why |
   |---|---|
   | Draw over other apps | Lets the island float above all other apps |
   | Notification Access | Reads Spotify's now-playing notification |

   Both are required. The app guides you through each one.

5. **Play music on Spotify**
   - Open Spotify, start a song, then press Home or switch to another app
   - The Dynamic Island will smoothly pop up at the top of the screen

---

## Features

| Feature | Detail |
|---|---|
| Smooth pop-in | Expands from center outward with overshoot spring |
| Smooth pop-out | Shrinks to center and fades when music stops |
| Album art | Rounded square album art on the left |
| Song + Artist | Scrolling marquee text in the center |
| Waveform animation | 5 animated bars on the right side |
| Screen off/on | Island disappears on screen off, reappears on screen on |
| Draggable | Drag the island anywhere on screen |
| Survives reboots | Auto-starts via boot receiver |
| Settings screen | Adjust width, height, vertical position |

---

## Customising

### In-app Settings
Open the app → Settings screen lets you:
- **Width** — 100dp to 340dp
- **Height** — 28dp to 56dp
- **Top offset** — vertical position from the top

### Dragging
Press and hold the island while it's showing to drag it anywhere on screen. Position is saved automatically.

---

## How it Works

```
Spotify plays music
       ↓
SpotifyNotificationListener detects the notification
       ↓
Sends intent to FloatingMusicService
       ↓
FloatingMusicService creates a WindowManager overlay
       ↓
DynamicIslandView animates in with spring physics
WaveformView draws animated bars via Canvas
       ↓
Music stops → island animates out and removes itself
```

### Key files

| File | Purpose |
|---|---|
| `SpotifyNotificationListener.kt` | Reads Spotify's media notification |
| `FloatingMusicService.kt` | Foreground service + WindowManager overlay lifecycle |
| `DynamicIslandView.kt` | The island view — layout, animations, drag |
| `WaveformView.kt` | Canvas-drawn animated waveform |
| `SettingsActivity.kt` | In-app settings UI |
| `BootReceiver.kt` | Auto-start on reboot |

---

## Troubleshooting

**Island doesn't appear**
- Make sure both permissions are granted (overlay + notification access)
- Make sure Spotify shows a media notification in the status bar when playing
- Check Settings screen shows "Service: Running"

**Island appears but no album art**
- Some Spotify versions don't include art in the notification — this is normal. The music note icon shows instead.

**Island doesn't disappear when I stop music**
- This happens if Spotify keeps its notification alive after pausing. Try pressing stop (not just pause) in Spotify.

**Crashes on launch**
- Make sure you're running Android 8.0+ (API 26+)
- Check `minSdk 26` in `app/build.gradle`

---

## Adding More Apps

To detect media from other apps (YouTube Music, Apple Music, etc.), add their package names to `SpotifyNotificationListener.kt`:

```kotlin
private val SPOTIFY_PACKAGES = listOf(
    "com.spotify.music",
    "com.spotify.lite",
    "com.google.android.apps.youtube.music",   // YouTube Music
    "com.apple.android.music"                  // Apple Music
)
```
