package com.silentguard.utils;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EvidenceVault {
    private static final String TAG = "EvidenceVault";
    private static final String LOG_FILE = "evidence_log.json";

    public static void log(Context context, String type, Location loc, String note) {
        try {
            JSONArray arr = readAll(context);
            JSONObject obj = new JSONObject();
            obj.put("type", type);
            obj.put("timestamp", System.currentTimeMillis());
            obj.put("note", note != null ? note : "");
            if (loc != null) {
                obj.put("lat", loc.getLatitude());
                obj.put("lng", loc.getLongitude());
                obj.put("link", LocationHelper.buildMapsLink(loc));
            }
            // Insert newest first
            JSONArray newArr = new JSONArray();
            newArr.put(obj);
            for (int i = 0; i < Math.min(arr.length(), 499); i++) newArr.put(arr.get(i));
            getLogFile(context).getParentFile().mkdirs();
            java.io.FileWriter fw = new java.io.FileWriter(getLogFile(context));
            fw.write(newArr.toString());
            fw.close();
        } catch (Exception e) { Log.e(TAG, "Log failed: " + e.getMessage()); }
    }

    public static JSONArray readAll(Context context) {
        try {
            File f = getLogFile(context);
            if (!f.exists()) return new JSONArray();
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return new JSONArray(sb.toString());
        } catch (Exception e) { return new JSONArray(); }
    }

    public static void clearAll(Context context) {
        getLogFile(context).delete();
        for (File f : AudioRecorder.getAllRecordings(context)) f.delete();
    }

    public static String formatTime(long ts) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(new Date(ts));
    }

    private static File getLogFile(Context context) {
        return new File(AudioRecorder.getVaultDir(context), LOG_FILE);
    }
}
