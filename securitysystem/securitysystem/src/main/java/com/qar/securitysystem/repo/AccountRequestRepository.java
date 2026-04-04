package com.qar.securitysystem.repo;

import com.qar.securitysystem.model.AccountRequestEntity;
import com.qar.securitysystem.model.AccountRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRequestRepository extends JpaRepository<AccountRequestEntity, String> {
    List<AccountRequestEntity> findAllByStatusOrderByCreatedAtDesc(AccountRequestStatus status);

    Optional<AccountRequestEntity> findFirstByPersonNoOrderByCreatedAtDesc(String personNo);
}

