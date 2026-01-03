package com.teraapi.identity.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Security Properties Validator
 * Validates required security configuration on application startup
 */
@Slf4j
@Configuration
public class SecurityPropertiesValidator {

    @Value("${JWT_SECRET:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${cors.allowed.origins:}")
    private String allowedOrigins;

    @PostConstruct
    public void validateSecurityProperties() {
        List<String> errors = new ArrayList<>();

        // Validate JWT Secret
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            errors.add("JWT_SECRET is not configured");
        } else if (jwtSecret.length() < 32) {
            errors.add("JWT_SECRET must be at least 32 characters (256 bits)");
        } else if (isWeakSecret(jwtSecret)) {
            errors.add("JWT_SECRET appears to be a default/weak value - please use a cryptographically secure random value");
        }

        // Validate Database Password
        if (dbPassword == null || dbPassword.trim().isEmpty()) {
            errors.add("Database password (spring.datasource.password) is not configured");
        } else if (isWeakPassword(dbPassword)) {
            errors.add("Database password appears to be a default/weak value");
        }

        // Validate CORS Origins
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            log.warn("CORS allowed origins not configured - using defaults (http://localhost:5173,http://localhost:3000)");
        } else if (allowedOrigins.contains("*")) {
            errors.add("CORS wildcard (*) is not allowed in production - specify exact origins");
        }

        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("\n");
            errorMessage.append("═".repeat(80)).append("\n");
            errorMessage.append("❌ CRITICAL SECURITY CONFIGURATION ERRORS:\n");
            errorMessage.append("═".repeat(80)).append("\n\n");
            
            for (int i = 0; i < errors.size(); i++) {
                errorMessage.append(String.format("%d. %s\n", i + 1, errors.get(i)));
            }
            
            errorMessage.append("\n");
            errorMessage.append("📋 Required Actions:\n");
            errorMessage.append("─".repeat(80)).append("\n");
            errorMessage.append("1. Create a .env file in the project root\n");
            errorMessage.append("2. Generate secure secrets:\n");
            errorMessage.append("   JWT_SECRET=$(openssl rand -base64 64)\n");
            errorMessage.append("   DB_PASSWORD=$(openssl rand -base64 32)\n");
            errorMessage.append("3. Configure CORS_ALLOWED_ORIGINS with your frontend URLs\n");
            errorMessage.append("4. Restart the application\n");
            errorMessage.append("\n");
            errorMessage.append("See .env.example for full configuration template\n");
            errorMessage.append("═".repeat(80)).append("\n");

            throw new IllegalStateException(errorMessage.toString());
        }

        log.info("✅ Security configuration validation passed");
        log.info("   - JWT Secret: {} characters", jwtSecret.length());
        log.info("   - CORS Origins: {}", allowedOrigins);
        log.info("   - Database password configured: YES");
    }

    private boolean isWeakSecret(String secret) {
        // Check for common weak patterns
        String lowerSecret = secret.toLowerCase();
        return lowerSecret.contains("secret") ||
               lowerSecret.contains("change") ||
               lowerSecret.contains("password") ||
               lowerSecret.contains("example") ||
               lowerSecret.contains("test") ||
               lowerSecret.contains("demo") ||
               lowerSecret.equals("your-256-bit-secret-key-change-this-in-production");
    }

    private boolean isWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return lowerPassword.equals("password") ||
               lowerPassword.equals("change_me") ||
               lowerPassword.equals("changeme") ||
               lowerPassword.equals("change_this_password") ||
               lowerPassword.contains("example") ||
               lowerPassword.contains("test") ||
               password.length() < 12;
    }
}
