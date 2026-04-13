package com.qar.securitysystem.controller;

import com.qar.securitysystem.dto.QarTableRowRequest;
import com.qar.securitysystem.dto.QarTableRowResponse;
import com.qar.securitysystem.dto.QarXlsxBase64Request;
import com.qar.securitysystem.dto.QarXlsxPreviewResponse;
import com.qar.securitysystem.service.QarTableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/qar-table")
public class AdminQarTableController {
    private final QarTableService service;

    public AdminQarTableController(QarTableService service) {
        this.service = service;
    }

    @GetMapping("/rows")
    public ResponseEntity<?> list(@RequestParam(value = "sortBy", required = false) String sortBy,
                                  @RequestParam(value = "sortDir", required = false) String sortDir) {
        try {
            List<QarTableRowResponse> rows = service.list(sortBy, sortDir);
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/rows")
    public ResponseEntity<?> create(@RequestBody QarTableRowRequest req) {
        try {
            return ResponseEntity.ok(service.create(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PutMapping("/rows/{id}")
    public ResponseEntity<?> update(@PathVariable("id") String id, @RequestBody QarTableRowRequest req) {
        try {
            return ResponseEntity.ok(service.update(id, req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/xlsx/preview")
    public ResponseEntity<?> preview(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "maxRows", required = false, defaultValue = "50") int maxRows) {
        try {
            QarXlsxPreviewResponse resp = service.previewXlsx(file, maxRows);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/xlsx/import")
    public ResponseEntity<?> importXlsx(@RequestParam("file") MultipartFile file) {
        try {
            int count = service.importXlsx(file);
            return ResponseEntity.ok(Map.of("code", 200, "imported", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/xlsx/preview-b64")
    public ResponseEntity<?> previewB64(@RequestBody QarXlsxBase64Request req,
                                        @RequestParam(value = "maxRows", required = false, defaultValue = "50") int maxRows) {
        try {
            QarXlsxPreviewResponse resp = service.previewXlsxBase64(req == null ? null : req.getDataBase64(), maxRows);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/xlsx/import-b64")
    public ResponseEntity<?> importB64(@RequestBody QarXlsxBase64Request req) {
        try {
            int count = service.importXlsxBase64(req == null ? null : req.getDataBase64());
            return ResponseEntity.ok(Map.of("code", 200, "imported", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }
}
