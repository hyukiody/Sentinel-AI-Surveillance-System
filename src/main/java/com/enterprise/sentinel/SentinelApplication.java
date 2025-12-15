package com.enterprise.sentinel;

import com.enterprise.sentinel.client.SentinelFxApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SentinelApplication {

    public static void main(String[] args) {
        // Launch JavaFX (which will boot Spring)
        Application.launch(SentinelFxApplication.class, args);
    }
}