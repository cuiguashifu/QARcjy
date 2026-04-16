package com.qar.securitysystem.crypto;

public interface KeyWrapper {
    String wrap(byte[] keyBytes);

    byte[] unwrap(String wrappedKey);
}
