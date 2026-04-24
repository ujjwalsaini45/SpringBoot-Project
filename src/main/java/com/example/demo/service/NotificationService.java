package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RedisService redisService;

    public void handleNotification(Long userId, String message) {

        String cooldownKey = "notif:" + userId;

        if (redisService.exists(cooldownKey)) {
            redisService.pushToList("user:" + userId + ":pending", message);
        } else {
            System.out.println("Push Notification Sent");
            redisService.setWithTTL(cooldownKey, "1", 15);
        }
    }
}