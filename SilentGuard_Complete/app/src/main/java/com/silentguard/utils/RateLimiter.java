package com.silentguard.utils;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * RateLimiter - Prevents SMS and alert spam
 * Implements cooldown timers for sensitive operations
 */
public class RateLimiter {
    private static final String TAG = "RateLimiter";
    private static final int DEFAULT_COOLDOWN_MS = 30000; // 30 seconds

    private static Map<String, Long> lastTriggerTime = new HashMap<>();

    /**
     * Check if action is allowed based on cooldown
     * @param actionKey Unique key for this action (e.g., "SOS", "SMS_ALERT")
     * @return true if cooldown has passed, false otherwise
     */
    public static boolean isAllowed(String actionKey) {
        return isAllowed(actionKey, DEFAULT_COOLDOWN_MS);
    }

    /**
     * Check if action is allowed with custom cooldown
     */
    public static boolean isAllowed(String actionKey, int cooldownMs) {
        if (actionKey == null || actionKey.isEmpty()) {
            Log.w(TAG, "Action key is null or empty");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastTriggerTime.get(actionKey);

        if (lastTime == null) {
            // First time this action is triggered
            lastTriggerTime.put(actionKey, currentTime);
            Log.d(TAG, "Action '" + actionKey + "' allowed (first trigger)");
            return true;
        }

        long timeSinceLastTrigger = currentTime - lastTime;
        boolean allowed = timeSinceLastTrigger >= cooldownMs;

        if (allowed) {
            lastTriggerTime.put(actionKey, currentTime);
            Log.d(TAG, "Action '" + actionKey + "' allowed (cooldown passed)");
        } else {
            long remainingMs = cooldownMs - timeSinceLastTrigger;
            Log.w(TAG, "Action '" + actionKey + "' blocked (cooldown: " + remainingMs + "ms remaining)");
        }

        return allowed;
    }

    /**
     * Get remaining cooldown time in milliseconds
     */
    public static long getRemainingCooldown(String actionKey, int cooldownMs) {
        if (actionKey == null) return 0;

        Long lastTime = lastTriggerTime.get(actionKey);
        if (lastTime == null) return 0;

        long timeSinceLastTrigger = System.currentTimeMillis() - lastTime;
        long remaining = cooldownMs - timeSinceLastTrigger;

        return Math.max(0, remaining);
    }

    /**
     * Force reset cooldown for an action
     */
    public static void reset(String actionKey) {
        if (actionKey != null) {
            lastTriggerTime.remove(actionKey);
            Log.d(TAG, "Cooldown reset for '" + actionKey + "'");
        }
    }

    /**
     * Reset all cooldowns
     */
    public static void resetAll() {
        lastTriggerTime.clear();
        Log.d(TAG, "All cooldowns reset");
    }
}
