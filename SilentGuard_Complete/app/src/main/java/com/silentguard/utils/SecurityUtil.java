package com.silentguard.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;

/**
 * SecurityUtil - Encryption utilities for protecting sensitive data
 * Uses AES-256 GCM for authenticated encryption
 */
public class SecurityUtil {
    private static final String TAG = "SecurityUtil";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    // Encrypt text data
    public static String encryptText(String plainText, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] ciphertext = cipher.doFinal(plainText.getBytes());

            // Combine IV + ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            // Base64 encode
            return android.util.Base64.encodeToString(buffer.array(), android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed: " + e.getMessage());
            return null;
        }
    }

    // Decrypt text data
    public static String decryptText(String encryptedText, SecretKey key) {
        try {
            byte[] data = android.util.Base64.decode(encryptedText, android.util.Base64.NO_WRAP);

            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Decrypt
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed: " + e.getMessage());
            return null;
        }
    }

    // Encrypt file
    public static boolean encryptFile(File inputFile, File outputFile, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            FileInputStream fis = new FileInputStream(inputFile);
            FileOutputStream fos = new FileOutputStream(outputFile);

            // Write IV
            fos.write(iv);

            // Encrypt and write file
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(cipher.update(buffer, 0, read));
            }
            fos.write(cipher.doFinal());

            fis.close();
            fos.close();

            Log.d(TAG, "File encrypted successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "File encryption failed: " + e.getMessage());
            return false;
        }
    }

    // Decrypt file
    public static boolean decryptFile(File inputFile, File outputFile, SecretKey key) {
        try {
            FileInputStream fis = new FileInputStream(inputFile);

            // Read IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            fis.read(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            FileOutputStream fos = new FileOutputStream(outputFile);

            // Decrypt and write file
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(cipher.update(buffer, 0, read));
            }
            fos.write(cipher.doFinal());

            fis.close();
            fos.close();

            Log.d(TAG, "File decrypted successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "File decryption failed: " + e.getMessage());
            return false;
        }
    }

    // Generate AES-256 key
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            return keyGen.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "Key generation failed: " + e.getMessage());
            return null;
        }
    }

    // Check if text is sensitive (contains numbers, special chars)
    public static boolean isSensitiveData(String text) {
        return text != null && (text.matches(".*\\d+.*") || text.contains("+") || text.contains("@"));
    }
}
