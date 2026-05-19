package com.silentguard.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.silentguard.utils.AppConstants;
import com.silentguard.utils.PrefsManager;
import com.silentguard.utils.SmsHelper;
import com.silentguard.utils.LocationHelper;
import android.location.Location;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkReceiver";
    private static boolean wasConnected = true;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) return;
        PrefsManager prefs = new PrefsManager(context);
        if (!prefs.isGuardActive()) return;

        boolean isConnected = isNetworkAvailable(context);
        if (wasConnected && !isConnected) {
            Log.d(TAG, "Network LOST — sending alert SMS!");
            sendNetworkLostAlert(context, prefs);
        }
        wasConnected = isConnected;
    }

    private boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) { return false; }
    }

    private void sendNetworkLostAlert(final Context context, final PrefsManager prefs) {
        new Thread(new Runnable() {
            public void run() {
                String number = prefs.getEmergencyNumber();
                if (number.isEmpty()) number = AppConstants.EMERGENCY_NUMBER;
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                Location loc = LocationHelper.getLastLocation(context);
                String msg = "ALERT: My phone just lost network/internet at " + time + ".\n" +
                    (loc != null ? "Last location: " + LocationHelper.buildMapsLink(loc) + "\n" : "") +
                    "Please check on me.\n— SilentGuard";
                SmsHelper.sendTo(number, msg);
                Log.d(TAG, "Network lost alert sent to " + number);
            }
        }).start();
    }
}
