package com.example.demo.repository;

import com.example.demo.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotRepository extends JpaRepository<Bot, Long> {
}
