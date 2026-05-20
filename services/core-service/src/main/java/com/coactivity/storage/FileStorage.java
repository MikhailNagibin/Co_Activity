package com.coactivity.storage;

import java.io.InputStream;

public interface FileStorage {

  void save(String storageKey, InputStream content);

  InputStream open(String storageKey);

  void delete(String storageKey);

  boolean exists(String storageKey);
}
