package com.example.demo.scheduler;

import com.example.demo.service.RedisService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RedisService redisService;

    @Scheduled(fixedRate = 300000) // 5 min
    public void processNotifications() {

        String key = "user:1:pending";

        List<Object> list = redisService.getList(key);

        if (list != null && !list.isEmpty()) {
            System.out.println("Summarized Notification: " + list.size() + " events");
            redisService.delete(key);
        }
    }
}