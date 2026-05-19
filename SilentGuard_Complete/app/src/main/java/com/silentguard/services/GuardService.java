package com.silentguard.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.silentguard.utils.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GuardService extends Service {
    private static final String TAG = "GuardService";

    private boolean running              = false;
    private int     volumePressCount     = 0;
    private long    lastVolumePressTime  = 0;
    private int     lastVolume           = -1;
    private boolean alertSentForThisDrop = false;
    private int     noNetworkCount       = 0;
    private int     lastSignalStrength   = -1;

    private TelephonyManager   telephonyManager;
    private PhoneStateListener signalListener;
    private Handler            mainHandler;

    private Thread  volumeThread;
    private Thread  locationThread;
    private Thread  networkThread;
    private Thread  journeyMonitorThread;
    private Thread  autoTrackShareThread; // NEW: sends SMS at share interval

    public static void start(Context ctx) {
        NotifHelper.startFgService(ctx, new Intent(ctx, GuardService.class));
    }
    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, GuardService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        NotifHelper.createChannel(this, AppConstants.CHANNEL_GUARD,
            "Guard Service", NotifHelper.IMPORTANCE_MIN);
        startForeground(AppConstants.NOTIF_GUARD_ID,
            NotifHelper.buildNotification(this, AppConstants.CHANNEL_GUARD,
                "Notes", "Syncing notes...",
                android.R.drawable.ic_menu_edit, true));
        setupSignalMonitor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            startVolumeMonitor();
            startLocationSaver();
            startNetworkMonitor();
            startJourneyMonitor();
            startAutoTrackShareThread();
        }
        return START_STICKY;
    }

    // ── SIGNAL MONITOR ────────────────────────────────────────────────────
    private void setupSignalMonitor() {
        try {
            telephonyManager = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) return;
            signalListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength ss) {
                    if (!running) return;
                    PrefsManager prefs = new PrefsManager(GuardService.this);
                    if (!prefs.isGuardActive() || !prefs.isNetworkAlertEnabled()) return;
                    int dbm = extractSignalDbm(ss);
                    if (lastSignalStrength > -105 && dbm < -110) {
                        sendPreemptiveAlert(prefs);
                    }
                    lastSignalStrength = dbm;
                }
            };
            telephonyManager.listen(signalListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } catch (Exception e) {
            Log.e(TAG, "Signal monitor: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private int extractSignalDbm(SignalStrength ss) {
        try { int g = ss.getGsmSignalStrength(); if (g != 99) return -113+(2*g); }
        catch (Exception ignored) {}
        return -100;
    }

    private void sendPreemptiveAlert(final PrefsManager prefs) {
        if (alertSentForThisDrop) return;
        alertSentForThisDrop = true;
        new Thread(new Runnable() {
            public void run() {
                String time   = new SimpleDateFormat("HH:mm:ss",
                    Locale.getDefault()).format(new Date());
                String locStr = LocationHelper.getBestLocationString(
                    GuardService.this, prefs);
                String msg = "WARNING: Signal dropping at " + time + ".\n"
                    + locStr + "\n\n"
                    + CellLocator.buildCellSmsBlock(GuardService.this)
                    + "\n— SilentGuard";
                SmsHelper.sendTo(prefs.getActiveAlertNumber(), msg);
            }
        }).start();
    }

    // ── LIVE LOCATION — starts requestLocationUpdates so GPS actually updates ──
    private void startLocationSaver() {
        // Start LiveLocationManager on main thread (needs Looper)
        mainHandler.post(new Runnable() {
            public void run() {
                LiveLocationManager.getInstance(GuardService.this).start();
                Log.d(TAG, "LiveLocationManager started");
            }
        });

        // Background thread saves latest to prefs every 30s
        locationThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(AppConstants.LOCATION_UPDATE_INTERVAL);
                        PrefsManager prefs = new PrefsManager(GuardService.this);
                        if (!prefs.isGuardActive()) continue;
                        // Get live location (actually changes as user moves)
                        Location loc = LocationHelper.getCurrentLocation(
                            GuardService.this);
                        if (loc != null) {
                            prefs.saveLastLocation(
                                loc.getLatitude(), loc.getLongitude());
                            Log.d(TAG, "Saved live: "
                                + loc.getLatitude() + ","
                                + loc.getLongitude());
                        }
                    } catch (InterruptedException e) { break; }
                }
            }
        });
        locationThread.setDaemon(true);
        locationThread.start();
    }

    // ── AUTO TRACK SHARE THREAD ───────────────────────────────────────────
    // When journey active + AutoTracker running → sends SMS at share interval
    private void startAutoTrackShareThread() {
        autoTrackShareThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        // Check every 30 seconds if it's time to share
                        Thread.sleep(30 * 1000L);
                        PrefsManager prefs = new PrefsManager(GuardService.this);
                        if (!prefs.isJourneyActive()) continue;

                        AutoTracker tracker = AutoTracker.getInstance(GuardService.this);
                        if (!tracker.isTracking()) {
                            // Start tracker if not running (e.g. after reboot)
                            startAutoTracker(prefs);
                            continue;
                        }

                        int shareIntervalMin = prefs.getAutoTrackShareInterval();
                        int totalPoints      = tracker.getTotalPoints();
                        if (totalPoints == 0) continue;

                        // Share every N minutes — check if totalPoints divisible
                        // by (shareInterval / captureInterval)
                        int captureMin   = prefs.getAutoTrackCaptureInterval();
                        int shareEveryN  = Math.max(1, shareIntervalMin / captureMin);

                        if (totalPoints % shareEveryN == 0) {
                            String dest  = prefs.getJourneyDest();
                            String number = prefs.getActiveAlertNumber();
                            // Send last shareEveryN points
                            String msg = tracker.buildTrackSms(dest, shareEveryN);
                            if (msg != null) {
                                SmsHelper.sendTo(number, msg);
                                Log.d(TAG, "AutoTrack SMS sent — "
                                    + totalPoints + " points total");
                            }
                        }
                    } catch (InterruptedException e) { break; }
                }
            }
        });
        autoTrackShareThread.setDaemon(true);
        autoTrackShareThread.start();
    }

    // Starts AutoTracker on main thread (LocationManager needs main looper)
    public void startAutoTracker(final PrefsManager prefs) {
        final Context ctx = this;
        mainHandler.post(new Runnable() {
            public void run() {
                AutoTracker tracker = AutoTracker.getInstance(ctx);
                int captureMin = prefs.getAutoTrackCaptureInterval();
                tracker.startTracking(captureMin,
                    new AutoTracker.OnNewLocationCallback() {
                        public void onNewLocation(AutoTracker.TrackPoint point,
                                int total) {
                            prefs.saveLastLocation(point.lat, point.lng);
                        }
                    });
                Log.d(TAG, "AutoTracker started — every " + captureMin + " min");
            }
        });
    }

    public void stopAutoTracker() {
        final Context ctx = this;
        mainHandler.post(new Runnable() {
            public void run() {
                AutoTracker.getInstance(ctx).stopTracking();
            }
        });
    }

    // ── JOURNEY MONITOR ───────────────────────────────────────────────────
    private void startJourneyMonitor() {
        journeyMonitorThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(AppConstants.JOURNEY_CHECK_INTERVAL);
                        PrefsManager prefs = new PrefsManager(GuardService.this);
                        if (!prefs.isJourneyActive()) {
                            // Journey ended — stop tracker if still running
                            if (AutoTracker.getInstance(GuardService.this).isTracking()) {
                                stopAutoTracker();
                                LiveLocationManager.getInstance(GuardService.this).stop();
                            }
                            continue;
                        }

                        long now = System.currentTimeMillis();
                        long eta = prefs.getJourneyEta();

                        if (now > eta && now < eta + AppConstants.JOURNEY_OVERDUE_GRACE) {
                            String msg = LocationHelper.buildJourneyOverdueMessage(
                                GuardService.this, prefs, prefs.getJourneyDest());
                            SmsHelper.sendTo(prefs.getActiveAlertNumber(), msg);
                        }
                        if (now > eta + AppConstants.JOURNEY_OVERDUE_GRACE) {
                            // Send full track summary before SOS
                            AutoTracker tracker =
                                AutoTracker.getInstance(GuardService.this);
                            if (tracker.getTotalPoints() > 0) {
                                String summary = tracker.buildFullTrackSummary(
                                    prefs.getJourneyDest());
                                SmsHelper.sendTo(prefs.getActiveAlertNumber(), summary);
                            }
                            SosService.triggerSos(GuardService.this,
                                "JOURNEY_OVERDUE", prefs.getJourneyDest());
                            prefs.setJourneyActive(false);
                                            stopAutoTracker();
                            LiveLocationManager.getInstance(GuardService.this).stop();
                        }
                    } catch (InterruptedException e) { break; }
                }
            }
        });
        journeyMonitorThread.setDaemon(true);
        journeyMonitorThread.start();
    }

    // ── NETWORK MONITOR ───────────────────────────────────────────────────
    private void startNetworkMonitor() {
        networkThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(AppConstants.NETWORK_MONITOR_INTERVAL);
                        PrefsManager prefs = new PrefsManager(GuardService.this);
                        if (!prefs.isGuardActive()
                                || !prefs.isNetworkAlertEnabled()) continue;
                        boolean avail = isNetworkAvailable();
                        if (!avail) {
                            noNetworkCount++;
                            if (noNetworkCount == 2 && !alertSentForThisDrop) {
                                alertSentForThisDrop = true;
                                String msg = LocationHelper.buildNetworkLostMessage(
                                    GuardService.this,
                                    prefs.getLastLat(), prefs.getLastLng(),
                                    prefs.getLastLocationTime());
                                SmsHelper.sendTo(prefs.getActiveAlertNumber(), msg);
                            }
                        } else {
                            if (noNetworkCount >= 2) {
                                String time = new SimpleDateFormat("HH:mm:ss",
                                    Locale.getDefault()).format(new Date());
                                String locStr = LocationHelper.getBestLocationString(
                                    GuardService.this, prefs);
                                SmsHelper.sendTo(prefs.getActiveAlertNumber(),
                                    "Network restored at " + time + ".\n"
                                    + locStr + "\n— SilentGuard");
                            }
                            noNetworkCount       = 0;
                            alertSentForThisDrop = false;
                        }
                    } catch (InterruptedException e) { break; }
                }
            }
        });
        networkThread.setDaemon(true);
        networkThread.start();
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) { return false; }
    }

    // ── VOLUME SOS ────────────────────────────────────────────────────────
    private void startVolumeMonitor() {
        volumeThread = new Thread(new Runnable() {
            public void run() {
                AudioManager am = (AudioManager)
                    getSystemService(Context.AUDIO_SERVICE);
                if (am == null) return;
                lastVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                while (running) {
                    try {
                        Thread.sleep(200);
                        int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                        if (cur < lastVolume) {
                            long now = System.currentTimeMillis();
                            if (now - lastVolumePressTime
                                    > AppConstants.SOS_VOLUME_PRESS_WINDOW)
                                volumePressCount = 0;
                            volumePressCount++;
                            lastVolumePressTime = now;
                            if (volumePressCount >= AppConstants.SOS_VOLUME_PRESS_COUNT) {
                                volumePressCount = 0;
                                SosService.triggerSos(GuardService.this, "SOS", "");
                            }
                        }
                        lastVolume = cur;
                    } catch (InterruptedException e) { break; }
                }
            }
        });
        volumeThread.setDaemon(true);
        volumeThread.start();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        try {
            if (telephonyManager != null && signalListener != null)
                telephonyManager.listen(signalListener,
                    PhoneStateListener.LISTEN_NONE);
        } catch (Exception ignored) {}
        stopAutoTracker();
        LiveLocationManager.getInstance(this).stop();
        if (volumeThread         != null) volumeThread.interrupt();
        if (locationThread       != null) locationThread.interrupt();
        if (networkThread        != null) networkThread.interrupt();
        if (journeyMonitorThread != null) journeyMonitorThread.interrupt();
        if (autoTrackShareThread != null) autoTrackShareThread.interrupt();
    }
}
