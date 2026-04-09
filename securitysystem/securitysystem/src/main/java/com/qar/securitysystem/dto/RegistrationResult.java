package com.qar.securitysystem.dto;

import com.qar.securitysystem.model.AccountRequestEntity;

public class RegistrationResult {
    private AccountRequestEntity entity;
    private String privateKey;

    public RegistrationResult(AccountRequestEntity entity, String privateKey) {
        this.entity = entity;
        this.privateKey = privateKey;
    }

    public AccountRequestEntity getEntity() {
        return entity;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}
