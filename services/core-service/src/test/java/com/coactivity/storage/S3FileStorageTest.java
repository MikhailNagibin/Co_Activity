package com.coactivity.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coactivity.service.exception.StorageException;
import com.coactivity.support.MinioTestSupport;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

@Testcontainers
@Tag("docker")
class S3FileStorageTest {

  private static final String BUCKET = "coactivity-storage-unit-test";
  private static final String PREFIX = "unit-prefix";

  @Container
  static final GenericContainer<?> minio = MinioTestSupport.newContainer();

  private static S3Client s3Client;
  private S3FileStorage storage;

  @BeforeAll
  static void startS3Client() {
    s3Client = MinioTestSupport.createClient(MinioTestSupport.endpoint(minio));
    MinioTestSupport.ensureBucket(s3Client, BUCKET);
  }

  @AfterAll
  static void closeS3Client() {
    if (s3Client != null) {
      s3Client.close();
    }
  }

  @BeforeEach
  void setUp() {
    MinioTestSupport.cleanBucket(s3Client, BUCKET);
    storage = new S3FileStorage(s3Client, BUCKET, PREFIX);
  }

  @Test
  void saveOpenExistsAndDeleteRoundTripThroughS3Api() throws Exception {
    String storageKey = "avatars/avatar.png";
    byte[] payload = "avatar-bytes".getBytes(StandardCharsets.UTF_8);

    storage.save(storageKey, new ByteArrayInputStream(payload));

    assertTrue(storage.exists(storageKey));
    assertTrue(MinioTestSupport.objectExists(s3Client, BUCKET, PREFIX + "/" + storageKey));
    try (var inputStream = storage.open(storageKey)) {
      assertArrayEquals(payload, inputStream.readAllBytes());
    }

    storage.delete(storageKey);

    assertFalse(storage.exists(storageKey));
    assertFalse(MinioTestSupport.objectExists(s3Client, BUCKET, PREFIX + "/" + storageKey));
  }

  @Test
  void existsReturnsFalseForMissingObject() {
    assertFalse(storage.exists("room-images/missing.jpg"));
  }

  @Test
  void deleteIsIdempotentForMissingObject() {
    storage.delete("avatars/missing.png");

    assertFalse(storage.exists("avatars/missing.png"));
  }

  @Test
  void rejectsUnsafeStorageKeysBeforeCallingS3() {
    assertThrows(StorageException.class,
        () -> storage.save("../secret.txt", new ByteArrayInputStream(new byte[] {1})));
    assertThrows(StorageException.class,
        () -> storage.open("/absolute/key.png"));
    assertThrows(StorageException.class,
        () -> storage.exists("avatars/../secret.png"));
  }
}
