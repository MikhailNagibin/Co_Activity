package com.coactivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DataRepository {

  private DataSource dataSource;

  public DataRepository() {
    setupDataSourceFromConfig();
  }

  private void setupDataSourceFromConfig() {
    PGSimpleDataSource ds = new PGSimpleDataSource();

    try {
      InputStream inputStream = new ClassPathResource("config.json").getInputStream();
      JsonNode jsonNode = new ObjectMapper().readTree(inputStream);
      ds.setServerNames(new String[]{jsonNode.get("host").asText()});
      ds.setPortNumbers(new int[]{jsonNode.get("port").asInt()});
      ds.setDatabaseName(jsonNode.get("database").asText());
      ds.setUser(jsonNode.get("username").asText());
      ds.setPassword(jsonNode.get("password").asText());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    this.dataSource = ds;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}