package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Picture;
import com.coactivity.repository.PictureRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PictureRepositoryImpl implements PictureRepository {

  private final DataRepository dataRepository;
  private final RoomRepositoryImpl roomRepository;

  public PictureRepositoryImpl(DataRepository dataRepository, RoomRepositoryImpl roomRepository) {
    this.dataRepository = dataRepository;
    this.roomRepository = roomRepository;
  }

  @Override
  public Picture createPicture(Integer roomId) {
    String sql = "INSERT INTO Pictures (room_id) VALUES (?) RETURNING picture_id";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Integer generatedPictureId = resultSet.getInt("picture_id");
          return new Picture(roomRepository.getRoomById(roomId), generatedPictureId);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException();
    }
    throw new RuntimeException();
  }

  @Override
  public List<Picture> getRoomPictures(Integer roomId) {
    var pictures = new ArrayList<Picture>();
    String sql = "SELECT * FROM Pictures WHERE room_id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, roomId);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          Picture picture = mapResultSetToPicture(resultSet);
          pictures.add(picture);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException();
    }
    return pictures;
  }

  @Override
  public void deletePicture(Integer photoId) {
    String sql = "DELETE FROM Pictures WHERE picture_id = ?";

    try (Connection connection = dataRepository.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, photoId);
      int affectedRows = statement.executeUpdate();

      if (affectedRows == 0) {
        throw new RuntimeException();
      }
    } catch (SQLException e) {

      throw new RuntimeException();
    }
  }

  private Picture mapResultSetToPicture(ResultSet resultSet) throws SQLException {
    Integer pictureId = resultSet.getInt("picture_id");
    Integer roomId = resultSet.getInt("room_id");

    return new Picture(roomRepository.getRoomById(roomId), pictureId);
  }
}