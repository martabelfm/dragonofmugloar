package com.mugloar;

import com.mugloar.infrastructure.MugloarProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableConfigurationProperties(MugloarProperties.class)
public class DragonTrainerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DragonTrainerApplication.class, args);
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
