package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Ban;
import com.coactivity.repository.BanRepository;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BanRepositoryImpl implements BanRepository {

  private final DataRepository dataRepository;

  public BanRepositoryImpl(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  @Override
  public Ban createBan(int userId, int roomId, Duration durationOfBan, Instant dateOfBan) {
    String sql = "INSERT INTO bans (user_id, room_id, durationOfBan, ban_date) VALUES (?, ?, ?, ?)";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);
      statement.setInt(2, roomId);
      statement.setTime(3, Time.valueOf(convertDurationToTime(durationOfBan)));
      statement.setTimestamp(4, Timestamp.from(dateOfBan)); // Используем Timestamp для Instant

      int affectedRows = statement.executeUpdate();

      if (affectedRows > 0) {
        return new Ban(userId, roomId, durationOfBan, dateOfBan);
      }

    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public List<Ban> getBansByRoom(int roomId) {
    var bans = new ArrayList<Ban>();
    String sql = "SELECT * FROM bans WHERE room_id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          var ban = mapResultSetToBan(resultSet);
          bans.add(ban);
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return bans;
  }

  @Override
  public List<Ban> getBansByUser(int userId) {
    var bans = new ArrayList<Ban>();
    String sql = "SELECT * FROM bans WHERE user_id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, userId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          var ban = mapResultSetToBan(resultSet);
          bans.add(ban);
        }
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException();
    }
    return bans;
  }

  private Ban mapResultSetToBan(ResultSet resultSet) throws SQLException {
    int userId = resultSet.getInt("user_id");
    int roomId = resultSet.getInt("room_id");
    Duration duration = convertTimeToDuration(resultSet.getTime("durationOfBan"));
    Instant banDate = resultSet.getTimestamp("ban_date").toInstant();

    return new Ban(userId, roomId, duration, banDate);
  }

  private String convertDurationToTime(Duration duration) {
    long seconds = duration.getSeconds();
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, secs);
  }

  private Duration convertTimeToDuration(Time time) {
    String timeString = time.toString();
    String[] parts = timeString.split(":");
    long hours = Long.parseLong(parts[0]);
    long minutes = Long.parseLong(parts[1]);
    long seconds = Long.parseLong(parts[2]);
    return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
  }
}