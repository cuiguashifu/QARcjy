package com.qar.securitysystem.repo;

import com.qar.securitysystem.model.QarTableRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QarTableRowRepository extends JpaRepository<QarTableRowEntity, String> {
}

