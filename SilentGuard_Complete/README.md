# SilentGuard v9 — Women Safety App

A silent, disguised Android safety app for women traveling alone.

## Features
- **Disguise** — Appears as "My Notes" app to anyone who picks up the phone
- **Silent SOS** — Press Volume Down 3x = SMS sent with GPS to emergency contact
- **Auto Track** — GPS captured offline every N minutes during journey, SMS sent at set intervals
- **Live Location** — Uses requestLocationUpdates (not cached) so coordinates actually change
- **Cell Tower Locator** — MCC/MNC/LAC/TAC info + estimated coordinates without internet
- **Network Loss Alert** — Sends location SMS the moment network drops
- **Signal Drop Alert** — Sends coordinates BEFORE signal fully dies (preemptive)
- **Journey Tracker** — Auto SOS if you don't arrive on time
- **Evidence Vault** — Encrypted audio + GPS logs stored locally

## Emergency Contact
Default: (changeable in app from contacts)

## How to Open Real App
Tap "My Notes" title 5 times rapidly → app opens

## Tech Stack
- Language: Java (Android SDK)
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- No third-party libraries — pure Android SDK only
- Fully offline — no internet needed for core features

## Build Instructions
1. Open Android Studio
2. File → Open → select this SilentGuard folder
3. Wait for Gradle sync
4. Build → Build APK(s)
5. Install on device

## File Structure
```
app/src/main/
├── AndroidManifest.xml
├── java/com/silentguard/
│   ├── receivers/
│   │   ├── BootReceiver.java
│   │   └── NetworkReceiver.java
│   ├── services/
│   │   ├── GuardService.java       ← background monitor
│   │   └── SosService.java         ← SOS handler
│   ├── ui/
│   │   ├── DisguiseActivity.java   ← fake notes app
│   │   ├── MainActivity.java       ← real dashboard
│   │   ├── SetupActivity.java      ← permissions
│   │   ├── JourneyActivity.java    ← journey + autotrack
│   │   └── VaultActivity.java      ← evidence logs
│   └── utils/
│       ├── AppConstants.java       ← all constants
│       ├── AudioRecorder.java      ← audio evidence
│       ├── AutoTracker.java        ← GPS capture thread
│       ├── CellLocator.java        ← cell tower MCC/MNC/LAC/TAC
│       ├── EvidenceVault.java      ← JSON log storage
│       ├── LiveLocationManager.java← requestLocationUpdates
│       ├── LocationHelper.java     ← location + message builders
│       ├── NotifHelper.java        ← notification channels
│       ├── PrefsManager.java       ← SharedPreferences
│       └── SmsHelper.java          ← SMS sender
└── res/
    ├── values/
    │   ├── strings.xml
    │   ├── colors.xml
    │   └── themes.xml
    └── drawable/
        ├── ic_shield.xml
        ├── ic_note.xml
        └── ...
```
