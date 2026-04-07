package com.coactivity.storage;

import com.coactivity.service.exception.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

  private final Path rootPath;

  public LocalFileStorage(@Value("${app.storage.local.root:./var/app-storage}") String rootPath) {
    this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.rootPath);
    } catch (IOException ex) {
      throw new StorageException("Unable to initialize local storage", ex);
    }
  }

  @Override
  public void save(String storageKey, InputStream content) {
    Path targetPath = resolve(storageKey);
    try {
      Files.createDirectories(targetPath.getParent());
      Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new StorageException("Unable to store file " + storageKey, ex);
    }
  }

  @Override
  public InputStream open(String storageKey) {
    Path targetPath = resolve(storageKey);
    try {
      return Files.newInputStream(targetPath);
    } catch (IOException ex) {
      throw new StorageException("Unable to open file " + storageKey, ex);
    }
  }

  @Override
  public void delete(String storageKey) {
    Path targetPath = resolve(storageKey);
    try {
      Files.deleteIfExists(targetPath);
    } catch (IOException ex) {
      throw new StorageException("Unable to delete file " + storageKey, ex);
    }
  }

  @Override
  public boolean exists(String storageKey) {
    return Files.exists(resolve(storageKey));
  }

  private Path resolve(String storageKey) {
    Path resolved = rootPath.resolve(storageKey).normalize();
    if (!resolved.startsWith(rootPath)) {
      throw new StorageException("Invalid storage key");
    }
    return resolved;
  }
}
