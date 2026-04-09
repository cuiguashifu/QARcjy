package com.qar.securitysystem.transport;

public class TransportHandshakeRequest {
    private String clientPublicKey;
    private String protocol;

    public String getClientPublicKey() {
        return clientPublicKey;
    }

    public void setClientPublicKey(String clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}

