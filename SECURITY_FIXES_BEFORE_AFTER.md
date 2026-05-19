# SilentGuard Security Fixes: Before & After

## THREAT 1: Exported BootReceiver (CRITICAL)

### ❌ BEFORE
```xml
<receiver android:name=".receivers.BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```
**Risk:** Any app can trigger this receiver.

### ✅ AFTER
```xml
<receiver android:name=".receivers.BootReceiver"
    android:exported="false">  <!-- Fixed -->
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```
**Fix:** Set `android:exported="false"` to prevent external access.

---

## THREAT 2: No Phone Number Validation (SMS Injection)

### ❌ BEFORE (SmsHelper.java)
```java
public static void sendTo(String number, String message) {
    try {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(message);
        if (parts.size() == 1) {
            sms.sendTextMessage(number, null, message, null, null);
        } else {
            sms.sendMultipartTextMessage(number, null, parts, null, null);
        }
        Log.d(TAG, "SMS sent to " + number);  // ❌ Logs phone number
    } catch (Exception e) {
        Log.e(TAG, "SMS failed: " + e.getMessage());
    }
}
```
**Risks:**
- Phone number not validated → SMS injection possible
- Phone number logged in plaintext

### ✅ AFTER
```java
public static void sendTo(String number, String message) {
    try {
        // ✅ Validate phone number
        String safeNumber = PhoneValidator.getSafePhoneNumber(number);
        if (safeNumber == null || safeNumber.isEmpty()) {
            LogHelper.logError(TAG, "Invalid phone number rejected");
            return;
        }

        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(message);
        if (parts.size() == 1) {
            sms.sendTextMessage(safeNumber, null, message, null, null);
        } else {
            sms.sendMultipartTextMessage(safeNumber, null, parts, null, null);
        }
        // ✅ Use safe logging (masks phone number)
        LogHelper.logPhoneSafe(TAG, "SMS sent to " + safeNumber);
    } catch (Exception e) {
        LogHelper.logError(TAG, "SMS failed: " + e.getMessage());
    }
}
```

---

## THREAT 3: Sensitive Location Data in Logs

### ❌ BEFORE (LiveLocationManager.java)
```java
public void onLocationChanged(Location loc) {
    if (isBetter(loc, bestLocation)) {
        bestLocation = loc;
        Log.d(TAG, "GPS update: " + loc.getLatitude()  // ❌ Real coords in log
            + "," + loc.getLongitude()
            + " acc=" + (int)loc.getAccuracy() + "m");
    }
}
```
**Risk:** Real-time coordinates exposed in logcat.

### ✅ AFTER
```java
public void onLocationChanged(Location loc) {
    if (isBetter(loc, bestLocation)) {
        bestLocation = loc;
        // ✅ Use safe logging (masks coordinates)
        LogHelper.logLocationSafe(TAG, loc.getLatitude(), loc.getLongitude());
    }
}
```

**Output Comparison:**
- ❌ BEFORE: `GPS update: 12.9716, 79.1578 acc=10m`
- ✅ AFTER: `GPS update: [MASKED]~12.97,79.16`

---

## THREAT 4: No Rate Limiting on SOS

### ❌ BEFORE (SosService.java)
```java
private void handleSos(final String type, final String dest) {
    vibrate();
    AudioRecorder.startRecording(this);
    final Context ctx = this;
    new Thread(new Runnable() {
        public void run() {
            // ❌ No rate limiting - can spam SMS repeatedly
            PrefsManager prefs = new PrefsManager(ctx);
            String number = prefs.getActiveAlertNumber();
            String msg = LocationHelper.buildSosMessage(ctx, prefs);
            SmsHelper.sendTo(number, msg);
            // ...
        }
    }).start();
}
```
**Risk:** Volume button pressed 3x → SMS sent 3x → possible carrier blocking.

### ✅ AFTER
```java
private void handleSos(final String type, final String dest) {
    vibrate();
    AudioRecorder.startRecording(this);
    final Context ctx = this;
    new Thread(new Runnable() {
        public void run() {
            // ✅ Rate limit to 30 seconds
            if (!RateLimiter.isAllowed("SOS_TRIGGER", 30000)) {
                LogHelper.logWarn(TAG, "SOS rate limited - cooldown active");
                return;  // Prevent spam
            }

            PrefsManager prefs = new PrefsManager(ctx);
            String number = prefs.getActiveAlertNumber();
            String msg = LocationHelper.buildSosMessage(ctx, prefs);
            SmsHelper.sendTo(number, msg);
            // ...
        }
    }).start();
}
```

---

## THREAT 5: No Input Validation on Contacts

