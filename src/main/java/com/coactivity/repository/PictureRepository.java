package com.coactivity.repository;

import com.coactivity.domain.Picture;
import com.coactivity.domain.Room;

import java.util.List;

public interface PictureRepository {
  /**
   *
   * @param roomId
   * @return
   */
  Picture createPicture(int roomId);

  /**
   *
   * @param roomId
   * @return
   */
  List<Picture> getRoomPictures(int roomId);

  /**
   *
   * @param photoId
   */
  void deletePicture(int photoId);
}
