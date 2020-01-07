package com.example.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.integration.annotation.IntegrationComponentScan;

import static org.springframework.boot.WebApplicationType.NONE;

@SpringBootApplication
@IntegrationComponentScan
public class Main {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
                .web(NONE)
                .run(args);
    }
}
