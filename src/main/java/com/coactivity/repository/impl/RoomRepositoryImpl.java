package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Room;
import com.coactivity.repository.RoomRepository;

import java.sql.*;
import java.time.Instant;
import java.util.AbstractMap;

public class RoomRepositoryImpl implements RoomRepository {

  private final DataRepository dataRepository;

  public RoomRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
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
      int newRoomId = roomId != 0 ? roomId : room.getRoomId();

      statement.setBoolean(1, newIsActive);
      statement.setBoolean(2, newIsVisible);
      statement.setString(3, newDescription);
      statement.setTimestamp(4, newDateOfStart);
      statement.setTimestamp(5, newDateOfEnd);
      statement.setInt(6, newAgeRating);
      statement.setInt(7, newFrequency);
      statement.setInt(8, newMaximumNumberOfPeople);
      statement.setInt(9, newRoomId);

      room.setActive(newIsActive);
      room.setVisible(newIsVisible);
      room.setDescription(newDescription);
      room.setDateOfStartEvent(newDateOfStart != null ? newDateOfStart.toInstant() : null);
      room.setDateOfEndEvent(newDateOfEnd != null ? newDateOfEnd.toInstant() : null);
      room.setAgeRating(newAgeRating);
      room.setFrequency(newFrequency);
      room.setMaximumNumberOfPeople(newMaximumNumberOfPeople);
      room.setRoomId(newRoomId);

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
    boolean isActive = resultSet.getBoolean("is_active");
    boolean isVisible = resultSet.getBoolean("is_visible");
    String chatLink = resultSet.getString("chat_link");
    int categoryId = resultSet.getInt("category_id");
    String categoryName = resultSet.getString("category_name");
    String name = resultSet.getString("name");
    String description = resultSet.getString("description");
    Instant startDate = resultSet.getTimestamp("start_date") != null ?
      resultSet.getTimestamp("start_date").toInstant() : null;
    Instant endDate = resultSet.getTimestamp("end_date") != null ?
      resultSet.getTimestamp("end_date").toInstant() : null;
    int ageRating = resultSet.getInt("age_rating");
    int frequency = resultSet.getInt("frequency");
    int maxPeople = resultSet.getInt("maximum_number_of_people");
    int currentPeople = resultSet.getInt("current_number_of_people");

    return new Room(id, isActive, isVisible, chatLink, categoryId, name, description,
      startDate, endDate, ageRating, frequency, maxPeople, currentPeople);
  }
}