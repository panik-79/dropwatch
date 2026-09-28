package com.dropwatch.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.dropwatch")
@EnableMongoRepositories(basePackages = "com.dropwatch")
@EntityScan(basePackages = "com.dropwatch")
@EnableScheduling
@EnableAsync
public class DropWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DropWatchApplication.class, args);
    }
}
