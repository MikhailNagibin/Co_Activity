package com.coactivity.repository;

import com.coactivity.domain.Picture;
import java.util.List;

public interface PictureRepository {

  /**
   *
   * @param roomId
   * @param storageKey
   * @param originalFilename
   * @param contentType
   * @param sizeBytes
   * @param sortOrder
   * @return
   */
  Picture createPicture(Integer roomId, String storageKey, String originalFilename,
      String contentType, long sizeBytes, int sortOrder);

  /**
   *
   * @param roomId
   * @return
   */
  List<Picture> getRoomPictures(Integer roomId);

  Picture getRoomPicture(Integer roomId, Integer photoId);

  long countRoomPictures(Integer roomId);

  /**
   *
   * @param photoId
   */
  void deletePicture(Integer photoId);

  void updatePictureSortOrder(Integer photoId, int sortOrder);

  void deleteAllPicturesByRoomId(Integer roomId);
}
