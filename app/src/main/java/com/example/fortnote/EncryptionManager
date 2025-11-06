package com.example.fortnote;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {

    private static final int SALT_LENGTH = 16; // 16 bytes
    private static final int IV_LENGTH = 12;   // recommended for GCM
    private static final int KEY_LENGTH = 256; // AES-256
    private static final int ITERATIONS = 65536;

    // Generate a SecretKey using password + salt (PBKDF2)
    private static SecretKey getKeyFromPassword(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    // Encrypt text using AES-GCM with random salt + IV
    public static String encrypt(String plainText, String password) throws Exception {
        // 1. Generate random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // 2. Derive AES key
        SecretKey key = getKeyFromPassword(password, salt);

        // 3. Generate random IV
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 4. Initialize AES-GCM cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

        // 5. Encrypt
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 6. Combine salt + IV + ciphertext into one byte array
        byte[] combined = new byte[salt.length + iv.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length + iv.length, encryptedBytes.length);

        // 7. Convert to Base64 string for storage
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    // Decrypt using stored salt + IV + ciphertext
    public static String decrypt(String cipherText, String password) throws Exception {
        byte[] combined = Base64.decode(cipherText, Base64.NO_WRAP);

        // 1. Extract salt
        byte[] salt = new byte[SALT_LENGTH];
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

        // 2. Extract IV
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH);

        // 3. Extract encrypted data
        int encryptedSize = combined.length - SALT_LENGTH - IV_LENGTH;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, encryptedBytes, 0, encryptedSize);

        // 4. Regenerate key from password + salt
        SecretKey key = getKeyFromPassword(password, salt);

        // 5. Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

        // 6. Decrypt
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
