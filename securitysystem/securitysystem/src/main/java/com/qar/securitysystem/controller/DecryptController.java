package com.qar.securitysystem.controller;

import com.qar.securitysystem.service.ServerKeyPairService;
import com.qar.securitysystem.util.AesGcmUtil;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DecryptController {
    private static final int GCM_IV_LENGTH = 12;

    private final ServerKeyPairService serverKeyPairService;

    public DecryptController(ServerKeyPairService serverKeyPairService) {
        this.serverKeyPairService = serverKeyPairService;
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

            String wrappedKey = request.getOrDefault("wrappedKey", "");
            byte[] keyBytes;
            if (wrappedKey != null && !wrappedKey.isBlank()) {
                String privateKey = request.getOrDefault("privateKey", "");
                keyBytes = privateKey == null || privateKey.isBlank()
                        ? serverKeyPairService.unwrapKey(wrappedKey)
                        : serverKeyPairService.unwrapKeyWithPrivateKey(wrappedKey, privateKey);
            } else if (key != null && !key.isBlank()) {
                keyBytes = Base64.getDecoder().decode(key);
            } else {
                throw new IllegalArgumentException("未找到解密密钥，请检查wrappedKey格式！");
            }
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] encryptedDataWithIv = Base64.getDecoder().decode(encryptedData);
            if (encryptedDataWithIv.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("加密数据格式不正确，缺少IV！");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encryptedDataWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedDataWithIv, 0, iv, 0, iv.length);
            System.arraycopy(encryptedDataWithIv, iv.length, cipherText, 0, cipherText.length);

            byte[] plainText = AesGcmUtil.decrypt(secretKey, iv, cipherText, policy.getBytes(StandardCharsets.UTF_8));
            String decryptedData = new String(plainText, StandardCharsets.UTF_8);

            System.out.println("[成功] AES-256-GCM 解密完成 (BouncyCastle)！");
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

            String wrappedKey = request.getOrDefault("wrappedKey", "");
            String privateKey = request.getOrDefault("privateKey", "");
            if (wrappedKey.isBlank()) {
                throw new IllegalArgumentException("未找到Excel解密密钥，请检查wrappedKey格式！");
            }

            byte[] keyBytes = privateKey == null || privateKey.isBlank()
                    ? serverKeyPairService.unwrapKey(wrappedKey)
                    : serverKeyPairService.unwrapKeyWithPrivateKey(wrappedKey, privateKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] encryptedDataWithIv = Base64.getDecoder().decode(encryptedData);
            if (encryptedDataWithIv.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Excel加密数据格式不正确，缺少IV！");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encryptedDataWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedDataWithIv, 0, iv, 0, iv.length);
            System.arraycopy(encryptedDataWithIv, iv.length, cipherText, 0, cipherText.length);

            byte[] plainText = AesGcmUtil.decrypt(secretKey, iv, cipherText, policy.getBytes(StandardCharsets.UTF_8));
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
