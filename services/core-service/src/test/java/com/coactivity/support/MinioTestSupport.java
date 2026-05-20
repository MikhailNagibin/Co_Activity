package com.coactivity.support;

import java.net.URI;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;

public final class MinioTestSupport {

  public static final int API_PORT = 9000;
  public static final String ACCESS_KEY = "minioadmin";
  public static final String SECRET_KEY = "minioadmin";
  public static final String REGION = "us-east-1";

  private MinioTestSupport() {
  }

  @SuppressWarnings("resource")
  public static GenericContainer<?> newContainer() {
    return new GenericContainer<>(
        DockerImageName.parse("minio/minio:RELEASE.2024-01-16T16-07-38Z"))
        .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
        .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
        .withCommand("server", "/data", "--console-address", ":9001")
        .withExposedPorts(API_PORT)
        .waitingFor(Wait.forHttp("/minio/health/ready")
            .forPort(API_PORT)
            .forStatusCode(200));
  }

  public static String endpoint(GenericContainer<?> container) {
    return "http://" + container.getHost() + ":" + container.getMappedPort(API_PORT);
  }

  public static S3Client createClient(String endpoint) {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(REGION))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
        .httpClient(UrlConnectionHttpClient.create())
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
  }

  public static void ensureBucket(S3Client s3Client, String bucket) {
    try {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    } catch (S3Exception ex) {
      if (ex.statusCode() != 409) {
        throw ex;
      }
    }
  }

  public static void cleanBucket(S3Client s3Client, String bucket) {
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).build())
        .contents()
        .forEach(object -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(object.key())
            .build()));
  }

  public static boolean objectExists(S3Client s3Client, String bucket, String key) {
    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build());
      return true;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return false;
      }
      throw ex;
    } catch (SdkException ex) {
      throw ex;
    }
  }
}
