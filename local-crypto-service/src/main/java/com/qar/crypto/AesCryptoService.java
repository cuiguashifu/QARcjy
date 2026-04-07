package com.qar.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AesCryptoService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;

    private final KeyStore keyStore;

    public AesCryptoService() {
        this.keyStore = new KeyStore();
    }

    public Map<String, Object> encrypt(String plaintext, String policy) throws Exception {
        System.out.println("\n========== 加密请求 ==========");
        System.out.println("[输入] 明文长度: " + plaintext.length() + " 字符");
        System.out.println("[输入] 策略: " + policy);

        SecretKey aesKey = generateAesKey();
        byte[] iv = generateIv();

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM, "BC");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = cipher.doFinal(plaintextBytes);

        byte[] encryptedDataWithIv = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, encryptedDataWithIv, 0, IV_LENGTH);
        System.arraycopy(ciphertext, 0, encryptedDataWithIv, IV_LENGTH, ciphertext.length);

        String encryptedDataBase64 = Base64.getEncoder().encodeToString(encryptedDataWithIv);

        String wrappedKey = wrapKey(aesKey, policy);

        keyStore.storeKey(wrappedKey, aesKey);

        System.out.println("[成功] AES-256-GCM 加密完成");
        System.out.println("[输出] 密文长度: " + encryptedDataBase64.length() + " 字符");
        System.out.println("[输出] WrappedKey: " + wrappedKey);
        System.out.println("================================");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "加密成功");
        result.put("encryptedData", encryptedDataBase64);
        result.put("wrappedKey", wrappedKey);
        result.put("policy", policy);
        result.put("algorithm", "AES-256-GCM");

        return result;
    }

    public Map<String, Object> decrypt(String encryptedDataBase64, String wrappedKey, String policy) throws Exception {
        System.out.println("\n========== 解密请求 ==========");
        System.out.println("[输入] 密文长度: " + encryptedDataBase64.length() + " 字符");
        System.out.println("[输入] WrappedKey: " + wrappedKey);
        System.out.println("[输入] 策略: " + policy);

        SecretKey aesKey = unwrapKey(wrappedKey, policy);

        if (aesKey == null) {
            throw new IllegalStateException("无法获取解密密钥，请检查WrappedKey或策略");
        }

        byte[] encryptedDataWithIv = Base64.getDecoder().decode(encryptedDataBase64);

        byte[] iv = new byte[IV_LENGTH];
        byte[] ciphertext = new byte[encryptedDataWithIv.length - IV_LENGTH];
        System.arraycopy(encryptedDataWithIv, 0, iv, 0, IV_LENGTH);
        System.arraycopy(encryptedDataWithIv, IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM, "BC");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

        byte[] plaintextBytes = cipher.doFinal(ciphertext);
        String plaintext = new String(plaintextBytes, StandardCharsets.UTF_8);

        System.out.println("[成功] AES-256-GCM 解密完成");
        System.out.println("[输出] 明文长度: " + plaintext.length() + " 字符");
        System.out.println("================================");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "解密成功");
        result.put("decryptedData", plaintext);
        result.put("policy", policy);

        return result;
    }

    private SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
        keyGenerator.init(KEY_LENGTH, new SecureRandom());
        return keyGenerator.generateKey();
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private String wrapKey(SecretKey key, String policy) {
        String keyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());
        return "LABE_WRAP:" + policy + ":" + keyBase64.substring(0, 16) + "_LABE_RESERVED";
    }

    private SecretKey unwrapKey(String wrappedKey, String policy) {
        if (wrappedKey == null || !wrappedKey.startsWith("LABE_WRAP:")) {
            System.err.println("[警告] WrappedKey格式不正确，尝试从本地存储获取密钥");
            return keyStore.getKey(wrappedKey);
        }

        return keyStore.getKey(wrappedKey);
    }

    public int getKeyCount() {
        return keyStore.getKeyCount();
    }

    public java.util.List<String> getStoredKeys() {
        return keyStore.getStoredKeys();
    }
}
