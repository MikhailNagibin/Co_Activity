package com.coactivity;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DataRepository {

  private final DataSource dataSource;

  public DataRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

    public <T> T inTransaction(ConnectionCallback<T> callback) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);

      try {
        T result = callback.execute(connection);
        connection.commit();
        return result;
      } catch (SQLException e) {
        rollback(connection, e);
        throw new RuntimeException("Database transaction failed", e);
      } catch (RuntimeException | Error e) {
        rollback(connection, e);
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to open database transaction", e);
    }
  }

  private void rollback(Connection connection, Throwable originalError) {
    try {
      connection.rollback();
    } catch (SQLException rollbackError) {
      originalError.addSuppressed(rollbackError);
    }
  }

  @FunctionalInterface
  public interface ConnectionCallback<T> {

    T execute(Connection connection) throws SQLException;
  }
}
