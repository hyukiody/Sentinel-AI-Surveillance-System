package com.enterprise.sentinel.service.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Component
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    private static final String ENV_SECRET_KEY = "SENTINEL_SECRET_KEY";
    
    // Injected from properties, with a default for dev/test to prevent null pointers
    @Value("${sentinel.security.secret-key:x/A?D(G+KbPeShVmYq3t6w9z$B&E)H@M}")
    private String secretKeyProperty;

    private Key key;
    private Cipher cipher;

    /**
     * Initializes the cipher and key. 
     * Priority:
     * 1. Environment variable SENTINEL_SECRET_KEY (production - from vault)
     * 2. Application property sentinel.security.secret-key (development)
     * 3. Hardcoded default (tests only)
     * 
     * Called automatically by Spring or manually in tests.
     */
    public void init() {
        try {
            // Load secret key with priority order
            String secretKey = System.getenv(ENV_SECRET_KEY);
            if (secretKey == null || secretKey.trim().isEmpty()) {
                secretKey = secretKeyProperty;
                if (secretKey == null) {
                    throw new IllegalStateException(
                            "No encryption key found. Set SENTINEL_SECRET_KEY environment variable or " +
                            "sentinel.security.secret-key property");
                }
            }

            // Ensure key length is valid (16, 24, 32 bytes). 
            // Using first 16 chars (128-bit) for simplicity if key is longer.
            // In prod, use full 32 bytes (256-bit).
            byte[] keyBytes = secretKey.getBytes();
            if (keyBytes.length > 16) {
                byte[] truncated = new byte[16];
                System.arraycopy(keyBytes, 0, truncated, 0, 16);
                keyBytes = truncated;
            }
            
            key = new SecretKeySpec(keyBytes, AES);
            cipher = Cipher.getInstance(AES);
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize encryption", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        if (cipher == null) init(); // Lazy init for JPA usage where Spring injection might lag
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = cipher.doFinal(attribute.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting attribute", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (cipher == null) init();
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return new String(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Error decrypting attribute", e);
        }
    }
}