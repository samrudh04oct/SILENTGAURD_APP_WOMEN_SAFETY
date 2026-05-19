package com.silentguard.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import com.silentguard.utils.*;

public class SosService extends Service {
    private static final String TAG = "SosService";

    public static void triggerSos(Context ctx, String type, String dest) {
        Intent i = new Intent(ctx, SosService.class);
        i.setAction(AppConstants.ACTION_SOS_TRIGGER);
        i.putExtra("type", type);
        i.putExtra("dest", dest);
        NotifHelper.startFgService(ctx, i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotifHelper.createChannel(this, AppConstants.CHANNEL_SOS,
            "SOS Alerts", NotifHelper.IMPORTANCE_HIGH);
        startForeground(AppConstants.NOTIF_SOS_ID,
            NotifHelper.buildNotification(this, AppConstants.CHANNEL_SOS,
                "SOS Active", "Alert sent.",
                android.R.drawable.ic_dialog_alert, true));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        String type   = intent.getStringExtra("type");
        String dest   = intent.getStringExtra("dest");
        String action = intent.getAction();
        if (type == null) type = "SOS";
        if (dest == null) dest = "";
        if      (AppConstants.ACTION_SOS_TRIGGER.equals(action))  handleSos(type, dest);
        else if (AppConstants.ACTION_CHECKIN.equals(action))      handleCheckin();
        else if (AppConstants.ACTION_JOURNEY_SAFE.equals(action)) handleJourneySafe(dest);
        return START_NOT_STICKY;
    }

    private void handleSos(final String type, final String dest) {
        vibrate();
        AudioRecorder.startRecording(this);
        final Context ctx = this;
        new Thread(new Runnable() {
            public void run() {
                PrefsManager prefs  = new PrefsManager(ctx);
                String       number = prefs.getActiveAlertNumber();
                // Uses GPS → last saved → cell tower estimate
                String msg;
                if ("JOURNEY_OVERDUE".equals(type)) {
                    msg = LocationHelper.buildJourneyOverdueMessage(
                        ctx, prefs, dest);
                } else {
                    msg = LocationHelper.buildSosMessage(ctx, prefs);
                }
                SmsHelper.sendTo(number, msg);
                Location loc = LocationHelper.getLastLocation(ctx);
                EvidenceVault.log(ctx, type, loc, msg);
                Log.d(TAG, "SOS sent to " + number);
                try { Thread.sleep(5 * 60 * 1000L); }
                catch (InterruptedException ignored) {}
                AudioRecorder.stopRecording();
                stopSelf();
            }
        }).start();
    }

    private void handleCheckin() {
        final Context ctx = this;
        new Thread(new Runnable() {
            public void run() {
                PrefsManager prefs = new PrefsManager(ctx);
                Location loc = LocationHelper.getLastLocation(ctx);
                String msg = LocationHelper.buildCheckinMessage(loc);
                SmsHelper.sendTo(prefs.getActiveAlertNumber(), msg);
                EvidenceVault.log(ctx, "CHECKIN", loc, msg);
                stopSelf();
            }
        }).start();
    }

    private void handleJourneySafe(final String dest) {
        final Context ctx = this;
        new Thread(new Runnable() {
            public void run() {
                PrefsManager prefs  = new PrefsManager(ctx);
                String       number = prefs.getActiveAlertNumber();
                // Send arrival with best location — GPS or cell estimate
                String msg = LocationHelper.buildSafeArrivalMessage(ctx, prefs, dest);
                SmsHelper.sendTo(number, msg);
                Location loc = LocationHelper.getLastLocation(ctx);
                EvidenceVault.log(ctx, "ARRIVED_SAFE", loc, msg);
                prefs.setJourneyActive(false);
                prefs.setJourneyContactNumber("");
                prefs.setJourneyContactName("");
                stopSelf();
            }
        }).start();
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(new long[]{0,100,100,100,100,100}, -1);
        } catch (Exception ignored) {}
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
