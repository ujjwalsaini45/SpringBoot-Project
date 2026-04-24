package com.example.demo.service;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setWithTTL(String key, Object value, long minutes) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(minutes));
    }

    public void pushToList(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    public List<Object> getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}