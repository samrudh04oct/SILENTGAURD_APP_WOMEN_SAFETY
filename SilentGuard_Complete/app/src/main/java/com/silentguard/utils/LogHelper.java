package com.silentguard.utils;

import android.util.Log;

/**
 * LogHelper - Safe logging that prevents sensitive data exposure
 * Masks personal information before logging
 */
public class LogHelper {
    private static final String TAG = "SilentGuard";

    // Enable/disable debug logging based on BuildConfig
    private static final boolean DEBUG = BuildConfig.DEBUG;

    /**
     * Safe log - masks sensitive data
     */
    public static void logDebug(String tag, String message) {
        if (DEBUG) {
            String safe = maskSensitiveData(message);
            Log.d(tag, safe);
        }
    }

    /**
     * Safe error logging
     */
    public static void logError(String tag, String message) {
        String safe = maskSensitiveData(message);
        Log.e(tag, safe);
    }

    /**
     * Safe warning logging
     */
    public static void logWarn(String tag, String message) {
        String safe = maskSensitiveData(message);
        Log.w(tag, safe);
    }

    /**
     * Log location with masked coordinates
     */
    public static void logLocationSafe(String tag, double lat, double lng) {
        if (DEBUG) {
            // Show only approximate location (2 decimal places)
            double maskedLat = Math.round(lat * 100.0) / 100.0;
            double maskedLng = Math.round(lng * 100.0) / 100.0;
            Log.d(tag, "Location: [MASKED]~" + maskedLat + "," + maskedLng);
        }
    }

    /**
     * Log phone number with masking
     */
    public static void logPhoneSafe(String tag, String phone) {
        if (DEBUG) {
            String masked = maskPhoneNumber(phone);
            Log.d(tag, "Contact: " + masked);
        }
    }

    /**
     * Mask coordinates (show only first 2 decimals)
     */
    private static String maskCoordinates(String coords) {
        if (coords == null) return "[NULL]";
        try {
            String[] parts = coords.split(",");
            if (parts.length == 2) {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                double maskedLat = Math.round(lat * 100.0) / 100.0;
                double maskedLng = Math.round(lng * 100.0) / 100.0;
                return "[MASKED]~" + maskedLat + "," + maskedLng;
            }
        } catch (Exception ignored) {}
        return "[INVALID]";
    }

    /**
     * Mask phone number (show only last 4 digits)
     */
    private static String maskPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "[EMPTY]";
        if (phone.length() <= 4) return "[SHORT]";
        return "***-" + phone.substring(phone.length() - 4);
    }

    /**
     * Remove sensitive data from message
     */
    private static String maskSensitiveData(String message) {
        if (message == null) return "[NULL]";

        // Mask phone numbers (sequences of 7+ digits)
        message = message.replaceAll("\\b\\d{7,15}\\b", "[PHONE]");

        // Mask coordinates (e.g., "12.345,56.789")
        message = message.replaceAll("\\d+\\.\\d+,\\s*\\d+\\.\\d+", "[COORDS]");

        // Mask URLs
        message = message.replaceAll("(https?|ftp)://[^\\s]+", "[URL]");

        // Mask email
        message = message.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[EMAIL]");

        // Mask IP addresses
        message = message.replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP]");

        return message;
    }

    /**
     * Build config debug constant (add to BuildConfig.java or use reflection)
     */
    public static class BuildConfig {
        public static final boolean DEBUG = false; // Set to true in debug builds
    }
}
