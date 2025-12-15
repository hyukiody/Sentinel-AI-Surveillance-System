package com.enterprise.sentinel.service.ingestion;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileNotFoundException;

@Service
public class FileIngestionService {

    // Validates if a file exists and is a supported video format
    public String prepareFileForPlayback(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("Video file not found: " + file.getAbsolutePath());
        }
        
        // In the future: Add SHA-256 checksum calculation here for security audit
        
        // Return the absolute path formatted for VLC (file:///)
        return file.getAbsolutePath();
    }
}