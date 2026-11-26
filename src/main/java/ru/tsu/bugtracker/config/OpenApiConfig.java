package ru.tsu.bugtracker.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tsu.bugtracker.common.exception.ErrorResponse;

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

	private static final String HEALTH_PATH = "/api/v1/health";

	private static final Set<String> NO_NOT_FOUND_PATHS = Set.of(
			"/api/v1/auth/register",
			"/api/v1/auth/resend-verification",
			"/api/v1/auth/password-reset/request");

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

	@Bean
	public OpenApiCustomizer defaultErrorResponsesCustomizer() {
		return openApi -> {
			if (openApi.getComponents() == null) {
				openApi.setComponents(new Components());
			}
			var errorSchemas = ModelConverters.getInstance().readAll(ErrorResponse.class);
			errorSchemas.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));

			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().forEach((path, pathItem) -> {
				if (pathItem == null || HEALTH_PATH.equals(path)) {
					return;
				}
				applyErrorResponses(path, pathItem, pathItem.getGet(), "GET");
				applyErrorResponses(path, pathItem, pathItem.getPost(), "POST");
				applyErrorResponses(path, pathItem, pathItem.getPut(), "PUT");
				applyErrorResponses(path, pathItem, pathItem.getPatch(), "PATCH");
				applyErrorResponses(path, pathItem, pathItem.getDelete(), "DELETE");
			});
		};
	}

	private void applyErrorResponses(String path, PathItem pathItem, Operation operation, String method) {
		if (operation == null) {
			return;
		}
		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}

		boolean isGet = "GET".equals(method);
		boolean isPost = "POST".equals(method);
		boolean isPut = "PUT".equals(method);
		boolean isPatch = "PATCH".equals(method);
		boolean isDelete = "DELETE".equals(method);
		boolean isMutating = isPost || isPut || isPatch || isDelete;
		boolean hasParameters = hasParameters(pathItem, operation);

		if (isPost || isPut || isPatch || ((isGet || isDelete) && hasParameters)) {
			addErrorResponse(responses, "400", "Bad Request");
		}

		List<io.swagger.v3.oas.models.security.SecurityRequirement> security = operation.getSecurity();
		if (security == null || !security.isEmpty()) {
			addErrorResponse(responses, "401", "Unauthorized");
			addErrorResponse(responses, "403", "Forbidden");
		}

		if (path.contains("{") || (isMutating && !NO_NOT_FOUND_PATHS.contains(path))) {
			addErrorResponse(responses, "404", "Not Found");
		}

		if (isMutating) {
			addErrorResponse(responses, "409", "Conflict");
			addErrorResponse(responses, "422", "Unprocessable Entity");
		}
	}

	private boolean hasParameters(PathItem pathItem, Operation operation) {
		if (pathItem.getParameters() != null && !pathItem.getParameters().isEmpty()) {
			return true;
		}
		return operation.getParameters() != null && !operation.getParameters().isEmpty();
	}

	private void addErrorResponse(ApiResponses responses, String status, String description) {
		if (responses.get(status) != null) {
			return;
		}
		responses.addApiResponse(status, new ApiResponse()
				.description(description)
				.content(new Content().addMediaType("application/json", new MediaType()
						.schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
	}

}
