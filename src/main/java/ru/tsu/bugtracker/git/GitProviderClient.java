package ru.tsu.bugtracker.git;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.UnprocessableEntityException;

@Component
public class GitProviderClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {
            };

    private static final Pattern GITHUB_PULL_PATTERN = Pattern.compile("/pull/(\\d+)");
    private static final Pattern GITLAB_MERGE_REQUEST_PATTERN = Pattern.compile("/merge_requests/(\\d+)");

    private final RestClient restClient;
    private final GitProperties properties;

    public GitProviderClient(RestClient.Builder restClientBuilder, GitProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public record GitCommitPayload(
            GitCommitType type,
            String commitHash,
            String mergeRequestUrl,
            String title,
            String authorName,
            Instant committedAt,
            String branch) {
    }

    public void verifyRepositoryAccessible(String repositoryUrl, GitProvider provider, String accessToken) {
        String url = requireRepositoryUrl(repositoryUrl);

        if (isMockMode()) {
            String normalized = url.toLowerCase(Locale.ROOT);
            if (normalized.contains("invalid") || normalized.contains("unreachable")) {
                throw new UnprocessableEntityException("Git repository is not accessible: " + repositoryUrl);
            }
            return;
        }

        try {
            if (provider == null) {
                throw new UnprocessableEntityException("Git provider is not specified");
            }

            String requestUrl = switch (provider) {
                case GITHUB -> repositoryApiUrl(provider, url);
                case GITLAB -> projectApiUrl(provider, url);
                case CUSTOM -> url;
            };

            getMap(requestUrl, provider, accessToken);
        } catch (RestClientException | IllegalArgumentException e) {
            throw new UnprocessableEntityException("Git repository is not accessible: " + repositoryUrl, e);
        }
    }

    public List<GitCommitPayload> fetchCommitsAndMergeRequests(
            GitIntegration integration, String branch, Instant since) {
        if (isMockMode()) {
            Instant now = Instant.now();
            return List.of(
                    new GitCommitPayload(
                            GitCommitType.COMMIT,
                            "mock-commit-sha",
                            null,
                            "Mock commit",
                            "Mock Developer",
                            now,
                            branch),
                    new GitCommitPayload(
                            GitCommitType.MERGE_REQUEST,
                            null,
                            "https://example.com/mock/pull/1",
                            "Mock merge request",
                            "Mock Developer",
                            now,
                            branch));
        }

        if (integration == null) {
            throw new UnprocessableEntityException("Git integration is not specified");
        }

        GitProvider provider = integration.getProvider();
        String repositoryUrl = integration.getRepositoryUrl();
        String accessToken = integration.getAccessToken();

        if (provider == null) {
            throw new UnprocessableEntityException("Git provider is not specified");
        }

        if (provider == GitProvider.CUSTOM) {
            return List.of();
        }

        try {
            String repositoryApiUrl = repositoryApiUrl(provider, repositoryUrl);
            List<GitCommitPayload> commits = provider == GitProvider.GITHUB
                    ? fetchGitHubCommits(repositoryApiUrl, accessToken, branch, since)
                    : fetchGitLabCommits(repositoryApiUrl, accessToken, branch, since);

            String projectApiUrl = projectApiUrl(provider, repositoryUrl);
            List<GitCommitPayload> mergeRequests = provider == GitProvider.GITHUB
                    ? fetchGitHubPullRequests(projectApiUrl, accessToken, branch)
                    : fetchGitLabMergeRequests(projectApiUrl, accessToken, branch);

            List<GitCommitPayload> result = new ArrayList<>(commits.size() + mergeRequests.size());
            result.addAll(commits);
            result.addAll(mergeRequests);
            return result;
        } catch (RestClientException | IllegalArgumentException e) {
            throw new UnprocessableEntityException("Failed to fetch commits from Git provider", e);
        }
    }

    public GitCommitPayload verifyCommitOrMergeRequest(
            GitIntegration integration, String commitHash, String mergeRequestUrl) {
        boolean hasCommitHash = hasText(commitHash);
        boolean hasMergeRequestUrl = hasText(mergeRequestUrl);

        if (!hasCommitHash && !hasMergeRequestUrl) {
            throw new BadRequestException("Either commitHash or mergeRequestUrl must be provided");
        }

        if (isMockMode()) {
            String reference = hasCommitHash ? commitHash : mergeRequestUrl;
            if (containsNotFoundMarker(commitHash) || containsNotFoundMarker(mergeRequestUrl)) {
                throw new UnprocessableEntityException(
                        "Commit or merge request was not found in Git provider: " + reference);
            }

            GitCommitType type = hasCommitHash ? GitCommitType.COMMIT : GitCommitType.MERGE_REQUEST;
            return new GitCommitPayload(
                    type,
                    hasCommitHash ? commitHash : null,
                    hasMergeRequestUrl ? mergeRequestUrl : null,
                    hasCommitHash ? "Mock commit" : "Mock merge request",
                    "Mock Developer",
                    Instant.now(),
                    null);
        }

        if (integration == null) {
            throw new UnprocessableEntityException("Git integration is not specified");
        }

        GitProvider provider = integration.getProvider();
        String repositoryUrl = integration.getRepositoryUrl();
        String accessToken = integration.getAccessToken();

        if (provider == null) {
            throw new UnprocessableEntityException("Git provider is not specified");
        }

        try {
            return switch (provider) {
                case GITHUB -> verifyGitHubReference(provider, repositoryUrl, accessToken, commitHash, mergeRequestUrl);
                case GITLAB -> verifyGitLabReference(provider, repositoryUrl, accessToken, commitHash, mergeRequestUrl);
                case CUSTOM -> verifyCustomReference(provider, mergeRequestUrl);
            };
        } catch (RestClientException | IllegalArgumentException e) {
            throw new UnprocessableEntityException("Commit or merge request was not found in Git provider", e);
        }
    }

    private List<GitCommitPayload> fetchGitHubCommits(
            String repositoryApiUrl, String accessToken, String branch, Instant since) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("per_page", 30);
        if (hasText(branch)) {
            query.put("sha", branch);
        }
        if (since != null) {
            query.put("since", since.toString());
        }

        List<Map<String, Object>> response = getList(
                appendQuery(repositoryApiUrl + "/commits", query), GitProvider.GITHUB, accessToken);

        List<GitCommitPayload> result = new ArrayList<>(response.size());
        for (Map<String, Object> item : response) {
            Map<String, Object> commit = asMap(item.get("commit"));
            Map<String, Object> author = commit == null ? null : asMap(commit.get("author"));
            result.add(new GitCommitPayload(
                    GitCommitType.COMMIT,
                    asString(item.get("sha")),
                    null,
                    commit == null ? null : asString(commit.get("message")),
                    author == null ? null : asString(author.get("name")),
                    author == null ? Instant.now() : parseInstant(author.get("date")),
                    branch));
        }
        return result;
    }

    private List<GitCommitPayload> fetchGitHubPullRequests(
            String repositoryApiUrl, String accessToken, String branch) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("state", "all");
        query.put("per_page", 20);

        List<Map<String, Object>> response = getList(
                appendQuery(repositoryApiUrl + "/pulls", query), GitProvider.GITHUB, accessToken);

        List<GitCommitPayload> result = new ArrayList<>(response.size());
        for (Map<String, Object> item : response) {
            Map<String, Object> user = asMap(item.get("user"));
            result.add(new GitCommitPayload(
                    GitCommitType.MERGE_REQUEST,
                    null,
                    asString(item.get("html_url")),
                    asString(item.get("title")),
                    user == null ? null : asString(user.get("login")),
                    parseInstant(item.get("created_at")),
                    branch));
        }
        return result;
    }

    private List<GitCommitPayload> fetchGitLabCommits(
            String projectApiUrl, String accessToken, String branch, Instant since) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("per_page", 30);
        if (hasText(branch)) {
            query.put("ref_name", branch.trim());
        }
        if (since != null) {
            query.put("since", since.toString());
        }

        List<Map<String, Object>> response = getList(
                appendQuery(projectApiUrl + "/repository/commits", query), GitProvider.GITLAB, accessToken);

        List<GitCommitPayload> result = new ArrayList<>(response.size());
        for (Map<String, Object> item : response) {
            Object committedDate = item.containsKey("committed_date")
                    ? item.get("committed_date")
                    : item.get("created_at");
            result.add(new GitCommitPayload(
                    GitCommitType.COMMIT,
                    asString(item.get("id")),
                    null,
                    asString(item.get("title")),
                    asString(item.get("author_name")),
                    parseInstant(committedDate),
                    branch));
        }
        return result;
    }

    private List<GitCommitPayload> fetchGitLabMergeRequests(
            String projectApiUrl, String accessToken, String branch) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("per_page", 20);

        List<Map<String, Object>> response = getList(
                appendQuery(projectApiUrl + "/merge_requests", query), GitProvider.GITLAB, accessToken);

        List<GitCommitPayload> result = new ArrayList<>(response.size());
        for (Map<String, Object> item : response) {
            Map<String, Object> author = asMap(item.get("author"));
            result.add(new GitCommitPayload(
                    GitCommitType.MERGE_REQUEST,
                    null,
                    asString(item.get("web_url")),
                    asString(item.get("title")),
                    author == null ? null : asString(author.get("name")),
                    parseInstant(item.get("created_at")),
                    branch));
        }
        return result;
    }

    private GitCommitPayload verifyGitHubReference(
            GitProvider provider,
            String repositoryUrl,
            String accessToken,
            String commitHash,
            String mergeRequestUrl) {
        if (hasText(commitHash)) {
            String url = repositoryApiUrl(provider, repositoryUrl) + "/commits/" + encodePathSegment(commitHash);

            Map<String, Object> response = getMap(url, provider, accessToken);
            Map<String, Object> commit = asMap(response.get("commit"));
            Map<String, Object> author = commit == null ? null : asMap(commit.get("author"));
            return new GitCommitPayload(
                    GitCommitType.COMMIT,
                    asString(response.get("sha")),
                    null,
                    commit == null ? null : asString(commit.get("message")),
                    author == null ? null : asString(author.get("name")),
                    author == null ? Instant.now() : parseInstant(author.get("date")),
                    null);
        }

        Matcher matcher = GITHUB_PULL_PATTERN.matcher(mergeRequestUrl);
        if (!matcher.find()) {
            throw new UnprocessableEntityException(
                    "Unable to determine GitHub pull request number from URL: " + mergeRequestUrl);
        }

        String url = repositoryApiUrl(provider, repositoryUrl) + "/pulls/" + matcher.group(1);
        Map<String, Object> response = getMap(url, provider, accessToken);
        Map<String, Object> user = asMap(response.get("user"));
        return new GitCommitPayload(
                GitCommitType.MERGE_REQUEST,
                null,
                asString(response.get("html_url")),
                asString(response.get("title")),
                user == null ? null : asString(user.get("login")),
                parseInstant(response.get("created_at")),
                null);
    }

    private GitCommitPayload verifyGitLabReference(
            GitProvider provider,
            String repositoryUrl,
            String accessToken,
            String commitHash,
            String mergeRequestUrl) {
        String projectApiUrl = projectApiUrl(provider, repositoryUrl);

        if (hasText(commitHash)) {
            String url = projectApiUrl + "/repository/commits/" + encodePathSegment(commitHash);
            Map<String, Object> response = getMap(url, provider, accessToken);
            Object committedDate = response.containsKey("committed_date")
                    ? response.get("committed_date")
                    : response.get("created_at");
            return new GitCommitPayload(
                    GitCommitType.COMMIT,
                    asString(response.get("id")),
                    null,
                    asString(response.get("title")),
                    asString(response.get("author_name")),
                    parseInstant(committedDate),
                    null);
        }

        Matcher matcher = GITLAB_MERGE_REQUEST_PATTERN.matcher(mergeRequestUrl);
        if (!matcher.find()) {
            throw new UnprocessableEntityException(
                    "Unable to determine GitLab merge request number from URL: " + mergeRequestUrl);
        }

        String url = projectApiUrl + "/merge_requests/" + matcher.group(1);
        Map<String, Object> response = getMap(url, provider, accessToken);
        Map<String, Object> author = asMap(response.get("author"));
        return new GitCommitPayload(
                GitCommitType.MERGE_REQUEST,
                null,
                asString(response.get("web_url")),
                asString(response.get("title")),
                author == null ? null : asString(author.get("name")),
                parseInstant(response.get("created_at")),
                null);
    }

    private GitCommitPayload verifyCustomReference(GitProvider provider, String mergeRequestUrl) {
        if (!hasText(mergeRequestUrl)) {
            throw new UnprocessableEntityException(
                    "mergeRequestUrl must be provided for a custom Git provider");
        }

        getMap(mergeRequestUrl, provider, null);
        return new GitCommitPayload(
                GitCommitType.MERGE_REQUEST,
                null,
                mergeRequestUrl,
                null,
                "Unknown",
                Instant.now(),
                null);
    }

    private Map<String, Object> getMap(String url, GitProvider provider, String accessToken) {
        return restClient.get()
                .uri(URI.create(url))
                .headers(headers -> applyHeaders(headers, provider, accessToken))
                .retrieve()
                .body(MAP);
    }

    private List<Map<String, Object>> getList(String url, GitProvider provider, String accessToken) {
        List<Map<String, Object>> body = restClient.get()
                .uri(URI.create(url))
                .headers(headers -> applyHeaders(headers, provider, accessToken))
                .retrieve()
                .body(LIST_OF_MAPS);
        return body == null ? List.of() : body;
    }

    private void applyHeaders(HttpHeaders headers, GitProvider provider, String accessToken) {
        if (provider == GitProvider.GITHUB) {
            headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github+json")));
        }
        if (hasText(accessToken)) {
            if (provider == GitProvider.GITLAB) {
                headers.set("PRIVATE-TOKEN", accessToken);
            } else {
                headers.setBearerAuth(accessToken);
            }
        }
    }

    private String repositoryApiUrl(GitProvider provider, String repositoryUrl) {
        String baseUrl = apiBaseUrl(provider);
        return joinUrl(baseUrl, "/repos/" + repositoryPath(repositoryUrl, 2));
    }

    private String projectApiUrl(GitProvider provider, String repositoryUrl) {
        String baseUrl = apiBaseUrl(provider);
        String encodedPath = URLEncoder.encode(repositoryPath(repositoryUrl, -1), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return joinUrl(baseUrl, "/projects/" + encodedPath);
    }

    private String apiBaseUrl(GitProvider provider) {
        String baseUrl = switch (provider) {
            case GITHUB -> properties.getGithubApiUrl();
            case GITLAB -> properties.getGitlabApiUrl();
            case CUSTOM -> null;
        };

        if (!hasText(baseUrl)) {
            throw new UnprocessableEntityException(
                    "Git provider API URL is not configured for " + provider);
        }
        return baseUrl;
    }

    private String repositoryPath(String repositoryUrl, int requiredSegments) {
        String path = extractRepositoryPath(repositoryUrl);
        String[] segments = path.split("/");
        int minimumSegments = requiredSegments > 0 ? requiredSegments : 2;
        if (segments.length < minimumSegments) {
            throw new UnprocessableEntityException(
                    "Unable to determine repository owner and name from URL: " + repositoryUrl);
        }
        if (requiredSegments > 0) {
            return encodePathSegment(segments[0]) + "/" + encodePathSegment(segments[1]);
        }
        return path;
    }

    private String extractRepositoryPath(String repositoryUrl) {
        String value = requireRepositoryUrl(repositoryUrl).trim();

        if (value.startsWith("git@")) {
            int colonIndex = value.indexOf(':');
            if (colonIndex < 0 || colonIndex == value.length() - 1) {
                throw new UnprocessableEntityException(
                        "Unable to determine repository path from URL: " + repositoryUrl);
            }
            value = value.substring(colonIndex + 1);
        } else {
            try {
                URI uri = URI.create(value);
                value = uri.getPath();
            } catch (IllegalArgumentException e) {
                throw new UnprocessableEntityException(
                        "Unable to determine repository path from URL: " + repositoryUrl, e);
            }
        }

        if (value == null) {
            throw new UnprocessableEntityException(
                    "Unable to determine repository path from URL: " + repositoryUrl);
        }

        value = value.replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith(".git")) {
            value = value.substring(0, value.length() - 4);
        }

        if (!hasText(value)) {
            throw new UnprocessableEntityException(
                    "Unable to determine repository path from URL: " + repositoryUrl);
        }
        return value;
    }

    private String requireRepositoryUrl(String repositoryUrl) {
        if (!hasText(repositoryUrl)) {
            throw new UnprocessableEntityException("Git repository URL must not be empty");
        }

        String value = repositoryUrl.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://") && !value.startsWith("git@")) {
            throw new UnprocessableEntityException(
                    "Git repository URL must start with http://, https:// or git@: " + repositoryUrl);
        }
        return value;
    }

    private boolean isMockMode() {
        return "mock".equalsIgnoreCase(properties.getMode());
    }

    private boolean containsNotFoundMarker(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("missing")
                || normalized.contains("notfound")
                || normalized.contains("invalid");
    }

    private String appendQuery(String url, Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return url;
        }

        StringBuilder result = new StringBuilder(url);
        char separator = url.contains("?") ? '&' : '?';
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            result.append(separator)
                    .append(encodeQuery(entry.getKey()))
                    .append('=')
                    .append(encodeQuery(String.valueOf(entry.getValue())));
            separator = '&';
        }
        return result.toString();
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return Instant.now();
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return Instant.now();
        }

        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // Try the offset and zoned representations used by GitLab.
        }

        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try the zoned representation as a last resort.
        }

        try {
            return ZonedDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.now();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
