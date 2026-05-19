package com.silentguard.utils;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoTracker {

    private static final String TAG = "AutoTracker";
    private static AutoTracker instance;

    private Context  appContext;
    private boolean  tracking     = false;
    private int      intervalMin  = 1;
    private Thread   captureThread;

    private final List<TrackPoint> trackLog = new ArrayList<TrackPoint>();

    public interface OnNewLocationCallback {
        void onNewLocation(TrackPoint point, int totalPoints);
    }
    private OnNewLocationCallback callback;

    // ── TRACK POINT ───────────────────────────────────────────────────────
    public static class TrackPoint {
        public double lat;
        public double lng;
        public float  accuracy;
        public long   timestamp;
        public String provider;
        public int    index;

        public TrackPoint(Location loc, int idx) {
            this.lat       = loc.getLatitude();
            this.lng       = loc.getLongitude();
            this.accuracy  = loc.getAccuracy();
            this.timestamp = System.currentTimeMillis();
            this.provider  = loc.getProvider() != null
                ? loc.getProvider().toUpperCase() : "GPS";
            this.index     = idx;
        }

        public String toSmsLine() {
            String t = new SimpleDateFormat("HH:mm:ss",
                Locale.getDefault()).format(new Date(timestamp));
            return "#" + index + " [" + t + "] "
                + String.format("%.5f", lat) + ","
                + String.format("%.5f", lng)
                + " (~" + (int) accuracy + "m " + provider + ")";
        }

        public String toMapsLink() {
            return "https://maps.google.com/?q=" + lat + "," + lng;
        }
    }

    // ── SINGLETON ─────────────────────────────────────────────────────────
    public static AutoTracker getInstance(Context context) {
        if (instance == null)
            instance = new AutoTracker(context.getApplicationContext());
        return instance;
    }

    private AutoTracker(Context context) {
        this.appContext = context;
    }

    // ── START TRACKING ────────────────────────────────────────────────────
    public boolean startTracking(int intervalMinutes, OnNewLocationCallback cb) {
        if (tracking) return true;
        this.intervalMin = intervalMinutes;
        this.callback    = cb;
        trackLog.clear();

        // Ensure LiveLocationManager is running
        LiveLocationManager.getInstance(appContext).start();

        final long intervalMs = intervalMinutes * 60 * 1000L;

        captureThread = new Thread(new Runnable() {
            public void run() {
                // Wait one interval before first capture
                try { Thread.sleep(intervalMs); } catch (InterruptedException e) { return; }

                while (tracking) {
                    try {
                        // Get live location — actually changes as user moves
                        Location loc = LiveLocationManager
                            .getInstance(appContext).getBestLocation();

                        // Fallback: fresh fix if live not available
                        if (loc == null)
                            loc = LocationHelper.requestFreshFix(appContext, 10);

                        if (loc != null) {
                            logPoint(loc);
                        } else {
                            Log.d(TAG, "No location for track point #"
                                + (trackLog.size() + 1));
                        }

                        Thread.sleep(intervalMs);
                    } catch (InterruptedException e) { break; }
                }
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();
        tracking = true;
        Log.d(TAG, "AutoTracker ON — every " + intervalMinutes + " min");
        return true;
    }

    // ── STOP TRACKING ─────────────────────────────────────────────────────
    public void stopTracking() {
        tracking = false;
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        Log.d(TAG, "AutoTracker OFF. Points: " + trackLog.size());
    }

    // ── LOG POINT ─────────────────────────────────────────────────────────
    private synchronized void logPoint(Location loc) {
        TrackPoint point = new TrackPoint(loc, trackLog.size() + 1);
        trackLog.add(point);
        // Save to prefs
        new PrefsManager(appContext).saveLastLocation(point.lat, point.lng);
        Log.d(TAG, "Track #" + point.index + ": "
            + point.lat + "," + point.lng
            + " acc=" + (int)point.accuracy + "m");
        if (callback != null)
            callback.onNewLocation(point, trackLog.size());
    }

    // ── BUILD TRACK SMS ───────────────────────────────────────────────────
    public String buildTrackSms(String dest, int lastN) {
        if (trackLog.isEmpty()) return null;
        int total = trackLog.size();
        int from  = Math.max(0, total - lastN);

        StringBuilder sb = new StringBuilder();
        sb.append("AUTO TRACK — To: ").append(dest).append("\n");
        sb.append("Points ").append(from+1).append("–").append(total)
          .append(" of ").append(total).append("\n\n");

        TrackPoint latest = trackLog.get(total - 1);
        sb.append("Latest: ").append(latest.toMapsLink()).append("\n\n");
        sb.append("Log:\n");
        for (int i = from; i < total; i++)
            sb.append(trackLog.get(i).toSmsLine()).append("\n");

        sb.append("\n").append(CellLocator.buildCellSmsBlock(appContext));
        sb.append("\n— SilentGuard AutoTrack");
        return sb.toString();
    }

    // ── FULL SUMMARY ──────────────────────────────────────────────────────
    public String buildFullTrackSummary(String dest) {
        if (trackLog.isEmpty()) return "No track points recorded.";
        TrackPoint first  = trackLog.get(0);
        TrackPoint latest = trackLog.get(trackLog.size() - 1);
        long durationMin  = (latest.timestamp - first.timestamp) / 60000;

        StringBuilder sb = new StringBuilder();
        sb.append("JOURNEY TRACK SUMMARY\n");
        sb.append("To     : ").append(dest).append("\n");
        sb.append("Duration: ").append(durationMin).append(" min\n");
        sb.append("Points : ").append(trackLog.size()).append("\n\n");
        sb.append("Start  : ").append(first.toMapsLink()).append("\n");
        sb.append("Current: ").append(latest.toMapsLink()).append("\n\n");
        int from = Math.max(0, trackLog.size() - 5);
        sb.append("Last 5 points:\n");
        for (int i = from; i < trackLog.size(); i++)
            sb.append(trackLog.get(i).toSmsLine()).append("\n");
        sb.append("\n— SilentGuard AutoTrack");
        return sb.toString();
    }

    public boolean       isTracking()    { return tracking; }
    public int           getTotalPoints(){ return trackLog.size(); }
    public int           getIntervalMin(){ return intervalMin; }
    public void          clearLog()      { trackLog.clear(); }
    public TrackPoint    getLatestPoint(){
        return trackLog.isEmpty() ? null : trackLog.get(trackLog.size()-1);
    }
}
