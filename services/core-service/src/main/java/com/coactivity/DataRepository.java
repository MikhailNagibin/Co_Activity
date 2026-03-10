package com.coactivity;

import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class DataRepository {

  private final DataSource dataSource;

  public DataRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
