# SilentGuard v9 — Women Safety App

> The safest action is the invisible one.

SilentGuard v9 is a stealth-based Android women safety application designed to appear as a normal notes app while secretly operating a full emergency protection system in the background.

It provides silent SOS, journey tracking, signal-loss detection, offline fallback location, and evidence capture without exposing its real purpose.

---

## Features

### Disguise Layer

* Appears in launcher as **My Notes**
* Opens like a regular notes app
* Tap title **5 times** rapidly to unlock hidden dashboard

### Silent SOS

* Trigger: **Volume Down × 3**
* Screen remains off
* Fresh GPS acquired instantly
* SMS sent to emergency contact
* Audio recording starts automatically

### Auto Track

* Captures GPS every defined interval
* Sends periodic SMS updates during travel
* Works offline using SMS

### Network Loss Alert

* Detects signal drop before total loss
* Sends final known coordinates before disconnect

### Cell Tower Locator

Includes:

* MCC
* MNC
* LAC/TAC
* Cell ID
* Operator estimate
* Approximate region

### Journey Tracker

* Destination setup
* Arrival deadline
* Auto SOS if overdue
* Full route log transmission

### Evidence Vault

* Audio recordings
* GPS logs
* Event timeline
* Biometric lock
* Auto delete after 72 hours

---

## Tech Stack

| Item       | Value                    |
| ---------- | ------------------------ |
| Language   | Java                     |
| Framework  | Android SDK              |
| Min SDK    | 26                       |
| Target SDK | 34                       |
| Libraries  | None                     |
| Offline    | Yes                      |
| Storage    | SharedPreferences + JSON |
| SMS        | SmsManager               |
| GPS        | requestLocationUpdates   |

---

## Project Structure

```text
receivers/
 ├── BootReceiver
 └── NetworkReceiver

services/
 ├── GuardService
 └── SosService

ui/
 ├── DisguiseActivity
 ├── MainActivity
 ├── SetupActivity
 ├── JourneyActivity
 └── VaultActivity

utils/
 ├── AppConstants
 ├── AudioRecorder
 ├── AutoTracker
 ├── CellLocator
 ├── EvidenceVault
 ├── LiveLocationManager
 ├── LocationHelper
 ├── NotifHelper
 ├── PrefsManager
 └── SmsHelper
```

---

## SOS Flow

```text
Volume Down × 3
      ↓
GuardService
      ↓
Fresh GPS
      ↓
SMS Alert
      ↓
Audio Recording
      ↓
Evidence Vault
```

---

## Privacy

* No cloud
* No login
* No analytics
* No trackers
* No third-party SDK
* Data stored only on device
* Biometric protected vault

---

## Future Scope

* Wearable trigger
* Hidden voice activation
* Fake shutdown mode
* Emergency beacon
* Vehicle capture
* AI threat prediction

---

## Author

**Samrudh**
## Tech Enthusiast
## Computer Science & Technology Student | Product Builder | Innovator

---

## License

Private Prototype

