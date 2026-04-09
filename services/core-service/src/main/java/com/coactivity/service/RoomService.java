package com.coactivity.service;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.request.RoomUpdateRequest;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomImageResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomStatus;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.util.AvatarUrlResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

  private final RoomRepository roomRepository;
  private final RoomsRequestRepository roomsRequestRepository;
  private final RoomImageService roomImageService;
  private final BulletinBoardRepository bulletinBoardRepository;
  private final NotificationService notificationService;

  public RoomService(RoomRepository roomRepository,
      RoomsRequestRepository roomsRequestRepository,
      RoomImageService roomImageService,
      BulletinBoardRepository bulletinBoardRepository,
      NotificationService notificationService) {
    this.roomRepository = roomRepository;
    this.roomsRequestRepository = roomsRequestRepository;
    this.roomImageService = roomImageService;
    this.bulletinBoardRepository = bulletinBoardRepository;
    this.notificationService = notificationService;
  }

  /**
   * Creates a new room and assigns the provided user as the room OWNER.
   */
  @Transactional
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

  @Transactional
  public RoomDetailedResponse updateRoom(Integer requesterId, Integer roomId,
      RoomUpdateRequest request) {
    if (requesterId == null || roomId == null) {
      throw new ValidationException("Requester id and room id are required");
    }
    Room currentRoom = getExistingRoomForUpdate(roomId);
    requireOwner(requesterId, roomId, "Only room owner can manage the room");
    validateRoomUpdateRequest(roomId, request);
    ImportantRoomUpdateEmail importantUpdate = detectImportantRoomUpdate(currentRoom, request);
    List<RoomsRequestAutoUpdate> autoUpdatedPendingRequests =
        collectAutoUpdatedPendingRequests(currentRoom, request);
    reconcilePendingJoinRequests(currentRoom, request);

    Room updatedRoom = roomRepository.updateRoom(roomId, request);
    if (updatedRoom == null) {
      throw new ResourceNotFoundException("Room could not be updated");
    }
    notifyAboutImportantRoomUpdate(updatedRoom, importantUpdate);
    notifyAutoUpdatedPendingRequests(updatedRoom, request, autoUpdatedPendingRequests);

    return getRoomById(roomId, requesterId);
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
        .filter(Room::isActive)
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
    detailed.setStatus(summary.getStatus());
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

  @Transactional
  public void deleteRoom(Integer requesterId, Integer roomId) {
    if (requesterId == null || roomId == null) {
      throw new ValidationException("Requester id and room id are required");
    }
    Room room = getExistingRoom(roomId);
    requireOwner(requesterId, roomId, "Only owners can delete rooms");

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
    validateCommonRoomFields(request.getCategory(), request.getName(), request.getDescription(),
        request.getMaximumNumberOfPeople(), request.getAgeRating(), request.getDateOfStartEvent(),
        request.getDateOfEndEvent());
  }

  private void validateRoomUpdateRequest(Integer roomId, RoomUpdateRequest request) {
    if (request == null) {
      throw new ValidationException("Room update request is required");
    }
    if (request.getIsPublic() == null) {
      throw new ValidationException("Public visibility flag is required");
    }
    if (request.getStatus() == null) {
      throw new ValidationException("Room status is required");
    }
    validateCommonRoomFields(request.getCategory(), request.getName(), request.getDescription(),
        request.getMaximumNumberOfPeople(), request.getAgeRating(), request.getDateOfStartEvent(),
        request.getDateOfEndEvent());

    int participantCount = roomRepository.getRoomParticipantCount(roomId);
    if (request.getMaximumNumberOfPeople() < participantCount) {
      throw new ValidationException(
          "Room capacity cannot be lower than current participant count");
    }
  }

  private void validateCommonRoomFields(String category, String name, String description,
      Integer maximumNumberOfPeople, Integer ageRating, java.time.Instant dateOfStartEvent,
      java.time.Instant dateOfEndEvent) {
    if (name == null || name.trim().isEmpty()) {
      throw new ValidationException("Room name cannot be empty");
    }
    String trimmedName = name.trim();
    if (trimmedName.length() < 3 || trimmedName.length() > 100) {
      throw new ValidationException("Room name length must be between 3 and 100 characters");
    }
    if (category == null) {
      throw new ValidationException("Category is required");
    }
    if (category.isBlank()) {
      throw new ValidationException("Category is required");
    }
    if (description == null || description.trim().isEmpty()) {
      throw new ValidationException("Room description cannot be empty");
    }
    if (description.length() > 2000) {
      throw new ValidationException("Room description must be at most 2000 characters");
    }
    if (maximumNumberOfPeople == null) {
      throw new ValidationException("Maximum number of people is required");
    }
    if (maximumNumberOfPeople < 2 || maximumNumberOfPeople > 100000) {
      throw new ValidationException("Maximum number of people must be between 2 and 100000");
    }
    if (ageRating == null) {
      throw new ValidationException("Age rating is required");
    }
    if (ageRating < 0 || ageRating > 21) {
      throw new ValidationException("Age rating must be between 0 and 21");
    }
    if (dateOfStartEvent != null
        && dateOfEndEvent != null
        && !dateOfEndEvent.isAfter(dateOfStartEvent)) {
      throw new ValidationException("Room end date must be after start date");
    }
  }

  private void requireOwner(Integer requesterId, Integer roomId, String message) {
    getExistingRoom(roomId);
    if (!roomRepository.isUserInMembers(roomId, requesterId)) {
      throw new AuthorizationException(message);
    }
    Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, requesterId);
    if (requesterRole != Role.OWNER) {
      throw new AuthorizationException(message);
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

  private Room getExistingRoomForUpdate(Integer roomId) {
    Room room = roomRepository.getRoomByIdForUpdate(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found: " + roomId);
    }
    return room;
  }

  private void reconcilePendingJoinRequests(Room currentRoom, RoomUpdateRequest request) {
    boolean movesToNonActive = currentRoom.getStatus() == RoomStatus.ACTIVE
        && request.getStatus() != RoomStatus.ACTIVE;
    boolean becomesPublic = !currentRoom.isPublic() && Boolean.TRUE.equals(request.getIsPublic());

    if (movesToNonActive) {
      roomsRequestRepository.updatePendingRequestsByRoom(currentRoom.getId(), RequestStatus.REFUSED);
      return;
    }

    if (becomesPublic) {
      roomsRequestRepository.deletePendingRequestsByRoom(currentRoom.getId());
    }
  }

  private ImportantRoomUpdateEmail detectImportantRoomUpdate(Room currentRoom,
      RoomUpdateRequest request) {
    boolean statusChanged = currentRoom.getStatus() != request.getStatus()
        && (request.getStatus() == RoomStatus.INACTIVE || request.getStatus() == RoomStatus.COMPLETED);
    boolean scheduleChanged = !Objects.equals(currentRoom.getDateOfStartEvent(),
        request.getDateOfStartEvent())
        || !Objects.equals(currentRoom.getDateOfEndEvent(), request.getDateOfEndEvent())
        || !Objects.equals(currentRoom.getFrequency(), request.getFrequency());
    boolean chatLinkChanged = !Objects.equals(currentRoom.getChatLink(), request.getChatLink());

    return new ImportantRoomUpdateEmail(
        request.getName(),
        currentRoom.getStatus(),
        request.getStatus(),
        statusChanged,
        currentRoom.getDateOfStartEvent(),
        request.getDateOfStartEvent(),
        currentRoom.getDateOfEndEvent(),
        request.getDateOfEndEvent(),
        currentRoom.getFrequency(),
        request.getFrequency(),
        scheduleChanged,
        currentRoom.getChatLink(),
        request.getChatLink(),
        chatLinkChanged);
  }

  private List<RoomsRequestAutoUpdate> collectAutoUpdatedPendingRequests(Room currentRoom,
      RoomUpdateRequest request) {
    boolean movesToNonActive = currentRoom.getStatus() == RoomStatus.ACTIVE
        && request.getStatus() != RoomStatus.ACTIVE;
    boolean becomesPublic = !currentRoom.isPublic() && Boolean.TRUE.equals(request.getIsPublic());

    if (!movesToNonActive && !becomesPublic) {
      return List.of();
    }

    return roomsRequestRepository.getRoomRequests(currentRoom.getId()).stream()
        .filter(roomRequest -> roomRequest.getStatus() == RequestStatus.CONSIDERATION)
        .map(roomRequest -> new RoomsRequestAutoUpdate(
            roomRequest.getUser() != null ? roomRequest.getUser().getId() : null,
            movesToNonActive
                ? AutoRequestUpdateReason.DECLINED_BECAUSE_ROOM_BECAME_NON_ACTIVE
                : AutoRequestUpdateReason.REMOVED_BECAUSE_ROOM_BECAME_PUBLIC))
        .filter(update -> update.userId() != null)
        .toList();
  }

  private void notifyAboutImportantRoomUpdate(Room updatedRoom, ImportantRoomUpdateEmail update) {
    if (!update.hasAnyChange()) {
      return;
    }

    for (Integer participantId : collectParticipantIds(updatedRoom)) {
      notificationService.sendImportantRoomUpdate(participantId, update);
    }
  }

  private void notifyAutoUpdatedPendingRequests(Room updatedRoom, RoomUpdateRequest request,
      List<RoomsRequestAutoUpdate> autoUpdatedPendingRequests) {
    for (RoomsRequestAutoUpdate update : autoUpdatedPendingRequests) {
      if (update.reason() == AutoRequestUpdateReason.REMOVED_BECAUSE_ROOM_BECAME_PUBLIC) {
        notificationService.sendPendingRequestApprovalNoLongerNeeded(update.userId(),
            updatedRoom.getName());
        continue;
      }

      String reason = request.getStatus() == RoomStatus.COMPLETED
          ? "the room became completed"
          : "the room became inactive";
      notificationService.sendPendingRequestAutoDeclined(update.userId(), updatedRoom.getName(),
          reason);
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
    response.setStatus(room.getStatus() != null ? room.getStatus()
        : (room.isActive() ? RoomStatus.ACTIVE : RoomStatus.INACTIVE));
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

  private record RoomsRequestAutoUpdate(Integer userId, AutoRequestUpdateReason reason) {
  }

  private enum AutoRequestUpdateReason {
    DECLINED_BECAUSE_ROOM_BECAME_NON_ACTIVE,
    REMOVED_BECAUSE_ROOM_BECAME_PUBLIC
  }
}
