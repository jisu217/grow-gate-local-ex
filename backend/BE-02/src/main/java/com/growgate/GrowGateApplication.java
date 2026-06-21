package com.growgate;

// 스프링 부트 애플리케이션 진입점
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
