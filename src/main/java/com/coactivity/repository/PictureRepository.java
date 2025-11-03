package com.coactivity.repository;

import com.coactivity.domain.Picture;
import com.coactivity.domain.Room;

import java.util.List;

public interface PictureRepository {
  /*
  создание картинки

  @param Room room
  @param int photoId - номер картинки в системе
  @return Picture
   */
  Picture createPicture(Room room, int photoId);

  /*
  получение всех картинок по комнате

  @param Room room
  @return List<Picture>
   */
  List<Picture> getRoomPictures(Room room);

  /*
  изменить фотографию

  @param int oldPhotoId
  @param int newPhotoId
  @return Picture
   */
  Picture updatePicture(int oldPhotoId, int newPhotoId);

  /*
  удалить фотографию

  @param int photoId
   */
  void deletePicture(int photoId);
}
