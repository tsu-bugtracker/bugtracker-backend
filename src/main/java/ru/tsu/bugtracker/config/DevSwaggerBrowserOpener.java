package ru.tsu.bugtracker.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.swagger.auto-open", havingValue = "true")
public class DevSwaggerBrowserOpener {

    private static final Logger log = LoggerFactory.getLogger(DevSwaggerBrowserOpener.class);

    private final Environment environment;

    public DevSwaggerBrowserOpener(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openSwaggerUi() {
        String port = environment.getProperty("local.server.port");
        if (port == null || port.isBlank()) {
            port = environment.getProperty("server.port", "8080");
        }

        String url = "http://localhost:" + port + "/swagger-ui.html";
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder processBuilder;

            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
            } else if (os.contains("mac")) {
                processBuilder = new ProcessBuilder("open", url);
            } else {
                processBuilder = new ProcessBuilder("xdg-open", url);
            }

            processBuilder.start();
            log.info("Opening Swagger UI at {}", url);
        } catch (Exception e) {
            log.warn("Could not open Swagger UI at {}: {}", url, e.getMessage());
        }
    }
}
