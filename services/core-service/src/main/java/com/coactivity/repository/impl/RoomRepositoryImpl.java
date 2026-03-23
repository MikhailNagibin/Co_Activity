package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Category;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ValidationException;
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
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepositoryImpl implements RoomRepository {

  private final DataRepository dataRepository;
  private final QuestionRepositoryImpl qaRepository;
  private final UserRepository userRepository;

  public RoomRepositoryImpl(DataRepository dataRepository, QuestionRepositoryImpl qaRepository,
      UserRepository userRepository) {
    this.dataRepository = dataRepository;
    this.qaRepository = qaRepository;
    this.userRepository = userRepository;
  }

  @Override
  public Room createRoom(Integer ownerId, RoomCreationRequest request) {
    Integer roomId = dataRepository.inTransaction(connection -> {
      String sql = """
          INSERT INTO Rooms (is_active, is_public, chat_link, category_id, name, description, start_date, end_date,
                              age_rating, frequency, maximum_number_of_people)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          RETURNING id
          """;

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBoolean(1, true);
        statement.setBoolean(2, request.getIsPublic());
        statement.setString(3, request.getChatLink());
        statement.setInt(4, getCategoryIdByNameInTransaction(connection, request.getCategory()));
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
            Integer createdRoomId = resultSet.getInt("id");
            addUserToRoomInTransaction(connection, createdRoomId, ownerId, Role.OWNER);
            return createdRoomId;
          }
        }
      }

      throw new RuntimeException("Failed to create room");
    });

    return getRoomById(roomId);
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
      throw new RuntimeException("Failed to get room with id: " + roomId, e);
    }
    return null;
  }

  @Override
  public void addUserToRoom(Integer roomId, Integer userId, Role role) {
    try (Connection connection = dataRepository.getDataSource().getConnection()) {
      addUserToRoomInTransaction(connection, roomId, userId, role);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to add user to room", e);
    }
  }

  public boolean isUserBannedInRoom(Integer roomId, Integer userId) {
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
      throw new RuntimeException("Failed to check user ban status", e);
    }
    return false;
  }

  public boolean isUserBannedInTransaction(Connection connection, Integer roomId, Integer userId) {
    String sql = "SELECT EXISTS(SELECT 1 FROM Bans WHERE room_id = ? AND user_id = ?)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getBoolean(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check user ban status", e);
    }

    return false;
  }

  public int getRoomParticipantCount(Integer roomId) {
    String sql = "SELECT COUNT(*) FROM Rooms_members WHERE room_id = ?";
    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get room participant count", e);
    }
    return 0;
  }

  public int getRoomParticipantCountInTransaction(Connection connection, Integer roomId) {
    String sql = "SELECT COUNT(*) FROM Rooms_members WHERE room_id = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get room participant count", e);
    }

    return 0;
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
        throw new RuntimeException(
            "User " + userId + " is not a member of room " + roomId);
      }
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to remove user " + userId + " from room " + roomId, e);
    }
  }

  public void addUserBan(Integer roomId, Integer userId) {
    try (Connection connection = dataRepository.getDataSource().getConnection()) {
      addUserBanInTransaction(connection, roomId, userId);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to add user ban", e);
    }
  }

  @Override
  public void deleteRoom(Integer roomId) {
    dataRepository.inTransaction(connection -> {
      deleteRoomInTransaction(connection, roomId);
      return null;
    });
  }

  /**
   * Удалить из БД всё, связанное с данной комнатой
   *
   * @param roomId
   */
  private void deleteAllWithRoomsInTransaction(Connection connection, Integer roomId) {
    deleteByRoomId(connection, "DELETE FROM BulletinBoard WHERE room_id = ?", roomId);
    deleteByRoomId(connection, "DELETE FROM Bans WHERE room_id = ?", roomId);
    deleteByRoomId(connection, "DELETE FROM Rooms_requests WHERE room_id = ?", roomId);
    deleteByRoomId(connection, "DELETE FROM Rooms_members WHERE room_id = ?", roomId);
    deleteByRoomId(connection, "DELETE FROM Pictures WHERE room_id = ?", roomId);
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
      throw new RuntimeException("Failed to get bans for room: " + roomId, e);
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
      throw new RuntimeException("Failed to check room membership", e);
    }
    return false;
  }

  public boolean isUserInMembersInTransaction(Connection connection, Integer roomId, Integer userId) {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM Rooms_members
            WHERE room_id = ? AND user_id = ?
        )
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getBoolean(1);
        }
      }
    } catch (SQLException e) {
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
      throw new RuntimeException(
          "Failed to check whether user " + userId + " owns room " + roomId, e);
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
        throw new RuntimeException(
            "Room membership not found for user " + userId + " in room " + roomId);
      }
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to update role for user " + userId + " in room " + roomId, e);
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
          int roleId = resultSet.getInt("role_id");

          if (resultSet.wasNull() || roleFromDb == null) {
            throw new RuntimeException(
                "Role mapping not found for role_id " + roleId + " in room " + roomId);
          }

          return Role.valueOf(roleFromDb.toUpperCase());
        } else {
          throw new RuntimeException("User not found in room members");
        }
      }
    } catch (SQLException | IllegalArgumentException e) {
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
      throw new RuntimeException("Failed to get rooms", e);
    }
  }

  public int getCategoryIdByName(String categoryName) {
    try (Connection connection = dataRepository.getDataSource().getConnection()) {
      return getCategoryIdByNameInTransaction(connection, categoryName);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to retrieve category ID", e);
    }
  }

  public int getCategoryIdByNameInTransaction(Connection connection, String categoryName) {
    String sql = """
        SELECT id FROM Categories
        WHERE LOWER(name) = LOWER(?);
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, categoryName);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("id");
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to retrieve category ID", e);
    }

    throw new ValidationException("Category not found: " + categoryName);
  }

  public void addUserToRoomInTransaction(Connection connection, Integer roomId, Integer userId,
      Role role) {
    if (isUserBannedInTransaction(connection, roomId, userId)) {
      throw new IllegalStateException("User is banned from this room");
    }

    if (isUserInMembersInTransaction(connection, roomId, userId)) {
      throw new IllegalStateException("User is already a member of this room");
    }

    if (!roomExistsInTransaction(connection, roomId)) {
      throw new IllegalArgumentException("Room with id " + roomId + " does not exist");
    }

    String sql = """
        INSERT INTO Rooms_members (room_id, user_id, role_id)
        VALUES (?, ?, (SELECT id FROM Roles WHERE LOWER(role) = LOWER(?)))
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      statement.setString(3, role.name());

      int affectedRows = statement.executeUpdate();
      if (affectedRows == 0) {
        throw new RuntimeException("Failed to add user to room");
      }
    } catch (SQLException e) {
      String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
      if (message.contains("foreign key constraint")) {
        throw new IllegalArgumentException("User or room does not exist", e);
      }
      if (message.contains("unique constraint") || message.contains("duplicate key")) {
        throw new IllegalStateException("User is already a member of this room", e);
      }
      throw new RuntimeException("Failed to add user to room", e);
    }
  }

  public void addUserBanInTransaction(Connection connection, Integer roomId, Integer userId) {
    String sql = """
        INSERT INTO Bans (room_id, user_id)
        VALUES (?, ?)
        ON CONFLICT (user_id, room_id) DO NOTHING
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to add user ban", e);
    }
  }

  public void deleteRoomInTransaction(Connection connection, Integer roomId) {
    deleteAllWithRoomsInTransaction(connection, roomId);

    String sql = "DELETE FROM Rooms WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException("Room not found with id: " + roomId);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete room", e);
    }
  }

  private boolean roomExistsInTransaction(Connection connection, Integer roomId) {
    String sql = "SELECT EXISTS(SELECT 1 FROM Rooms WHERE id = ?)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getBoolean(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to verify room existence", e);
    }

    return false;
  }

  private void deleteByRoomId(Connection connection, String sql, Integer roomId) {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete room related records", e);
    }
  }
}
