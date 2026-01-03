package com.teraapi.identity.exception;

/**
 * Exception thrown when license restrictions are violated.
 * Results in HTTP 402 Payment Required response.
 */
public class LicenseException extends RuntimeException {
    public LicenseException(String message) {
        super(message);
    }
    
    public LicenseException(String feature, String tier) {
        super(String.format("Feature '%s' requires %s tier or higher", feature, tier));
    }
}
