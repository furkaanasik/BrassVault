package com.brassvault.api.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterKeyProviderTest {

    @Test
    @DisplayName("missing key refuses to construct")
    void missingKeyThrows() {
        assertThatThrownBy(() -> new MasterKeyProvider(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VAULT_MASTER_KEY");
    }

    @Test
    @DisplayName("non-base64 key refuses to construct")
    void invalidBase64Throws() {
        assertThatThrownBy(() -> new MasterKeyProvider("not-base64!!!"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("key with wrong length refuses to construct")
    void wrongLengthThrows() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new MasterKeyProvider(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("application context fails to start without VAULT_MASTER_KEY")
    void contextFailsWithoutKey() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withConfiguration(UserConfigurations.of(MasterKeyProvider.class))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("application context starts with a valid 32-byte key")
    void contextStartsWithValidKey() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        new ApplicationContextRunner()
                .withPropertyValues("vault.master-key=" + key)
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withConfiguration(UserConfigurations.of(MasterKeyProvider.class))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("toString never leaks key material")
    void toStringRedacted() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        assertThat(new MasterKeyProvider(key).toString()).doesNotContain(key).contains("redacted");
    }
}
