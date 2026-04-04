package com.qar.securitysystem.security;

import com.qar.securitysystem.model.UserRole;

public class AppPrincipal {
    private final String userId;
    private final String emailOrUsername;
    private final UserRole role;
    private final String personId;

    public AppPrincipal(String userId, String emailOrUsername, UserRole role, String personId) {
        this.userId = userId;
        this.emailOrUsername = emailOrUsername;
        this.role = role;
        this.personId = personId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmailOrUsername() {
        return emailOrUsername;
    }

    public UserRole getRole() {
        return role;
    }

    public String getPersonId() {
        return personId;
    }
}
