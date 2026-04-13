package com.qar.securitysystem.transport;

public class TransportHandshakeResponse {
    private String protocol;
    private String suite;
    private String sessionId;
    private String wrappedKey;
    private String expiresAt;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSuite() {
        return suite;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getWrappedKey() {
        return wrappedKey;
    }

    public void setWrappedKey(String wrappedKey) {
        this.wrappedKey = wrappedKey;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}

