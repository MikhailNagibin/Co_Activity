package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.*;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.impl.UserRepositoryImpl;



import java.sql.*;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class RoomRepositoryImpl implements RoomRepository {

  private final DataRepository dataRepository;
  private final UserRepositoryImpl userRepository;

  public RoomRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.userRepository = new UserRepositoryImpl(dataRepository);
  }

  @Override
  public Room createRoom(boolean isActive, boolean isVisible, String chatLink, int categoryId,
                         String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
                         int ageRating, int frequency, int maximumNumberOfPeople,
                         AbstractMap.SimpleEntry<Integer, Integer> users) {

    String sql = """
      INSERT INTO rooms (is_active, is_visible, chat_link, category_id, name, description, start_date, end_date,
                          age_rating, frequency, maximum_number_of_people, current_number_of_people)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
      RETURNING id
      """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setBoolean(1, isActive);
      statement.setBoolean(2, isVisible);
      statement.setString(3, chatLink);
      statement.setInt(4, categoryId);
      statement.setString(5, name);
      statement.setString(6, description);
      statement.setTimestamp(7, dateOfStartEvent != null ? Timestamp.from(dateOfStartEvent) : null);
      statement.setTimestamp(8, dateOfEndEvent != null ? Timestamp.from(dateOfEndEvent) : null);
      statement.setInt(9, ageRating);
      statement.setInt(10, frequency);
      statement.setInt(11, maximumNumberOfPeople);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int roomId = resultSet.getInt("id");
          if (users != null) {
            addUserToRoom(roomId, users.getKey(), users.getValue());
          }
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
  public Room updateRoom(Room room, int roomId, boolean isActive, boolean isVisible, String description,
                         Instant dateOfStartEvent, Instant dateOfEndEvent, int ageRating,
                         int frequency, int maximumNumberOfPeople) {

    String sql = """
      UPDATE rooms
      SET is_active = ?, is_visible = ?, description = ?, start_date = ?,
          end_date = ?, age_rating = ?, frequency = ?, maximum_number_of_people = ?
      WHERE id = ?
      """;

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      boolean newIsActive = isActive; // если isActive не может быть null, то просто присваиваем
      boolean newIsVisible = isVisible; // аналогично для boolean
      String newDescription = description != null ? description : room.getDescription();
      Timestamp newDateOfStart = dateOfStartEvent != null ? Timestamp.from(dateOfStartEvent) :
        (room.getDateOfStartEvent() != null ? Timestamp.from(room.getDateOfStartEvent()) : null);
      Timestamp newDateOfEnd = dateOfEndEvent != null ? Timestamp.from(dateOfEndEvent) :
        (room.getDateOfEndEvent() != null ? Timestamp.from(room.getDateOfEndEvent()) : null);
      int newAgeRating = ageRating != 0 ? ageRating : room.getAgeRating(); // предполагая, что 0 - значение по умолчанию
      int newFrequency = frequency != 0 ? frequency : room.getFrequency();
      int newMaximumNumberOfPeople = maximumNumberOfPeople != 0 ? maximumNumberOfPeople : room.getMaximumNumberOfPeople();

      statement.setBoolean(1, newIsActive);
      statement.setBoolean(2, newIsVisible);
      statement.setString(3, newDescription);
      statement.setTimestamp(4, newDateOfStart);
      statement.setTimestamp(5, newDateOfEnd);
      statement.setInt(6, newAgeRating);
      statement.setInt(7, newFrequency);
      statement.setInt(8, newMaximumNumberOfPeople);

      room.setActive(newIsActive);
      room.setVisible(newIsVisible);
      room.setDescription(newDescription);
      room.setDateOfStartEvent(newDateOfStart != null ? newDateOfStart.toInstant() : null);
      room.setDateOfEndEvent(newDateOfEnd != null ? newDateOfEnd.toInstant() : null);
      room.setAgeRating(newAgeRating);
      room.setFrequency(newFrequency);
      room.setMaximumNumberOfPeople(newMaximumNumberOfPeople);

      int affectedRows = statement.executeUpdate();

      if (affectedRows > 0) {
        return getRoomById(roomId);
      } else {
        throw new RuntimeException();
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
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
    boolean isActive = resultSet.getBoolean("isActive");
    boolean isVisible = resultSet.getBoolean("isPrivate");
    String chatLink = resultSet.getString("chatLink");
    int categoryId = resultSet.getInt("categoryId");
    String name = resultSet.getString("name");
    String description = resultSet.getString("description");
    Instant startDate = resultSet.getTimestamp("startDate") != null ?
      resultSet.getTimestamp("startDate").toInstant() : null;
    Instant endDate = resultSet.getTimestamp("endDate") != null ?
      resultSet.getTimestamp("endDate").toInstant() : null;
    int ageRating = resultSet.getInt("ageRating");
    int frequency = resultSet.getInt("frequency");
    int maxPeople = resultSet.getInt("maximumNumberOfPeople");
    Category category = Category.getByIndex(categoryId);

    return new Room(id, isActive, isVisible, chatLink, category, name, description,
      startDate, endDate, ageRating, frequency, maxPeople, getUsersInRoom(id));
  }

  private Map<User, Role> getUsersInRoom(int roomId) {
    String sql = """
      SELECT u.id, r.id FROM user AS u INNER JOIN Rooms_members AS rm ON rm.userId = u.id
       INNER JOIN role AS r ON r.id = rm.RoleId;
      """;
    var usersInRoom = new HashMap<User, Role>();
    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

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
}