package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Category;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.QuestionRepository;
import com.coactivity.repository.RoomRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepositoryImpl implements RoomRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;
  private final QuestionRepositoryImpl qaRepository;

  public RoomRepositoryImpl(DataRepository dataRepository, @Lazy UserRepositoryImpl userRepository,
                            QuestionRepositoryImpl qaRepository) {
    this.dataRepository = dataRepository;
    this.userRepository = userRepository;
    this.qaRepository = qaRepository;
  }

  @Override
  public Room createRoom(Integer ownerId, RoomCreationRequest request) {

    String sql = """
        INSERT INTO Rooms (is_active, is_public, chat_link, category_id, name, description, start_date, end_date,
                            age_rating, frequency, maximum_number_of_people)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setBoolean(1, true);
      statement.setBoolean(2, request.getIsPublic());
      statement.setString(3, request.getChatLink());
      statement.setInt(4, getCategoryIdByName(request.getCategory()));
      statement.setString(5, request.getName());
      statement.setString(6, request.getDescription());
      statement.setTimestamp(7, request.getDateOfStartEvent() != null ?
        Timestamp.from(request.getDateOfStartEvent()) : null);
      statement.setTimestamp(8, request.getDateOfEndEvent() != null ?
        Timestamp.from(request.getDateOfEndEvent()) : null);
      statement.setInt(9, request.getAgeRating());
      statement.setTimestamp(10, request.getFrequency() != null ?
        Timestamp.from(request.getFrequency()) : null);
      statement.setInt(11, request.getMaximumNumberOfPeople());

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer roomId = resultSet.getInt("id");
          addUserToRoom(roomId, ownerId, Role.OWNER);
          return getRoomById(roomId);
        }
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public Room getRoomById(Integer roomId) {
    String sql = "SELECT * FROM Rooms WHERE id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapResultSetToRoom(resultSet);
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return null;
  }

  @Override
  public void addUserToRoom(Integer roomId, Integer userId, Role role) {
    // Сначала проверяем, не забанен ли пользователь в этой комнате
    if (isUserBannedInRoom(roomId, userId)) {
      throw new IllegalStateException("User is banned from this room");
    }

    // Проверяем, не состоит ли пользователь уже в комнате
    if (isUserInMembers(roomId, userId)) {
      throw new IllegalStateException("User is already a member of this room");
    }

    // Проверяем, существует ли комната
    Room room = getRoomById(roomId);
    if (room == null) {
      throw new IllegalArgumentException("Room with id " + roomId + " does not exist");
    }

    String sql = """
    INSERT INTO Rooms_members (room_id, user_id, role_id)
    VALUES (?, ?, (SELECT id FROM Roles WHERE LOWER(role) = LOWER(?)))
    """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      statement.setString(3, role.name());

      int affectedRows = statement.executeUpdate();
//      connection.commit();
      if (affectedRows == 0) {
        throw new RuntimeException("Failed to add user to room line 131");
      }

      // Логирование успешного добавления - оставляем, но НЕ кидаем исключение!
      System.out.println("User " + userId + " added to room " + roomId + " with role " + role);

      // Для отладки можно добавить (без throw):
      System.out.println("Current users in room: " + getUsersInRoom(roomId));

    } catch (SQLException e) {
      System.err.println("Error adding user to room: " + e.getMessage());
      // Более специфичное исключение в зависимости от ошибки
      if (e.getMessage().contains("foreign key constraint")) {
        throw new IllegalArgumentException("User or room does not exist", e);
      } else if (e.getMessage().contains("unique constraint") ||
        e.getMessage().contains("duplicate key")) {
        throw new IllegalStateException("User is already a member of this room", e);
      }
      throw new RuntimeException("Failed to add user to room", e);
    }
  }

  // Дополнительный метод для проверки бана
  private boolean isUserBannedInRoom(Integer roomId, Integer userId) {
    String sql = "SELECT EXISTS(SELECT 1 FROM Bans WHERE room_id = ? AND user_id = ?)";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);
      statement.setInt(2, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getBoolean(1);
        }
      }
    } catch (SQLException e) {
      System.err.println("Error checking user ban: " + e.getMessage());
      throw new RuntimeException("Failed to check user ban status", e);
    }
    return false;
  }

  @Override
  public void removeUserFromRoom(Integer roomId, Integer userId) {
    String sql = "DELETE FROM Rooms_members WHERE room_id = ? AND user_id = ?";
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      int affectedRows = statement.executeUpdate();
      if (affectedRows == 0) {
        throw new RuntimeException();
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  public void addUserBan(Integer roomId, Integer userId) {
    String sql = """
        INSERT INTO Bans (room_id, user_id)
        VALUES (?, ?)
        ON CONFLICT (user_id, room_id) DO NOTHING
        """;
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      statement.executeUpdate();
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  @Override
  public void deleteRoom(Integer roomId) {
    deleteAllWithRooms(roomId);
    String sql = "DELETE FROM Rooms WHERE id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException();
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  /**
   * Удалить из БД всё, связанное с данной комнатой
   *
   * @param roomId
   */
  private void deleteAllWithRooms(Integer roomId) {
    String sql = """
        DELETE FROM BulletinBoard where room_id = ?;
        DELETE FROM Bans WHERE room_id = ?;
        DELETE FROM Rooms_requests WHERE room_id = ?;
        DELETE FROM Rooms_members WHERE room_id = ?;
        DELETE FROM Pictures WHERE room_id = ?;
        """;
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 1; i <= 5; i++) {
        statement.setInt(i, roomId);
      }
      statement.execute();

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  private Room mapResultSetToRoom(ResultSet resultSet) throws SQLException {
    Integer id = resultSet.getInt("id");
    boolean isActive = resultSet.getBoolean("is_active");
    boolean isPublic = resultSet.getBoolean("is_public");
    String chatLink = resultSet.getString("chat_link");
    Integer categoryId = resultSet.getInt("category_id");
    String name = resultSet.getString("name");
    String description = resultSet.getString("description");
    Instant startDate = resultSet.getTimestamp("start_date") != null ?
      resultSet.getTimestamp("start_date").toInstant() : null;
    Instant endDate = resultSet.getTimestamp("end_date") != null ?
      resultSet.getTimestamp("end_date").toInstant() : null;
    int ageRating = resultSet.getInt("age_rating");
    Instant frequency = resultSet.getTimestamp("frequency") != null ?
      resultSet.getTimestamp("frequency").toInstant() : null;
    int maxPeople = resultSet.getInt("maximum_number_of_people");
    Category category = qaRepository.getCategoryById(categoryId);

    return new Room(id, isActive, isPublic, chatLink, category, name, description,
      startDate, endDate, ageRating, frequency, maxPeople, null,
      null);
  }

  public Map<User, Role> getUsersInRoom(Integer roomId) {
    String sql = """
      SELECT u.id, r.role
      FROM Users AS u
      INNER JOIN Rooms_members AS rm ON rm.user_id = u.id
      INNER JOIN Roles AS r ON r.id = rm.role_id
      WHERE rm.room_id = ?;
        """;

    var usersInRoom = new HashMap<User, Role>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          User user = userRepository.getUserById(resultSet.getInt("id"));
          Role role = Role.valueOf(resultSet.getString("role").toUpperCase());
          usersInRoom.put(user, role);
        }
      }
    } catch (SQLException e) {
      System.err.println("Error getting users in room: " + e.getMessage());
      throw new RuntimeException("Failed to get room users", e);
    }
    return usersInRoom;
  }

  /**
   * Получение всех пользователей с баном в данной комнате
   *
   * @param roomId
   * @return
   */
  private List<Integer> getUsersWithBanInRoom(Integer roomId) {
    String sql = "select user_id from Bans where room_id = ?";
    var bans = new ArrayList<Integer>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          bans.add(resultSet.getInt(1));
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return bans;
  }

  public boolean isUserInMembers(Integer roomId, Integer userId) {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM Rooms_members
            WHERE room_id = ? AND user_id = ?
        )
        """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getBoolean(1);
        }
      }
    } catch (SQLException e) {
      System.err.println("Error checking if user is in room members: " + e.getMessage());
      throw new RuntimeException("Failed to check room membership", e);
    }
    return false;
  }

  public boolean isUserOwnerOfRoom(Integer userId, Integer roomId) {
    if (!isUserInMembers(roomId, userId)) {
      return false;
    }
    String sql = """
        select * from Rooms_members
        where user_id = ? and room_id = ? and
        role_id in (select id from Roles where role = 'owner')
        """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, userId);
      statement.setInt(2, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return true;
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return false;
  }

  public void setRoleByUserIdAndRoomId(Integer userId, Integer roomId, Role role) {
    String sql = """
        UPDATE Rooms_members
        SET role_id = (select id from Roles where LOWER(role) = LOWER(?))
        where room_id = ? and user_id = ?""";
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, role.name());
      statement.setInt(2, roomId);
      statement.setInt(3, userId);

      int affectedRows = statement.executeUpdate();

      if (affectedRows > 0) {
        return;
      } else {
        throw new RuntimeException();
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  public Role getUserRoleByRoomId(Integer roomId, Integer userId) {
    String sql = """
        SELECT r.role, rm.role_id
        FROM Rooms_members AS rm 
        LEFT JOIN Roles AS r ON r.id = rm.role_id
        WHERE rm.room_id = ? AND rm.user_id = ?""";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String roleFromDb = resultSet.getString("role");
          Integer roleId = resultSet.getInt("role_id");

          if (resultSet.wasNull() || roleFromDb == null) {
            System.err.println("WARNING: role_id = " + roleId + " but no matching role in Roles table");
            // Возвращаем роль по умолчанию
            return Role.PARTICIPANT;
          }

          return Role.valueOf(roleFromDb.toUpperCase());
        } else {
          throw new RuntimeException("User not found in room members");
        }
      }
    } catch (SQLException | IllegalArgumentException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException("Error getting user role", e);
    }
  }

  @Override
  public List<Room> getAllRooms() {
    String sql = "SELECT * FROM Rooms";
    var rooms = new ArrayList<Room>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        rooms.add(mapResultSetToRoom(resultSet));
      }
      return rooms;
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  public int getCategoryIdByName(String categoryName) {
    String sql = """
        SELECT id FROM Categories
        WHERE LOWER(name) = LOWER(?);
        """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, categoryName);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("id");
        }
      }
    } catch (SQLException e) {
      System.err.println("Error getting category ID by name: " + e.getMessage());
      throw new RuntimeException("Failed to retrieve category ID", e);
    }
    return -1;
  }
}