package com.qar.securitysystem.controller;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EncryptController {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @PostMapping("/encrypt")
    public Map<String, Object> processEncryption(@RequestBody Map<String, String> request) {
        String originalData = request.getOrDefault("data", "未收到数据");
        String policy = request.getOrDefault("policy", "未收到策略");

        // ==========================================
        // 在 IDEA 控制台醒目地打印接收到的数据
        // ==========================================
        System.out.println("\n========== 收到新的加密请求 ==========");
        System.out.println("[接收] 待加密的 QAR 数据: " + originalData);
        System.out.println("[接收] 绑定的 L-ABE 策略: " + policy);

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("[处理] 正在调用 BouncyCastle 引擎...");
            System.out.println("[处理] 正在生成随机 AES-256 密钥及 IV 向量...");

            // 1. 生成随机 AES-256 密钥
            KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();

            // 2. 生成随机 12 字节 IV (GCM 推荐长度)
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            // 3. 使用 BouncyCastle 进行加密
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] cipherText = cipher.doFinal(originalData.getBytes(StandardCharsets.UTF_8));

            // 4. 拼接 IV + CipherText
            byte[] encryptedDataWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedDataWithIv, iv.length, cipherText.length);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedDataWithIv);

            // 5. 模拟密钥封装 (实际应使用 L-ABE，这里将密钥 Base64 包装后返回)
            String keyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            String wrappedKeyMock = "LABE_WRAP_BC:" + policy + ":" + keyBase64;

            // ==========================================
            // 在 IDEA 控制台打印加密后的硬核结果
            // ==========================================
            System.out.println("[成功] AES-256-GCM 加密完成 (BouncyCastle)！");
            System.out.println("[输出] 密钥 (Base64): " + keyBase64);
            System.out.println("[输出] 最终生成的 Base64 密文: " + encryptedBase64);
            System.out.println("[输出] " + wrappedKeyMock);
            System.out.println("======================================\n");

            response.put("code", 200);
            response.put("message", "真实 AES-256 加密成功 (Powered by BouncyCastle)！");
            response.put("encryptedData", encryptedBase64);
            response.put("wrappedKey", wrappedKeyMock);
            response.put("algorithm", "AES-256-GCM-BC");

        } catch (Exception e) {
            System.err.println("[错误] 加密过程发生崩溃: " + e.getMessage());
            e.printStackTrace();

            response.put("code", 500);
            response.put("message", "加密过程出错：" + e.getMessage());
        }

        return response;
    }
}