package com.qar.securitysystem.controller;

import com.qar.securitysystem.service.ServerKeyPairService;
import com.qar.securitysystem.util.AesGcmUtil;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EncryptController {
    private final ServerKeyPairService serverKeyPairService;

    public EncryptController(ServerKeyPairService serverKeyPairService) {
        this.serverKeyPairService = serverKeyPairService;
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

            javax.crypto.SecretKey secretKey = AesGcmUtil.generateKey();
            byte[] iv = AesGcmUtil.newIv();
            byte[] cipherText = AesGcmUtil.encrypt(secretKey, iv, originalData.getBytes(StandardCharsets.UTF_8), policy.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedDataWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedDataWithIv, iv.length, cipherText.length);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedDataWithIv);

            String publicKey = request.get("publicKey");
            String wrappedKey = (publicKey == null || publicKey.isBlank())
                    ? serverKeyPairService.wrapKey(secretKey.getEncoded())
                    : serverKeyPairService.wrapKeyForPublicKey(secretKey.getEncoded(), publicKey.trim());

            System.out.println("[成功] AES-256-GCM 加密完成 (BouncyCastle)！");
            System.out.println("[输出] 最终生成的 Base64 密文: " + encryptedBase64);
            System.out.println("[输出] " + wrappedKey);
            System.out.println("======================================\n");

            response.put("code", 200);
            response.put("message", "真实 AES-256 加密成功 (Powered by BouncyCastle)！");
            response.put("encryptedData", encryptedBase64);
            response.put("wrappedKey", wrappedKey);
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
