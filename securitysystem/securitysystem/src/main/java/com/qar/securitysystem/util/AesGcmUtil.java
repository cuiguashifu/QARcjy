package com.qar.securitysystem.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class AesGcmUtil {
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;

    public static byte[] encrypt(SecretKey key, byte[] iv, byte[] plaintext, byte[] aad) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    public static byte[] decrypt(SecretKey key, byte[] iv, byte[] ciphertext, byte[] aad) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }
}

