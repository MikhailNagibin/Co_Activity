package com.coactivity.repository;

import com.coactivity.domain.Picture;
import com.coactivity.domain.Room;

import java.util.List;

public interface PictureRepository {
  /*
  создание картинки

  @param int roomId
  @return Picture
   */
  Picture createPicture(int roomId);

  /*
  получение всех картинок по комнате

  @param int roomId
  @return List<Picture>
   */
  List<Picture> getRoomPictures(int roomId);

  /*
  удалить фотографию

  @param int photoId
   */
  void deletePicture(int photoId);
}
