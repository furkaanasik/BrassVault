package com.brassvault.api.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * AES-256-GCM encryption for vault credentials.
 * Stored blob layout: IV (12 bytes) || ciphertext || GCM tag (16 bytes).
 * The item id is bound as AAD so a blob cannot be decrypted under another item.
 */
@Service
public class VaultCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final MasterKeyProvider masterKeyProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public VaultCryptoService(MasterKeyProvider masterKeyProvider) {
        this.masterKeyProvider = masterKeyProvider;
    }

    public byte[] encrypt(String plaintext, long itemId) {
        return encrypt(plaintext, itemId, "");
    }

    /** context differentiates blobs of the same item (e.g. ":fields") so they cannot be swapped across columns. */
    public byte[] encrypt(String plaintext, long itemId, String context) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKeyProvider.key(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad(itemId, context));
            byte[] ciphertextAndTag = cipher.doFinal(plaintext.getBytes(UTF_8));
            return ByteBuffer.allocate(iv.length + ciphertextAndTag.length)
                    .put(iv)
                    .put(ciphertextAndTag)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new VaultCryptoException("Encryption failed", e);
        }
    }

    public String decrypt(byte[] blob, long itemId) {
        return decrypt(blob, itemId, "");
    }

    public String decrypt(byte[] blob, long itemId, String context) {
        if (blob == null || blob.length < IV_LENGTH_BYTES + TAG_LENGTH_BITS / 8) {
            throw new VaultCryptoException("Encrypted blob is too short");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKeyProvider.key(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, blob, 0, IV_LENGTH_BYTES));
            cipher.updateAAD(aad(itemId, context));
            byte[] plaintext = cipher.doFinal(blob, IV_LENGTH_BYTES, blob.length - IV_LENGTH_BYTES);
            return new String(plaintext, UTF_8);
        } catch (GeneralSecurityException e) {
            throw new VaultCryptoException("Decryption failed: blob is corrupt or bound to another item", e);
        }
    }

    // "" context yields the same bytes as the pre-context AAD, so old blobs still decrypt
    private static byte[] aad(long itemId, String context) {
        return (itemId + context).getBytes(UTF_8);
    }
}
