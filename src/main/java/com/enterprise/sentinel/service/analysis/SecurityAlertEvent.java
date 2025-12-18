package com.enterprise.sentinel.service.analysis;

import com.enterprise.sentinel.domain.model.SecurityAlert;
import org.springframework.context.ApplicationEvent;

/**
 * SecurityAlertEvent is published by AlertEngine when an alert is triggered.
 * Decouples alert creation from notification handling via Spring's event infrastructure.
 */
public class SecurityAlertEvent extends ApplicationEvent {

    private final SecurityAlert alert;

    public SecurityAlertEvent(Object source, SecurityAlert alert) {
        super(source);
        this.alert = alert;
    }

    public SecurityAlertEvent(SecurityAlert alert) {
        super(alert);
        this.alert = alert;
    }

    public SecurityAlert getAlert() {
        return alert;
    }
}
