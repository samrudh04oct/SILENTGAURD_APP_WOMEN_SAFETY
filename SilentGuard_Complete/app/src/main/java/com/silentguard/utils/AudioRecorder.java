package com.silentguard.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private static MediaRecorder recorder;
    private static boolean recording = false;

    public static void startRecording(Context context) {
        if (recording) return;
        try {
            File dir = getVaultDir(context);
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(dir, "evidence_" + ts + ".3gp");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(file.getAbsolutePath());
            recorder.setMaxDuration(5 * 60 * 1000);
            recorder.prepare();
            recorder.start();
            recording = true;
            Log.d(TAG, "Recording: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Record failed: " + e.getMessage());
        }
    }

    public static void stopRecording() {
        try {
            if (recorder != null) { recorder.stop(); recorder.release(); recorder = null; }
            recording = false;
        } catch (Exception e) { Log.e(TAG, "Stop failed: " + e.getMessage()); }
    }

    public static File getVaultDir(Context context) {
        File dir = new File(context.getFilesDir(), "vault");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File[] getAllRecordings(Context context) {
        File[] files = getVaultDir(context).listFiles();
        return files != null ? files : new File[0];
    }
}
