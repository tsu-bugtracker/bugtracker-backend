package com.bugtracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Profile({"dev", "local"})
public class SwaggerBrowserOpener {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${app.open-swagger-on-startup:true}")
    private boolean openOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!openOnStartup) {
            return;
        }

        String url = "http://localhost:" + serverPort + "/swagger-ui.html";
        log.info("Application started. Opening Swagger UI in browser: {}", url);

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (IOException e) {
            log.warn("Could not automatically open browser for Swagger UI: {}", e.getMessage());
        }
    }
}
