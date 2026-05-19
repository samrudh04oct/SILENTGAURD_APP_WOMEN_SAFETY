package com.silentguard.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LocationHelper {
    private static final String TAG = "LocationHelper";

    // ── PRIMARY: GET LIVE LOCATION ────────────────────────────────────────
    // Always returns the most recent location from LiveLocationManager.
    // Falls back to on-demand fix if live manager hasn't started.
    public static Location getCurrentLocation(Context context) {
        // 1. Try live manager (best — actually updates as user moves)
        LiveLocationManager live = LiveLocationManager.getInstance(context);
        if (live.isStarted()) {
            Location loc = live.getBestLocation();
            if (loc != null) {
                Log.d(TAG, "Live location: "
                    + loc.getLatitude() + "," + loc.getLongitude());
                return loc;
            }
        }
        // 2. On-demand fresh fix (blocking 8s)
        Location fresh = requestFreshFix(context, 8);
        if (fresh != null) return fresh;
        // 3. Last known (static fallback)
        return getLastKnown(context);
    }

    // ── REQUEST FRESH FIX — blocking ──────────────────────────────────────
    @SuppressWarnings("MissingPermission")
    public static Location requestFreshFix(Context context, int timeoutSec) {
        try {
            LocationManager lm = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;

            final Location[]     result = {null};
            final CountDownLatch latch  = new CountDownLatch(1);

            LocationListener listener = new LocationListener() {
                public void onLocationChanged(Location loc) {
                    // Accept any fix — we need it fast
                    if (result[0] == null
                            || loc.getAccuracy() < result[0].getAccuracy()) {
                        result[0] = loc;
                    }
                    latch.countDown();
                }
                public void onStatusChanged(String p, int s, Bundle e) {}
                public void onProviderEnabled(String p) {}
                public void onProviderDisabled(String p) {}
            };

            boolean any = false;
            // Try GPS first (most accurate)
            try {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER, listener, null);
                    any = true;
                }
            } catch (Exception ignored) {}
            // Also try network (faster first fix)
            try {
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER, listener, null);
                    any = true;
                }
            } catch (Exception ignored) {}

            if (!any) return null;

            // Wait for fix
            latch.await(timeoutSec, TimeUnit.SECONDS);
            try { lm.removeUpdates(listener); } catch (Exception ignored) {}

            if (result[0] != null) {
                Log.d(TAG, "Fresh fix: "
                    + result[0].getLatitude() + ","
                    + result[0].getLongitude()
                    + " acc=" + (int)result[0].getAccuracy() + "m");
            }
            return result[0];
        } catch (Exception e) {
            Log.e(TAG, "requestFreshFix: " + e.getMessage());
            return null;
        }
    }

    // ── LAST KNOWN (STATIC) ───────────────────────────────────────────────
    @SuppressWarnings("MissingPermission")
    public static Location getLastKnown(Context context) {
        try {
            LocationManager lm = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;
            Location best = null;
            for (String p : new String[]{
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER}) {
                try {
                    Location l = lm.getLastKnownLocation(p);
                    if (l != null && (best == null
                            || l.getAccuracy() < best.getAccuracy())) best = l;
                } catch (Exception ignored) {}
            }
            return best;
        } catch (Exception e) { return null; }
    }

    // Keep old name for compatibility
    public static Location getLastLocation(Context context) {
        return getCurrentLocation(context);
    }

    // Keep old blocking name for compatibility
    public static Location getFreshLocationBlocking(Context ctx, int timeoutSec) {
        Location live = getCurrentLocation(ctx);
        if (live != null) return live;
        return requestFreshFix(ctx, timeoutSec);
    }

    // ── BEST LOCATION STRING ──────────────────────────────────────────────
    public static String getBestLocationString(Context context, PrefsManager prefs) {
        // Try live / fresh GPS
        Location loc = getCurrentLocation(context);

        if (loc != null) {
            if (prefs != null)
                prefs.saveLastLocation(loc.getLatitude(), loc.getLongitude());
            return "GPS Location:\n"
                + buildMapsLink(loc) + "\n"
                + "Coords : " + String.format("%.5f", loc.getLatitude())
                + ", " + String.format("%.5f", loc.getLongitude()) + "\n"
                + "Accuracy: ~" + (int) loc.getAccuracy() + "m"
                + " via " + (loc.getProvider() != null
                    ? loc.getProvider().toUpperCase() : "GPS");
        }

        // Last saved
        if (prefs != null && prefs.hasLastLocation()) {
            long ageSec = (System.currentTimeMillis()
                - prefs.getLastLocationTime()) / 1000;
            if (ageSec < 600) {
                return "Last GPS (" + ageSec + "s ago):\n"
                    + buildMapsLinkFromCoords(
                        prefs.getLastLat(), prefs.getLastLng()) + "\n"
                    + "Coords : "
                    + String.format("%.5f", prefs.getLastLat())
                    + ", "
                    + String.format("%.5f", prefs.getLastLng());
            }
        }

        // Cell tower estimate
        CellLocator.CellLocation cellLoc = CellLocator.estimateLocation(context);
        if (cellLoc != null) {
            return "Cell Tower Estimate (~1-5km):\n"
                + cellLoc.mapsLink + "\n"
                + "Coords : " + String.format("%.4f", cellLoc.lat)
                + ", " + String.format("%.4f", cellLoc.lng) + "\n"
                + "(Approx — GPS unavailable)";
        }

        return "Location unavailable. See cell tower info.";
    }

    // ── CELL INFO ─────────────────────────────────────────────────────────
    public static String getCellInfo(Context context) {
        CellLocator.CellData cell = CellLocator.getPrimaryCellData(context);
        if (cell == null) return "No cell data";
        return cell.type + " | " + cell.operatorName
            + " | " + cell.areaCodeLabel + "=" + cell.areaCode
            + " | Signal: " + cell.signal + " dBm";
    }

    // ── MESSAGE BUILDERS ──────────────────────────────────────────────────
    public static String buildSosMessage(Context context, PrefsManager prefs) {
        return "URGENT: I need help!\nTime: " + ts() + "\n\n"
            + getBestLocationString(context, prefs) + "\n\n"
            + CellLocator.buildCellSmsBlock(context)
            + "\n— SilentGuard";
    }

    public static String buildNetworkLostMessage(Context context,
            double savedLat, double savedLng, long savedTime) {
        long ageSec = (System.currentTimeMillis() - savedTime) / 1000;
        StringBuilder msg = new StringBuilder();
        msg.append("ALERT: Phone lost network at ").append(tsShort()).append(".\n\n");

        // Try live GPS first
        Location loc = getCurrentLocation(context);
        if (loc != null) {
            msg.append("Current GPS:\n")
               .append(buildMapsLink(loc)).append("\n")
               .append("Coords: ").append(String.format("%.5f", loc.getLatitude()))
               .append(", ").append(String.format("%.5f", loc.getLongitude()))
               .append("\n\n");
        } else if (savedLat != 0 && savedLng != 0) {
            msg.append("Last GPS (").append(ageSec).append("s ago):\n")
               .append(buildMapsLinkFromCoords(savedLat, savedLng)).append("\n")
               .append("Coords: ").append(String.format("%.5f", savedLat))
               .append(", ").append(String.format("%.5f", savedLng)).append("\n\n");
        }

        // Cell tower estimate always appended
        CellLocator.CellLocation cellLoc = CellLocator.estimateLocation(context);
        if (cellLoc != null) {
            msg.append("Cell Tower Estimate:\n")
               .append(cellLoc.mapsLink).append("\n\n");
        }

        msg.append(CellLocator.buildCellSmsBlock(context)).append("\n");
        msg.append("Please check on me.\n— SilentGuard");
        return msg.toString();
    }

    public static String buildJourneyUpdateMessage(Context context,
            PrefsManager prefs, int count, String dest) {
        return "Journey Update #" + count + " — " + tsShort() + "\n"
            + "To: " + dest + "\n\n"
            + getBestLocationString(context, prefs) + "\n\n"
            + CellLocator.buildCellSmsBlock(context)
            + "\n— SilentGuard";
    }

    public static String buildJourneyOverdueMessage(Context context,
            PrefsManager prefs, String dest) {
        return "OVERDUE: Not arrived at " + dest + " by " + tsShort() + ".\n\n"
            + getBestLocationString(context, prefs) + "\n\n"
            + CellLocator.buildCellSmsBlock(context)
            + "\nPlease check immediately!\n— SilentGuard";
    }

    public static String buildJourneyOverdueMessage(String dest, Location loc) {
        if (loc != null) return "Not arrived at " + dest + ".\n"
            + "Location: " + buildMapsLink(loc) + "\n"
            + "Time: " + tsShort() + "\n— SilentGuard";
        return "Not arrived at " + dest + ".\nTime: " + tsShort()
            + "\n— SilentGuard";
    }

    public static String buildSafeArrivalMessage(Context context,
            PrefsManager prefs, String dest) {
        return "SAFE: Arrived at " + dest + " at " + tsShort() + ".\n\n"
            + getBestLocationString(context, prefs) + "\n\n"
            + CellLocator.buildCellSmsBlock(context)
            + "\nJourney complete.\n— SilentGuard";
    }

    public static String buildSafeArrivalMessage(String dest) {
        return "Arrived safely at " + dest + " at " + tsShort()
            + ".\n— SilentGuard";
    }

    public static String buildCheckinMessage(Location loc) {
        if (loc != null) return "Safe check-in at " + tsShort() + ".\n"
            + "Location: " + buildMapsLink(loc) + "\n— SilentGuard";
        return "Safe check-in at " + tsShort() + ".\n— SilentGuard";
    }

    public static String buildSosMessage(Location loc) {
        if (loc != null) return "URGENT: I need help!\n"
            + "Location: " + buildMapsLink(loc) + "\n"
            + "Coords: " + loc.getLatitude() + ", " + loc.getLongitude() + "\n"
            + "Time: " + ts() + "\n— SilentGuard";
        return "URGENT: I need help!\nTime: " + ts() + "\n— SilentGuard";
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    public static String buildMapsLink(Location loc) {
        return "https://maps.google.com/?q="
            + loc.getLatitude() + "," + loc.getLongitude();
    }
    public static String buildMapsLinkFromCoords(double lat, double lng) {
        return "https://maps.google.com/?q=" + lat + "," + lng;
    }
    private static String ts() {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm:ss",
            Locale.getDefault()).format(new Date());
    }
    private static String tsShort() {
        return new SimpleDateFormat("HH:mm:ss",
            Locale.getDefault()).format(new Date());
    }
}
