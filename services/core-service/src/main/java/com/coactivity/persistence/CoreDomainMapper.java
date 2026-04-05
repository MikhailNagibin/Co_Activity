package com.coactivity.persistence;

import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Notification;
import com.coactivity.domain.Picture;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.persistence.entity.BulletinBoardEntity;
import com.coactivity.persistence.entity.PictureEntity;
import com.coactivity.persistence.entity.RoomEntity;
import com.coactivity.persistence.entity.RoomsRequestEntity;
import com.coactivity.persistence.entity.UserEntity;
import java.util.List;
import java.util.Map;

public final class CoreDomainMapper {

  private CoreDomainMapper() {
  }

  public static User toUser(UserEntity entity, List<Room> rooms, List<Notification> notifications) {
    if (entity == null) {
      return null;
    }
    return new User(
        entity.getId(),
        entity.getLogin(),
        entity.getUserName(),
        entity.getPassword(),
        entity.getDataOfBirth(),
        entity.getCountry(),
        entity.getCity(),
        entity.getDescription(),
        entity.getAvatarId(),
        rooms,
        notifications);
  }

  public static User toUserSummary(UserEntity entity) {
    return toUser(entity, List.of(), List.of());
  }

  public static Room toRoom(RoomEntity entity) {
    return toRoom(entity, null, null);
  }

  public static Room toRoom(RoomEntity entity, Map<User, Role> users, List<User> bans) {
    if (entity == null) {
      return null;
    }
    return new Room(
        entity.getId(),
        entity.isActive(),
        entity.isPublicRoom(),
        entity.getChatLink(),
        CoreLookupMapper.toCategory(entity.getCategory().getName()),
        entity.getName(),
        entity.getDescription(),
        entity.getDateOfStartEvent(),
        entity.getDateOfEndEvent(),
        entity.getAgeRating(),
        entity.getFrequency(),
        entity.getMaximumNumberOfPeople(),
        users,
        bans);
  }

  public static BulletinBoard toBulletinBoard(BulletinBoardEntity entity, Room room, User author) {
    if (entity == null) {
      return null;
    }
    return new BulletinBoard(
        entity.getId(),
        room,
        entity.getContent(),
        author,
        entity.getUpdatedAt());
  }

  public static Picture toPicture(PictureEntity entity, Room room) {
    if (entity == null) {
      return null;
    }
    return new Picture(room, entity.getId());
  }

  public static RoomsRequest toRoomsRequest(RoomsRequestEntity entity, User user, Room room) {
    if (entity == null) {
      return null;
    }
    return new RoomsRequest(
        entity.getId(),
        user,
        room,
        entity.getCreatedAt(),
        CoreLookupMapper.toRequestStatus(entity.getStatus().getStatusInfo()));
  }
}
