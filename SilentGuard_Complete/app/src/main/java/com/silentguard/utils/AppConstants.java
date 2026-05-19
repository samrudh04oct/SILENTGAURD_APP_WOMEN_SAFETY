package com.silentguard.utils;

public class AppConstants {
    public static final String EMERGENCY_NUMBER      = "9481768381";
    public static final String EMERGENCY_NAME        = "Emergency Contact";

    public static final int    SOS_VOLUME_PRESS_COUNT  = 3;
    public static final long   SOS_VOLUME_PRESS_WINDOW = 2000L;

    public static final long   JOURNEY_CHECK_INTERVAL  = 5 * 60 * 1000L;
    public static final long   JOURNEY_OVERDUE_GRACE   = 10 * 60 * 1000L;
    public static final long   NETWORK_MONITOR_INTERVAL = 10 * 1000L; // every 10 seconds
    public static final long   LOCATION_UPDATE_INTERVAL = 30 * 1000L; // every 30 seconds

    public static final String PREFS_NAME              = "sg_prefs";
    public static final String PREF_SETUP_DONE         = "setup_done";
    public static final String PREF_GUARD_ACTIVE       = "guard_active";
    public static final String PREF_JOURNEY_ACTIVE     = "journey_active";
    public static final String PREF_JOURNEY_ETA        = "journey_eta";
    public static final String PREF_JOURNEY_DEST       = "journey_dest";
    public static final String PREF_JOURNEY_CONTACT_NUMBER = "journey_contact_number";
    public static final String PREF_JOURNEY_CONTACT_NAME   = "journey_contact_name";
    public static final String PREF_EMERGENCY_NUMBER   = "emergency_number";
    public static final String PREF_EMERGENCY_NAME     = "emergency_name";
    public static final String PREF_NETWORK_ALERT      = "network_alert";
    public static final String PREF_LAST_LAT           = "last_lat";
    public static final String PREF_LAST_LNG           = "last_lng";
    public static final String PREF_LAST_LOCATION_TIME = "last_location_time";

    public static final String CHANNEL_GUARD           = "sg_guard";
    public static final String CHANNEL_SOS             = "sg_sos";
    public static final int    NOTIF_GUARD_ID          = 1001;
    public static final int    NOTIF_SOS_ID            = 1002;

    public static final String ACTION_SOS_TRIGGER      = "com.silentguard.SOS_TRIGGER";
    public static final String ACTION_CHECKIN          = "com.silentguard.CHECKIN";
    public static final String ACTION_JOURNEY_SAFE     = "com.silentguard.JOURNEY_SAFE";

    public static final int    SECRET_TAP_COUNT        = 5;
    public static final long   SECRET_TAP_WINDOW       = 2000L;

    public static final int    REQUEST_CONTACT_PICKER        = 2001;
    public static final int    REQUEST_JOURNEY_CONTACT_PICKER = 2002;
    public static final String PREF_AUTO_TRACK_INTERVAL    = "auto_track_interval";
    public static final String PREF_AUTO_TRACK_SHARE_EVERY = "auto_track_share_every";
    // AutoTrack defaults
    public static final int    AUTO_TRACK_DEFAULT_CAPTURE_MIN = 1;  // capture every 1 min
    public static final int    AUTO_TRACK_DEFAULT_SHARE_MIN   = 5;  // share SMS every 5 min
}
