package com.qar.securitysystem.startup;

import com.qar.securitysystem.repo.QarTableRowRepository;
import com.qar.securitysystem.service.QarTableService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Component
public class QarTableSeeder implements ApplicationRunner {
    private final QarTableRowRepository repo;
    private final QarTableService service;
    private final ResourceLoader resourceLoader;

    public QarTableSeeder(QarTableRowRepository repo, QarTableService service, ResourceLoader resourceLoader) {
        this.repo = repo;
        this.service = service;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (repo.count() > 0) {
                return;
            }

            byte[] bytes = tryLoadSampleXlsxBytes();
            if (bytes == null || bytes.length == 0) {
                return;
            }

            String b64 = Base64.getEncoder().encodeToString(bytes);
            service.importXlsxBase64(b64);
        } catch (Exception e) {
        }
    }

    private byte[] tryLoadSampleXlsxBytes() {
        try {
            Resource r = resourceLoader.getResource("classpath:QAR示例数据.xlsx");
            if (r.exists()) {
                return r.getInputStream().readAllBytes();
            }
        } catch (Exception e) {
        }

        String[] candidates = new String[] {
                "QAR示例数据.xlsx",
                "../QAR示例数据.xlsx",
                "../../QAR示例数据.xlsx"
        };
        for (String c : candidates) {
            try {
                Path p = Path.of(c).toAbsolutePath().normalize();
                if (Files.exists(p)) {
                    return Files.readAllBytes(p);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }
}

