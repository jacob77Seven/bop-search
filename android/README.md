# Bop-Search Android

Package: `com.jacob77.bopsearch`  
minSdk 26 · target/compileSdk 34 · Kotlin + Jetpack Compose + Material 3

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

## Logging

Logcat filters: `BopSync`, `BopPc`, `BopLibrary`, `BopPlayer`.
