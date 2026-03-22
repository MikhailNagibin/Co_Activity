package com.coactivity.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.coactivity.DataRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ValidationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("RoomRepository category validation tests")
class RoomRepositoryImplTest {

  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement statement;
  private ResultSet resultSet;
  private RoomRepositoryImpl roomRepository;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = Mockito.mock(DataSource.class);
    connection = Mockito.mock(Connection.class);
    statement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);

    DataRepository dataRepository = new DataRepository(dataSource);
    QuestionRepositoryImpl questionRepository = Mockito.mock(QuestionRepositoryImpl.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);

    roomRepository = new RoomRepositoryImpl(dataRepository, questionRepository, userRepository);
  }

  @Test
  @DisplayName("getCategoryIdByName should throw validation error when category does not exist")
  void getCategoryIdByNameThrowsValidationWhenCategoryMissing() throws Exception {
    when(resultSet.next()).thenReturn(false);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomRepository.getCategoryIdByName("UnknownCategory"));

    assertEquals("Category not found: UnknownCategory", exception.getMessage());
  }
}
