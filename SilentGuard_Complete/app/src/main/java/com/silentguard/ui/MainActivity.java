package com.silentguard.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.silentguard.services.GuardService;
import com.silentguard.services.SosService;
import com.silentguard.utils.*;

public class MainActivity extends Activity {

    private PrefsManager prefs;
    private TextView tvGuardStatus, tvStatus, tvJourneyDest;
    private TextView tvContactName, tvContactNumber;
    private Button   btnToggle;
    private LinearLayout cardJourney;
    private Switch   swNetworkAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsManager(this);
        if (!hasPerms()) {
            startActivity(new Intent(this, SetupActivity.class));
            finish(); return;
        }
        buildUI();
    }

    @Override protected void onResume() {
        super.onResume();
        if (tvGuardStatus != null) updateUI();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == AppConstants.REQUEST_CONTACT_PICKER
                && res == RESULT_OK && data != null)
            pickContact(data);
    }

    private void pickContact(Intent data) {
        try {
            Cursor c = getContentResolver().query(data.getData(),
                new String[]{ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                String id   = c.getString(0);
                String name = c.getString(1);
                c.close();
                Cursor ph = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                    new String[]{id}, null);
                if (ph != null && ph.moveToFirst()) {
                    String num = ph.getString(0).replaceAll("[\\s\\-()]+", "");
                    ph.close();
                    prefs.setEmergencyNumber(num);
                    prefs.setEmergencyName(name);
                    updateUI();
                    Toast.makeText(this, "Contact set: " + name,
                        Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not read contact",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0A0A0F);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(32), dp(20), dp(40));

        // Title
        TextView appName = new TextView(this);
        appName.setText("SilentGuard");
        appName.setTextSize(28); appName.setTextColor(0xFFF0ECE4);
        appName.setTypeface(Typeface.DEFAULT_BOLD); root.addView(appName);

        tvGuardStatus = new TextView(this);
        tvGuardStatus.setTextSize(12); tvGuardStatus.setTextColor(0xFF8A8578);
        tvGuardStatus.setPadding(0, dp(4), 0, dp(20)); root.addView(tvGuardStatus);

        // Emergency contact card
        LinearLayout cCard = makeCard();
        TextView cLbl = new TextView(this); cLbl.setText("EMERGENCY CONTACT");
        cLbl.setTextSize(10); cLbl.setTextColor(0xFF8A8578); cCard.addView(cLbl);
        tvContactName = new TextView(this);
        tvContactName.setTextSize(17); tvContactName.setTextColor(0xFFC8A96E);
        tvContactName.setTypeface(Typeface.DEFAULT_BOLD);
        tvContactName.setPadding(0, dp(4), 0, dp(2)); cCard.addView(tvContactName);
        tvContactNumber = new TextView(this);
        tvContactNumber.setTextSize(13); tvContactNumber.setTextColor(0xFF8A8578);
        cCard.addView(tvContactNumber);
        Button btnChg = new Button(this); btnChg.setText("CHANGE CONTACT");
        btnChg.setBackgroundColor(0xFF1A1A26); btnChg.setTextColor(0xFFC8A96E);
        btnChg.setTextSize(11);
        LinearLayout.LayoutParams bcp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        bcp.topMargin = dp(8); btnChg.setLayoutParams(bcp);
        btnChg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI),
                    AppConstants.REQUEST_CONTACT_PICKER);
            }
        });
        cCard.addView(btnChg); root.addView(cCard);

        // Network alert toggle
        LinearLayout nCard = makeCard();
        LinearLayout nRow = new LinearLayout(this);
        nRow.setOrientation(LinearLayout.HORIZONTAL);
        nRow.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout nText = new LinearLayout(this);
        nText.setOrientation(LinearLayout.VERTICAL);
        nText.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView nTitle = new TextView(this); nTitle.setText("Network Loss Alert");
        nTitle.setTextSize(14); nTitle.setTextColor(0xFFF0ECE4);
        nTitle.setTypeface(Typeface.DEFAULT_BOLD); nText.addView(nTitle);
        TextView nDesc = new TextView(this);
        nDesc.setText("Auto SMS + cell tower location when internet drops");
        nDesc.setTextSize(11); nDesc.setTextColor(0xFF8A8578);
        nDesc.setPadding(0, dp(2), dp(8), 0); nText.addView(nDesc);
        nRow.addView(nText);
        swNetworkAlert = new Switch(this);
        swNetworkAlert.setChecked(prefs.isNetworkAlertEnabled());
        swNetworkAlert.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    prefs.setNetworkAlertEnabled(checked);
                    Toast.makeText(MainActivity.this,
                        checked ? "Network alert ON" : "Network alert OFF",
                        Toast.LENGTH_SHORT).show();
                }
            });
        nRow.addView(swNetworkAlert); nCard.addView(nRow); root.addView(nCard);

        // Status card
        LinearLayout sCard = makeCard();
        tvStatus = new TextView(this);
        tvStatus.setTextSize(13); tvStatus.setTextColor(0xFFF0ECE4);
        tvStatus.setLineSpacing(0, 1.4f); sCard.addView(tvStatus); root.addView(sCard);

        // Journey active card
        cardJourney = makeCard(); cardJourney.setBackgroundColor(0x22C8A96E);
        cardJourney.setVisibility(View.GONE);
        TextView jT = new TextView(this); jT.setText("JOURNEY ACTIVE");
        jT.setTextColor(0xFFC8A96E); jT.setTextSize(12);
        jT.setTypeface(Typeface.DEFAULT_BOLD); cardJourney.addView(jT);
        tvJourneyDest = new TextView(this);
        tvJourneyDest.setTextColor(0xFFF0ECE4); tvJourneyDest.setTextSize(13);
        tvJourneyDest.setPadding(0, dp(4), 0, dp(10)); cardJourney.addView(tvJourneyDest);
        Button btnArr = makeBtn("I ARRIVED SAFELY", 0xFF5AB88A, 0xFF0A0A0F);
        btnArr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Arrived Safely?")
                    .setMessage("Notify " + prefs.getActiveAlertName()
                        + " with your arrival location?")
                    .setPositiveButton("YES, SAFE", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            Intent i = new Intent(MainActivity.this, SosService.class);
                            i.setAction(AppConstants.ACTION_JOURNEY_SAFE);
                            i.putExtra("dest", prefs.getJourneyDest());
                            startService(i);
                            prefs.setJourneyActive(false);
                            updateUI();
                            Toast.makeText(MainActivity.this,
                                "Safe arrival sent!", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancel", null).show();
            }
        });
        cardJourney.addView(btnArr); root.addView(cardJourney);

        // Main buttons
        btnToggle = makeBtn("ACTIVATE GUARD", 0xFFC8A96E, 0xFF0A0A0F);
        btnToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (prefs.isGuardActive()) {
                    GuardService.stop(MainActivity.this);
                    prefs.setGuardActive(false);
                    Toast.makeText(MainActivity.this,
                        "Guard deactivated", Toast.LENGTH_SHORT).show();
                } else {
                    GuardService.start(MainActivity.this);
                    prefs.setGuardActive(true);
                    Toast.makeText(MainActivity.this,
                        "Guard ON — Vol x3 = SOS", Toast.LENGTH_LONG).show();
                }
                updateUI();
            }
        });
        root.addView(btnToggle);

        Button btnSos = makeBtn("SEND MANUAL SOS", 0xFFE05A5A, 0xFFFFFFFF);
        btnSos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Send SOS?")
                    .setMessage("Sends your best location to "
                        + prefs.getActiveAlertName() + " now.")
                    .setPositiveButton("YES, SEND SOS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                SosService.triggerSos(MainActivity.this, "SOS", "");
                                Toast.makeText(MainActivity.this,
                                    "SOS sent!", Toast.LENGTH_LONG).show();
                            }
                        })
                    .setNegativeButton("Cancel", null).show();
            }
        });
        root.addView(btnSos);

        Button btnJourney = makeBtn("START JOURNEY", 0xFF1A1A26, 0xFFF0ECE4);
        btnJourney.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, JourneyActivity.class));
            }
        });
        root.addView(btnJourney);

        // LOCAL LOCATOR BUTTON — new feature
        Button btnLocator = makeBtn("LOCAL LOCATOR (CELL TOWER)", 0xFF0D2A1A, 0xFF5AB88A);
        btnLocator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showLocalLocator(); }
        });
        root.addView(btnLocator);

        Button btnVault = makeBtn("EVIDENCE VAULT", 0xFF1A1A26, 0xFFF0ECE4);
        btnVault.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VaultActivity.class));
            }
        });
        root.addView(btnVault);

        TextView footer = new TextView(this);
        footer.setText("GPS → Last saved → Cell tower — location always available.\nNo cloud. No account. No tracking.");
        footer.setTextSize(10); footer.setTextColor(0xFF8A8578);
        footer.setGravity(Gravity.CENTER); footer.setLineSpacing(0, 1.5f);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = dp(20); footer.setLayoutParams(fp); root.addView(footer);

        scroll.addView(root); setContentView(scroll);
        updateUI();
    }

    // ── LOCAL LOCATOR DIALOG ──────────────────────────────────────────────
    private void showLocalLocator() {
        final TextView tv = new TextView(this);
        tv.setText("Fetching cell tower data...");
        tv.setTextColor(0xFF5AB88A); tv.setTextSize(12);
        tv.setPadding(dp(20), dp(16), dp(20), dp(16));
        tv.setLineSpacing(0, 1.6f);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);

        new AlertDialog.Builder(this)
            .setTitle("Local Locator — Cell Tower")
            .setView(tv)
            .setPositiveButton("Send SMS to Contact",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        sendCellLocation();
                    }
                })
            .setNeutralButton("Refresh", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    showLocalLocator();
                }
            })
            .setNegativeButton("Close", null)
            .show();

        new Thread(new Runnable() {
            public void run() {
                CellLocator.CellData     cell = CellLocator.getPrimaryCellData(MainActivity.this);
                CellLocator.CellLocation est  = CellLocator.estimateLocation(MainActivity.this);

                final StringBuilder sb = new StringBuilder();

                if (est != null) {
                    sb.append("ESTIMATED LOCATION\n");
                    sb.append("(~1-5km — no internet needed)\n");
                    sb.append(est.mapsLink).append("\n");
                    sb.append("Lat: ").append(String.format("%.4f", est.lat)).append("\n");
                    sb.append("Lng: ").append(String.format("%.4f", est.lng)).append("\n\n");
                }

                if (cell != null) {
                    sb.append("CELL TOWER DETAILS\n");
                    sb.append("------------------\n");
                    sb.append("Type   : ").append(cell.type).append("\n");
                    sb.append("Country: ").append(cell.countryName)
                      .append(" (MCC=").append(cell.mcc).append(")\n");
                    sb.append("Network: ").append(cell.operatorName)
                      .append(" (MNC=").append(cell.mnc).append(")\n");
                    sb.append(cell.areaCodeLabel).append("    : ")
                      .append(cell.areaCode).append("\n");
                    sb.append("Cell ID: ").append(cell.cid).append("\n");
                    sb.append("Signal : ").append(cell.signal).append(" dBm\n");
                } else {
                    sb.append("No cell tower data available.\n");
                    sb.append("Make sure Phone permission is granted.");
                }

                runOnUiThread(new Runnable() {
                    public void run() { tv.setText(sb.toString()); }
                });
            }
        }).start();
    }

    private void sendCellLocation() {
        new Thread(new Runnable() {
            public void run() {
                String number = prefs.getActiveAlertNumber();
                String locStr = LocationHelper.getBestLocationString(
                    MainActivity.this, prefs);
                String msg = "My current location (cell tower estimate):\n"
                    + locStr + "\n— SilentGuard";
                SmsHelper.sendTo(number, msg);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this,
                            "Cell location sent to " + prefs.getActiveAlertName(),
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void updateUI() {
        String name = prefs.getEmergencyName();
        String num  = prefs.getEmergencyNumber();
        if (name == null || name.isEmpty()) name = AppConstants.EMERGENCY_NAME;
        if (num  == null || num.isEmpty())  num  = AppConstants.EMERGENCY_NUMBER;
        tvContactName.setText(name);
        tvContactNumber.setText(num);
        if (swNetworkAlert != null)
            swNetworkAlert.setChecked(prefs.isNetworkAlertEnabled());
        if (prefs.isGuardActive()) {
            tvGuardStatus.setText("● GUARD ACTIVE");
            tvGuardStatus.setTextColor(0xFF5AB88A);
            tvStatus.setText("Vol DOWN x3 = silent SOS\n"
                + "Network drop = auto cell location SMS\n"
                + "Journey = location every 1 min");
            btnToggle.setText("DEACTIVATE GUARD");
            btnToggle.setBackgroundColor(0xFFE05A5A);
        } else {
            tvGuardStatus.setText("○ GUARD INACTIVE");
            tvGuardStatus.setTextColor(0xFF8A8578);
            tvStatus.setText("Tap ACTIVATE GUARD to enable all protections");
            btnToggle.setText("ACTIVATE GUARD");
            btnToggle.setBackgroundColor(0xFFC8A96E);
        }
        if (prefs.isJourneyActive()) {
            cardJourney.setVisibility(View.VISIBLE);
            tvJourneyDest.setText("To: " + prefs.getJourneyDest());
        } else {
            cardJourney.setVisibility(View.GONE);
        }
    }

    private boolean hasPerms() {
        String[] p = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE};
        for (String perm : p)
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    private Button makeBtn(String text, int bg, int fg) {
        Button b = new Button(this);
        b.setText(text); b.setTextColor(fg); b.setBackgroundColor(bg);
        b.setTextSize(13); b.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        p.bottomMargin = dp(12); b.setLayoutParams(p);
        return b;
    }

    private LinearLayout makeCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(0xFF1A1A26);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(14); c.setLayoutParams(p);
        return c;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
