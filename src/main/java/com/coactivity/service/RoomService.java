package com.coactivity.service;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.BulletinBoardRepositoryImpl;
import com.coactivity.repository.impl.PictureRepositoryImpl;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final RoomRepositoryImpl roomRepository;
  private final PictureRepositoryImpl pictureRepository;
  private final BulletinBoardRepositoryImpl bulletinBoardRepository;

  public RoomService(RoomRepositoryImpl roomRepository,
      PictureRepositoryImpl pictureRepository,
      BulletinBoardRepositoryImpl bulletinBoardRepository) {
    this.roomRepository = roomRepository;
    this.pictureRepository = pictureRepository;
    this.bulletinBoardRepository = bulletinBoardRepository;
  }

  /**
   * Creates a new room and assigns the provided user as the room OWNER.
   */
  public RoomCreationResponse createRoom(Integer ownerId, RoomCreationRequest request) {
    if (ownerId == null) {
      throw new ValidationException("Owner id is required");
    }
    validateRoomCreationRequest(request);

    Room createdRoom = roomRepository.createRoom(ownerId, request);
    if (createdRoom == null) {
      throw new ResourceNotFoundException("Room could not be created");
    }

    return new RoomCreationResponse(createdRoom.getId(), createdRoom.getName(),
        createdRoom.getCategory());
  }

  /**
   * Public room listing with basic filtering and sorting.
   * <p>
   * If a token is provided, the response annotates whether the current user participates in each
   * room.
   * </p>
   */
  public List<RoomSummaryResponse> getRooms(Integer currentUserId, RoomFilter filter,
      RoomSort sortBy) {
    List<Room> rooms = roomRepository.getAllRooms();
    if (rooms.isEmpty()) {
      return Collections.emptyList();
    }

    return rooms.stream()
        .filter(room -> filter == null || matchesFilter(room, filter))
        .sorted(buildComparator(sortBy))
        .map(room -> mapRoomToSummaryResponse(room,
            currentUserId != null && roomRepository.isUserInMembers(room.getId(), currentUserId)))
        .collect(Collectors.toList());
  }

  /**
   * Returns detailed information about a room. Protected fields (chat link, bulletin board) are
   * only included when the user is an authenticated participant of the room.
   */
  public RoomDetailedResponse getRoomById(Integer roomId, Integer currentUserId) {
    if (roomId == null) {
      throw new ValidationException("Room id is required");
    }

    Room room = getExistingRoom(roomId);
    boolean hasProtectedAccess = currentUserId != null &&
        roomRepository.isUserInMembers(roomId, currentUserId);

    RoomSummaryResponse summary =
        mapRoomToSummaryResponse(room, hasProtectedAccess ? Boolean.TRUE : null);

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
    return detailed;
  }

  public void deleteRoom(Integer requesterId, Integer roomId) {
    if (requesterId == null || roomId == null) {
      throw new ValidationException("Requester id and room id are required");
    }
    getExistingRoom(roomId);

    Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, requesterId);
    if (requesterRole != Role.OWNER) {
      throw new AuthorizationException("Only owners can delete rooms");
    }
    roomRepository.deleteRoom(roomId);
  }

  private void validateRoomCreationRequest(RoomCreationRequest request) {
    if (request == null) {
      throw new ValidationException("Room creation request is required");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new ValidationException("Room name cannot be empty");
    }
    if (request.getCategoryId() == null) {
      throw new ValidationException("Category id is required");
    }
    if (request.getMaximumNumberOfPeople() == null) {
      throw new ValidationException("Maximum number of people is required");
    }
  }

  private Room getExistingRoom(Integer roomId) {
    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found: " + roomId);
    }
    return room;
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
      return name.contains(query) || description.contains(query);
    }
    // Room entity does not contain city/country fields, so these filters are ignored for now.
    return true;
  }

  private Comparator<Room> buildComparator(RoomSort sortBy) {
    RoomSort effectiveSort = sortBy != null ? sortBy : RoomSort.NEWEST;
    return switch (effectiveSort) {
      case POPULAR -> Comparator.comparingInt(
              (Room room) -> room.getUsers() != null ? room.getUsers().size() : 0)
          .reversed();
      case NAME ->
          Comparator.comparing(room -> room.getName() != null ? room.getName().toLowerCase() : "",
              Comparator.naturalOrder());
      case UPCOMING -> Comparator.comparing(Room::getDateOfStartEvent,
          Comparator.nullsLast(Comparator.naturalOrder()));
      case NEWEST -> Comparator.comparing(Room::getId,
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

    List<Integer> imageIds = new ArrayList<>();
    try {
      pictureRepository.getRoomPictures(room.getId())
          .forEach(p -> imageIds.add(p.getPhotoId()));
    } catch (Exception e) {
      // If pictures cannot be loaded, leave empty list
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
