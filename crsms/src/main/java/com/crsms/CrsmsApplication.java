package com.crsms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrsmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrsmsApplication.class, args);
    }
}
