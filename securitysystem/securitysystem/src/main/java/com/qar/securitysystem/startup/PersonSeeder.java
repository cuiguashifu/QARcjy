package com.qar.securitysystem.startup;

import com.qar.securitysystem.config.PersonSeedProperties;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class PersonSeeder implements ApplicationRunner {
    private final PersonRecordRepository personRecordRepository;
    private final PersonSeedProperties props;
    private final ResourceLoader resourceLoader;

    public PersonSeeder(PersonRecordRepository personRecordRepository, PersonSeedProperties props, ResourceLoader resourceLoader) {
        this.personRecordRepository = personRecordRepository;
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!props.isSeedEnabled()) {
            return;
        }
        if (personRecordRepository.count() > 0) {
            return;
        }
        Resource r = resourceLoader.getResource(props.getSeedCsv());
        if (!r.exists()) {
            return;
        }
        String content = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<String> lines = content.lines().toList();
        if (lines.isEmpty()) {
            return;
        }
        boolean first = true;
        for (String line : lines) {
            if (first) {
                first = false;
                continue;
            }
            String v = line.trim();
            if (v.isBlank()) {
                continue;
            }
            String[] parts = v.split(",", -1);
            if (parts.length < 3) {
                continue;
            }
            String personNo = parts[0].trim();
            String fullName = parts[1].trim();
            String idLast4 = parts[2].trim();
            String phone = parts.length >= 4 ? parts[3].trim() : "";
            String dept = parts.length >= 5 ? parts[4].trim() : "";
            String airline = parts.length >= 6 ? parts[5].trim() : "";
            String positionTitle = parts.length >= 7 ? parts[6].trim() : "";
            if (personNo.isBlank() || fullName.isBlank() || idLast4.isBlank()) {
                continue;
            }
            PersonRecordEntity e = new PersonRecordEntity();
            e.setId(IdUtil.newId());
            e.setPersonNo(personNo);
            e.setFullName(fullName);
            e.setIdLast4(idLast4);
            e.setPhone(phone.isBlank() ? null : phone);
            e.setDepartment(dept.isBlank() ? null : dept);
            e.setAirline(airline.isBlank() ? null : airline);
            e.setPositionTitle(positionTitle.isBlank() ? null : positionTitle);
            e.setCreatedAt(Instant.now());
            personRecordRepository.save(e);
        }
    }
}
