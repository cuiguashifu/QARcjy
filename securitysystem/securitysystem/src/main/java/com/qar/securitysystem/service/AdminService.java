package com.qar.securitysystem.service;

import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.dto.FeedbackResponse;
import com.qar.securitysystem.dto.AccountRequestResponse;
import com.qar.securitysystem.dto.AuditLogResponse;
import com.qar.securitysystem.dto.UserResponse;
import com.qar.securitysystem.model.AccountRequestEntity;
import com.qar.securitysystem.model.AccountRequestStatus;
import com.qar.securitysystem.model.AuditLogEntity;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.FeedbackEntity;
import com.qar.securitysystem.model.FeedbackStatus;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.model.UserRole;
import com.qar.securitysystem.repo.AccountRequestRepository;
import com.qar.securitysystem.repo.AuditLogRepository;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.repo.FeedbackRepository;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final FileRecordRepository fileRecordRepository;
    private final FeedbackRepository feedbackRepository;
    private final PersonRecordRepository personRecordRepository;
    private final AccountRequestRepository accountRequestRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository, FileRecordRepository fileRecordRepository, FeedbackRepository feedbackRepository, PersonRecordRepository personRecordRepository, AccountRequestRepository accountRequestRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.fileRecordRepository = fileRecordRepository;
        this.feedbackRepository = feedbackRepository;
        this.personRecordRepository = personRecordRepository;
        this.accountRequestRepository = accountRequestRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    public List<FileRecordResponse> listAllFiles() {
        return fileRecordRepository.findAll().stream().map(this::toFileResponse).toList();
    }

    public List<FileRecordEntity> listAllFileEntities() {
        return fileRecordRepository.findAll();
    }

    public List<FeedbackResponse> listAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toFeedbackResponse).toList();
    }

    public FeedbackResponse updateFeedback(String id, String status, String adminReply) {
        FeedbackEntity e = feedbackRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not_found"));
        boolean touched = false;
        if (status != null && !status.isBlank()) {
            FeedbackStatus s = parseStatus(status);
            e.setStatus(s);
            touched = true;
        }
        if (adminReply != null) {
            String r = adminReply.trim();
            if (r.length() > 8000) {
                r = r.substring(0, 8000);
            }
            e.setAdminReply(r.isBlank() ? null : r);
            e.setRepliedAt(r.isBlank() ? null : Instant.now());
            touched = true;
        }
        if (touched) {
            e.setUpdatedAt(Instant.now());
            feedbackRepository.save(e);
        }
        return toFeedbackResponse(e);
    }

    public List<AccountRequestResponse> listPendingAccountRequests() {
        return accountRequestRepository.findAllByStatusOrderByCreatedAtDesc(AccountRequestStatus.PENDING).stream().map(this::toAccountRequestResponse).toList();
    }

    public AccountRequestResponse approveAccountRequest(String id, String adminId, String adminNote) {
        AccountRequestEntity e = accountRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not_found"));
        if (e.getStatus() != AccountRequestStatus.PENDING) {
            throw new IllegalArgumentException("request_not_pending");
        }
        if (userRepository.existsByAccount(e.getPersonNo())) {
            throw new IllegalArgumentException("user_already_exists");
        }
        PersonRecordEntity pr = personRecordRepository.findByPersonNo(e.getPersonNo()).orElse(null);
        if (pr == null) {
            throw new IllegalArgumentException("profile_not_found");
        }
        UserEntity u = new UserEntity();
        u.setId(IdUtil.newId());
        u.setAccount(e.getPersonNo());
        u.setPasswordHash(e.getPasswordHash());
        u.setPersonId(pr.getId());
        u.setRole(UserRole.USER);
        u.setCreatedAt(Instant.now());
        userRepository.save(u);

        e.setStatus(AccountRequestStatus.APPROVED);
        e.setReviewedAt(Instant.now());
        e.setReviewedByAdminId(adminId);
        e.setAdminNote(trimNote(adminNote));
        accountRequestRepository.save(e);
        return toAccountRequestResponse(e);
    }

    public AccountRequestResponse rejectAccountRequest(String id, String adminId, String adminNote) {
        AccountRequestEntity e = accountRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not_found"));
        if (e.getStatus() != AccountRequestStatus.PENDING) {
            throw new IllegalArgumentException("request_not_pending");
        }
        e.setStatus(AccountRequestStatus.REJECTED);
        e.setReviewedAt(Instant.now());
        e.setReviewedByAdminId(adminId);
        e.setAdminNote(trimNote(adminNote));
        accountRequestRepository.save(e);
        return toAccountRequestResponse(e);
    }

    public List<AuditLogResponse> listAuditLogs() {
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc().stream().map(this::toAuditLogResponse).toList();
    }

    private UserResponse toUserResponse(UserEntity u) {
        UserResponse resp = new UserResponse();
        resp.setId(u.getId());
        resp.setEmailOrUsername(u.getAccount());
        resp.setRole(u.getRole() == null ? null : u.getRole().name().toLowerCase());
        resp.setCreatedAt(u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        return resp;
    }

    private FileRecordResponse toFileResponse(FileRecordEntity r) {
        FileRecordResponse resp = new FileRecordResponse();
        resp.setId(r.getId());
        resp.setOwnerId(r.getOwnerId());
        PersonRecordEntity pr = personRecordRepository.findById(r.getOwnerId()).orElse(null);
        if (pr != null) {
            resp.setOwnerLabel(pr.getPersonNo() + " " + pr.getFullName());
        }
        resp.setOriginalName(r.getOriginalName());
        resp.setContentType(r.getContentType());
        resp.setSizeBytes(r.getSizeBytes());
        resp.setPolicy(r.getPolicy());
        resp.setWrappedKey(r.getWrappedKey());
        resp.setCreatedAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        return resp;
    }

    private FeedbackResponse toFeedbackResponse(FeedbackEntity e) {
        return com.qar.securitysystem.service.FeedbackService.toResponse(e);
    }

    private AccountRequestResponse toAccountRequestResponse(AccountRequestEntity e) {
        AccountRequestResponse r = new AccountRequestResponse();
        r.setId(e.getId());
        r.setPersonNo(e.getPersonNo());
        r.setFullName(e.getFullName());
        r.setAirline(e.getAirline());
        r.setPositionTitle(e.getPositionTitle());
        r.setDepartment(e.getDepartment());
        r.setContact(e.getContact());
        r.setStatus(e.getStatus() == null ? null : e.getStatus().name().toLowerCase());
        r.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        r.setReviewedAt(e.getReviewedAt() == null ? null : e.getReviewedAt().toString());
        r.setAdminNote(e.getAdminNote());
        return r;
    }

    private String trimNote(String note) {
        if (note == null) {
            return null;
        }
        String v = note.trim();
        if (v.isBlank()) {
            return null;
        }
        if (v.length() > 400) {
            v = v.substring(0, 400);
        }
        return v;
    }

    private AuditLogResponse toAuditLogResponse(AuditLogEntity e) {
        AuditLogResponse r = new AuditLogResponse();
        r.setId(e.getId());
        r.setPersonNo(e.getPersonNo());
        r.setMethod(e.getMethod());
        r.setPath(e.getPath());
        r.setStatusCode(e.getStatusCode());
        r.setDurationMs(e.getDurationMs());
        r.setIp(e.getIp());
        r.setUserAgent(e.getUserAgent());
        r.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        return r;
    }

    private FeedbackStatus parseStatus(String s) {
        String v = s.trim().toUpperCase().replace("-", "_");
        try {
            return FeedbackStatus.valueOf(v);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid_status");
        }
    }
}
