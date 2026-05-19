package com.silentguard.utils;

import android.telephony.PhoneNumberUtils;
import android.util.Log;

/**
 * PhoneValidator - Validates and sanitizes phone numbers
 * Prevents SMS injection and invalid number handling
 */
public class PhoneValidator {
    private static final String TAG = "PhoneValidator";

    // Minimum and maximum phone number lengths
    private static final int MIN_PHONE_LENGTH = 7;
    private static final int MAX_PHONE_LENGTH = 15;

    /**
     * Validate phone number format
     * - Must be 7-15 digits
     * - Can only contain digits, +, -, (), spaces
     * - No special characters or injection vectors
     */
    public static boolean isValidPhoneNumber(String number) {
        if (number == null || number.isEmpty()) {
            Log.w(TAG, "Phone number is null or empty");
            return false;
        }

        String cleaned = cleanPhoneNumber(number);

        // Check length
        if (cleaned.length() < MIN_PHONE_LENGTH || cleaned.length() > MAX_PHONE_LENGTH) {
            Log.w(TAG, "Phone number length invalid: " + cleaned.length());
            return false;
        }

        // Check if it's all digits or valid format
        if (!cleaned.matches("^[0-9+]*$")) {
            Log.w(TAG, "Phone number contains invalid characters");
            return false;
        }

        // Check for injection patterns
        if (containsInjectionPatterns(cleaned)) {
            Log.w(TAG, "Phone number contains suspicious patterns");
            return false;
        }

        Log.d(TAG, "Phone number validated successfully");
        return true;
    }

    /**
     * Clean phone number by removing formatting
     */
    public static String cleanPhoneNumber(String number) {
        if (number == null) return "";
        // Remove all non-digit characters except leading +
        return number.replaceAll("[^0-9+]", "");
    }

    /**
     * Check for known SMS injection patterns
     */
    private static boolean containsInjectionPatterns(String number) {
        // Block suspicious patterns
        String[] suspicious = {
            "&&", "||", ";", "|", "&", "%", "$", "{", "}", "[", "]",
            "javascript:", "data:", "file://", "content://", "\n", "\r", "\t"
        };

        for (String pattern : suspicious) {
            if (number.toLowerCase().contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate and return sanitized phone number
     * Returns null if invalid
     */
    public static String getSafePhoneNumber(String number) {
        if (!isValidPhoneNumber(number)) {
            return null;
        }
        return cleanPhoneNumber(number);
    }

    /**
     * Validate contact name (no special characters)
     */
    public static boolean isValidContactName(String name) {
        if (name == null || name.isEmpty() || name.length() > 100) {
            return false;
        }
        // Allow only alphanumeric, spaces, and basic punctuation
        return name.matches("^[a-zA-Z0-9\\s\\-.,'\\.]*$");
    }

    /**
     * Sanitize contact name
     */
    public static String sanitizeContactName(String name) {
        if (name == null) return "Unknown";
        if (!isValidContactName(name)) {
            // Remove invalid characters
            return name.replaceAll("[^a-zA-Z0-9\\s\\-.,'\\.]*", "");
        }
        return name;
    }

    /**
     * Format phone number for display
     */
    public static String formatPhoneNumber(String number) {
        String cleaned = cleanPhoneNumber(number);
        if (cleaned.length() < 10) {
            return cleaned;
        }
        // Basic formatting: +X XXX XXX XXXX
        if (cleaned.startsWith("+")) {
            return cleaned.substring(0, cleaned.length() - 10) + " " +
                   cleaned.substring(cleaned.length() - 10, cleaned.length() - 5) + " " +
                   cleaned.substring(cleaned.length() - 5);
        }
        return cleaned;
    }
}
