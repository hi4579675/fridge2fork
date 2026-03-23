package com.fridge2fork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Fridge2forkApplication {

    public static void main(String[] args) {
        SpringApplication.run(Fridge2forkApplication.class, args);
    }
}
