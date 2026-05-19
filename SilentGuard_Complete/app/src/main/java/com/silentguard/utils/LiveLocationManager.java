package com.silentguard.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * LiveLocationManager — always gives the most recent GPS fix.
 * Uses requestLocationUpdates (not getLastKnownLocation) so coordinates
 * actually change as the device moves.
 * Works completely offline — raw GPS satellite signal, no internet needed.
 */
public class LiveLocationManager {

    private static final String TAG = "LiveLocation";
    private static LiveLocationManager instance;

    private LocationManager  locationManager;
    private Location         bestLocation;       // most recent best fix
    private boolean          started   = false;
    private Handler          handler   = new Handler(Looper.getMainLooper());

    private LocationListener gpsListener;
    private LocationListener networkListener;

    // ── SINGLETON ─────────────────────────────────────────────────────────
    public static LiveLocationManager getInstance(Context ctx) {
        if (instance == null)
            instance = new LiveLocationManager(ctx.getApplicationContext());
        return instance;
    }

    private LiveLocationManager(Context context) {
        locationManager = (LocationManager)
            context.getSystemService(Context.LOCATION_SERVICE);
    }

    // ── START LIVE UPDATES ────────────────────────────────────────────────
    // Call this once when Guard activates.
    // Requests updates every 10 seconds OR 5 metres movement — whichever first.
    @SuppressWarnings("MissingPermission")
    public void start() {
        if (started) return;

        gpsListener = new LocationListener() {
            public void onLocationChanged(Location loc) {
                if (isBetter(loc, bestLocation)) {
                    bestLocation = loc;
                    Log.d(TAG, "GPS update: " + loc.getLatitude()
                        + "," + loc.getLongitude()
                        + " acc=" + (int)loc.getAccuracy() + "m");
                }
            }
            public void onStatusChanged(String p, int s, Bundle e) {}
            public void onProviderEnabled(String p)  { Log.d(TAG, "GPS enabled"); }
            public void onProviderDisabled(String p) { Log.d(TAG, "GPS disabled"); }
        };

        networkListener = new LocationListener() {
            public void onLocationChanged(Location loc) {
                if (isBetter(loc, bestLocation)) {
                    bestLocation = loc;
                    Log.d(TAG, "Network update: " + loc.getLatitude()
                        + "," + loc.getLongitude()
                        + " acc=" + (int)loc.getAccuracy() + "m");
                }
            }
            public void onStatusChanged(String p, int s, Bundle e) {}
            public void onProviderEnabled(String p)  {}
            public void onProviderDisabled(String p) {}
        };

        handler.post(new Runnable() {
            @SuppressWarnings("MissingPermission")
            public void run() {
                boolean any = false;
                try {
                    if (locationManager.isProviderEnabled(
                            LocationManager.GPS_PROVIDER)) {
                        // Update every 10 seconds or 5 metres
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            10_000L,   // min time ms
                            5f,        // min distance metres
                            gpsListener
                        );
                        any = true;
                        Log.d(TAG, "GPS requestLocationUpdates started");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "GPS start: " + e.getMessage());
                }

                try {
                    if (locationManager.isProviderEnabled(
                            LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            10_000L,
                            5f,
                            networkListener
                        );
                        any = true;
                        Log.d(TAG, "Network requestLocationUpdates started");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Network start: " + e.getMessage());
                }

                if (any) {
                    started = true;
                    // Seed with last known so we have something immediately
                    seedLastKnown();
                }
            }
        });
    }

    // ── STOP ──────────────────────────────────────────────────────────────
    public void stop() {
        handler.post(new Runnable() {
            @SuppressWarnings("MissingPermission")
            public void run() {
                try {
                    if (gpsListener     != null)
                        locationManager.removeUpdates(gpsListener);
                    if (networkListener != null)
                        locationManager.removeUpdates(networkListener);
                } catch (Exception ignored) {}
                started = false;
                Log.d(TAG, "Live location stopped");
            }
        });
    }

    // ── GET BEST CURRENT LOCATION ─────────────────────────────────────────
    public Location getBestLocation() {
        return bestLocation;
    }

    // ── SEED WITH LAST KNOWN ──────────────────────────────────────────────
    @SuppressWarnings("MissingPermission")
    private void seedLastKnown() {
        try {
            Location gps = locationManager.getLastKnownLocation(
                LocationManager.GPS_PROVIDER);
            Location net = locationManager.getLastKnownLocation(
                LocationManager.NETWORK_PROVIDER);
            if (gps != null && isBetter(gps, bestLocation)) bestLocation = gps;
            if (net != null && isBetter(net, bestLocation)) bestLocation = net;
            if (bestLocation != null)
                Log.d(TAG, "Seeded: " + bestLocation.getLatitude()
                    + "," + bestLocation.getLongitude());
        } catch (Exception ignored) {}
    }

    // ── IS NEW LOCATION BETTER ────────────────────────────────────────────
    private boolean isBetter(Location newLoc, Location current) {
        if (current == null) return true;
        long timeDelta = newLoc.getTime() - current.getTime();
        // Prefer newer (>2min newer = always use)
        if (timeDelta > 2 * 60 * 1000L) return true;
        // Prefer more accurate
        if (newLoc.getAccuracy() < current.getAccuracy()) return true;
        return false;
    }

    public boolean isStarted() { return started; }
}
