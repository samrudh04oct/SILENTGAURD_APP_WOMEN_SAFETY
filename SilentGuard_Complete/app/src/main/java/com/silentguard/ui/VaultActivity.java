package com.silentguard.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.silentguard.utils.AudioRecorder;
import com.silentguard.utils.EvidenceVault;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;

public class VaultActivity extends Activity {
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0A0F);

        // Header
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setPadding(dp(20), dp(32), dp(20), dp(12));
        Button back = new Button(this); back.setText("<");
        back.setBackgroundColor(0x00000000); back.setTextColor(0xFFF0ECE4); back.setTextSize(20);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        hdr.addView(back);
        TextView title = new TextView(this); title.setText("Evidence Vault");
        title.setTextSize(22); title.setTextColor(0xFFF0ECE4);
        title.setTypeface(Typeface.DEFAULT_BOLD); title.setPadding(dp(8), dp(8), 0, 0);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tp); hdr.addView(title);
        Button clearBtn = new Button(this); clearBtn.setText("Clear All");
        clearBtn.setBackgroundColor(0xFFE05A5A); clearBtn.setTextColor(0xFFFFFFFF);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(VaultActivity.this)
                    .setTitle("Clear all evidence?")
                    .setMessage("Permanently deletes all logs and recordings.")
                    .setPositiveButton("DELETE ALL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            EvidenceVault.clearAll(VaultActivity.this);
                            loadData();
                            Toast.makeText(VaultActivity.this,"Cleared",Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null).show();
            }
        });
        hdr.addView(clearBtn);
        root.addView(hdr);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(12), 0, dp(12), dp(40));
        scroll.addView(listContainer);
        root.addView(scroll);
        setContentView(root);
        loadData();
    }

    private void loadData() {
        listContainer.removeAllViews();
        JSONArray entries = EvidenceVault.readAll(this);
        File[] recordings = AudioRecorder.getAllRecordings(this);

        if (entries.length() == 0 && recordings.length == 0) {
            TextView empty = new TextView(this);
            empty.setText("No evidence logged yet.\n\nLogs appear here when SOS is triggered, journeys are tracked, or check-ins occur.");
            empty.setTextSize(14); empty.setTextColor(0xFF8A8578);
            empty.setPadding(dp(20), dp(40), dp(20), dp(20));
            empty.setLineSpacing(0, 1.5f);
            listContainer.addView(empty); return;
        }

        for (int i = 0; i < entries.length(); i++) {
            try {
                JSONObject obj = entries.getJSONObject(i);
                LinearLayout card = makeCard();
                String type = obj.optString("type", "EVENT");
                String icon;
                if      ("SOS".equals(type))             icon = "SOS TRIGGERED";
                else if ("JOURNEY_OVERDUE".equals(type)) icon = "JOURNEY OVERDUE";
                else if ("JOURNEY_START".equals(type))   icon = "JOURNEY STARTED";
                else if ("ARRIVED_SAFE".equals(type))    icon = "ARRIVED SAFELY";
                else if ("CHECKIN".equals(type))         icon = "AUTO CHECK-IN";
                else                                      icon = type;

                TextView tvType = new TextView(this); tvType.setText(icon);
                tvType.setTextColor(0xFFC8A96E); tvType.setTextSize(13);
                tvType.setTypeface(Typeface.DEFAULT_BOLD); card.addView(tvType);

                TextView tvTime = new TextView(this);
                tvTime.setText(EvidenceVault.formatTime(obj.optLong("timestamp", 0)));
                tvTime.setTextColor(0xFF8A8578); tvTime.setTextSize(11);
                tvTime.setPadding(0, dp(3), 0, dp(4)); card.addView(tvTime);

                if (obj.has("link")) {
                    TextView tvLink = new TextView(this);
                    tvLink.setText(obj.getString("link"));
                    tvLink.setTextColor(0xFF5AB88A); tvLink.setTextSize(11); card.addView(tvLink);
                }
                listContainer.addView(card);
            } catch (Exception ignored) {}
        }

        for (File f : recordings) {
            LinearLayout card = makeCard();
            TextView tv = new TextView(this);
            tv.setText("Audio Evidence: " + f.getName());
            tv.setTextColor(0xFFF0ECE4); tv.setTextSize(13); card.addView(tv);
            listContainer.addView(card);
        }
    }

    private LinearLayout makeCard() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(0xFF1A1A26); c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(8); c.setLayoutParams(p); return c;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
