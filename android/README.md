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

**Settings** tab: host `bop-pc` (MagicDNS) or LAN/Tailscale IP, port `8765`.  
Android emulator → PC on the same machine: host `10.0.2.2`.

## Local library

Audio is scanned from app-private `files/library/`. Use Device File Explorer to
copy `.mp3` / `.wav` / etc., then tap **Rescan**.

## Logging

Logcat filters: `BopSync`, `BopPc`, `BopLibrary`, `BopPlayer`.
