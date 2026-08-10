package com.booking.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class CamundaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamundaServiceApplication.class, args);
    }

}
