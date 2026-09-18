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
`PlaybackService` (ExoPlayer + MediaSession). The session publishes metadata
(title) and system media controls.

### Verify notification / lock screen / headset

1. Sync Gradle, install a debug build, grant **Notifications** if prompted (API 33+).
2. Add a music folder or push a file into `files/library/`, **Rescan**, tap a track.
3. Confirm Library now-playing card shows title and play/pause still works.
4. Pull down the shade (or lock the device): media notification / lock-screen
   controls should show the track title with play/pause.
5. Leave the app (Home or another app): audio should continue; notification
   controls still pause/resume.
6. Optional: Bluetooth headset or wired headset play/pause / pause-on-unplug
   (audio becoming noisy).

Logcat filter: `BopPlayer` (also `BopSync`, `BopPc`, `BopLibrary`).

### Limitations

- Single-track playback (no queue / next-previous playlist yet).
- No playback resumption after process death (no MediaButtonReceiver /
  `onPlaybackResumption` yet).
- Artwork is not set; notification uses the default media style without album art.
- On API 33+, denying notification permission may hide the media notification
  while in-app and headset controls can still work.

## Logging

Logcat filters: `BopSync`, `BopPc`, `BopLibrary`, `BopPlayer`.
