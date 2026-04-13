package com.coactivity.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Tag("docker")
class BaselineSchemaMigrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2"));

  @Test
  void baselineCreatesCurrentS3ReadySchemaWithoutOldFileMetadata() throws Exception {
    Flyway flyway = flyway()
        .cleanDisabled(false)
        .load();
    flyway.clean();
    flyway.migrate();

    try (Connection connection = connection()) {
      assertEquals(1, queryInt(connection, "SELECT count(*) FROM flyway_schema_history"));
      assertEquals(0, queryInt(connection, "SELECT count(*) FROM user_avatars"));
      assertEquals(0, queryInt(connection, "SELECT count(*) FROM pictures"));
      assertEquals(0, queryInt(connection, "SELECT count(*) FROM users WHERE avatar_file_id IS NOT NULL"));

      assertTrue(columnExists(connection, "users", "password_hash"));
      assertTrue(columnExists(connection, "users", "email_normalized"));
      assertTrue(columnExists(connection, "users", "avatar_file_id"));
      assertTrue(columnExists(connection, "rooms", "status"));
      assertTrue(columnExists(connection, "rooms", "city"));
      assertTrue(columnExists(connection, "rooms", "country"));
      assertTrue(columnExists(connection, "pictures", "storage_key"));
      assertTrue(columnExists(connection, "pictures", "sort_order"));
      assertEquals(1, queryInt(connection, """
          SELECT count(*)
          FROM notifications
          WHERE notification = 'ImportantRoomUpdates'
          """));
    }
  }

  private org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
    return Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .locations("classpath:db/migration");
  }

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(),
        postgres.getUsername(),
        postgres.getPassword());
  }

  private int queryInt(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private boolean columnExists(Connection connection, String tableName, String columnName)
      throws SQLException {
    try (Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery("""
             SELECT count(*)
             FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = '%s'
               AND column_name = '%s'
             """.formatted(tableName, columnName))) {
      resultSet.next();
      return resultSet.getInt(1) == 1;
    }
  }
}
