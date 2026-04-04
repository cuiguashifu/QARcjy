package com.qar.securitysystem.controller;

import com.qar.securitysystem.dto.DepartmentUpdateRequest;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.security.AppPrincipal;
import com.qar.securitysystem.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileDepartmentController {
    private final PersonRecordRepository personRecordRepository;

    public ProfileDepartmentController(PersonRecordRepository personRecordRepository) {
        this.personRecordRepository = personRecordRepository;
    }

    @PatchMapping("/department")
    public ResponseEntity<?> updateDepartment(Authentication authentication, @RequestBody DepartmentUpdateRequest req) {
        AppPrincipal p = SecurityUtil.requirePrincipal(authentication);
        if (p.getPersonId() == null || p.getPersonId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "profile_not_found"));
        }
        String dept = req == null ? "" : (req.getDepartment() == null ? "" : req.getDepartment().trim());
        if (dept.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "department_required"));
        }
        if (dept.length() > 120) {
            dept = dept.substring(0, 120);
        }
        PersonRecordEntity pr = personRecordRepository.findById(p.getPersonId()).orElse(null);
        if (pr == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "profile_not_found"));
        }
        pr.setDepartment(dept);
        personRecordRepository.save(pr);
        return ResponseEntity.ok(Map.of("code", 200, "message", "ok"));
    }
}
