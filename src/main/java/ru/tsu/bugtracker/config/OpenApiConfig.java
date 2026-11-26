package ru.tsu.bugtracker.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Bugtracker Backend API",
				version = "1.0.0",
				description = "REST API for student project bugtracker"),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT")
public class OpenApiConfig {

	private static final List<String> TAG_ORDER = List.of(
			"Health",
			"Auth",
			"Profile",
			"Admin",
			"Projects",
			"Bugs",
			"Planning",
			"Releases",
			"Git Integration",
			"Notifications",
			"Analytics");

	@Bean
	public OpenApiCustomizer tagOrderCustomizer() {
		return openApi -> {
			List<Tag> tags = openApi.getTags();
			if (tags == null || tags.isEmpty()) {
				return;
			}
			List<Tag> sorted = new ArrayList<>(tags);
			sorted.sort(Comparator
					.comparingInt((Tag tag) -> {
						int index = TAG_ORDER.indexOf(tag.getName());
						return index >= 0 ? index : Integer.MAX_VALUE;
					})
					.thenComparing(Tag::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
			openApi.setTags(sorted);
		};
	}

}
