package com.coactivity.repository;

import com.coactivity.domain.Picture;
import java.util.List;

public interface PictureRepository {

  /**
   *
   * @param roomId
   * @return
   */
  Picture createPicture(Integer roomId);

  /**
   *
   * @param roomId
   * @return
   */
  List<Picture> getRoomPictures(Integer roomId);

  /**
   *
   * @param photoId
   */
  void deletePicture(Integer photoId);
}
