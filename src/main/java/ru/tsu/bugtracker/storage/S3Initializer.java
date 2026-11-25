package ru.tsu.bugtracker.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Component
public class S3Initializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(S3Initializer.class);

	private final S3Client s3Client;

	private final S3Properties props;

	public S3Initializer(S3Client s3Client, S3Properties props) {
		this.s3Client = s3Client;
		this.props = props;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!props.isAutoCreateBucket()) {
			return;
		}

		String bucket = props.getBucket();
		try {
			try {
				s3Client.headBucket(request -> request.bucket(bucket));
				log.debug("S3 bucket '{}' already exists", bucket);
			}
			catch (NoSuchBucketException e) {
				s3Client.createBucket(request -> request.bucket(bucket));
				log.info("Created S3 bucket '{}'", bucket);
			}
		}
		catch (Exception e) {
			log.warn("Could not verify or create S3 bucket '{}': {}", bucket, e.getMessage());
		}
	}
}
