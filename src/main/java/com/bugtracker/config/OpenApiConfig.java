package com.bugtracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bugTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bug Tracker API")
                        .description("REST API for Bug Tracking System. Designed according to domain model, activity diagrams, and component architecture.")
                        .version("1.0.0"));
    }
}
