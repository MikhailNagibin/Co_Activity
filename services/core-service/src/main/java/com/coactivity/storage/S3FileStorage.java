package com.coactivity.storage;

import com.coactivity.service.exception.StorageException;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

  private final S3Client s3Client;
  private final String bucket;
  private final String prefix;
  private final boolean closeClient;

  @Autowired
  public S3FileStorage(
      @Value("${app.storage.s3.endpoint}") String endpoint,
      @Value("${app.storage.s3.region:us-east-1}") String region,
      @Value("${app.storage.s3.bucket}") String bucket,
      @Value("${app.storage.s3.access-key}") String accessKey,
      @Value("${app.storage.s3.secret-key}") String secretKey,
      @Value("${app.storage.s3.path-style:true}") boolean pathStyle,
      @Value("${app.storage.s3.prefix:}") String prefix) {
    this(
        createClient(endpoint, region, accessKey, secretKey, pathStyle),
        bucket,
        prefix,
        true);
  }

  S3FileStorage(S3Client s3Client, String bucket, String prefix) {
    this(s3Client, bucket, prefix, false);
  }

  private S3FileStorage(S3Client s3Client, String bucket, String prefix, boolean closeClient) {
    if (!StringUtils.hasText(bucket)) {
      throw new StorageException("S3 bucket is required");
    }
    this.s3Client = Objects.requireNonNull(s3Client, "s3Client");
    this.bucket = bucket;
    this.prefix = normalizePrefix(prefix);
    this.closeClient = closeClient;
  }

  @Override
  public void save(String storageKey, InputStream content) {
    String objectKey = objectKey(storageKey);
    try {
      byte[] bytes = content.readAllBytes();
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .build(),
          RequestBody.fromBytes(bytes));
    } catch (IOException | SdkException ex) {
      throw new StorageException("Unable to store file " + storageKey + " in S3", ex);
    }
  }

  @Override
  public InputStream open(String storageKey) {
    String objectKey = objectKey(storageKey);
    try {
      ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
          GetObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .build());
      return response;
    } catch (SdkException ex) {
      throw new StorageException("Unable to open file " + storageKey + " from S3", ex);
    }
  }

  @Override
  public void delete(String storageKey) {
    String objectKey = objectKey(storageKey);
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .build());
    } catch (SdkException ex) {
      throw new StorageException("Unable to delete file " + storageKey + " from S3", ex);
    }
  }

  @Override
  public boolean exists(String storageKey) {
    String objectKey = objectKey(storageKey);
    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .build());
      return true;
    } catch (S3Exception ex) {
      if (isNotFound(ex)) {
        return false;
      }
      throw new StorageException("Unable to check file " + storageKey + " in S3", ex);
    } catch (SdkException ex) {
      throw new StorageException("Unable to check file " + storageKey + " in S3", ex);
    }
  }

  @PreDestroy
  public void close() {
    if (closeClient) {
      s3Client.close();
    }
  }

  private String objectKey(String storageKey) {
    validateRelativeKey(storageKey);
    return prefix + storageKey;
  }

  private static S3Client createClient(String endpoint, String region, String accessKey,
      String secretKey, boolean pathStyle) {
    if (!StringUtils.hasText(endpoint)) {
      throw new StorageException("S3 endpoint is required");
    }
    if (!StringUtils.hasText(region)) {
      throw new StorageException("S3 region is required");
    }
    if (!StringUtils.hasText(accessKey)) {
      throw new StorageException("S3 access key is required");
    }
    if (!StringUtils.hasText(secretKey)) {
      throw new StorageException("S3 secret key is required");
    }

    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .httpClient(UrlConnectionHttpClient.create())
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(pathStyle)
            .build())
        .build();
  }

  private static String normalizePrefix(String prefix) {
    if (!StringUtils.hasText(prefix)) {
      return "";
    }

    String normalized = prefix.trim().replace('\\', '/');
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (!StringUtils.hasText(normalized)) {
      return "";
    }

    validateRelativeKey(normalized);
    return normalized + "/";
  }

  private static void validateRelativeKey(String storageKey) {
    if (!StringUtils.hasText(storageKey)
        || storageKey.startsWith("/")
        || storageKey.contains("\\")) {
      throw new StorageException("Invalid storage key");
    }

    String[] segments = storageKey.split("/");
    for (String segment : segments) {
      if (!StringUtils.hasText(segment) || ".".equals(segment) || "..".equals(segment)) {
        throw new StorageException("Invalid storage key");
      }
    }
  }

  private static boolean isNotFound(S3Exception ex) {
    String errorCode = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorCode() : null;
    return ex.statusCode() == 404
        || "NoSuchKey".equals(errorCode)
        || "NotFound".equals(errorCode);
  }
}
