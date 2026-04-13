package com.qar.securitysystem.transport;

import com.qar.securitysystem.util.IdUtil;
import com.qar.securitysystem.util.RSAUtil;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransportSessionService {
    public static final String PROTOCOL_TLS = "tls";
    public static final String SUITE_TLS_AES_256_GCM_SHA256 = "TLS_AES_256_GCM_SHA256";

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public HandshakeResult handshake(String clientPublicKeyBase64, String protocol) {
        String chosenProtocol = (protocol == null || protocol.isBlank()) ? PROTOCOL_TLS : protocol.trim().toLowerCase();
        if (!PROTOCOL_TLS.equals(chosenProtocol)) {
            throw new IllegalArgumentException("unsupported_protocol");
        }

        if (clientPublicKeyBase64 == null || clientPublicKeyBase64.isBlank()) {
            throw new IllegalArgumentException("client_public_key_required");
        }

        SecretKey aesKey = generateAes256();
        PublicKey clientPub = RSAUtil.getPublicKey(clientPublicKeyBase64.trim());
        byte[] wrapped = RSAUtil.encrypt(aesKey.getEncoded(), clientPub);

        String sessionId = IdUtil.newId();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_TTL);
        sessions.put(sessionId, new Session(sessionId, chosenProtocol, aesKey, expiresAt));

        HandshakeResult r = new HandshakeResult();
        r.protocol = chosenProtocol;
        r.suite = SUITE_TLS_AES_256_GCM_SHA256;
        r.sessionId = sessionId;
        r.wrappedKeyBase64 = Base64.getEncoder().encodeToString(wrapped);
        r.expiresAt = expiresAt;
        return r;
    }

    public Session getSessionOrNull(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        Session s = sessions.get(sessionId);
        if (s == null) {
            return null;
        }
        if (s.expiresAt.isBefore(Instant.now())) {
            sessions.remove(sessionId);
            return null;
        }
        return s;
    }

    private static SecretKey generateAes256() {
        try {
            KeyGenerator g = KeyGenerator.getInstance("AES");
            g.init(256);
            return g.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("failed_to_generate_aes_key", e);
        }
    }

    public static class Session {
        private final String id;
        private final String protocol;
        private final SecretKeySpec aesKey;
        private final Instant expiresAt;

        public Session(String id, String protocol, SecretKey key, Instant expiresAt) {
            this.id = id;
            this.protocol = protocol;
            this.aesKey = new SecretKeySpec(key.getEncoded(), "AES");
            this.expiresAt = expiresAt;
        }

        public String getId() {
            return id;
        }

        public String getProtocol() {
            return protocol;
        }

        public SecretKeySpec getAesKey() {
            return aesKey;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    public static class HandshakeResult {
        public String protocol;
        public String suite;
        public String sessionId;
        public String wrappedKeyBase64;
        public Instant expiresAt;
    }
}

