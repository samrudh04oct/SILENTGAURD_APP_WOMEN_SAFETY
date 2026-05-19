package com.silentguard.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.silentguard.utils.AppConstants;
import java.util.ArrayList;
import java.util.List;

public class DisguiseActivity extends Activity {
    private int tapCount = 0;
    private long lastTapTime = 0;
    private LinearLayout notesContainer;
    private final List<String> notes = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFDE7);
        root.setPadding(dp(16), dp(24), dp(16), dp(16));

        final TextView title = new TextView(this);
        title.setText("  My Notes");
        title.setTextSize(26);
        title.setTextColor(0xFF4E342E);
        title.setPadding(0, 0, 0, dp(16));
        title.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if (now - lastTapTime > AppConstants.SECRET_TAP_WINDOW) tapCount = 0;
                tapCount++;
                lastTapTime = now;
                if (tapCount >= AppConstants.SECRET_TAP_COUNT) {
                    tapCount = 0;
                    startActivity(new Intent(DisguiseActivity.this, MainActivity.class));
                    overridePendingTransition(0, 0);
                }
            }
        });
        root.addView(title);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowP.bottomMargin = dp(12);
        inputRow.setLayoutParams(rowP);

        final EditText editText = new EditText(this);
        editText.setHint("Add a note...");
        editText.setTextColor(0xFF4E342E);
        editText.setHintTextColor(0xFFA1887F);
        editText.setBackgroundColor(0xFFFFF9C4);
        editText.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams etP = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etP.rightMargin = dp(8);
        editText.setLayoutParams(etP);

        Button addBtn = new Button(this);
        addBtn.setText("Add");
        addBtn.setBackgroundColor(0xFF795548);
        addBtn.setTextColor(0xFFFFFFFF);
        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String txt = editText.getText().toString().trim();
                if (!txt.isEmpty()) {
                    notes.add(0, txt);
                    editText.setText("");
                    renderNotes();
                }
            }
        });

        inputRow.addView(editText);
        inputRow.addView(addBtn);
        root.addView(inputRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        notesContainer = new LinearLayout(this);
        notesContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(notesContainer);
        root.addView(scroll);

        setContentView(root);

        notes.add("Remember to buy groceries");
        notes.add("Call mom on Sunday");
        notes.add("Meeting at 3pm tomorrow");
        renderNotes();
    }

    private void renderNotes() {
        notesContainer.removeAllViews();
        for (int i = 0; i < notes.size(); i++) {
            final int idx = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundColor(0xFFFFF9C4);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.bottomMargin = dp(8);
            card.setLayoutParams(cp);

            TextView tv = new TextView(this);
            tv.setText(notes.get(i));
            tv.setTextColor(0xFF4E342E);
            tv.setTextSize(14);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button del = new Button(this);
            del.setText("x");
            del.setTextColor(0xFFA1887F);
            del.setBackgroundColor(0x00000000);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    notes.remove(idx);
                    renderNotes();
                }
            });
            card.addView(tv);
            card.addView(del);
            notesContainer.addView(card);
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
