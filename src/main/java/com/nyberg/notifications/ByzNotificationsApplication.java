package com.nyberg.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ByzNotificationsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ByzNotificationsApplication.class, args);
    }
}
