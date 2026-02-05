package com.example.minipg.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class TestPgClientConfig {

    @Bean
    @Lazy
    public FakePgClient fakePgClient(
        RestClient.Builder builder,
        ObjectMapper objectMapper,
        @Value("${pg.fake.timeout-ms:2000}") long timeoutMs,
        ApplicationContext context
    ) {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        String baseUrl = "http://localhost:" + port;
        return new FakePgClient(builder, objectMapper, baseUrl, timeoutMs);
    }
}
