package com.loopers.infrastructure.alert;

import com.loopers.domain.alert.AlertNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogAlertNotifier implements AlertNotifier {

    @Override
    public void notify(String title, String message) {
        // TODO: Slack 또는 Email 연동
        log.warn("[ALERT] {} - {}", title, message);
    }
}