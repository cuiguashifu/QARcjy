package com.qar.securitysystem.controller;

import com.qar.securitysystem.dto.FlightXlsxFileResponse;
import com.qar.securitysystem.dto.FlightXlsxPreviewResponse;
import com.qar.securitysystem.service.FlightXlsxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/flight-xlsx")
public class AdminFlightXlsxController {
    private final FlightXlsxService service;

    public AdminFlightXlsxController(FlightXlsxService service) {
        this.service = service;
    }

    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        try {
            List<FlightXlsxFileResponse> files = service.listXlsxFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/files/{id}/preview")
    public ResponseEntity<?> preview(@PathVariable("id") String id,
                                     @RequestParam(value = "maxRows", required = false, defaultValue = "200") int maxRows) {
        try {
            FlightXlsxPreviewResponse resp = service.previewFile(id, maxRows);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }
}

