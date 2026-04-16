package com.qar.securitysystem.crypto;

import com.qar.securitysystem.util.RSAUtil;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.util.Base64;

@Component
public class RsaKeyWrapper implements KeyWrapper {
    private PublicKey clientPublicKey;

    public void setClientPublicKey(String clientPublicKeyBase64) {
        this.clientPublicKey = RSAUtil.getPublicKey(clientPublicKeyBase64);
    }

    @Override
    public String wrap(byte[] keyBytes) {
        if (clientPublicKey == null) {
            throw new IllegalStateException("Client public key not set");
        }
        byte[] wrapped = RSAUtil.encrypt(keyBytes, clientPublicKey);
        return Base64.getEncoder().encodeToString(wrapped);
    }

    @Override
    public byte[] unwrap(String wrappedKey) {
        throw new UnsupportedOperationException("RSA unwrapping is done on client side");
    }
}
