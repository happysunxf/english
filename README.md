# 🌅 MorningEnglish

> **Android app that auto-plays a Peppa Pig (UK English) episode every morning at 6:30.**
> Built for a kid who just started learning English.

## What's in this repo

```
english/
├── design/MorningEnglish.md     # Product design doc (8 chapters)
└── code/MorningEnglish/         # ← This Android project
    ├── app/                     # Main module
    │   ├── src/main/java/com/morningenglish/app/
    │   │   ├── MorningEnglishApp.kt     # Application entry
    │   │   ├── data/                    # Room DB + repositories
    │   │   ├── scheduler/               # AlarmManager + WorkManager
    │   │   ├── service/                 # Foreground service + boot receiver
    │   │   ├── ui/                      # Compose UI (home/settings/permission/player)
    │   │   └── util/                    # PermissionHelper + RomHelper + VideoSeeder
    │   ├── src/main/AndroidManifest.xml
    │   ├── src/main/res/                # Themes, strings, icons
    │   └── src/main/assets/manifest/    # Peppa Pig catalog (30 episodes)
    ├── scripts/download_assets.py       # yt-dlp batch downloader
    ├── build.gradle.kts
    └── README.md
```

## Architecture (the part that matters)

```
┌─────────────────────────────────────────────────────────────┐
│  Trigger layer (who wakes the app at 6:30)                  │
│  ┌──────────────────┐    ┌──────────────────┐               │
│  │ AlarmManager     │    │ WorkManager      │               │
│  │ setExactAndAllow │    │ Periodic 24h     │               │
│  │ WhileIdle        │    │ fallback check   │               │
│  └────────┬─────────┘    └────────┬─────────┘               │
│           └───────────┬───────────┘                         │
│                       ▼                                     │
│              ┌────────────────────┐                         │
│              │ BootReceiver       │ ← re-registers on reboot│
│              └────────────────────┘                         │
└─────────────────────┬───────────────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  Execution layer                                            │
│  ┌────────────────────┐                                     │
│  │ AlarmReceiver      │ → picks today's video              │
│  └────────┬───────────┘                                     │
│           ▼                                                 │
│  ┌────────────────────┐                                     │
│  │ DailyPlayerService │ ← Foreground service + WakeLock    │
│  └────────┬───────────┘                                     │
│           ▼                                                 │
│  ┌────────────────────┐                                     │
│  │ PlayerActivity     │ ← Fullscreen ExoPlayer             │
│  └────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

## Build & run

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- A real Android device (API 26+, Android 8.0+) — emulator works for UI but alarm behavior is best tested on real hardware

### Build the APK
```bash
cd code/MorningEnglish
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

### Install on device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.morningenglish.app.debug/com.morningenglish.app.ui.MainActivity
```

## Download Peppa Pig episodes

The app expects episodes at `{context.filesDir}/videos/{id}.mp4`. The script downloads from YouTube and saves them as `app/src/main/assets/videos/`. You'll need to push them onto the device manually (a future version will bundle them inside the APK or auto-download on first launch).

### Step 1: install yt-dlp
```bash
pip install --user yt-dlp
export PATH=$HOME/.local/bin:$PATH
yt-dlp --version  # verify
```

### Step 2: download (audio-only is recommended for kids — saves space)
```bash
cd code/MorningEnglish
python3 scripts/download_assets.py --audio-only
```

### Step 3: push to device
```bash
# Pick the device first (will list all)
adb devices

# Push
adb push app/src/main/assets/videos/ /data/local/tmp/videos/
adb shell run-as com.morningenglish.app.debug mkdir -p files/videos
adb shell run-as com.morningenglish.app.debug cp -r /data/local/tmp/videos/* files/videos/
adb shell run-as com.morningenglish.app.debug ls -la files/videos/
```

## Manual testing checklist (real device)

After installing, walk through these scenarios. **Each must pass before considering the app done.**

### Phase 1: Permissions
- [ ] First launch shows permission screen
- [ ] All 4 permission cards visible (notification, exact alarm, battery, ROM-specific)
- [ ] Each "去设置" button opens the correct system settings page
- [ ] After granting all, button enables and navigates to home

### Phase 2: Alarm fires
- [ ] Set alarm time to 2 minutes from now (e.g. 06:30 → 14:28)
- [ ] Lock the screen
- [ ] Wait for alarm → screen turns on, video starts fullscreen
- [ ] Video plays to end → returns to home
- [ ] Play history shows the entry

### Phase 3: Background survival
- [ ] Set alarm, kill the app via Recent Apps swipe-up
- [ ] Wait for alarm → still fires ✓
- [ ] Force-stop via Settings → Apps → MorningEnglish → Force stop
- [ ] Wait for alarm → may NOT fire (Android prevents this; document as expected)

### Phase 4: Reboot survival
- [ ] Set alarm, reboot device
- [ ] After boot completes, verify alarm is re-scheduled:
  ```bash
  adb shell dumpsys alarm | grep morningenglish
  ```
- [ ] Wait for alarm → fires ✓ (only if BootReceiver not blocked by ROM)

### Phase 5: Brand compatibility
Test on at least 3 of these (or borrow a friend's phone):
- [ ] Pixel / stock Android
- [ ] Xiaomi (MIUI)
- [ ] Huawei (EMUI/HarmonyOS)
- [ ] OPPO (ColorOS)
- [ ] vivo (FuntouchOS)
- [ ] Samsung (One UI)

For non-stock ROMs, the auto-start whitelist step is critical. Without it, the alarm won't fire after reboot.

### Phase 6: Real-world (7-day test)
- [ ] Hand device to your kid for 7 days
- [ ] Each morning, verify the alarm fired by checking Play History
- [ ] Collect pain points and iterate

## Known limitations / TODO

- [ ] **In-app asset download**: Currently requires manual `adb push`. A future version should auto-download on first Wi-Fi connect.
- [ ] **No parental control UI**: Kids can exit the player mid-video and re-launch other apps. Acceptable for v0.1, but should add a kiosk-mode option.
- [ ] **Single asset catalog**: Only Peppa Pig. Future: Numberblocks, Bluey, parent's own recordings.
- [ ] **No analytics**: We don't know which episodes get played to end vs abandoned. Adding telemetry would help tune the recommendation algorithm.

## Troubleshooting

### Alarm doesn't fire on time
1. Check logcat for "AlarmScheduler" tag:
   ```bash
   adb logcat -s AlarmScheduler AlarmReceiver DailyPlayerService
   ```
2. Verify battery optimization whitelist
3. Verify ROM auto-start whitelist (Xiaomi/Huawei/etc)
4. Try toggling the "启用每日播放" switch off then on

### Video doesn't play
1. Verify file exists:
   ```bash
   adb shell run-as com.morningenglish.app.debug ls files/videos/
   ```
2. Verify filePath was updated in Room (currently manual — see `data/repo/VideoRepository.updateFilePath`)
3. Check logcat for "PlayerActivity" tag

### Screen stays off
- The WakeLock is held for 10 minutes. After that, screen may turn off. Watch the full 5-minute episode within 10 minutes of alarm fire.

## License & copyright

Peppa Pig is © Astley Baker Davies / Hasbro / Entertainment One.
This app is for personal, non-commercial use only. Do not redistribute the videos.

The source code in this repo is MIT-licensed.

---

*Built with: Kotlin 1.9.24, Jetpack Compose, Media3 ExoPlayer 1.3.1, Room 2.6.1, WorkManager 2.9.0*