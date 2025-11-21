package com.coactivity.service;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.Role;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.BulletinBoardRepositoryImpl;
import com.coactivity.repository.impl.PictureRepositoryImpl;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

// TODO:
//  add following methods: createRoom, getRooms, getRoomById, deleteRoom
@Service
public class RoomService {

  private final RoomRepositoryImpl roomRepository;
  private final TokenService tokenService;
  private final PictureRepositoryImpl pictureRepository;
  private final BulletinBoardRepositoryImpl bulletinBoardRepository;

  public RoomService(RoomRepositoryImpl roomRepository,
      TokenService tokenService,
      PictureRepositoryImpl pictureRepository,
      BulletinBoardRepositoryImpl bulletinBoardRepository) {
    this.roomRepository = roomRepository;
    this.tokenService = tokenService;
    this.pictureRepository = pictureRepository;
    this.bulletinBoardRepository = bulletinBoardRepository;
  }

  /**
   * Creates a new room and assigns the token owner as the room OWNER.
   */
  public ApiResponse<RoomCreationResponse> createRoom(String token, RoomCreationRequest request) {
    if (token == null || !tokenService.isTokenActive(token)) {
      return ApiResponse.error("401");
    }

    if (request == null
        || request.getName() == null || request.getName().trim().isEmpty()
        || request.getCategoryId() == null
        || request.getMaximumNumberOfPeople() == null) {
      return ApiResponse.error("400");
    }

    try {
      Integer ownerId = tokenService.decodeToken(token).userId();
      Room createdRoom = roomRepository.createRoom(ownerId, request);
      if (createdRoom == null) {
        return ApiResponse.error("500");
      }

      RoomCreationResponse response =
          new RoomCreationResponse(createdRoom.getId(), createdRoom.getName(),
              createdRoom.getCategory());
      return ApiResponse.success(response);
    } catch (IllegalArgumentException e) {
      return ApiResponse.error("400");
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  /**
   * Public room listing with basic filtering and sorting.
   * <p>
   * If a token is provided, the response annotates whether the current user participates
   * in each room.
   * </p>
   */
  public ApiResponse<List<RoomSummaryResponse>> getRooms(String token, RoomFilter filter,
      RoomSort sortBy) {
    try {
      final Integer currentUserId =
          (token != null && tokenService.isTokenActive(token))
              ? tokenService.decodeToken(token).userId()
              : null;

      List<Room> rooms = roomRepository.getAllRooms();
      if (rooms.isEmpty()) {
        return ApiResponse.success(Collections.emptyList());
      }

      List<RoomSummaryResponse> responses = rooms.stream()
          .filter(room -> filter == null || matchesFilter(room, filter))
          .sorted(buildComparator(sortBy))
          .map(room -> mapRoomToSummaryResponse(room,
              currentUserId != null && roomRepository.isUserInMembers(room.getId(), currentUserId)))
          .collect(Collectors.toList());

      return ApiResponse.success(responses);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  /**
   * Returns detailed information about a room. Protected fields (chat link, bulletin board) are
   * only included when the user is an authenticated participant of the room.
   */
  public ApiResponse<RoomDetailedResponse> getRoomById(Integer roomId, String token) {
    if (roomId == null) {
      return ApiResponse.error("400");
    }

    try {
      Room room = roomRepository.getRoomById(roomId);
      if (room == null) {
        return ApiResponse.error("404");
      }

      Integer currentUserId = null;
      boolean hasProtectedAccess = false;
      Boolean isCurrentUserParticipant = null;

      if (token != null && tokenService.isTokenActive(token)) {
        currentUserId = tokenService.decodeToken(token).userId();
        hasProtectedAccess = roomRepository.isUserInMembers(roomId, currentUserId);
        isCurrentUserParticipant = hasProtectedAccess;
      }

      RoomSummaryResponse summary =
          mapRoomToSummaryResponse(room, isCurrentUserParticipant);

      BulletinBoardResponse bulletinDto = null;
      if (hasProtectedAccess) {
        BulletinBoard board = bulletinBoardRepository.getBulletinBoard(roomId);
        if (board != null) {
          bulletinDto = mapBulletinBoardToResponse(board);
        }
      }

      RoomDetailedResponse detailed = new RoomDetailedResponse();
      detailed.setId(summary.getId());
      detailed.setActive(summary.isActive());
      detailed.setIsPublic(summary.getIsPublic());
      detailed.setCategory(summary.getCategory());
      detailed.setName(summary.getName());
      detailed.setDescription(summary.getDescription());
      detailed.setDateOfStartEvent(summary.getDateOfStartEvent());
      detailed.setDateOfEndEvent(summary.getDateOfEndEvent());
      detailed.setAgeRating(summary.getAgeRating());
      detailed.setFrequency(summary.getFrequency());
      detailed.setParticipantCount(summary.getParticipantCount());
      detailed.setMaximumParticipants(summary.getMaximumParticipants());
      detailed.setCreator(summary.getCreator());
      detailed.setIsCurrentUserParticipant(summary.getIsCurrentUserParticipant());
      detailed.setImageIds(summary.getImageIds());

      detailed.setHasProtectedAccess(hasProtectedAccess);
      detailed.setChatLink(hasProtectedAccess ? room.getChatLink() : null);
      detailed.setBulletinBoard(bulletinDto);

      return ApiResponse.success(detailed);
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  public ApiResponse<Void> deleteRoom(String token, Integer roomId) {
    if (roomRepository.getUserRoleByRoomId(roomId, tokenService.decodeToken(token).userId())
        .equals(
            Role.OWNER)) {
      roomRepository.deleteRoom(roomId);
      return ApiResponse.success(null);
    } else {
      return ApiResponse.error("401");
    }
  }

  private boolean matchesFilter(Room room, RoomFilter filter) {
    if (filter == null) {
      return true;
    }
    if (filter.getCategory() != null && room.getCategory() != filter.getCategory()) {
      return false;
    }
    if (filter.getIsPublic() != null && room.isPublic() != filter.getIsPublic()) {
      return false;
    }
    if (filter.getMaxParticipants() != null
        && room.getMaximumNumberOfPeople() > filter.getMaxParticipants()) {
      return false;
    }
    if (filter.getQuery() != null && !filter.getQuery().trim().isEmpty()) {
      String query = filter.getQuery().trim().toLowerCase();
      String name = room.getName() != null ? room.getName().toLowerCase() : "";
      String description = room.getDescription() != null ? room.getDescription().toLowerCase() : "";
      if (!name.contains(query) && !description.contains(query)) {
        return false;
      }
    }
    // Room entity does not contain city/country fields, so these filters are ignored for now.
    return true;
  }

  private Comparator<Room> buildComparator(RoomSort sortBy) {
    RoomSort effectiveSort = sortBy != null ? sortBy : RoomSort.NEWEST;
    return switch (effectiveSort) {
      case POPULAR ->
          Comparator.comparingInt((Room room) -> room.getUsers() != null ? room.getUsers().size() : 0)
              .reversed();
      case NAME ->
          Comparator.comparing(room -> room.getName() != null ? room.getName().toLowerCase() : "",
              Comparator.naturalOrder());
      case UPCOMING ->
          Comparator.comparing(Room::getDateOfStartEvent,
              Comparator.nullsLast(Comparator.naturalOrder()));
      case NEWEST ->
          Comparator.comparing(Room::getId,
              Comparator.nullsLast(Comparator.reverseOrder()));
    };
  }

  private RoomSummaryResponse mapRoomToSummaryResponse(Room room,
      Boolean isCurrentUserParticipant) {
    RoomSummaryResponse response = new RoomSummaryResponse();
    response.setId(room.getId());
    response.setActive(room.isActive());
    response.setIsPublic(room.isPublic());
    response.setCategory(room.getCategory());
    response.setName(room.getName());
    response.setDescription(room.getDescription());
    response.setDateOfStartEvent(room.getDateOfStartEvent());
    response.setDateOfEndEvent(room.getDateOfEndEvent());
    response.setAgeRating(room.getAgeRating());
    response.setFrequency(room.getFrequency());

    Map<User, Role> users = room.getUsers();
    int participantCount = users != null ? users.size() : 0;
    response.setParticipantCount(participantCount);
    response.setMaximumParticipants(room.getMaximumNumberOfPeople());

    User creator = findOwner(users);
    if (creator != null) {
      response.setCreator(mapUserToSummaryResponse(creator));
    }

    response.setIsCurrentUserParticipant(isCurrentUserParticipant);

    // Map picture IDs
    List<Integer> imageIds = new ArrayList<>();
    try {
      pictureRepository.getRoomPictures(room.getId())
          .forEach(p -> imageIds.add(p.getPhotoId()));
    } catch (Exception e) {
      // If pictures cannot be loaded, we simply leave the list empty
    }
    response.setImageIds(imageIds);

    return response;
  }

  private UserSummaryResponse mapUserToSummaryResponse(User user) {
    UserSummaryResponse dto = new UserSummaryResponse();
    dto.setId(user.getId());
    dto.setUserName(user.getUserName());
    dto.setDateOfBirth(user.getDataOfBirth());
    dto.setCity(user.getCity());
    dto.setCountry(user.getCountry());
    dto.setDescription(user.getDescription());
    dto.setAvatarId(user.getAvatarId());
    return dto;
  }

  private User findOwner(Map<User, Role> users) {
    if (users == null || users.isEmpty()) {
      return null;
    }
    for (Map.Entry<User, Role> entry : users.entrySet()) {
      if (entry.getValue() == Role.OWNER) {
        return entry.getKey();
      }
    }
    // Fallback: return any user if owner is not explicitly set
    return users.keySet().iterator().next();
  }

  private BulletinBoardResponse mapBulletinBoardToResponse(BulletinBoard board) {
    BulletinBoardResponse response = new BulletinBoardResponse();
    response.setId(board.getId());
    response.setContent(board.getContent());
    response.setUpdatedAt(board.getUpdatedAt());
    response.setAuthor(mapUserToSummaryResponse(board.getAuthor()));
    return response;
  }
}
