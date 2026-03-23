package com.coactivity.service;

import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import org.springframework.stereotype.Service;

@Service
public class BulletinBoardService {

  private final BulletinBoardRepository bulletinBoardRepository;
  private final RoomRepository roomRepository;
  private final UserRepository userRepository;

  public BulletinBoardService(BulletinBoardRepository bulletinBoardRepository,
      RoomRepository roomRepository,
      UserRepository userRepository) {
    this.bulletinBoardRepository = bulletinBoardRepository;
    this.roomRepository = roomRepository;
    this.userRepository = userRepository;
  }

  /**
   * Updates the bulletin board content for a specific room.
   * <p>
   * Validates the inputs, ensures both room and author exist, persists the new content, and returns
   * a DTO representation. Domain errors propagate as custom exceptions handled by the global
   * exception handler.
   * </p>
   *
   * @param roomId   unique identifier of the room whose bulletin board is updated
   * @param content  the new bulletin text
   * @param authorId identifier of the user performing the update
   * @return {@link BulletinBoardResponse} describing the updated bulletin board
   */
  public BulletinBoardResponse updateBulletinBoard(Integer roomId, String content,
      Integer authorId) {
    if (roomId == null) {
      throw new ValidationException("Room id is required");
    }
    if (authorId == null) {
      throw new ValidationException("Author id is required");
    }
    if (content == null || content.trim().isEmpty()) {
      throw new ValidationException("Bulletin content cannot be empty");
    }

    User author = getExistingUser(authorId);
    getExistingRoom(roomId);
    enforceBulletinModerationRights(roomId, authorId);

    BulletinBoard existingBoard = bulletinBoardRepository.getBulletinBoard(roomId);
    BulletinBoard updatedDomainBoard = existingBoard == null
        ? bulletinBoardRepository.createBulletinBoard(roomId, content, authorId)
        : bulletinBoardRepository.updateBulletinBoard(roomId, content, authorId);
    if (updatedDomainBoard == null) {
      throw new ResourceNotFoundException("Failed to update bulletin board for room " + roomId);
    }

    BulletinBoardResponse responseDto = new BulletinBoardResponse();
    responseDto.setId(updatedDomainBoard.getId());
    responseDto.setContent(updatedDomainBoard.getContent());
    responseDto.setAuthor(mapUserToSummaryResponse(author));
    responseDto.setUpdatedAt(updatedDomainBoard.getUpdatedAt());
    return responseDto;
  }

  public void deleteBulletinBoard(Integer roomId, Integer requesterId) {
    if (roomId == null) {
      throw new ValidationException("Room id is required");
    }
    if (requesterId == null) {
      throw new ValidationException("Requester id is required");
    }
    getExistingRoom(roomId);
    enforceBulletinModerationRights(roomId, requesterId);
    if (!bulletinBoardRepository.isBulletinBoardExists(roomId)) {
      throw new ResourceNotFoundException("Bulletin board not found for room " + roomId);
    }
    bulletinBoardRepository.deleteBulletinBoard(roomId);
  }

  private void enforceBulletinModerationRights(Integer roomId, Integer userId) {
    if (!roomRepository.isUserInMembers(roomId, userId)) {
      throw new AuthorizationException("User is not a participant of room " + roomId);
    }
    Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
    if (role != Role.OWNER && role != Role.ADMIN) {
      throw new AuthorizationException("Only room owner or admin can manage bulletin board");
    }
  }

  private Room getExistingRoom(Integer roomId) {
    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found: " + roomId);
    }
    return room;
  }

  private User getExistingUser(Integer userId) {
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("User not found: " + userId);
    }
    return user;
  }

  private UserSummaryResponse mapUserToSummaryResponse(User author) {
    UserSummaryResponse authorSummary = new UserSummaryResponse();
    authorSummary.setId(author.getId());
    authorSummary.setUserName(author.getUserName());
    authorSummary.setDateOfBirth(author.getDataOfBirth());
    authorSummary.setCity(author.getCity());
    authorSummary.setCountry(author.getCountry());
    authorSummary.setDescription(author.getDescription());
    authorSummary.setAvatarId(author.getAvatarId());
    return authorSummary;
  }
}
