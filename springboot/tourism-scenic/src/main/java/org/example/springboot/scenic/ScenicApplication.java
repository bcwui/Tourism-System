package org.example.springboot.scenic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.example.springboot")
public class ScenicApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScenicApplication.class, args);
    }
}
