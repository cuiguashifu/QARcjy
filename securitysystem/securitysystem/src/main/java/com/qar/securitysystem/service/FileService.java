package com.qar.securitysystem.service;

import com.qar.securitysystem.controller.DecryptController;
import com.qar.securitysystem.controller.EncryptController;
import com.qar.securitysystem.dto.EncryptedFileResponse;
import com.qar.securitysystem.dto.EncryptedFileUploadRequest;
import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.util.IdUtil;
import com.qar.securitysystem.util.RSAUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
public class FileService {
    private final FileRecordRepository fileRecordRepository;
    private final UserRepository userRepository;
    private final EncryptController encryptController;
    private final DecryptController decryptController;

    public FileService(FileRecordRepository fileRecordRepository, UserRepository userRepository, EncryptController encryptController, DecryptController decryptController) {
        this.fileRecordRepository = fileRecordRepository;
        this.userRepository = userRepository;
        this.encryptController = encryptController;
        this.decryptController = decryptController;
    }

    public FileRecordResponse uploadAndEncrypt(UserEntity user, MultipartFile file, String policy) {
        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("file_read_failed", e);
        }

        // Store as plain text on server
        String plainDataBase64 = Base64.getEncoder().encodeToString(raw);

        FileRecordEntity r = new FileRecordEntity();
        r.setId(IdUtil.newId());
        String ownerKey = user.getPersonId() == null || user.getPersonId().isBlank() ? user.getId() : user.getPersonId();
        r.setOwnerId(ownerKey);
        r.setOriginalName(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        r.setContentType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType());
        r.setSizeBytes(raw.length);
        r.setPolicy(policy);
        r.setWrappedKey("PLAIN_TEXT"); // Indicates it's stored as plain text
        r.setEncryptedData(plainDataBase64); // Actually storing plain text
        r.setCreatedAt(Instant.now());

        fileRecordRepository.save(r);
        return toResponse(r);
    }

    public FileRecordResponse storeEncrypted(UserEntity user, EncryptedFileUploadRequest request) {
        // Even if the client sends "encrypted" data, we treat it as the data to store.
        // If the requirement is server stores plain text, we assume the client sends plain text here.
        FileRecordEntity r = new FileRecordEntity();
        r.setId(IdUtil.newId());
        String ownerKey = user.getPersonId() == null || user.getPersonId().isBlank() ? user.getId() : user.getPersonId();
        r.setOwnerId(ownerKey);
        r.setOriginalName(normalizeFilename(request.getOriginalName()));
        r.setContentType(request.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : request.getContentType());
        r.setSizeBytes(request.getSizeBytes() != null ? request.getSizeBytes() : 0L);
        r.setPolicy(request.getPolicy());
        r.setWrappedKey("PLAIN_TEXT");
        r.setEncryptedData(request.getEncryptedData()); // Storing as-is
        r.setCreatedAt(Instant.now());

        fileRecordRepository.save(r);
        return toResponse(r);
    }

    public List<FileRecordResponse> listMine(List<String> ownerIds) {
        return fileRecordRepository.findAllByOwnerIdInOrderByCreatedAtDesc(ownerIds).stream().map(this::toResponse).toList();
    }

    public List<FileRecordResponse> listAll() {
        return fileRecordRepository.findAll().stream().map(this::toResponse).toList();
    }

    public FileRecordEntity getRecordOrNull(String id) {
        return fileRecordRepository.findById(id).orElse(null);
    }

    public byte[] decryptForDownload(FileRecordEntity record) {
        // Since it's stored as plain text, we just decode the base64
        return Base64.getDecoder().decode(record.getEncryptedData());
    }

    public EncryptedFileResponse getEncryptedDataForUser(FileRecordEntity record, UserEntity user) {
        if (user.getPublicKey() == null || user.getPublicKey().isBlank()) {
            throw new IllegalStateException("user_public_key_missing");
        }

        byte[] plainData = Base64.getDecoder().decode(record.getEncryptedData());
        
        try {
            // 1. Generate random AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();
            
            // 2. Generate random IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            
            // 3. Encrypt data with AES-CBC
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
            byte[] encryptedData = aesCipher.doFinal(plainData);
            
            // 4. Combine IV + EncryptedData
            byte[] ivAndData = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, ivAndData, 0, iv.length);
            System.arraycopy(encryptedData, 0, ivAndData, iv.length, encryptedData.length);
            
            // 5. Encrypt AES key with User's RSA Public Key
            PublicKey pubKey = RSAUtil.getPublicKey(user.getPublicKey());
            byte[] encryptedAesKey = RSAUtil.encrypt(aesKey.getEncoded(), pubKey);
            
            EncryptedFileResponse resp = new EncryptedFileResponse();
            resp.setEncryptedData(Base64.getEncoder().encodeToString(ivAndData));
            resp.setWrappedKey(Base64.getEncoder().encodeToString(encryptedAesKey));
            resp.setOriginalName(record.getOriginalName());
            resp.setContentType(record.getContentType());
            resp.setPolicy(record.getPolicy());
            
            return resp;
        } catch (Exception e) {
            throw new RuntimeException("failed_to_encrypt_for_transmission", e);
        }
    }

    public EncryptedFileResponse getEncryptedData(FileRecordEntity record) {
        // This is the old method, we should probably not use it if we want transmission encryption
        EncryptedFileResponse resp = new EncryptedFileResponse();
        resp.setEncryptedData(record.getEncryptedData());
        resp.setWrappedKey(record.getWrappedKey());
        resp.setOriginalName(record.getOriginalName());
        resp.setContentType(record.getContentType());
        resp.setPolicy(record.getPolicy());
        return resp;
    }

    private FileRecordResponse toResponse(FileRecordEntity r) {
        FileRecordResponse resp = new FileRecordResponse();
        resp.setId(r.getId());
        resp.setOwnerId(r.getOwnerId());
        resp.setOriginalName(normalizeFilename(r.getOriginalName()));
        resp.setContentType(r.getContentType());
        resp.setSizeBytes(r.getSizeBytes());
        resp.setPolicy(r.getPolicy());
        resp.setWrappedKey(r.getWrappedKey());
        resp.setCreatedAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        return resp;
    }

    private static String normalizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "data.bin";
        }
        String cleaned = name.replace("\u0000", "").replace("\r", " ").replace("\n", " ").trim();
        if (cleaned.matches("(?i)^QAR\\?+\\.xlsx$")) {
            return "QAR示例数据.xlsx";
        }
        if (!cleaned.contains("?")) {
            return cleaned;
        }
        try {
            byte[] bytes = cleaned.getBytes(StandardCharsets.ISO_8859_1);
            String utf8 = new String(bytes, StandardCharsets.UTF_8).trim();
            if (!utf8.isBlank() && !utf8.contains("�")) {
                return utf8;
            }
        } catch (Exception e) {
        }
        return cleaned;
    }
}
