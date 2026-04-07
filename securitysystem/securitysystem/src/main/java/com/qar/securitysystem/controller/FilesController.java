package com.qar.securitysystem.controller;

import com.qar.securitysystem.dto.EncryptedFileResponse;
import com.qar.securitysystem.dto.EncryptedFileUploadRequest;
import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.security.AppPrincipal;
import com.qar.securitysystem.service.FileService;
import com.qar.securitysystem.util.SecurityUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FilesController {
    private final FileService fileService;
    private final UserRepository userRepository;
    private final PersonRecordRepository personRecordRepository;

    public FilesController(FileService fileService, UserRepository userRepository, PersonRecordRepository personRecordRepository) {
        this.fileService = fileService;
        this.userRepository = userRepository;
        this.personRecordRepository = personRecordRepository;
    }

    @Deprecated
    @PostMapping
    public ResponseEntity<FileRecordResponse> upload(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "policy", required = false) String policy,
            @RequestParam(value = "personNo", required = false) String personNo
    ) {
        AppPrincipal p = SecurityUtil.requirePrincipal(authentication);
        boolean isAdmin = p.getRole().name().equals("ADMIN");
        if (!isAdmin) {
            return ResponseEntity.status(403).build();
        }
        UserEntity uploader = userRepository.findById(p.getUserId()).orElseThrow();
        UserEntity target = new UserEntity();
        target.setId(uploader.getId());
        if (personNo != null && !personNo.isBlank()) {
            PersonRecordEntity pr = personRecordRepository.findByPersonNo(personNo.trim()).orElse(null);
            if (pr == null) {
                return ResponseEntity.badRequest().build();
            }
            target.setPersonId(pr.getId());
        } else {
            target.setPersonId(uploader.getPersonId());
        }
        FileRecordResponse resp = fileService.uploadAndEncrypt(target, file, policy);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/encrypted")
    public ResponseEntity<FileRecordResponse> uploadEncrypted(
            Authentication authentication,
            @RequestBody EncryptedFileUploadRequest request
    ) {
        AppPrincipal p = SecurityUtil.requirePrincipal(authentication);
        boolean isAdmin = p.getRole().name().equals("ADMIN");
        if (!isAdmin) {
            return ResponseEntity.status(403).build();
        }
        
        UserEntity uploader = userRepository.findById(p.getUserId()).orElseThrow();
        UserEntity target = new UserEntity();
        target.setId(uploader.getId());
        target.setPersonId(uploader.getPersonId());
        
        FileRecordResponse resp = fileService.storeEncrypted(target, request);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<List<FileRecordResponse>> listMine(Authentication authentication) {
        AppPrincipal p = SecurityUtil.requirePrincipal(authentication);
        return ResponseEntity.ok(fileService.listAll());
    }

    @Deprecated
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(Authentication authentication, @PathVariable("id") String id) {
        AppPrincipal p = SecurityUtil.requirePrincipal(authentication);
        FileRecordEntity r = fileService.getRecordOrNull(id);
        if (r == null) {
            return ResponseEntity.status(404).build();
        }
        boolean isAdmin = p.getRole().name().equals("ADMIN");
        if (!isAdmin && !r.getOwnerId().equals(p.getUserId()) && (p.getPersonId() == null || !r.getOwnerId().equals(p.getPersonId()))) {
            return ResponseEntity.status(403).build();
        }
        byte[] raw = fileService.decryptForDownload(r);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename(r.getOriginalName()) + "\"")
                .contentType(MediaType.parseMediaType(r.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : r.getContentType()))
                .body(raw);
    }

    @GetMapping("/{id}/encrypted")
    public ResponseEntity<EncryptedFileResponse> downloadEncrypted(Authentication authentication, @PathVariable("id") String id) {
        AppPrincipal p = SecurityUtil.requirePrincipal(authentication);
        FileRecordEntity r = fileService.getRecordOrNull(id);
        if (r == null) {
            return ResponseEntity.status(404).build();
        }
        
        EncryptedFileResponse resp = fileService.getEncryptedData(r);
        return ResponseEntity.ok(resp);
    }

    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "download.bin";
        }
        return name.replace("\n", " ").replace("\r", " ");
    }
}
