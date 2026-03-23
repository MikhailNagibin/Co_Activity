package com.coactivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("DataRepository transaction tests")
class DataRepositoryTest {

  private DataSource dataSource;
  private Connection connection;
  private DataRepository dataRepository;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = Mockito.mock(DataSource.class);
    connection = Mockito.mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    dataRepository = new DataRepository(dataSource);
  }

  @Test
  @DisplayName("inTransaction commits when callback succeeds")
  void inTransaction_commitsOnSuccess() throws Exception {
    String result = dataRepository.inTransaction(ignored -> "ok");

    assertEquals("ok", result);
    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection, never()).rollback();
    verify(connection).close();
  }

  @Test
  @DisplayName("inTransaction rolls back when callback throws")
  void inTransaction_rollsBackOnFailure() throws Exception {
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> dataRepository.inTransaction(ignored -> {
          throw new RuntimeException("boom");
        }));

    assertEquals("boom", exception.getMessage());
    verify(connection).setAutoCommit(false);
    verify(connection).rollback();
    verify(connection, never()).commit();
    verify(connection).close();
  }
}
