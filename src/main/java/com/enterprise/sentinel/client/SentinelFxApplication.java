package com.enterprise.sentinel.client;

import com.enterprise.sentinel.SentinelApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

public class SentinelFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // 1. Discover VLC Native Libraries (Crucial Step)
        // This scans standard install paths (like C:\Program Files\VideoLAN\VLC)
        boolean found = new NativeDiscovery().discover();
        if (!found) {
            System.err.println("❌ FATAL: VLC Media Player not found!");
            System.err.println("   Please install VLC (64-bit) from https://www.videolan.org/");
            // In a real app, you might show a Swing dialog here before exiting
            System.exit(1); 
        } else {
            System.out.println("✅ VLC Native Libraries found.");
        }

        // 2. Initialize Spring Boot
        this.context = new SpringApplicationBuilder(SentinelApplication.class)
                .run();
    }

    @Override
    public void start(Stage stage) {
        this.context.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        this.context.close();
        Platform.exit();
    }
}