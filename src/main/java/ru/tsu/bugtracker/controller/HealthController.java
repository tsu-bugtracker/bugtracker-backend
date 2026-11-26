package ru.tsu.bugtracker.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service health endpoints")
public class HealthController {

	@GetMapping
	@Operation(summary = "Check service health", description = "Returns the current status of the backend service")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public Map<String, String> health() {
		return Map.of(
			"status", "UP",
			"service", "bugtracker-backend"
		);
	}
}
