package ru.tsu.bugtracker.git;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.git")
public class GitProperties {

	private String mode = "live";

	private String githubApiUrl = "https://api.github.com";

	private String gitlabApiUrl = "https://gitlab.com/api/v4";

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getGithubApiUrl() {
		return githubApiUrl;
	}

	public void setGithubApiUrl(String githubApiUrl) {
		this.githubApiUrl = githubApiUrl;
	}

	public String getGitlabApiUrl() {
		return gitlabApiUrl;
	}

	public void setGitlabApiUrl(String gitlabApiUrl) {
		this.gitlabApiUrl = gitlabApiUrl;
	}

}
