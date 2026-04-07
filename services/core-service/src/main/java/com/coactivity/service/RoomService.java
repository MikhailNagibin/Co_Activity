package com.coactivity.service;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomImageResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.util.AvatarUrlResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final RoomRepository roomRepository;
  private final RoomImageService roomImageService;
  private final BulletinBoardRepository bulletinBoardRepository;
  private final NotificationService notificationService;

  public RoomService(RoomRepository roomRepository,
      RoomImageService roomImageService,
      BulletinBoardRepository bulletinBoardRepository,
      NotificationService notificationService) {
    this.roomRepository = roomRepository;
    this.roomImageService = roomImageService;
    this.bulletinBoardRepository = bulletinBoardRepository;
    this.notificationService = notificationService;
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
   * Authenticated room listing with basic filtering and sorting.
   * <p>
   * The response annotates whether the current user participates in each room.
   * </p>
   */
  public List<RoomSummaryResponse> getRooms(Integer currentUserId, RoomFilter filter,
      RoomSort sortBy) {
    validateCatalogFilter(filter);
    List<Room> rooms = roomRepository.getAllRooms();
    if (rooms == null || rooms.isEmpty()) {
      return Collections.emptyList();
    }

    return rooms.stream()
        .filter(Room::isPublic)
        .filter(room -> filter == null || matchesFilter(room, filter))
        .map(room -> mapRoomToSummaryResponse(room,
            currentUserId != null && roomRepository.isUserInMembers(room.getId(), currentUserId)))
        .sorted(buildSummaryComparator(sortBy))
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
    detailed.setImages(summary.getImages());
    detailed.setHasProtectedAccess(hasProtectedAccess);
    detailed.setChatLink(hasProtectedAccess ? room.getChatLink() : null);
    detailed.setBulletinBoard(bulletinDto);
    return detailed;
  }

  public void deleteRoom(Integer requesterId, Integer roomId) {
    if (requesterId == null || roomId == null) {
      throw new ValidationException("Requester id and room id are required");
    }
    Room room = getExistingRoom(roomId);
    if (!roomRepository.isUserInMembers(roomId, requesterId)) {
      throw new AuthorizationException("Only owners can delete rooms");
    }

    Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, requesterId);
    if (requesterRole != Role.OWNER) {
      throw new AuthorizationException("Only owners can delete rooms");
    }

    List<Integer> participantIds = collectParticipantIds(room);

    roomImageService.deleteAllImagesForRoom(roomId);
    roomRepository.deleteRoom(roomId);

    for (Integer participantId : participantIds) {
      notificationService.sendActivityClosed(participantId, room.getName());
    }
  }

  private void validateRoomCreationRequest(RoomCreationRequest request) {
    if (request == null) {
      throw new ValidationException("Room creation request is required");
    }
    if (request.getIsPublic() == null) {
      throw new ValidationException("Public visibility flag is required");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new ValidationException("Room name cannot be empty");
    }
    String trimmedName = request.getName().trim();
    if (trimmedName.length() < 3 || trimmedName.length() > 100) {
      throw new ValidationException("Room name length must be between 3 and 100 characters");
    }
    if (request.getCategory() == null) {
      throw new ValidationException("Category is required");
    }
    if (request.getCategory().isBlank()) {
      throw new ValidationException("Category is required");
    }
    if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
      throw new ValidationException("Room description cannot be empty");
    }
    if (request.getDescription().length() > 2000) {
      throw new ValidationException("Room description must be at most 2000 characters");
    }
    if (request.getMaximumNumberOfPeople() == null) {
      throw new ValidationException("Maximum number of people is required");
    }
    if (request.getMaximumNumberOfPeople() < 2 || request.getMaximumNumberOfPeople() > 100000) {
      throw new ValidationException("Maximum number of people must be between 2 and 100000");
    }
    if (request.getAgeRating() < 0 || request.getAgeRating() > 21) {
      throw new ValidationException("Age rating must be between 0 and 21");
    }
    if (request.getDateOfStartEvent() != null
        && request.getDateOfEndEvent() != null
        && !request.getDateOfEndEvent().isAfter(request.getDateOfStartEvent())) {
      throw new ValidationException("Room end date must be after start date");
    }
  }

  private void validateCatalogFilter(RoomFilter filter) {
    if (filter != null && filter.hasLocationFilter()) {
      throw new ValidationException("Filtering rooms by city or country is not supported");
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
    return true;
  }

  private Comparator<RoomSummaryResponse> buildSummaryComparator(RoomSort sortBy) {
    RoomSort effectiveSort = sortBy != null ? sortBy : RoomSort.NEWEST;
    return switch (effectiveSort) {
      case POPULAR -> Comparator.comparingInt(
              (RoomSummaryResponse response) ->
                  response.getParticipantCount() != null ? response.getParticipantCount() : 0)
          .reversed();
      case NAME ->
          Comparator.comparing(
              response -> response.getName() != null ? response.getName().toLowerCase() : "",
              Comparator.naturalOrder());
      case UPCOMING -> Comparator.comparing(RoomSummaryResponse::getDateOfStartEvent,
          Comparator.nullsLast(Comparator.naturalOrder()));
      case NEWEST -> Comparator.comparing(RoomSummaryResponse::getId,
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

    Map<User, Role> users = loadRoomUsers(room);
    int participantCount = users.size();
    response.setParticipantCount(participantCount);
    response.setMaximumParticipants(room.getMaximumNumberOfPeople());

    User creator = findOwner(users);
    if (creator != null) {
      response.setCreator(mapUserToSummaryResponse(creator));
    }

    response.setIsCurrentUserParticipant(isCurrentUserParticipant);
    populateRoomImages(response, room.getId());

    return response;
  }

  private void populateRoomImages(RoomSummaryResponse response, Integer roomId) {
    List<RoomImageResponse> images = roomImageService.listRoomImages(roomId);
    response.setImages(images);
    response.setImageIds(images.stream().map(RoomImageResponse::getId).toList());
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
    dto.setAvatarUrl(AvatarUrlResolver.resolveUserAvatarUrl(user.getId(), user.getAvatarFileId()));
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
    return null;
  }

  private BulletinBoardResponse mapBulletinBoardToResponse(BulletinBoard board) {
    BulletinBoardResponse response = new BulletinBoardResponse();
    response.setId(board.getId());
    response.setContent(board.getContent());
    response.setUpdatedAt(board.getUpdatedAt());
    response.setAuthor(mapUserToSummaryResponse(board.getAuthor()));
    return response;
  }

  private List<Integer> collectParticipantIds(Room room) {
    List<Integer> participantIds = new ArrayList<>();
    for (User user : loadRoomUsers(room).keySet()) {
      if (user != null && user.getId() != null) {
        participantIds.add(user.getId());
      }
    }
    return participantIds;
  }

  private Map<User, Role> loadRoomUsers(Room room) {
    if (room.getUsers() != null) {
      return room.getUsers();
    }

    Map<User, Role> users = roomRepository.getUsersInRoom(room.getId());
    return users != null ? users : Collections.emptyMap();
  }
}
