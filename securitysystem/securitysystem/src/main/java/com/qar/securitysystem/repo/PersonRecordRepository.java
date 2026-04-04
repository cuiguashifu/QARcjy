package com.qar.securitysystem.repo;

import com.qar.securitysystem.model.PersonRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRecordRepository extends JpaRepository<PersonRecordEntity, String> {
    Optional<PersonRecordEntity> findByPersonNo(String personNo);
}

