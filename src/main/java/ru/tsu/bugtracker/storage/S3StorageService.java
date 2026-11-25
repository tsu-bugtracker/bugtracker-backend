package ru.tsu.bugtracker.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService {

	private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

	private final S3Client s3Client;

	private final S3Properties props;

	public S3StorageService(S3Client s3Client, S3Properties props) {
		this.s3Client = s3Client;
		this.props = props;
	}

	public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
		try {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(props.getBucket())
				.key(objectKey)
				.contentLength(contentLength)
				.contentType(contentType)
				.build();
			s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
			log.debug("Uploaded object '{}' to S3 bucket '{}'", objectKey, props.getBucket());
		}
		catch (Exception e) {
			log.error("Failed to upload object '{}' to S3 bucket '{}'", objectKey, props.getBucket(), e);
			throw new S3StorageException("Failed to upload object '" + objectKey + "' to S3", e);
		}
	}

	public byte[] download(String objectKey) {
		GetObjectRequest request = GetObjectRequest.builder()
			.bucket(props.getBucket())
			.key(objectKey)
			.build();
		try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
			return response.readAllBytes();
		}
		catch (IOException e) {
			log.error("Failed to read object '{}' from S3 bucket '{}'", objectKey, props.getBucket(), e);
			throw new UncheckedIOException("Failed to read object '" + objectKey + "' from S3", e);
		}
		catch (Exception e) {
			log.error("Failed to download object '{}' from S3 bucket '{}'", objectKey, props.getBucket(), e);
			throw new S3StorageException("Failed to download object '" + objectKey + "' from S3", e);
		}
	}

	public void delete(String objectKey) {
		try {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
				.bucket(props.getBucket())
				.key(objectKey)
				.build();
			s3Client.deleteObject(request);
			log.debug("Deleted object '{}' from S3 bucket '{}'", objectKey, props.getBucket());
		}
		catch (Exception e) {
			log.error("Failed to delete object '{}' from S3 bucket '{}'", objectKey, props.getBucket(), e);
			throw new S3StorageException("Failed to delete object '" + objectKey + "' from S3", e);
		}
	}

	public static class S3StorageException extends RuntimeException {

		public S3StorageException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
