package com.coactivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Optional;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DataRepository {

  private DataSource dataSource;

  public DataRepository() {
    setupDataSourceFromConfig();
  }

  /**
   * In Spring runtime, always prefer the framework-managed DataSource to avoid configuration drift
   * between custom JDBC repositories and Spring infrastructure.
   */
  @Autowired(required = false)
  public void useSpringManagedDataSource(DataSource springDataSource) {
    if (springDataSource != null) {
      this.dataSource = springDataSource;
    }
  }

  private void setupDataSourceFromConfig() {
    PGSimpleDataSource ds = new PGSimpleDataSource();

    try {
      InputStream inputStream = new ClassPathResource("config.json").getInputStream();
      JsonNode jsonNode = new ObjectMapper().readTree(inputStream);
      String host = readSetting("DB_HOST").orElse(jsonNode.get("host").asText());
      int port = readSetting("DB_PORT").map(Integer::parseInt).orElse(jsonNode.get("port").asInt());
      String database = readSetting("DB_NAME").orElse(jsonNode.get("database").asText());
      String username = readSetting("DB_USER").orElse(jsonNode.get("username").asText());
      String password = readSetting("DB_PASSWORD").orElse(jsonNode.get("password").asText());

      ds.setServerNames(new String[]{host});
      ds.setPortNumbers(new int[]{port});
      ds.setDatabaseName(database);
      ds.setUser(username);
      ds.setPassword(password);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    this.dataSource = ds;
  }

  private Optional<String> readSetting(String key) {
    String valueFromEnv = System.getenv(key);
    if (valueFromEnv != null && !valueFromEnv.isBlank()) {
      return Optional.of(valueFromEnv);
    }
    String valueFromProperty = System.getProperty(key);
    if (valueFromProperty != null && !valueFromProperty.isBlank()) {
      return Optional.of(valueFromProperty);
    }
    return Optional.empty();
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
