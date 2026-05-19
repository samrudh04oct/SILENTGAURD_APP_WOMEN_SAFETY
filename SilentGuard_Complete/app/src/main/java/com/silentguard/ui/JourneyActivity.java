package com.silentguard.ui;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.silentguard.services.GuardService;
import com.silentguard.utils.*;
import java.util.Calendar;

public class JourneyActivity extends Activity {

    private EditText  etDest;
    private TextView  tvEta, tvJourneyContact, tvTrackStatus;
    private int       etaHour = -1, etaMin = -1;
    private String    journeyContactNumber = "";
    private String    journeyContactName   = "";
    private int       captureIntervalMin   = 1; // GPS capture every N minutes
    private int       shareIntervalMin     = 5; // SMS share every N minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager prefs = new PrefsManager(this);
        journeyContactNumber = prefs.getJourneyContactNumber();
        journeyContactName   = prefs.getJourneyContactName();
        captureIntervalMin   = prefs.getAutoTrackCaptureInterval();
        shareIntervalMin     = prefs.getAutoTrackShareInterval();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0A0A0F);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(32), dp(20), dp(40));

        // ── BACK + TITLE ──────────────────────────────────────────────
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setPadding(0, 0, 0, dp(20));
        Button back = new Button(this); back.setText("<");
        back.setBackgroundColor(0x00000000); back.setTextColor(0xFFF0ECE4);
        back.setTextSize(20);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        hdr.addView(back);
        TextView title = new TextView(this); title.setText("Start Journey");
        title.setTextSize(22); title.setTextColor(0xFFF0ECE4);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(8), dp(8), 0, 0); hdr.addView(title);
        root.addView(hdr);

        // ── JOURNEY CONTACT ───────────────────────────────────────────
        LinearLayout cCard = makeCard();
        addLabel2(cCard, "ALERT CONTACT FOR THIS JOURNEY");
        tvJourneyContact = new TextView(this);
        tvJourneyContact.setTextSize(14); tvJourneyContact.setTextColor(0xFFC8A96E);
        tvJourneyContact.setTypeface(Typeface.DEFAULT_BOLD);
        tvJourneyContact.setPadding(0, dp(4), 0, dp(8));
        cCard.addView(tvJourneyContact);
        refreshContactDisplay();
        LinearLayout cBtnRow = new LinearLayout(this);
        cBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button btnPick = new Button(this); btnPick.setText("PICK CONTACT");
        btnPick.setBackgroundColor(0xFF1A1A26); btnPick.setTextColor(0xFFC8A96E);
        btnPick.setTextSize(11);
        LinearLayout.LayoutParams bpp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        bpp.rightMargin = dp(8); btnPick.setLayoutParams(bpp);
        btnPick.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openContactPicker(); }
        });
        cBtnRow.addView(btnPick);
        Button btnClr = new Button(this); btnClr.setText("CLEAR");
        btnClr.setBackgroundColor(0xFF1A1A26); btnClr.setTextColor(0xFF8A8578);
        btnClr.setTextSize(11); btnClr.setLayoutParams(new LinearLayout.LayoutParams(dp(80), dp(38)));
        btnClr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                journeyContactNumber = ""; journeyContactName = "";
                refreshContactDisplay();
            }
        });
        cBtnRow.addView(btnClr); cCard.addView(cBtnRow); root.addView(cCard);

        // ── DESTINATION ───────────────────────────────────────────────
        addLabel(root, "DESTINATION");
        etDest = new EditText(this);
        etDest.setHint("e.g. Home, Office, MG Road");
        etDest.setTextColor(0xFFF0ECE4); etDest.setHintTextColor(0xFF8A8578);
        etDest.setBackgroundColor(0xFF1A1A26);
        etDest.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.bottomMargin = dp(20); etDest.setLayoutParams(ep); root.addView(etDest);

        // ── ARRIVAL TIME ──────────────────────────────────────────────
        addLabel(root, "ARRIVAL TIME");
        Button btnTime = new Button(this); btnTime.setText("TAP TO SET ARRIVAL TIME");
        btnTime.setBackgroundColor(0xFF1A1A26); btnTime.setTextColor(0xFF8A8578);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        tp.bottomMargin = dp(6); btnTime.setLayoutParams(tp);
        btnTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                new TimePickerDialog(JourneyActivity.this,
                    new TimePickerDialog.OnTimeSetListener() {
                        public void onTimeSet(TimePicker view, int h, int m) {
                            etaHour = h; etaMin = m;
                            tvEta.setText(String.format("ETA: %02d:%02d", h, m));
                        }
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
            }
        });
        root.addView(btnTime);
        tvEta = new TextView(this); tvEta.setText("");
        tvEta.setTextSize(15); tvEta.setTextColor(0xFFC8A96E);
        tvEta.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams etap = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etap.bottomMargin = dp(20); tvEta.setLayoutParams(etap); root.addView(tvEta);

        // ── AUTO TRACK SETTINGS ───────────────────────────────────────
        LinearLayout trackCard = makeCard();
        trackCard.setBackgroundColor(0xFF0D1A2A);

        TextView trackTitle = new TextView(this);
        trackTitle.setText("AUTO TRACK");
        trackTitle.setTextSize(12); trackTitle.setTextColor(0xFF5AB88A);
        trackTitle.setTypeface(Typeface.DEFAULT_BOLD);
        trackTitle.setPadding(0, 0, 0, dp(6)); trackCard.addView(trackTitle);

        TextView trackDesc = new TextView(this);
        trackDesc.setText("GPS turns ON automatically when journey starts.\n"
            + "Coordinates captured offline and shared at set intervals.");
        trackDesc.setTextSize(11); trackDesc.setTextColor(0xFF8A8578);
        trackDesc.setLineSpacing(0, 1.4f); trackDesc.setPadding(0, 0, 0, dp(12));
        trackCard.addView(trackDesc);

        // Capture interval
        addLabel2(trackCard, "GPS CAPTURE EVERY");
        final String[] captureOpts = {"30 seconds","1 minute","2 minutes","5 minutes"};
        final int[]    captureVals = {1, 1, 2, 5}; // minutes (30s = use 1min)
        Spinner spinCapture = makeSpinner(captureOpts);
        spinCapture.setSelection(1); // default 1 min
        spinCapture.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                captureIntervalMin = captureVals[pos];
                refreshTrackStatus();
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        trackCard.addView(spinCapture);

        // Share interval
        TextView shareLabel = new TextView(this);
        shareLabel.setText("SEND SMS EVERY");
        shareLabel.setTextSize(10); shareLabel.setTextColor(0xFF8A8578);
        shareLabel.setPadding(0, dp(10), 0, dp(4)); trackCard.addView(shareLabel);

        final String[] shareOpts = {"1 minute","2 minutes","5 minutes","10 minutes","15 minutes"};
        final int[]    shareVals = {1, 2, 5, 10, 15};
        Spinner spinShare = makeSpinner(shareOpts);
        spinShare.setSelection(2); // default 5 min
        spinShare.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                shareIntervalMin = shareVals[pos];
                refreshTrackStatus();
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        trackCard.addView(spinShare);

        tvTrackStatus = new TextView(this);
        tvTrackStatus.setTextSize(11); tvTrackStatus.setTextColor(0xFF5AB88A);
        tvTrackStatus.setPadding(0, dp(10), 0, 0);
        refreshTrackStatus(); trackCard.addView(tvTrackStatus);

        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tcp.bottomMargin = dp(20); trackCard.setLayoutParams(tcp);
        root.addView(trackCard);

        // ── INFO CARD ─────────────────────────────────────────────────
        LinearLayout infoCard = makeCard();
        infoCard.setBackgroundColor(0xFF0D1A0D);
        TextView infoTxt = new TextView(this);
        infoTxt.setText("During this journey:\n"
            + "• GPS captured offline every set interval\n"
            + "• Location SMS sent at share interval\n"
            + "• Network drop → last GPS + cell tower SMS\n"
            + "• Overdue → full track log sent + SOS");
        infoTxt.setTextSize(11); infoTxt.setTextColor(0xFF5AB88A);
        infoTxt.setLineSpacing(0, 1.5f); infoCard.addView(infoTxt);
        LinearLayout.LayoutParams icp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        icp.bottomMargin = dp(20); infoCard.setLayoutParams(icp);
        root.addView(infoCard);

        // ── START BUTTON ──────────────────────────────────────────────
        Button btnStart = new Button(this);
        btnStart.setText("START JOURNEY + AUTO TRACK");
        btnStart.setBackgroundColor(0xFFC8A96E); btnStart.setTextColor(0xFF0A0A0F);
        btnStart.setTypeface(Typeface.DEFAULT_BOLD);
        btnStart.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startJourney(); }
        });
        root.addView(btnStart);

        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == AppConstants.REQUEST_JOURNEY_CONTACT_PICKER
                && res == RESULT_OK && data != null)
            readContact(data);
    }

    private void openContactPicker() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI),
                AppConstants.REQUEST_JOURNEY_CONTACT_PICKER);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open contacts", Toast.LENGTH_SHORT).show();
        }
    }

    private void readContact(Intent data) {
        try {
            Cursor c = getContentResolver().query(data.getData(),
                new String[]{ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                String id   = c.getString(0);
                String name = c.getString(1); c.close();
                Cursor ph = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                    new String[]{id}, null);
                if (ph != null && ph.moveToFirst()) {
                    journeyContactNumber = ph.getString(0)
                        .replaceAll("[\\s\\-()]+", "");
                    journeyContactName   = name; ph.close();
                    refreshContactDisplay();
                    Toast.makeText(this, "Contact: " + name,
                        Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not read contact",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshContactDisplay() {
        if (tvJourneyContact == null) return;
        if (journeyContactName.isEmpty()) {
            PrefsManager prefs = new PrefsManager(this);
            tvJourneyContact.setText("Default: " + prefs.getEmergencyName()
                + " — " + prefs.getEmergencyNumber());
            tvJourneyContact.setTextColor(0xFF8A8578);
        } else {
            tvJourneyContact.setText(journeyContactName
                + "  " + journeyContactNumber);
            tvJourneyContact.setTextColor(0xFFC8A96E);
        }
    }

    private void refreshTrackStatus() {
        if (tvTrackStatus == null) return;
        tvTrackStatus.setText("GPS captured every " + captureIntervalMin
            + " min  •  SMS sent every " + shareIntervalMin + " min");
    }

    private void startJourney() {
        String dest = etDest.getText().toString().trim();
        if (dest.isEmpty()) {
            Toast.makeText(this,"Enter destination",Toast.LENGTH_SHORT).show(); return;
        }
        if (etaHour < 0) {
            Toast.makeText(this,"Set arrival time",Toast.LENGTH_SHORT).show(); return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, etaHour);
        cal.set(Calendar.MINUTE, etaMin);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() < System.currentTimeMillis())
            cal.add(Calendar.DAY_OF_YEAR, 1);

        final PrefsManager prefs = new PrefsManager(this);
        prefs.setJourneyActive(true);
        prefs.setJourneyEta(cal.getTimeInMillis());
        prefs.setJourneyDest(dest);
        prefs.setJourneyContactNumber(journeyContactNumber);
        prefs.setJourneyContactName(journeyContactName);
        prefs.setAutoTrackCaptureInterval(captureIntervalMin);
        prefs.setAutoTrackShareInterval(shareIntervalMin);

        // Ensure guard is active
        if (!prefs.isGuardActive()) {
            GuardService.start(this);
            prefs.setGuardActive(true);
        }

        // Start AutoTracker immediately on this activity's main thread
        AutoTracker tracker = AutoTracker.getInstance(this);
        tracker.clearLog();
        tracker.startTracking(captureIntervalMin,
            new AutoTracker.OnNewLocationCallback() {
                public void onNewLocation(AutoTracker.TrackPoint point, int total) {
                    prefs.saveLastLocation(point.lat, point.lng);
                }
            });

        // Send start SMS with first location
        final String alertNumber = prefs.getActiveAlertNumber();
        final String alertName   = prefs.getActiveAlertName();
        final int    cMin        = captureIntervalMin;
        final int    sMin        = shareIntervalMin;
        final int    fH          = etaHour;
        final int    fM          = etaMin;
        final String finalDest   = dest;

        new Thread(new Runnable() {
            public void run() {
                Location loc = LocationHelper.getFreshLocationBlocking(
                    JourneyActivity.this, 8);
                if (loc != null)
                    prefs.saveLastLocation(loc.getLatitude(), loc.getLongitude());

                String locPart = loc != null
                    ? "Start: " + LocationHelper.buildMapsLink(loc) + "\n"
                      + "Coords: " + String.format("%.5f", loc.getLatitude())
                      + ", " + String.format("%.5f", loc.getLongitude()) + "\n"
                    : "Start location: acquiring GPS...\n";

                String msg = "Journey started to " + finalDest + ".\n"
                    + "ETA: " + String.format("%02d:%02d", fH, fM) + "\n\n"
                    + locPart + "\n"
                    + "Auto Track ON:\n"
                    + "• GPS captured every " + cMin + " min\n"
                    + "• Location SMS every " + sMin + " min\n\n"
                    + CellLocator.buildCellSmsBlock(JourneyActivity.this)
                    + "\n— SilentGuard";
                SmsHelper.sendTo(alertNumber, msg);
            }
        }).start();

        Toast.makeText(this,
            "Journey started! AutoTrack ON — GPS every "
            + captureIntervalMin + "min, SMS every " + shareIntervalMin + "min",
            Toast.LENGTH_LONG).show();
        finish();
    }

    private Spinner makeSpinner(String[] items) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setBackgroundColor(0xFF1A1A26);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        p.bottomMargin = dp(4); sp.setLayoutParams(p);
        return sp;
    }

    private LinearLayout makeCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(0xFF1A1A26);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(16); c.setLayoutParams(p); return c;
    }

    private void addLabel(LinearLayout root, String text) {
        TextView lbl = new TextView(this); lbl.setText(text);
        lbl.setTextSize(10); lbl.setTextColor(0xFF8A8578);
        lbl.setPadding(0, 0, 0, dp(6)); root.addView(lbl);
    }

    private void addLabel2(LinearLayout root, String text) {
        TextView lbl = new TextView(this); lbl.setText(text);
        lbl.setTextSize(10); lbl.setTextColor(0xFF8A8578);
        lbl.setPadding(0, 0, 0, dp(4)); root.addView(lbl);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
