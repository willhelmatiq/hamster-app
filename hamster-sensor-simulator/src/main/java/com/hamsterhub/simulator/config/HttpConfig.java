package com.hamsterhub.simulator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpConfig {

    @Bean
    public WebClient trackerClient(
            WebClient.Builder builder,
            @Value("${tracker.base-url:http://localhost:8080}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}