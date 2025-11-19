package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.*;
import com.coactivity.repository.RoomRepository;


import java.sql.*;
import java.time.Instant;
import java.util.*;

public class RoomRepositoryImpl implements RoomRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;

  public RoomRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.userRepository = new UserRepositoryImpl(dataRepository);
  }

  @Override
  public Room createRoom(int ownerId, RoomCreationRequest request) {

    String sql = """
      INSERT INTO rooms (is_active, is_private, chat_link, category_id, name, description, start_date, end_date,
                          age_rating, frequency, maximum_number_of_people)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      RETURNING id
      """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setBoolean(1, true);
      statement.setBoolean(2, request.getIsPublic());
      statement.setString(3, request.getChatLink());
      statement.setInt(4, request.getCategoryId());
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
          int roomId = resultSet.getInt("id");
          addUserToRoom(roomId, ownerId, 1);
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
  public Room getRoomById(int roomId) {
    String sql = "SELECT * FROM rooms WHERE id = ?";

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
  public void addUserToRoom(int roomId, int userId, int roleId) {
    String sql = "INSERT INTO rooms_members (room_id, user_id, role_id) " +
      "VALUES (?, ?, ?)";
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setInt(2, userId);
      statement.setInt(3, roleId);
      statement.executeUpdate();
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }

  @Override
  public void deleteRoom(int roomId) {
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
  private void deleteAllWithRooms(int roomId) {
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
    int id = resultSet.getInt("id");
    boolean isActive = resultSet.getBoolean("is_active");
    boolean isVisible = resultSet.getBoolean("is_private");
    String chatLink = resultSet.getString("chat_link");
    int categoryId = resultSet.getInt("category_id");
    String name = resultSet.getString("name");
    String description = resultSet.getString("description");
    Instant startDate = resultSet.getTimestamp("start_date") != null ?
      resultSet.getTimestamp("start_date").toInstant() : null;
    Instant endDate = resultSet.getTimestamp("end_date") != null ?
      resultSet.getTimestamp("end_date").toInstant() : null;
    int ageRating = resultSet.getInt("age_rating");
    Instant frequency = resultSet.getTimestamp("frequency").toInstant();
    int maxPeople = resultSet.getInt("maximum_number_of_people");
    Category category = Category.getByIndex(categoryId);

    return new Room(id, isActive, isVisible, chatLink, category, name, description,
      startDate, endDate, ageRating, frequency, maxPeople, getUsersInRoom(id), getUsersWithBanInRoom(id));
  }

  private Map<User, Role> getUsersInRoom(int roomId) {
    String sql = """
      SELECT u.id, r.id FROM user AS u INNER JOIN Rooms_members AS rm ON rm.user_id = u.id
       INNER JOIN role AS r ON r.id = rm.Role_id
       where rm.room_id = ?;
      """;
    var usersInRoom = new HashMap<User, Role>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setInt(1, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          User user = userRepository.getUserById(resultSet.getInt(1));
          Role role = Role.getByIndex(resultSet.getInt(2) - 1);
          usersInRoom.put(user, role);
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return usersInRoom;
  }

  /**
   * Получение всех пользователей с баном в данной комнате
   *
   * @param roomId
   * @return
   */
  private List<User> getUsersWithBanInRoom(int roomId) {
    String sql = "select user_id from Bans where room_id = ?";
    var bans = new ArrayList<User>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          bans.add(userRepository.getUserById(resultSet.getInt(1)));
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return bans;
  }

  public boolean isUserInMembers(int roomId, int userId) {
    Room room = getRoomById(roomId);
    Map<User, Role> users = room.getUsers();

    return users.containsKey(userRepository.getUserById(userId));
  }

  public boolean isUserOwnerOfRoom(int userId, int roomId) {
    if (!isUserInMembers(roomId, userId)) {
      return false;
    }
    String sql = """
      select * from Rooms_members
      where user_id = ? and room_id = ? and
      role_id in (select id from Roles where role == 'Owner')
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

  public void setRoleByUserIdAndRoomId(int userId, int roomId, Role role) {
    String sql = """
      UPDATE Rooms_members
      SET role_id = (select id from Roles where role = ?)
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

  public Role getUserRoleByRoomId(int roomId, int userId) {
    String sql = """
      select r.role from Rooms_members as rm inner join Roles as r on r.id = rm.role_id
       where rm.room_id = ? and rm.user_id = ?""";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, userId);
      statement.setInt(2, roomId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Role.valueOf(resultSet.getString(1).toUpperCase());
        } else {
          throw new RuntimeException();
        }
      }
    } catch (SQLException | IllegalArgumentException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
  }
}