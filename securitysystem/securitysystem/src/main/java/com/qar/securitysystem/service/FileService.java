package com.qar.securitysystem.service;

import com.qar.securitysystem.dto.EncryptedFileResponse;
import com.qar.securitysystem.dto.EncryptedFileUploadRequest;
import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.util.AesGcmUtil;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class FileService {
    private static final String LEGACY_PLAIN = "PLAIN_TEXT";
    private static final int GCM_IV_LENGTH = 12;

    private final FileRecordRepository fileRecordRepository;
    private final UserRepository userRepository;
    private final ServerKeyPairService serverKeyPairService;

    public FileService(FileRecordRepository fileRecordRepository, UserRepository userRepository, ServerKeyPairService serverKeyPairService) {
        this.fileRecordRepository = fileRecordRepository;
        this.userRepository = userRepository;
        this.serverKeyPairService = serverKeyPairService;
    }

    public FileRecordResponse uploadAndEncrypt(UserEntity user, MultipartFile file, String policy) {
        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("file_read_failed", e);
        }

        StoredFilePayload stored = encryptForStorage(raw, policy);

        FileRecordEntity r = new FileRecordEntity();
        r.setId(IdUtil.newId());
        String ownerKey = user.getPersonId() == null || user.getPersonId().isBlank() ? user.getId() : user.getPersonId();
        r.setOwnerId(ownerKey);
        r.setOriginalName(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        r.setContentType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType());
        r.setSizeBytes(raw.length);
        r.setPolicy(policy);
        r.setWrappedKey(stored.wrappedKey());
        r.setEncryptedData(stored.encryptedDataBase64());
        r.setCreatedAt(Instant.now());

        fileRecordRepository.save(r);
        return toResponse(r);
    }

    public FileRecordResponse storeEncrypted(UserEntity user, EncryptedFileUploadRequest request) {
        byte[] plainData;
        try {
            plainData = Base64.getDecoder().decode(request.getEncryptedData());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid_file_payload", e);
        }

        StoredFilePayload stored = encryptForStorage(plainData, request.getPolicy());
        FileRecordEntity r = new FileRecordEntity();
        r.setId(IdUtil.newId());
        String ownerKey = user.getPersonId() == null || user.getPersonId().isBlank() ? user.getId() : user.getPersonId();
        r.setOwnerId(ownerKey);
        r.setOriginalName(normalizeFilename(request.getOriginalName()));
        r.setContentType(request.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : request.getContentType());
        r.setSizeBytes(request.getSizeBytes() != null ? request.getSizeBytes() : (long) plainData.length);
        r.setPolicy(request.getPolicy());
        r.setWrappedKey(stored.wrappedKey());
        r.setEncryptedData(stored.encryptedDataBase64());
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
        return decryptStoredData(record);
    }

    public EncryptedFileResponse getEncryptedDataForUser(FileRecordEntity record, UserEntity user) {
        if (user.getPublicKey() == null || user.getPublicKey().isBlank()) {
            throw new IllegalStateException("user_public_key_missing");
        }

        byte[] plainData = decryptStoredData(record);
        try {
            javax.crypto.SecretKey aesKey = AesGcmUtil.generateKey();
            byte[] iv = AesGcmUtil.newIv();
            byte[] encryptedData = AesGcmUtil.encrypt(aesKey, iv, plainData, buildFileAad(record.getPolicy()));

            EncryptedFileResponse resp = new EncryptedFileResponse();
            resp.setEncryptedData(Base64.getEncoder().encodeToString(joinIvAndCiphertext(iv, encryptedData)));
            resp.setWrappedKey(serverKeyPairService.wrapKeyForPublicKey(aesKey.getEncoded(), user.getPublicKey()));
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

    private StoredFilePayload encryptForStorage(byte[] plainData, String policy) {
        javax.crypto.SecretKey aesKey = AesGcmUtil.generateKey();
        byte[] iv = AesGcmUtil.newIv();
        byte[] ciphertext = AesGcmUtil.encrypt(aesKey, iv, plainData, buildFileAad(policy));
        return new StoredFilePayload(
                Base64.getEncoder().encodeToString(joinIvAndCiphertext(iv, ciphertext)),
                serverKeyPairService.wrapKey(aesKey.getEncoded())
        );
    }

    private byte[] decryptStoredData(FileRecordEntity record) {
        if (record == null || record.getEncryptedData() == null || record.getEncryptedData().isBlank()) {
            return new byte[0];
        }
        if (record.getWrappedKey() == null || record.getWrappedKey().isBlank() || LEGACY_PLAIN.equals(record.getWrappedKey())) {
            return Base64.getDecoder().decode(record.getEncryptedData());
        }

        byte[] keyBytes = serverKeyPairService.unwrapKey(record.getWrappedKey());
        SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");
        byte[] ivAndCiphertext = Base64.getDecoder().decode(record.getEncryptedData());
        if (ivAndCiphertext.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("invalid_stored_ciphertext");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[ivAndCiphertext.length - GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, iv.length);
        System.arraycopy(ivAndCiphertext, iv.length, ciphertext, 0, ciphertext.length);
        return AesGcmUtil.decrypt(aesKey, iv, ciphertext, buildFileAad(record.getPolicy()));
    }

    private static byte[] buildFileAad(String policy) {
        String safePolicy = policy == null ? "" : policy.trim();
        return safePolicy.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] joinIvAndCiphertext(byte[] iv, byte[] ciphertext) {
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return combined;
    }

    private record StoredFilePayload(String encryptedDataBase64, String wrappedKey) {
    }
}
