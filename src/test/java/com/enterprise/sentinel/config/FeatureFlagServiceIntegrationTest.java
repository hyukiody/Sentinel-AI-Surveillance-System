package com.enterprise.sentinel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FeatureFlagServiceIntegrationTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @Test
    void shouldLoadConfigurationFromApplicationYml() {
        // This test verifies that the values in application.yml are correctly picked up
        assertTrue(featureFlagService.isClaudeHaikuEnabledForAllClients(), 
            "Expected Claude Haiku to be enabled in application.yml");
        
        assertEquals("claude-haiku-4.5", featureFlagService.getClaudeHaikuModel(), 
            "Expected model to be claude-haiku-4.5 in application.yml");
    }
}
