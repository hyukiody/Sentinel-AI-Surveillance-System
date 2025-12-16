package com.enterprise.sentinel.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeEncryptorTest {

    private AttributeEncryptor encryptor;
    private final String SECRET_KEY = "x/A?D(G+KbPeShVmYq3t6w9z$B&E)H@M"; // 256-bit test key

    @BeforeEach
    void setUp() {
        encryptor = new AttributeEncryptor();
        // Inject the secret key via Reflection since it's typically @Value injected
        ReflectionTestUtils.setField(encryptor, "secretKey", SECRET_KEY);
        encryptor.init(); // Initialize the cipher logic
    }

    @Test
    @DisplayName("Should encrypt raw data into a Base64 string")
    void convertToDatabaseColumn() {
        String originalData = "/var/storage/videos/private/cam_01.mp4";
        String encryptedData = encryptor.convertToDatabaseColumn(originalData);

        assertThat(encryptedData).isNotNull();
        assertThat(encryptedData).isNotEqualTo(originalData);
        // Base64 regex check
        assertThat(encryptedData).matches("^[A-Za-z0-9+/=]+$"); 
    }

    @Test
    @DisplayName("Should decrypt data back to original string")
    void convertToEntityAttribute() {
        String originalData = "/var/storage/videos/private/cam_02.mp4";
        
        // Encrypt first
        String encrypted = encryptor.convertToDatabaseColumn(originalData);
        
        // Then Decrypt
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        assertThat(decrypted).isEqualTo(originalData);
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void handleNulls() {
        assertThat(encryptor.convertToDatabaseColumn(null)).isNull();
        assertThat(encryptor.convertToEntityAttribute(null)).isNull();
    }
    
    @Test
    @DisplayName("Should fail decryption with wrong key")
    void failWithWrongKey() {
        String originalData = "sensitive_data";
        String encrypted = encryptor.convertToDatabaseColumn(originalData);

        // Re-initialize with different key
        AttributeEncryptor wrongEncryptor = new AttributeEncryptor();
        ReflectionTestUtils.setField(wrongEncryptor, "secretKey", "wrongKey12345678901234567890123");
        wrongEncryptor.init();

        assertThatThrownBy(() -> wrongEncryptor.convertToEntityAttribute(encrypted))
                .isInstanceOf(IllegalStateException.class); // Or whatever runtime exception your encryptor throws
    }
}