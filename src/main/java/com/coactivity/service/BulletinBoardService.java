package com.coactivity.service;

import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.BulletinBoardRepositoryImpl;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import org.springframework.stereotype.Service;

@Service
public class BulletinBoardService {

  private final BulletinBoardRepositoryImpl bulletinBoardRepository;
  private final RoomRepositoryImpl roomRepository;
  private final UserRepositoryImpl userRepository;

  public BulletinBoardService(BulletinBoardRepositoryImpl bulletinBoardRepository,
      RoomRepositoryImpl roomRepository,
      UserRepositoryImpl userRepository) {
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


    // Validate room existence before proceeding
    var board = bulletinBoardRepository.getBulletinBoard(roomId);

    if (board == null) {
      bulletinBoardRepository.createBulletinBoard(roomId, "temp content", authorId);
    }

    getExistingRoom(roomId);    BulletinBoard updatedDomainBoard = bulletinBoardRepository.updateBulletinBoard(roomId, content,
        authorId);
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

  public void deleteBulletinBoard(Integer roomId) {
    if (roomId == null) {
      throw new ValidationException("Room id is required");
    }
    if (!bulletinBoardRepository.isBulletinBoardExists(roomId)) {
      throw new ResourceNotFoundException("Bulletin board not found for room " + roomId);
    }
    bulletinBoardRepository.deleteBulletinBoard(roomId);
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