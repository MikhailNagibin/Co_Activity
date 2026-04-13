package com.coactivity.storage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FileStorageConditionalConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(LocalFileStorage.class, S3FileStorage.class);

  @Test
  void localFileStorageIsSelectedWhenStorageTypeIsLocal() throws Exception {
    String root = Files.createTempDirectory("coactivity-local-storage-test").toString();

    contextRunner
        .withPropertyValues(
            "app.storage.type=local",
            "app.storage.local.root=" + root)
        .run(context -> assertInstanceOf(LocalFileStorage.class, context.getBean(FileStorage.class)));
  }
}
