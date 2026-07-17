package org.example.springboot.accommodation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.example.springboot")
public class AccommodationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccommodationApplication.class, args);
    }
}