### ❌ BEFORE (MainActivity.java)
```java
private void pickContact(Intent data) {
    try {
        Cursor c = getContentResolver().query(data.getData(), ...);
        if (c != null && c.moveToFirst()) {
            String id   = c.getString(0);
            String name = c.getString(1);  // ❌ No validation
            c.close();
            Cursor ph = getContentResolver().query(...);
            if (ph != null && ph.moveToFirst()) {
                String num = ph.getString(0).replaceAll("[\\s\\-()]+", "");
                ph.close();
                prefs.setEmergencyNumber(num);  // ❌ No validation
                prefs.setEmergencyName(name);   // ❌ No validation
                updateUI();
            }
        }
    } catch (Exception e) { ... }
}
```
**Risks:**
- Invalid phone numbers stored
- Injection attacks via contact names
- No feedback if contact invalid

### ✅ AFTER
```java
private void pickContact(Intent data) {
    try {
        Cursor c = getContentResolver().query(data.getData(), ...);
        if (c != null && c.moveToFirst()) {
            String id   = c.getString(0);
            String name = c.getString(1);
            c.close();
            Cursor ph = getContentResolver().query(...);
            if (ph != null && ph.moveToFirst()) {
                String num = ph.getString(0).replaceAll("[\\s\\-()]+", "");
                ph.close();

                // ✅ Validate and sanitize inputs
                String safeNum = PhoneValidator.getSafePhoneNumber(num);
                String safeName = PhoneValidator.sanitizeContactName(name);

                if (safeNum == null) {
                    Toast.makeText(this, "Invalid phone number",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                prefs.setEmergencyNumber(safeNum);
                prefs.setEmergencyName(safeName);
                updateUI();
                Toast.makeText(this, "Contact set: " + safeName,
                    Toast.LENGTH_SHORT).show();
            }
        }
    } catch (Exception e) { ... }
}
```

---

## THREAT 6: Plaintext Data Storage (Requires AndroidX Security)

### ❌ BEFORE (PrefsManager.java)
```java
public class PrefsManager {
    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        // ❌ Plaintext storage - anyone can read
        prefs = context.getSharedPreferences(
            AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getEmergencyNumber() {
        return prefs.getString(
            AppConstants.PREF_EMERGENCY_NUMBER,
            AppConstants.EMERGENCY_NUMBER);  // ❌ Exposed
    }
    // ...
}
```

### ✅ AFTER (Recommended)
```java
public class PrefsManager {
    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        try {
            // ✅ Use encrypted SharedPreferences
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            prefs = EncryptedSharedPreferences.create(
                context,
                "encrypted_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getEmergencyNumber() {
        return prefs.getString(
            AppConstants.PREF_EMERGENCY_NUMBER,
            AppConstants.EMERGENCY_NUMBER);  // ✅ Encrypted
    }
    // ...
}
```

**Changes needed:**
1. Add to `build.gradle`:
   ```gradle
   implementation "androidx.security:security-crypto:1.1.0-alpha06"
   ```

2. Add permission to `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.USE_KEYSTORE" />
   ```

---

## THREAT 7: Unencrypted Audio Evidence

### ❌ BEFORE (AudioRecorder.java)
```java
public static void startRecording(Context context) {
    if (recording) return;
    try {
        File dir = getVaultDir(context);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", 
            Locale.getDefault()).format(new Date());
        File file = new File(dir, "evidence_" + ts + ".3gp");
        // ❌ Audio stored as plaintext file
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(file.getAbsolutePath());
        // ...
        recorder.start();
        recording = true;
    } catch (Exception e) { ... }
}
```
**Risk:** Audio files can be read and listened to by any app.

### ✅ AFTER (Recommended)
```java
public static void startRecording(Context context) {
    if (recording) return;
    try {
        File dir = getVaultDir(context);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", 
            Locale.getDefault()).format(new Date());
        
        // Record to temporary file first
        File tempFile = new File(dir, "evidence_" + ts + ".3gp.tmp");
        File encryptedFile = new File(dir, "evidence_" + ts + ".3gp.enc");
        
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(tempFile.getAbsolutePath());
        // ... rest of setup ...
        recorder.start();
        recording = true;
        
        // Store encrypted file reference for later encryption
        PENDING_ENCRYPTION_FILE = tempFile;
        FINAL_ENCRYPTED_FILE = encryptedFile;
    } catch (Exception e) { ... }
}

public static void stopRecording() {
    try {
        if (recorder != null) { 
            recorder.stop(); 
            recorder.release(); 
            recorder = null; 
        }
        recording = false;
        
        // ✅ Encrypt after recording finishes
        if (PENDING_ENCRYPTION_FILE != null && PENDING_ENCRYPTION_FILE.exists()) {
            SecretKey key = SecurityUtil.generateKey();
            if (SecurityUtil.encryptFile(PENDING_ENCRYPTION_FILE, 
                    FINAL_ENCRYPTED_FILE, key)) {
                PENDING_ENCRYPTION_FILE.delete();  // Delete plaintext
                Log.d(TAG, "Audio encrypted: " + FINAL_ENCRYPTED_FILE);
            }
        }
    } catch (Exception e) { ... }
}
```

