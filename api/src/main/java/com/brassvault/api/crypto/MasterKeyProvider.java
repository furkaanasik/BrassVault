package com.brassvault.api.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Loads the AES-256 master key from the VAULT_MASTER_KEY environment variable.
 * Fails fast at startup when the key is missing or not exactly 32 bytes.
 * The key is never persisted, logged or exposed.
 */
@Component
public class MasterKeyProvider {

    private final SecretKey key;

    public MasterKeyProvider(@Value("${vault.master-key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "VAULT_MASTER_KEY is not set. The application refuses to start without it.");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("VAULT_MASTER_KEY is not valid base64.", e);
        }
        if (raw.length != 32) {
            throw new IllegalStateException(
                    "VAULT_MASTER_KEY must decode to exactly 32 bytes, got " + raw.length + ".");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public SecretKey key() {
        return key;
    }

    @Override
    public String toString() {
        return "MasterKeyProvider[redacted]";
    }
}
