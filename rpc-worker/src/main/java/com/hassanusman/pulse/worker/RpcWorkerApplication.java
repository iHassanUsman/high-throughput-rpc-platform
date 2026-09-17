package com.hassanusman.pulse.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RpcWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcWorkerApplication.class, args);
    }
}
