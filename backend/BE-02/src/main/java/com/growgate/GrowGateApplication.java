package com.growgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GrowGateApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrowGateApplication.class, args);
    }
}
