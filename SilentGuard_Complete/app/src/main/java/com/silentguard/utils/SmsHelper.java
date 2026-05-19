package com.silentguard.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.ArrayList;

public class SmsHelper {
    private static final String TAG = "SmsHelper";

    public static void sendSos(Context context, String message) {
        // Use dynamic number from prefs
        String number = new PrefsManager(context).getEmergencyNumber();
        if (number == null || number.isEmpty()) number = AppConstants.EMERGENCY_NUMBER;
        sendTo(number, message);
    }

    public static void sendTo(String number, String message) {
        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            if (parts.size() == 1) {
                sms.sendTextMessage(number, null, message, null, null);
            } else {
                sms.sendMultipartTextMessage(number, null, parts, null, null);
            }
            Log.d(TAG, "SMS sent to " + number);
        } catch (Exception e) {
            Log.e(TAG, "SMS failed: " + e.getMessage());
        }
    }

    public static void callEmergency(Context context) {
        try {
            String number = new PrefsManager(context).getEmergencyNumber();
            if (number == null || number.isEmpty()) number = AppConstants.EMERGENCY_NUMBER;
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Call failed: " + e.getMessage());
        }
    }
}
