package com.brassvault.api.crypto;

public class VaultCryptoException extends RuntimeException {

    public VaultCryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    public VaultCryptoException(String message) {
        super(message);
    }
}
