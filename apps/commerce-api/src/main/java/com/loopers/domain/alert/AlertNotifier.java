package com.loopers.domain.alert;

public interface AlertNotifier {
    void notify(String title, String message);
}
