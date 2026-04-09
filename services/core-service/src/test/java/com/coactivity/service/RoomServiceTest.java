package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.request.RoomUpdateRequest;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Category;
import com.coactivity.domain.Notification;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

@DisplayName("RoomService Tests")
class RoomServiceTest {

    private RoomRepository roomRepository;
    private RoomImageService roomImageService;
    private BulletinBoardRepository bulletinBoardRepository;
    private RoomsRequestRepository roomsRequestRepository;
    private NotificationService notificationService;
    private RoomService roomService;

    private Integer ownerId;
    private Integer roomId;
    private Room room;
    private Map<User, Role> roomUsers;

    @BeforeEach
    void setUp() {
        roomRepository = Mockito.mock(RoomRepository.class);
        roomImageService = Mockito.mock(RoomImageService.class);
        bulletinBoardRepository = Mockito.mock(BulletinBoardRepository.class);
        roomsRequestRepository = Mockito.mock(RoomsRequestRepository.class);
        notificationService = Mockito.mock(NotificationService.class);
        roomService = new RoomService(roomRepository, roomsRequestRepository, roomImageService, bulletinBoardRepository,
                notificationService);

        ownerId = 10;
        roomId = 100;

        User owner = createUser(ownerId, "owner@example.com", "owner");
        User participant = createUser(20, "participant@example.com", "participant");

        roomUsers = new LinkedHashMap<>();
        roomUsers.put(owner, Role.OWNER);
        roomUsers.put(participant, Role.PARTICIPANT);

        room = createRoom(roomId, false, roomUsers);
        when(roomImageService.listRoomImages(Mockito.anyInt())).thenReturn(List.of());

        when(roomRepository.getRoomById(roomId)).thenReturn(room);
        when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.OWNER);
    }

    @Test
    @DisplayName("deleteRoom should notify participants only after successful room deletion")
    void deleteRoom_notifiesParticipantsAfterSuccessfulDelete() {
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
        roomService.deleteRoom(ownerId, roomId);

        InOrder inOrder = inOrder(roomRepository, notificationService);
        inOrder.verify(roomRepository).deleteRoom(roomId);
        inOrder.verify(notificationService).sendActivityClosed(ownerId, "Morning Run");
        inOrder.verify(notificationService).sendActivityClosed(20, "Morning Run");
    }

    @Test
    @DisplayName("deleteRoom should not send activity closed notifications when deletion fails")
    void deleteRoom_doesNotNotifyParticipantsWhenDeletionFails() {
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
        doThrow(new RuntimeException("delete failed"))
                .when(roomRepository)
                .deleteRoom(roomId);

        assertThrows(RuntimeException.class, () -> roomService.deleteRoom(ownerId, roomId));

        verify(notificationService, never()).sendActivityClosed(ownerId, "Morning Run");
        verify(notificationService, never()).sendActivityClosed(20, "Morning Run");
    }

    @Test
    @DisplayName("getRooms should expose only public rooms in the public catalog")
    void getRooms_returnsOnlyPublicRooms() {
        Room publicRoom = createRoom(101, true, null);
        Room privateRoom = createRoom(102, false, null);
        Room completedRoom = createRoom(103, true, null);
        completedRoom.setStatus(RoomStatus.COMPLETED);

        when(roomRepository.getAllRooms()).thenReturn(List.of(privateRoom, completedRoom, publicRoom));
        when(roomRepository.getUsersInRoom(101)).thenReturn(Map.of());

        List<RoomSummaryResponse> responses = roomService.getRooms(null, null, RoomSort.NEWEST);

        assertEquals(1, responses.size());
        assertEquals(101, responses.getFirst().getId());
    }

    @Test
    @DisplayName("getRooms should return empty list when repository returns null")
    void getRooms_returnsEmptyListWhenRepositoryReturnsNull() {
        when(roomRepository.getAllRooms()).thenReturn(null);

        List<RoomSummaryResponse> responses = roomService.getRooms(null, null, RoomSort.NEWEST);

        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("getRooms should filter by text query and max participants")
    void getRooms_filtersByQueryAndMaxParticipants() {
        Room matchingRoom = createRoom(201, true, null);
        matchingRoom.setName("Morning Chess");
        matchingRoom.setDescription("Quiet strategy meetup");
        matchingRoom.setMaximumNumberOfPeople(8);

        Room oversizedRoom = createRoom(202, true, null);
        oversizedRoom.setName("Morning Marathon");
        oversizedRoom.setDescription("Large outdoor training");
        oversizedRoom.setMaximumNumberOfPeople(50);

        Room unrelatedRoom = createRoom(203, true, null);
        unrelatedRoom.setName("Evening Cinema");
        unrelatedRoom.setDescription("Watch movies together");
        unrelatedRoom.setMaximumNumberOfPeople(6);

        when(roomRepository.getAllRooms()).thenReturn(List.of(matchingRoom, oversizedRoom, unrelatedRoom));
        when(roomRepository.getUsersInRoom(201)).thenReturn(Map.of());

        RoomFilter filter = new RoomFilter();
        filter.setQuery("morning");
        filter.setMaxParticipants(10);

        List<RoomSummaryResponse> responses = roomService.getRooms(null, filter, RoomSort.NAME);

        assertEquals(1, responses.size());
        assertEquals(201, responses.getFirst().getId());
        assertEquals("Morning Chess", responses.getFirst().getName());
    }

    @Test
    @DisplayName("getRooms should sort by popularity descending")
    void getRooms_sortsByPopularity() {
        Room crowdedRoom = createRoom(301, true, null);
        Room mediumRoom = createRoom(302, true, null);
        Room quietRoom = createRoom(303, true, null);

        when(roomRepository.getAllRooms()).thenReturn(List.of(quietRoom, crowdedRoom, mediumRoom));
        when(roomRepository.getUsersInRoom(301)).thenReturn(Map.of(
                createUser(1, "owner1@example.com", "owner1"), Role.OWNER,
                createUser(2, "member2@example.com", "member2"), Role.PARTICIPANT,
                createUser(3, "member3@example.com", "member3"), Role.PARTICIPANT));
        when(roomRepository.getUsersInRoom(302)).thenReturn(Map.of(
                createUser(4, "owner4@example.com", "owner4"), Role.OWNER,
                createUser(5, "member5@example.com", "member5"), Role.PARTICIPANT));
        when(roomRepository.getUsersInRoom(303)).thenReturn(Map.of(
                createUser(6, "owner6@example.com", "owner6"), Role.OWNER));

        List<RoomSummaryResponse> responses = roomService.getRooms(null, null, RoomSort.POPULAR);

        assertEquals(List.of(301, 302, 303),
                responses.stream().map(RoomSummaryResponse::getId).toList());
    }

    @Test
    @DisplayName("getRooms should sort alphabetically by name")
    void getRooms_sortsByName() {
        Room zooRoom = createRoom(401, true, null);
        zooRoom.setName("Zoo trip");
        Room alphaRoom = createRoom(402, true, null);
        alphaRoom.setName("Art meetup");
        Room middleRoom = createRoom(403, true, null);
        middleRoom.setName("Board games");

        when(roomRepository.getAllRooms()).thenReturn(List.of(zooRoom, middleRoom, alphaRoom));
        when(roomRepository.getUsersInRoom(401)).thenReturn(Map.of());
        when(roomRepository.getUsersInRoom(402)).thenReturn(Map.of());
        when(roomRepository.getUsersInRoom(403)).thenReturn(Map.of());

        List<RoomSummaryResponse> responses = roomService.getRooms(null, null, RoomSort.NAME);

        assertEquals(List.of("Art meetup", "Board games", "Zoo trip"),
                responses.stream().map(RoomSummaryResponse::getName).toList());
    }

    @Test
    @DisplayName("getRooms should mark current user participation in public catalog")
    void getRooms_marksCurrentUserParticipation() {
        Room joinedRoom = createRoom(501, true, null);
        Room notJoinedRoom = createRoom(502, true, null);

        when(roomRepository.getAllRooms()).thenReturn(List.of(joinedRoom, notJoinedRoom));
        when(roomRepository.isUserInMembers(501, ownerId)).thenReturn(true);
        when(roomRepository.isUserInMembers(502, ownerId)).thenReturn(false);
        when(roomRepository.getUsersInRoom(501)).thenReturn(roomUsers);
        when(roomRepository.getUsersInRoom(502)).thenReturn(Map.of());

        List<RoomSummaryResponse> responses = roomService.getRooms(ownerId, null, RoomSort.NEWEST);

        RoomSummaryResponse first = responses.stream()
                .filter(response -> response.getId().equals(502))
                .findFirst()
                .orElseThrow();
        RoomSummaryResponse second = responses.stream()
                .filter(response -> response.getId().equals(501))
                .findFirst()
                .orElseThrow();

        assertEquals(false, first.getIsCurrentUserParticipant());
        assertEquals(true, second.getIsCurrentUserParticipant());
    }

    @Test
    @DisplayName("getRooms should reject unsupported location filters instead of silently ignoring them")
    void getRooms_rejectsUnsupportedLocationFilters() {
        RoomFilter filter = new RoomFilter();
        filter.setCity("Moscow");

        assertThrows(ValidationException.class, () -> roomService.getRooms(null, filter, null));
    }

    @Test
    @DisplayName("getRooms should ignore blank location filter values")
    void getRooms_ignoresBlankLocationFilterValues() {
        Room publicRoom = createRoom(101, true, null);
        RoomFilter filter = new RoomFilter();
        filter.setCity("   ");

        when(roomRepository.getAllRooms()).thenReturn(List.of(publicRoom));
        when(roomRepository.getUsersInRoom(101)).thenReturn(Map.of());

        List<RoomSummaryResponse> responses = roomService.getRooms(null, filter, RoomSort.NEWEST);

        assertEquals(1, responses.size());
        assertEquals(101, responses.getFirst().getId());
    }

    @Test
    @DisplayName("getRoomById should load creator and participant count from repository when room entity is not hydrated")
    void getRoomById_loadsUsersFromRepositoryWhenRoomEntityHasNoUsers() {
        Room roomWithoutUsers = createRoom(roomId, true, null);

        when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
        when(roomRepository.getUsersInRoom(roomId)).thenReturn(roomUsers);
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);

        RoomDetailedResponse response = roomService.getRoomById(roomId, ownerId);

        assertNotNull(response);
        assertEquals(2, response.getParticipantCount());
        assertNotNull(response.getCreator());
        assertEquals("owner", response.getCreator().getUserName());
    }

    @Test
    @DisplayName("deleteRoom should load participants from repository when room entity is not hydrated")
    void deleteRoom_loadsParticipantsFromRepositoryWhenRoomEntityHasNoUsers() {
        Room roomWithoutUsers = createRoom(roomId, false, null);

        when(roomRepository.getRoomById(roomId)).thenReturn(roomWithoutUsers);
        when(roomRepository.getUsersInRoom(roomId)).thenReturn(roomUsers);
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);

        roomService.deleteRoom(ownerId, roomId);

        verify(roomImageService).deleteAllImagesForRoom(roomId);
        verify(notificationService).sendActivityClosed(ownerId, "Morning Run");
        verify(notificationService).sendActivityClosed(20, "Morning Run");
    }

    @Test
    @DisplayName("createRoom should return created room summary when request is valid")
    void createRoom_returnsCreationResponse() {
        RoomCreationRequest request = validRoomCreationRequest();
        Room createdRoom = createRoom(777, true, null);
        createdRoom.setName("New Room");
        createdRoom.setCategory(Category.ART);

        when(roomRepository.createRoom(ownerId, request)).thenReturn(createdRoom);

        RoomCreationResponse response = roomService.createRoom(ownerId, request);

        assertEquals(777, response.getRoomId());
        assertEquals("New Room", response.getName());
        assertEquals(Category.ART, response.getCategory());
    }

    @Test
    @DisplayName("createRoom should reject missing owner id")
    void createRoom_rejectsMissingOwnerId() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> roomService.createRoom(null, validRoomCreationRequest()));

        assertEquals("Owner id is required", exception.getMessage());
    }

    @Test
    @DisplayName("createRoom should reject request without category")
    void createRoom_rejectsMissingCategory() {
        RoomCreationRequest request = validRoomCreationRequest();
        request.setCategory(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> roomService.createRoom(ownerId, request));

        assertEquals("Category is required", exception.getMessage());
    }

    @Test
    @DisplayName("createRoom should reject missing public visibility flag")
    void createRoom_rejectsMissingPublicFlag() {
        RoomCreationRequest request = validRoomCreationRequest();
        request.setIsPublic(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> roomService.createRoom(ownerId, request));

        assertEquals("Public visibility flag is required", exception.getMessage());
        verify(roomRepository, never()).createRoom(ownerId, request);
    }

    @Test
    @DisplayName("createRoom should reject name shorter than three characters")
    void createRoom_rejectsTooShortName() {
        RoomCreationRequest request = validRoomCreationRequest();
        request.setName("Hi");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> roomService.createRoom(ownerId, request));

        assertEquals("Room name length must be between 3 and 100 characters",
                exception.getMessage());
        verify(roomRepository, never()).createRoom(ownerId, request);
    }

    @Test
    @DisplayName("createRoom should reject too few participants")
    void createRoom_rejectsTooFewParticipants() {
        RoomCreationRequest request = validRoomCreationRequest();
        request.setMaximumNumberOfPeople(1);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> roomService.createRoom(ownerId, request));

        assertEquals("Maximum number of people must be between 2 and 100000",
                exception.getMessage());
        verify(roomRepository, never()).createRoom(ownerId, request);
    }

    @Test
    @DisplayName("createRoom should reject end date earlier than start date")
    void createRoom_rejectsEndDateBeforeStartDate() {
        RoomCreationRequest request = validRoomCreationRequest();
        request.setDateOfStartEvent(Instant.parse("2030-01-02T10:00:00Z"));
        request.setDateOfEndEvent(Instant.parse("2030-01-02T09:00:00Z"));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> roomService.createRoom(ownerId, request));

        assertEquals("Room end date must be after start date", exception.getMessage());
        verify(roomRepository, never()).createRoom(ownerId, request);
    }

    @Test
    @DisplayName("getRoomById should hide protected fields for non-participant")
    void getRoomById_hidesProtectedFieldsForNonParticipant() {
        BulletinBoard board = new BulletinBoard(5, room, "Private notes", createUser(30,
                "author@example.com", "author"), Instant.now());
        when(bulletinBoardRepository.getBulletinBoard(roomId)).thenReturn(board);
        when(roomRepository.isUserInMembers(roomId, 999)).thenReturn(false);

        RoomDetailedResponse response = roomService.getRoomById(roomId, 999);

        assertEquals(false, response.getHasProtectedAccess());
        assertNull(response.getChatLink());
        assertNull(response.getBulletinBoard());
    }

    @Test
    @DisplayName("getRoomById should expose protected fields for participant")
    void getRoomById_exposesProtectedFieldsForParticipant() {
        BulletinBoard board = new BulletinBoard(5, room, "Private notes", createUser(30,
                "author@example.com", "author"), Instant.parse("2030-01-01T09:00:00Z"));
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
        when(bulletinBoardRepository.getBulletinBoard(roomId)).thenReturn(board);

        RoomDetailedResponse response = roomService.getRoomById(roomId, ownerId);

        assertEquals(true, response.getHasProtectedAccess());
        assertEquals("https://chat.example.com", response.getChatLink());
        assertNotNull(response.getBulletinBoard());
        assertEquals("Private notes", response.getBulletinBoard().getContent());
    }

    @Test
    @DisplayName("deleteRoom should reject non-owner requester")
    void deleteRoom_rejectsNonOwnerRequester() {
        when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.PARTICIPANT);

        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () -> roomService.deleteRoom(ownerId, roomId));

        assertEquals("Only owners can delete rooms", exception.getMessage());
        verify(roomRepository, never()).deleteRoom(roomId);
    }

    @Test
    @DisplayName("deleteRoom should reject requester who is not a room member instead of leaking repository failure")
    void deleteRoom_rejectsNonMemberRequester() {
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(false);

        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () -> roomService.deleteRoom(ownerId, roomId));

        assertEquals("Only owners can delete rooms", exception.getMessage());
        verify(roomRepository, never()).getUserRoleByRoomId(roomId, ownerId);
        verify(roomRepository, never()).deleteRoom(roomId);
    }

    @Test
    @DisplayName("getRoomById should fail when room does not exist")
    void getRoomById_rejectsUnknownRoom() {
        when(roomRepository.getRoomById(404)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> roomService.getRoomById(404, ownerId));

        assertEquals("Room not found: 404", exception.getMessage());
    }

    @Test
    @DisplayName("updateRoom should let owner change room fields and lifecycle status")
    void updateRoom_updatesRoomFieldsAndStatus() {
        RoomUpdateRequest request = validRoomUpdateRequest();
        request.setStatus(RoomStatus.COMPLETED);
        request.setIsPublic(false);
        request.setMaximumNumberOfPeople(25);
        request.setName("Updated room");
        request.setDescription("Updated description");

        Room updatedRoom = createRoom(roomId, false, roomUsers);
        updatedRoom.setStatus(RoomStatus.COMPLETED);
        updatedRoom.setName("Updated room");
        updatedRoom.setDescription("Updated description");
        updatedRoom.setMaximumNumberOfPeople(25);

        when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(room);
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
        when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(2);
        when(roomRepository.updateRoom(roomId, request)).thenReturn(updatedRoom);
        when(roomRepository.getRoomById(roomId)).thenReturn(updatedRoom);

        RoomDetailedResponse response = roomService.updateRoom(ownerId, roomId, request);

        assertEquals(RoomStatus.COMPLETED, response.getStatus());
        assertEquals(false, response.isActive());
        assertEquals(false, response.getIsPublic());
        assertEquals(25, response.getMaximumParticipants());
        assertEquals("Updated room", response.getName());
        verify(roomsRequestRepository).updatePendingRequestsByRoom(roomId, com.coactivity.domain.RequestStatus.REFUSED);
        verify(roomRepository).updateRoom(roomId, request);
    }

    @Test
    @DisplayName("updateRoom should reject non-owner requester")
    void updateRoom_rejectsNonOwnerRequester() {
        RoomUpdateRequest request = validRoomUpdateRequest();
        when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(room);
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
        when(roomRepository.getUserRoleByRoomId(roomId, ownerId)).thenReturn(Role.PARTICIPANT);

        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () -> roomService.updateRoom(ownerId, roomId, request));

        assertEquals("Only room owner can manage the room", exception.getMessage());
        verify(roomRepository, never()).updateRoom(roomId, request);
    }

    @Test
    @DisplayName("updateRoom should reject capacity below current participant count")
    void updateRoom_rejectsCapacityBelowCurrentParticipantCount() {
        RoomUpdateRequest request = validRoomUpdateRequest();
        request.setMaximumNumberOfPeople(2);

        when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(room);
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
        when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(3);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> roomService.updateRoom(ownerId, roomId, request));

        assertEquals("Room capacity cannot be lower than current participant count",
                exception.getMessage());
        verify(roomRepository, never()).updateRoom(roomId, request);
    }

    @Test
    @DisplayName("updateRoom should remove pending requests when room becomes public")
    void updateRoom_deletesPendingRequestsWhenRoomBecomesPublic() {
        RoomUpdateRequest request = validRoomUpdateRequest();
        request.setIsPublic(true);
        request.setStatus(RoomStatus.ACTIVE);
        room.setPublic(false);

        when(roomRepository.getRoomByIdForUpdate(roomId)).thenReturn(room);
        when(roomRepository.isUserInMembers(roomId, ownerId)).thenReturn(true);
        when(roomRepository.getRoomParticipantCount(roomId)).thenReturn(2);
        when(roomRepository.updateRoom(roomId, request)).thenReturn(room);
        when(roomRepository.getRoomById(roomId)).thenReturn(room);

        roomService.updateRoom(ownerId, roomId, request);

        verify(roomsRequestRepository).deletePendingRequestsByRoom(roomId);
        verify(roomsRequestRepository, never())
                .updatePendingRequestsByRoom(roomId, com.coactivity.domain.RequestStatus.REFUSED);
    }

    private RoomCreationRequest validRoomCreationRequest() {
        RoomCreationRequest request = new RoomCreationRequest();
        request.setIsPublic(true);
        request.setCategory("ART");
        request.setName("New Room");
        request.setDescription("Created in test");
        request.setMaximumNumberOfPeople(12);
        request.setDateOfStartEvent(Instant.now().plusSeconds(3600));
        request.setDateOfEndEvent(Instant.now().plusSeconds(7200));
        request.setAgeRating(18);
        return request;
    }

    private RoomUpdateRequest validRoomUpdateRequest() {
        RoomUpdateRequest request = new RoomUpdateRequest();
        request.setIsPublic(true);
        request.setCategory("ART");
        request.setName("Updated room");
        request.setDescription("Updated in test");
        request.setMaximumNumberOfPeople(12);
        request.setChatLink("https://chat.example.com/updated");
        request.setDateOfStartEvent(Instant.parse("2030-01-02T10:00:00Z"));
        request.setDateOfEndEvent(Instant.parse("2030-01-02T12:00:00Z"));
        request.setStatus(RoomStatus.ACTIVE);
        request.setAgeRating(18);
        return request;
    }

    private User createUser(Integer id, String login, String username) {
        return new User(
                id,
                login,
                username,
                Instant.parse("2000-01-01T00:00:00Z"),
                "Russia",
                "Moscow",
                username,
                1,
                null,
                List.of(),
                List.of(Notification.ACTIVITY_CLOSED));
    }

    private Room createRoom(Integer id, boolean isPublic, Map<User, Role> users) {
        return new Room(
                id,
                true,
                isPublic,
                "https://chat.example.com",
                Category.SPORT,
                "Morning Run",
                "Test room",
                Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T12:00:00Z"),
                18,
                Instant.parse("2029-12-01T10:00:00Z"),
                10,
                users,
                List.of());
    }
}
