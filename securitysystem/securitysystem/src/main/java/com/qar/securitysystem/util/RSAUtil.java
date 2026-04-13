package com.qar.securitysystem.util;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtil {
    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String OAEP_SHA256 = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final OAEPParameterSpec OAEP_SHA256_SPEC = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public static String encodeKey(byte[] key) {
        return Base64.getEncoder().encodeToString(key);
    }

    public static byte[] decodeKey(String keyStr) {
        return Base64.getDecoder().decode(keyStr);
    }

    public static PublicKey getPublicKey(String base64PublicKey) {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeKey(base64PublicKey));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get public key", e);
        }
    }

    public static PrivateKey getPrivateKey(String base64PrivateKey) {
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodeKey(base64PrivateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get private key", e);
        }
    }

    public static byte[] encrypt(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(OAEP_SHA256);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256_SPEC);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    public static byte[] decrypt(byte[] data, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(OAEP_SHA256);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SHA256_SPEC);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }
}
