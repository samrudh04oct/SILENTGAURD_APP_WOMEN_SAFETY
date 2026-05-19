package com.silentguard.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.silentguard.utils.AppConstants;
import com.silentguard.utils.PrefsManager;

public class SetupActivity extends Activity {
    private static final int REQ = 1001;
    private static final String[] PERMS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (allGranted()) { done(); return; }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0A0F);
        root.setPadding(dp(24), dp(60), dp(24), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView emoji = new TextView(this);
        emoji.setText("SilentGuard");
        emoji.setTextSize(36); emoji.setTextColor(0xFFC8A96E);
        emoji.setTypeface(Typeface.DEFAULT_BOLD);
        emoji.setGravity(Gravity.CENTER);
        emoji.setPadding(0, 0, 0, dp(8)); root.addView(emoji);

        TextView sub = new TextView(this);
        sub.setText("Women Safety App");
        sub.setTextSize(14); sub.setTextColor(0xFF8A8578);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, dp(32)); root.addView(sub);

        TextView emNum = new TextView(this);
        emNum.setText("Default Emergency: " + AppConstants.EMERGENCY_NUMBER);
        emNum.setTextSize(13); emNum.setTextColor(0xFFC8A96E);
        emNum.setGravity(Gravity.CENTER);
        emNum.setPadding(0, 0, 0, dp(32)); root.addView(emNum);

        TextView desc = new TextView(this);
        desc.setText("SilentGuard needs these permissions:\n\n"
            + "  Location — GPS coordinates for SOS\n"
            + "  SMS — alert emergency contact\n"
            + "  Microphone — record audio evidence\n"
            + "  Phone — monitor signal strength\n"
            + "  Contacts — pick emergency contact\n\n"
            + "All data stays on your device only.\nNo cloud. No servers. No tracking.");
        desc.setTextSize(13); desc.setTextColor(0xFF8A8578);
        desc.setLineSpacing(0, 1.6f);
        desc.setPadding(0, 0, 0, dp(32)); root.addView(desc);

        Button btn = new Button(this);
        btn.setText("GRANT PERMISSIONS");
        btn.setBackgroundColor(0xFFC8A96E);
        btn.setTextColor(0xFF0A0A0F);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { requestPermissions(PERMS, REQ); }
        });
        root.addView(btn);

        TextView skip = new TextView(this);
        skip.setText("Skip for now");
        skip.setTextSize(13); skip.setTextColor(0xFF8A8578);
        skip.setGravity(Gravity.CENTER);
        skip.setPadding(0, dp(16), 0, 0);
        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { done(); }
        });
        root.addView(skip);

        setContentView(root);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        done();
    }

    private void done() {
        new PrefsManager(this).setSetupDone(true);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean allGranted() {
        for (String p : PERMS)
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
