package com.example.positionmanagementservice;

import com.example.positionmanagementservice.config.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FileStorageProperties.class)
public class PositionManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PositionManagementServiceApplication.class, args);
    }
}
