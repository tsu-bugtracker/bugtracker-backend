package ru.tsu.bugtracker.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

	private String endpoint;

	private String accessKey;

	private String secretKey;

	private String bucket;

	private String region = "us-east-1";

	private boolean autoCreateBucket = true;

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public boolean isAutoCreateBucket() {
		return autoCreateBucket;
	}

	public void setAutoCreateBucket(boolean autoCreateBucket) {
		this.autoCreateBucket = autoCreateBucket;
	}
}
