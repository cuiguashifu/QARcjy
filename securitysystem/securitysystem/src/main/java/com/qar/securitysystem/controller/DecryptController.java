package com.qar.securitysystem.controller;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DecryptController {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }



    @PostMapping("/decrypt")
    public Map<String, Object> processDecryption(@RequestBody Map<String, String> request) {
        String encryptedData = request.getOrDefault("encryptedData", "");
        String key = request.getOrDefault("key", "");
        String policy = request.getOrDefault("policy", "未收到策略");

        System.out.println("\n========== 收到新的解密请求 ==========");
        System.out.println("[接收] 加密数据长度: " + encryptedData.length() + " 字符");
        System.out.println("[接收] 绑定的 L-ABE 策略: " + policy);

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("[处理] 正在调用 BouncyCastle 引擎...");
            System.out.println("[处理] 正在准备解密...");

            // 1. 提取密钥 (从 wrappedKey 或直接传参)
            String wrappedKey = request.getOrDefault("wrappedKey", "");
            String keyBase64 = "";
            if (wrappedKey.startsWith("LABE_WRAP_BC:")) {
                String[] parts = wrappedKey.split(":");
                if (parts.length >= 3) {
                    keyBase64 = parts[2];
                }
            } else {
                keyBase64 = key; // 兼容旧版或直接传key的情况
            }

            if (keyBase64.isEmpty()) {
                throw new IllegalArgumentException("未找到解密密钥，请检查wrappedKey格式！");
            }

            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // 2. 解码Base64加密数据
            byte[] encryptedDataWithIv = Base64.getDecoder().decode(encryptedData);
            
            // 3. 提取IV (12字节) 和 密文
            if (encryptedDataWithIv.length < 12) {
                throw new IllegalArgumentException("加密数据格式不正确，缺少IV！");
            }
            byte[] iv = new byte[12];
            byte[] cipherText = new byte[encryptedDataWithIv.length - 12];
            System.arraycopy(encryptedDataWithIv, 0, iv, 0, iv.length);
            System.arraycopy(encryptedDataWithIv, iv.length, cipherText, 0, cipherText.length);

            // 4. 使用 BouncyCastle 进行解密
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            byte[] plainText = cipher.doFinal(cipherText);
            
            String decryptedData = new String(plainText, StandardCharsets.UTF_8);

            System.out.println("[成功] AES-256-GCM 解密完成 (BouncyCastle)！");
            System.out.println("[输出] 密钥 (Base64): " + keyBase64);
            System.out.println("[输出] 解密后数据长度: " + decryptedData.length() + " 字符");
            System.out.println("[输出] 解密后数据预览: " + decryptedData.substring(0, Math.min(100, decryptedData.length())) + "...");
            System.out.println("======================================\n");

            response.put("code", 200);
            response.put("message", "真实 AES-256 解密成功 (Powered by BouncyCastle)！");
            response.put("decryptedData", decryptedData);
            response.put("policy", policy);

        } catch (Exception e) {
            System.err.println("[错误] 解密过程发生崩溃: " + e.getMessage());
            e.printStackTrace();

            response.put("code", 500);
            response.put("message", "解密过程出错：" + e.getMessage());
        }

        return response;
    }
    
    @PostMapping("/decrypt-excel")
    public Map<String, Object> decryptExcelData(@RequestBody Map<String, String> request) {
        String encryptedData = request.getOrDefault("encryptedData", "");
        String policy = request.getOrDefault("policy", "未收到策略");

        System.out.println("\n========== 收到Excel解密请求 ==========");
        System.out.println("[接收] 加密数据长度: " + encryptedData.length() + " 字符");
        System.out.println("[接收] 绑定的 L-ABE 策略: " + policy);

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("[处理] 正在调用 BouncyCastle 引擎...");
            System.out.println("[处理] 正在准备解密Excel数据...");

            // 1. 提取密钥
            String wrappedKey = request.getOrDefault("wrappedKey", "");
            String keyBase64 = "";
            if (wrappedKey.startsWith("LABE_WRAP_BC:")) {
                String[] parts = wrappedKey.split(":");
                if (parts.length >= 3) {
                    keyBase64 = parts[2];
                }
            }
            
            if (keyBase64.isEmpty()) {
                throw new IllegalArgumentException("未找到Excel解密密钥，请检查wrappedKey格式！");
            }

            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // 2. 解码Base64加密数据
            byte[] encryptedDataWithIv = Base64.getDecoder().decode(encryptedData);
            
            // 3. 提取IV和密文
            if (encryptedDataWithIv.length < 12) {
                throw new IllegalArgumentException("Excel加密数据格式不正确，缺少IV！");
            }
            byte[] iv = new byte[12];
            byte[] cipherText = new byte[encryptedDataWithIv.length - 12];
            System.arraycopy(encryptedDataWithIv, 0, iv, 0, iv.length);
            System.arraycopy(encryptedDataWithIv, iv.length, cipherText, 0, cipherText.length);

            // 4. 使用 BouncyCastle 进行解密
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            byte[] plainText = cipher.doFinal(cipherText);
            
            String decryptedData = new String(plainText, StandardCharsets.UTF_8);

            System.out.println("[成功] Excel数据 AES-256-GCM 解密完成 (BouncyCastle)！");
            System.out.println("[输出] 解密后数据预览: " + decryptedData.substring(0, Math.min(200, decryptedData.length())) + "...");
            System.out.println("======================================\n");

            response.put("code", 200);
            response.put("message", "Excel数据解密成功 (Powered by BouncyCastle)！");
            response.put("decryptedData", decryptedData);
            response.put("preview", decryptedData.substring(0, Math.min(500, decryptedData.length())));
            response.put("policy", policy);

        } catch (Exception e) {
            System.err.println("[错误] 解密过程发生崩溃: " + e.getMessage());
            e.printStackTrace();

            response.put("code", 500);
            response.put("message", "解密过程出错：" + e.getMessage());
        }

        return response;
    }
}