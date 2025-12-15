package com.enterprise.sentinel.service.ingestion;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class RtspService {

    /**
     * Validates and prepares an RTSP URL for playback.
     * Enforces strict URI syntax to prevent injection.
     */
    public String prepareStream(String rawUrl) {
        try {
            // Basic validation
            URI uri = new URI(rawUrl);
            String scheme = uri.getScheme();
            
            if (scheme == null || (!scheme.equalsIgnoreCase("rtsp") && !scheme.equalsIgnoreCase("http"))) {
                throw new IllegalArgumentException("Invalid scheme. Only RTSP and HTTP streams are supported.");
            }
            
            // In a real scenario, you might ping the camera here to verify uptime
            return rawUrl;
            
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + rawUrl);
        }
    }
}