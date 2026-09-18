# Bop-Search Android

Package: `com.jacob77.bopsearch`  
minSdk 26 · target/compileSdk 34 · Kotlin + Jetpack Compose + Material 3  
Playback: Jetpack Media3 ExoPlayer + `MediaSessionService` (notification / lock screen / BT)

## Open in Android Studio

1. **File → Open** → select this `android/` folder (Gradle project root).
2. Trust the project and wait for Gradle sync.
3. Run on emulator or device.

If the Gradle wrapper JAR is missing, Android Studio will offer to generate it,
or use *File → New → Import Project*.

## Configure peer

**Settings** tab: host `powerspec` (MagicDNS) or LAN/Tailscale IP, port `8765`.  
Android emulator → PC on the same machine: host `10.0.2.2`.

## Local library

Audio is scanned from:

1. App-private `files/library/` (push files via Device File Explorer, then **Rescan**).
2. **Music folders** you add with **Add music folder** (Storage Access Framework /
   Open Document Tree). Pick a directory on the phone; Bop-Search keeps a
   persistable read URI and rescans it recursively for `.mp3` / `.m4a` / `.aac` /
   `.wav` / `.ogg` / `.flac` / `.opus`. Remove a folder anytime from the Library
   list (releases the persisted permission).

If the system later revokes SAF access, remove and re-add the folder. Very large
trees may take a few seconds to scan.

## Playback (Media3)

Library taps start playback through `LocalPlayer` → `MediaController` →
`PlaybackService` (ExoPlayer + MediaSession). The session uses
`DefaultMediaNotificationProvider` (channel **Now playing** / `bop_search_media`).

On **Android 13+ (API 33+)**, the app requests **POST_NOTIFICATIONS** at launch and
again before the first play. If you deny and the system greys out Notifications,
use **Settings → Notification settings** in-app, or:

- **App info → Notifications → Allow** (enable the **Now playing** channel)
- OEM paths: Samsung *Notifications*, Pixel *App notifications*, Xiaomi *App permissions → Notifications*

### Verify system media controls

1. Install/rebuild. When prompted, tap **Allow** for notifications.
2. Library → play a track (in-app now-playing should work).
3. Pull down the shade / lock screen: **Now playing** with title + play/pause.
4. Leave the app — audio continues; notification still controls it.
5. Optional: BT/headset buttons; unplug headphones should pause.

If audio plays but the shade is empty / Notifications is greyed: open in-app
**Notification settings** (or App info → Notifications) and allow Bop-Search.

### Limitations

- Single-track playback (no queue / next-previous playlist yet).
- Artwork is not set; notification uses the default media style without album art.

## Logging

Logcat filters: `BopSync`, `BopPc`, `BopLibrary`, `BopPlayer`.

## Verify system media controls (Media3)

1. Install/rebuild, **Allow notifications** when prompted (Android 13+).
2. Library → play a track (in-app now-playing should work).
3. Pull down the shade / check lock screen: **Bop-Search / Now playing** media notification with title + play/pause.
4. Leave the app — audio continues; notification still controls it.
5. Optional: BT/headset buttons; unplug headphones should pause.

If the shade is empty but audio plays: open App info → Notifications and enable the **Now playing** channel.
