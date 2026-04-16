package com.qar.securitysystem.startup;

import com.qar.securitysystem.config.PersonSeedProperties;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.util.IdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PersonSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PersonSeeder.class);
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
        seedMissingPersons();
    }

    public Optional<PersonRecordEntity> ensurePersonLoaded(String personNo) {
        String normalized = personNo == null ? "" : personNo.trim();
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        Optional<PersonRecordEntity> existing = personRecordRepository.findByPersonNo(normalized);
        if (existing.isPresent() || !props.isSeedEnabled()) {
            return existing;
        }
        return seedSinglePerson(normalized);
    }

    private void seedMissingPersons() throws Exception {
        Resource r = resourceLoader.getResource(props.getSeedCsv());
        if (!r.exists()) {
            return;
        }
        byte[] rawBytes = r.getInputStream().readAllBytes();
        log.info("Raw bytes (first 100): {}", java.util.Arrays.toString(java.util.Arrays.copyOf(rawBytes, Math.min(100, rawBytes.length))));
        String rawContent = new String(rawBytes, StandardCharsets.UTF_8);
        log.info("Raw content as UTF-8: {}", rawContent);
        log.info("Testing '张' encoding: {}", java.util.Arrays.toString("张".getBytes(StandardCharsets.UTF_8)));
        int zhangIndex = rawContent.indexOf("张");
        if (zhangIndex >= 0) {
            log.info("'张' found at index: {}", zhangIndex);
            log.info("'张' bytes in file: {}", java.util.Arrays.toString(java.util.Arrays.copyOfRange(rawBytes, zhangIndex, zhangIndex + 3)));
        }
        List<String> lines = rawContent.lines().toList();
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
            if (personRecordRepository.existsByPersonNo(personNo)) {
                continue;
            }
            savePerson(personNo, fullName, idLast4, phone, dept, airline, positionTitle);
        }
    }

    private Optional<PersonRecordEntity> seedSinglePerson(String targetPersonNo) {
        try {
            Resource r = resourceLoader.getResource(props.getSeedCsv());
            if (!r.exists()) {
                return Optional.empty();
            }
            String rawContent = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean first = true;
            for (String line : rawContent.lines().toList()) {
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
                if (!targetPersonNo.equals(personNo)) {
                    continue;
                }
                String fullName = parts[1].trim();
                String idLast4 = parts[2].trim();
                String phone = parts.length >= 4 ? parts[3].trim() : "";
                String dept = parts.length >= 5 ? parts[4].trim() : "";
                String airline = parts.length >= 6 ? parts[5].trim() : "";
                String positionTitle = parts.length >= 7 ? parts[6].trim() : "";
                return Optional.of(savePerson(personNo, fullName, idLast4, phone, dept, airline, positionTitle));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to seed person on demand for personNo={}", targetPersonNo, e);
            return Optional.empty();
        }
    }

    private PersonRecordEntity savePerson(String personNo, String fullName, String idLast4, String phone, String dept, String airline, String positionTitle) {
        PersonRecordEntity e = new PersonRecordEntity();
        e.setId(IdUtil.newId());
        e.setPersonNo(personNo);
        e.setFullName(fullName);
        e.setIdLast4(idLast4);
        e.setPhone(phone == null || phone.isBlank() ? null : phone);
        e.setDepartment(dept == null || dept.isBlank() ? null : dept);
        e.setAirline(airline == null || airline.isBlank() ? null : airline);
        e.setPositionTitle(positionTitle == null || positionTitle.isBlank() ? null : positionTitle);
        e.setCreatedAt(Instant.now());
        log.info("Saving person: personNo={}, fullName={}, fullName bytes={}",
                personNo, fullName, java.util.Arrays.toString(fullName.getBytes(StandardCharsets.UTF_8)));
        return personRecordRepository.save(e);
    }
}
