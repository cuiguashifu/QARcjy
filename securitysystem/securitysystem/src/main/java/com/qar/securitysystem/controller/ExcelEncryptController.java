package com.qar.securitysystem.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ExcelEncryptController {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }



    @PostMapping("/encrypt-excel")
    public Map<String, Object> encryptExcelFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "policy", defaultValue = "role:admin") String policy) {
        
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("\n========== 收到Excel加密请求 ==========");
        System.out.println("[接收] 文件名: " + file.getOriginalFilename());
        System.out.println("[接收] 文件大小: " + file.getSize() + " bytes");
        System.out.println("[接收] 策略: " + policy);
        
        try {
            // 读取Excel文件
            String excelContent = readExcelContent(file);
            System.out.println("[处理] Excel内容读取完成，长度: " + excelContent.length() + " 字符");
            
            // 加密数据
            System.out.println("[处理] 正在调用 BouncyCastle 引擎...");
            System.out.println("[处理] 正在生成随机 AES-256 密钥及 IV 向量...");
            
            // 1. 生成随机 AES-256 密钥
            KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();

            // 2. 生成随机 12 字节 IV
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            // 3. 使用 BouncyCastle 进行加密
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] cipherText = cipher.doFinal(excelContent.getBytes(StandardCharsets.UTF_8));

            // 4. 拼接 IV + CipherText
            byte[] encryptedDataWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedDataWithIv, iv.length, cipherText.length);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedDataWithIv);

            // 5. 模拟密钥封装
            String keyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            String wrappedKeyMock = "LABE_WRAP_BC:" + policy + ":" + keyBase64;

            System.out.println("[成功] Excel AES-256-GCM 加密完成 (BouncyCastle)！");
            System.out.println("[输出] 密钥 (Base64): " + keyBase64);
            System.out.println("[输出] 最终生成的 Base64 密文长度: " + encryptedBase64.length() + " 字符");
            System.out.println("[输出] " + wrappedKeyMock);
            System.out.println("======================================\n");

            response.put("code", 200);
            response.put("message", "Excel文件加密成功 (Powered by BouncyCastle)！");
            response.put("originalFileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("encryptedData", encryptedBase64);
            response.put("wrappedKey", wrappedKeyMock);
            response.put("algorithm", "AES-256-GCM-BC");
            response.put("preview", excelContent.substring(0, Math.min(500, excelContent.length())));

        } catch (Exception e) {
            System.err.println("[错误] 加密过程发生崩溃: " + e.getMessage());
            e.printStackTrace();

            response.put("code", 500);
            response.put("message", "加密过程出错：" + e.getMessage());
        }

        return response;
    }
    
    private String readExcelContent(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("=== Sheet: " + sheet.getSheetName() + " ===\n");
                
                for (Row row : sheet) {
                    StringBuilder rowContent = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        rowContent.append(cellValue + "\t");
                    }
                    if (rowContent.length() > 0) {
                        content.append(rowContent.toString().trim() + "\n");
                    }
                }
                content.append("\n");
            }
        }
        
        return content.toString();
    }
    
    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
