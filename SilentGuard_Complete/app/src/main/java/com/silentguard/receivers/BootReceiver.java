package com.silentguard.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.silentguard.services.GuardService;
import com.silentguard.utils.PrefsManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            PrefsManager prefs = new PrefsManager(context);
            if (prefs.isSetupDone() && prefs.isGuardActive()) {
                GuardService.start(context);
            }
        }
    }
}
