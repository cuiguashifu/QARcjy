package com.qar.securitysystem.service;

import com.qar.securitysystem.util.RSAUtil;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@Service
public class ServerKeyPairService {
    private static final String WRAP_PREFIX = "RSA_WRAP_BC:";

    private final Path privateKeyPath = Path.of("data", "crypto", "server-private.key");
    private final Path publicKeyPath = Path.of("data", "crypto", "server-public.key");

    private volatile KeyPair keyPair;

    public String getPublicKeyBase64() {
        return RSAUtil.encodeKey(loadOrCreate().getPublic().getEncoded());
    }

    public String wrapKey(byte[] keyBytes) {
        byte[] wrapped = RSAUtil.encrypt(keyBytes, loadOrCreate().getPublic());
        return WRAP_PREFIX + Base64.getEncoder().encodeToString(wrapped);
    }

    public byte[] unwrapKey(String wrappedKey) {
        String raw = wrappedKey == null ? "" : wrappedKey.trim();
        if (raw.startsWith(WRAP_PREFIX)) {
            raw = raw.substring(WRAP_PREFIX.length());
        }
        if (raw.isBlank()) {
            throw new IllegalArgumentException("wrapped_key_required");
        }
        byte[] wrappedBytes = Base64.getDecoder().decode(raw);
        return RSAUtil.decrypt(wrappedBytes, loadOrCreate().getPrivate());
    }

    public byte[] unwrapKeyWithPrivateKey(String wrappedKey, String privateKeyBase64) {
        String raw = wrappedKey == null ? "" : wrappedKey.trim();
        if (raw.startsWith(WRAP_PREFIX)) {
            raw = raw.substring(WRAP_PREFIX.length());
        }
        if (raw.isBlank()) {
            throw new IllegalArgumentException("wrapped_key_required");
        }
        PrivateKey privateKey = RSAUtil.getPrivateKey(privateKeyBase64);
        return RSAUtil.decrypt(Base64.getDecoder().decode(raw), privateKey);
    }

    public String wrapKeyForPublicKey(byte[] keyBytes, String publicKeyBase64) {
        PublicKey publicKey = RSAUtil.getPublicKey(publicKeyBase64);
        byte[] wrapped = RSAUtil.encrypt(keyBytes, publicKey);
        return WRAP_PREFIX + Base64.getEncoder().encodeToString(wrapped);
    }

    private KeyPair loadOrCreate() {
        KeyPair local = keyPair;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (keyPair == null) {
                keyPair = loadOrGenerateKeyPair();
            }
            return keyPair;
        }
    }

    private KeyPair loadOrGenerateKeyPair() {
        try {
            Files.createDirectories(privateKeyPath.getParent());
            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                String privateKeyBase64 = Files.readString(privateKeyPath, StandardCharsets.UTF_8).trim();
                String publicKeyBase64 = Files.readString(publicKeyPath, StandardCharsets.UTF_8).trim();
                return new KeyPair(RSAUtil.getPublicKey(publicKeyBase64), RSAUtil.getPrivateKey(privateKeyBase64));
            }

            KeyPair generated = RSAUtil.generateKeyPair();
            Files.writeString(publicKeyPath, RSAUtil.encodeKey(generated.getPublic().getEncoded()), StandardCharsets.UTF_8);
            Files.writeString(privateKeyPath, RSAUtil.encodeKey(generated.getPrivate().getEncoded()), StandardCharsets.UTF_8);
            return generated;
        } catch (Exception e) {
            throw new RuntimeException("failed_to_initialize_server_key_pair", e);
        }
    }
}
