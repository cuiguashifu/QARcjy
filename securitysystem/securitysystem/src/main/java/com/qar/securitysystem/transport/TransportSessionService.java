package com.qar.securitysystem.transport;

import com.qar.securitysystem.service.ServerKeyPairService;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;

@Service
public class TransportSessionService {
    public static final String PROTOCOL_TLS = "tls";
    public static final String SUITE_TLS_AES_256_GCM_SHA256 = "TLS_AES_256_GCM_SHA256";

    private final ServerKeyPairService serverKeyPairService;

    public TransportSessionService(ServerKeyPairService serverKeyPairService) {
        this.serverKeyPairService = serverKeyPairService;
    }

    public HandshakeResult handshake(String protocol) {
        String chosenProtocol = (protocol == null || protocol.isBlank()) ? PROTOCOL_TLS : protocol.trim().toLowerCase();
        if (!PROTOCOL_TLS.equals(chosenProtocol)) {
            throw new IllegalArgumentException("unsupported_protocol");
        }

        HandshakeResult r = new HandshakeResult();
        r.protocol = chosenProtocol;
        r.suite = SUITE_TLS_AES_256_GCM_SHA256;
        r.serverPublicKeyBase64 = serverKeyPairService.getPublicKeyBase64();
        return r;
    }

    public SecretKeySpec unwrapTransportKey(String wrappedKeyBase64) {
        return new SecretKeySpec(serverKeyPairService.unwrapKey(wrappedKeyBase64), "AES");
    }

    public static class HandshakeResult {
        public String protocol;
        public String suite;
        public String serverPublicKeyBase64;
    }
}

