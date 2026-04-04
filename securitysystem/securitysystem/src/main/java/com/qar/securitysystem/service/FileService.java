package com.qar.securitysystem.service;

import com.qar.securitysystem.controller.DecryptController;
import com.qar.securitysystem.controller.EncryptController;
import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileService {
    private final FileRecordRepository fileRecordRepository;
    private final EncryptController encryptController;
    private final DecryptController decryptController;

    public FileService(FileRecordRepository fileRecordRepository, EncryptController encryptController, DecryptController decryptController) {
        this.fileRecordRepository = fileRecordRepository;
        this.encryptController = encryptController;
        this.decryptController = decryptController;
    }

    public FileRecordResponse uploadAndEncrypt(UserEntity user, MultipartFile file, String policy) {
        if (policy == null || policy.isBlank()) {
            policy = "role:user";
        }

        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("file_read_failed", e);
        }

        String base64Original = Base64.getEncoder().encodeToString(raw);
        Map<String, String> req = new HashMap<>();
        req.put("data", base64Original);
        req.put("policy", policy);
        Map<String, Object> crypto = encryptController.processEncryption(req);

        Object codeObj = crypto.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : 500;
        if (code != 200) {
            throw new IllegalStateException("encrypt_failed");
        }

        String encryptedData = (String) crypto.get("encryptedData");
        String wrappedKey = (String) crypto.get("wrappedKey");
        if (encryptedData == null || encryptedData.isBlank()) {
            throw new IllegalStateException("encrypt_no_ciphertext");
        }

        FileRecordEntity r = new FileRecordEntity();
        r.setId(IdUtil.newId());
        String ownerKey = user.getPersonId() == null || user.getPersonId().isBlank() ? user.getId() : user.getPersonId();
        r.setOwnerId(ownerKey);
        r.setOriginalName(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        r.setContentType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType());
        r.setSizeBytes(raw.length);
        r.setPolicy(policy);
        r.setWrappedKey(wrappedKey);
        r.setEncryptedData(encryptedData);
        r.setCreatedAt(Instant.now());

        fileRecordRepository.save(r);
        return toResponse(r);
    }

    public List<FileRecordResponse> listMine(List<String> ownerIds) {
        return fileRecordRepository.findAllByOwnerIdInOrderByCreatedAtDesc(ownerIds).stream().map(this::toResponse).toList();
    }

    public FileRecordEntity getRecordOrNull(String id) {
        return fileRecordRepository.findById(id).orElse(null);
    }

    public byte[] decryptForDownload(FileRecordEntity record) {
        Map<String, String> req = new HashMap<>();
        req.put("encryptedData", record.getEncryptedData());
        req.put("key", "");
        req.put("policy", record.getPolicy() == null ? "" : record.getPolicy());
        Map<String, Object> out = decryptController.processDecryption(req);

        Object codeObj = out.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : 500;
        if (code != 200) {
            throw new IllegalStateException("decrypt_failed");
        }

        String decryptedData = (String) out.get("decryptedData");
        if (decryptedData == null) {
            throw new IllegalStateException("decrypt_no_plaintext");
        }

        return Base64.getDecoder().decode(decryptedData);
    }

    private FileRecordResponse toResponse(FileRecordEntity r) {
        FileRecordResponse resp = new FileRecordResponse();
        resp.setId(r.getId());
        resp.setOwnerId(r.getOwnerId());
        resp.setOriginalName(r.getOriginalName());
        resp.setContentType(r.getContentType());
        resp.setSizeBytes(r.getSizeBytes());
        resp.setPolicy(r.getPolicy());
        resp.setWrappedKey(r.getWrappedKey());
        resp.setCreatedAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        return resp;
    }
}