---

## THREAT 8: Plaintext Evidence Vault JSON

### ❌ BEFORE (EvidenceVault.java)
```java
public static void log(Context context, String type, Location loc, String note) {
    try {
        JSONArray arr = readAll(context);
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("timestamp", System.currentTimeMillis());
        obj.put("note", note);
        if (loc != null) {
            obj.put("lat", loc.getLatitude());   // ❌ Plaintext coords
            obj.put("lng", loc.getLongitude());
            obj.put("link", LocationHelper.buildMapsLink(loc));
        }
        JSONArray newArr = new JSONArray();
        newArr.put(obj);
        for (int i = 0; i < Math.min(arr.length(), 499); i++) 
            newArr.put(arr.get(i));
        
        getLogFile(context).getParentFile().mkdirs();
        java.io.FileWriter fw = new java.io.FileWriter(
            getLogFile(context));
        fw.write(newArr.toString());  // ❌ Plaintext storage
        fw.close();
    } catch (Exception e) { ... }
}
```

### ✅ AFTER (Recommended)
```java
public static void log(Context context, String type, Location loc, String note) {
    try {
        JSONArray arr = readAll(context);
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("timestamp", System.currentTimeMillis());
        obj.put("note", note);
        if (loc != null) {
            obj.put("lat", loc.getLatitude());
            obj.put("lng", loc.getLongitude());
            obj.put("link", LocationHelper.buildMapsLink(loc));
        }
        JSONArray newArr = new JSONArray();
        newArr.put(obj);
        for (int i = 0; i < Math.min(arr.length(), 499); i++) 
            newArr.put(arr.get(i));
        
        String jsonString = newArr.toString();
        
        // ✅ Encrypt before writing to disk
        SecretKey key = getOrCreateEncryptionKey(context);
        String encrypted = SecurityUtil.encryptText(jsonString, key);
        
        getLogFile(context).getParentFile().mkdirs();
        java.io.FileWriter fw = new java.io.FileWriter(
            getLogFile(context));
        fw.write(encrypted);  // ✅ Encrypted storage
        fw.close();
    } catch (Exception e) { ... }
}

// Decrypt when reading
public static JSONArray readAll(Context context) {
    try {
        File f = getLogFile(context);
        if (!f.exists()) return new JSONArray();
        
        java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.FileReader(f));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) 
            sb.append(line);
        br.close();
        
        // ✅ Decrypt before parsing
        SecretKey key = getOrCreateEncryptionKey(context);
        String decrypted = SecurityUtil.decryptText(sb.toString(), key);
        return new JSONArray(decrypted);
    } catch (Exception e) { 
        return new JSONArray(); 
    }
}
```

---

## SUMMARY OF FIXES

| Threat | Severity | Fix | File(s) |
|--------|----------|-----|---------|
| Exported Receiver | 🔴 CRITICAL | Set android:exported="false" | AndroidManifest.xml |
| SMS Injection | 🔴 HIGH | PhoneValidator | SmsHelper.java |
| Location in Logs | 🔴 HIGH | LogHelper masking | LiveLocationManager.java |
| SOS Spam | 🟡 MEDIUM | RateLimiter | SosService.java |
| No Contact Validation | 🟡 MEDIUM | PhoneValidator | MainActivity.java |
| Plaintext Prefs | 🔴 HIGH | EncryptedSharedPreferences | PrefsManager.java (TODO) |
| Plaintext Audio | 🔴 HIGH | SecurityUtil encryption | AudioRecorder.java (TODO) |
| Plaintext JSON | 🔴 HIGH | SecurityUtil encryption | EvidenceVault.java (TODO) |

---

## IMPLEMENTATION PRIORITY

1. **Immediate** (All done)
   - ✅ Fix exported receiver
   - ✅ Phone number validation
   - ✅ Safe logging
   - ✅ Rate limiting

2. **Short-term** (TODO)
   - [ ] Encrypt SharedPreferences
   - [ ] Encrypt audio files
   - [ ] Encrypt evidence vault

3. **Long-term**
   - [ ] Biometric authentication
   - [ ] Certificate pinning
   - [ ] Secure key derivation

---
