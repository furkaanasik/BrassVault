package com.brassvault.api.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VaultCryptoServiceTest {

    private VaultCryptoService crypto;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        crypto = new VaultCryptoService(new MasterKeyProvider(Base64.getEncoder().encodeToString(key)));
    }

    @Test
    @DisplayName("encrypt/decrypt round-trips the plaintext")
    void roundTrip() {
        byte[] blob = crypto.encrypt("s3cr3t-Pa$$word", 42L);
        assertThat(crypto.decrypt(blob, 42L)).isEqualTo("s3cr3t-Pa$$word");
    }

    @Test
    @DisplayName("same plaintext encrypted twice yields different ciphertext (random IV)")
    void randomIvProducesDifferentCiphertext() {
        byte[] first = crypto.encrypt("same-plaintext", 1L);
        byte[] second = crypto.encrypt("same-plaintext", 1L);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("blob layout is IV(12) || ciphertext || tag(16)")
    void blobLayout() {
        String plaintext = "abc";
        byte[] blob = crypto.encrypt(plaintext, 1L);
        assertThat(blob).hasSize(12 + plaintext.getBytes().length + 16);
    }

    @Test
    @DisplayName("flipping any single byte of the blob makes decrypt throw, never return garbage")
    void tamperedBlobThrows() {
        byte[] blob = crypto.encrypt("integrity-matters", 7L);
        for (int i = 0; i < blob.length; i++) {
            byte[] tampered = blob.clone();
            tampered[i] ^= 0x01;
            int index = i;
            assertThatThrownBy(() -> crypto.decrypt(tampered, 7L))
                    .as("byte %d flipped", index)
                    .isInstanceOf(VaultCryptoException.class);
        }
    }

    @Test
    @DisplayName("decrypting with another item's id fails (AAD binding)")
    void wrongItemIdThrows() {
        byte[] blob = crypto.encrypt("bound-to-item-10", 10L);
        assertThatThrownBy(() -> crypto.decrypt(blob, 11L))
                .isInstanceOf(VaultCryptoException.class);
    }

    @Test
    @DisplayName("truncated blob is rejected")
    void truncatedBlobThrows() {
        assertThatThrownBy(() -> crypto.decrypt(new byte[10], 1L))
                .isInstanceOf(VaultCryptoException.class);
    }
}
