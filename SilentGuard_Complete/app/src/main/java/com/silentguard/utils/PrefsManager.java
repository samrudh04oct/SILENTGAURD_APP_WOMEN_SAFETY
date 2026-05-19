package com.silentguard.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Setup
    public boolean isSetupDone()           { return prefs.getBoolean(AppConstants.PREF_SETUP_DONE, false); }
    public void    setSetupDone(boolean v) { prefs.edit().putBoolean(AppConstants.PREF_SETUP_DONE, v).apply(); }

    // Guard
    public boolean isGuardActive()           { return prefs.getBoolean(AppConstants.PREF_GUARD_ACTIVE, false); }
    public void    setGuardActive(boolean v) { prefs.edit().putBoolean(AppConstants.PREF_GUARD_ACTIVE, v).apply(); }

    // Journey
    public boolean isJourneyActive()           { return prefs.getBoolean(AppConstants.PREF_JOURNEY_ACTIVE, false); }
    public void    setJourneyActive(boolean v) { prefs.edit().putBoolean(AppConstants.PREF_JOURNEY_ACTIVE, v).apply(); }

    public long    getJourneyEta()         { return prefs.getLong(AppConstants.PREF_JOURNEY_ETA, 0L); }
    public void    setJourneyEta(long v)   { prefs.edit().putLong(AppConstants.PREF_JOURNEY_ETA, v).apply(); }

    public String  getJourneyDest()        { return prefs.getString(AppConstants.PREF_JOURNEY_DEST, ""); }
    public void    setJourneyDest(String v){ prefs.edit().putString(AppConstants.PREF_JOURNEY_DEST, v).apply(); }

    // Journey-specific contact
    public String  getJourneyContactNumber() {
        return prefs.getString(AppConstants.PREF_JOURNEY_CONTACT_NUMBER, "");
    }
    public void    setJourneyContactNumber(String v) {
        prefs.edit().putString(AppConstants.PREF_JOURNEY_CONTACT_NUMBER, v).apply();
    }
    public String  getJourneyContactName() {
        return prefs.getString(AppConstants.PREF_JOURNEY_CONTACT_NAME, "");
    }
    public void    setJourneyContactName(String v) {
        prefs.edit().putString(AppConstants.PREF_JOURNEY_CONTACT_NAME, v).apply();
    }

    // Default emergency contact
    public String  getEmergencyNumber() {
        return prefs.getString(AppConstants.PREF_EMERGENCY_NUMBER, AppConstants.EMERGENCY_NUMBER);
    }
    public void    setEmergencyNumber(String v) {
        prefs.edit().putString(AppConstants.PREF_EMERGENCY_NUMBER, v).apply();
    }
    public String  getEmergencyName() {
        return prefs.getString(AppConstants.PREF_EMERGENCY_NAME, AppConstants.EMERGENCY_NAME);
    }
    public void    setEmergencyName(String v) {
        prefs.edit().putString(AppConstants.PREF_EMERGENCY_NAME, v).apply();
    }

    // Network alert
    public boolean isNetworkAlertEnabled()           { return prefs.getBoolean(AppConstants.PREF_NETWORK_ALERT, true); }
    public void    setNetworkAlertEnabled(boolean v) { prefs.edit().putBoolean(AppConstants.PREF_NETWORK_ALERT, v).apply(); }

    // Last known location (saved continuously by GuardService)
    public void    saveLastLocation(double lat, double lng) {
        prefs.edit()
            .putString(AppConstants.PREF_LAST_LAT, String.valueOf(lat))
            .putString(AppConstants.PREF_LAST_LNG, String.valueOf(lng))
            .putLong(AppConstants.PREF_LAST_LOCATION_TIME, System.currentTimeMillis())
            .apply();
    }
    public double  getLastLat() {
        try { return Double.parseDouble(prefs.getString(AppConstants.PREF_LAST_LAT, "0")); }
        catch (Exception e) { return 0; }
    }
    public double  getLastLng() {
        try { return Double.parseDouble(prefs.getString(AppConstants.PREF_LAST_LNG, "0")); }
        catch (Exception e) { return 0; }
    }
    public long    getLastLocationTime() { return prefs.getLong(AppConstants.PREF_LAST_LOCATION_TIME, 0L); }
    public boolean hasLastLocation()     { return getLastLat() != 0 && getLastLng() != 0; }

    // AutoTrack settings
    public int  getAutoTrackCaptureInterval() {
        return prefs.getInt(AppConstants.PREF_AUTO_TRACK_INTERVAL,
            AppConstants.AUTO_TRACK_DEFAULT_CAPTURE_MIN);
    }
    public void setAutoTrackCaptureInterval(int v) {
        prefs.edit().putInt(AppConstants.PREF_AUTO_TRACK_INTERVAL, v).apply();
    }
    public int  getAutoTrackShareInterval() {
        return prefs.getInt(AppConstants.PREF_AUTO_TRACK_SHARE_EVERY,
            AppConstants.AUTO_TRACK_DEFAULT_SHARE_MIN);
    }
    public void setAutoTrackShareInterval(int v) {
        prefs.edit().putInt(AppConstants.PREF_AUTO_TRACK_SHARE_EVERY, v).apply();
    }

    // Resolve which number to alert for journey
    public String getActiveAlertNumber() {
        String journeyNum = getJourneyContactNumber();
        if (!journeyNum.isEmpty()) return journeyNum;
        return getEmergencyNumber();
    }
    public String getActiveAlertName() {
        String journeyName = getJourneyContactName();
        if (!journeyName.isEmpty()) return journeyName;
        return getEmergencyName();
    }
}
