package com.coactivity.service;

import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.BulletinBoardRepositoryImpl;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
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
   * This method retrieves the room and user based on the provided IDs, creates a new BulletinBoard
   * entity with the updated content, persists it via the repository, and returns a response DTO.
   * </p>
   *
   * @param roomId   The unique identifier of the room whose bulletin board is to be updated.
   * @param content  The new content for the bulletin board.
   * @param authorId The unique identifier of the user updating the bulletin board.
   * @return ApiResponse containing the updated BulletinBoardResponse DTO, or an error response if
   * the room/user does not exist or update fails.
   */
  public ApiResponse<BulletinBoardResponse> updateBulletinBoard(Integer roomId, String content,
      Integer authorId) {
    // Validate input parameters
    if (roomId == null || content == null || authorId == null) {
      return ApiResponse.error("400"); // Bad request if any required parameter is missing
    }

    Room room = roomRepository.getRoomById(roomId);
    User author = userRepository.getUserById(authorId);

    // Check if room and user exist
    if (room == null || author == null) {
      return ApiResponse.error("404"); // Not found if room or user doesn't exist
    }

    BulletinBoard updatedDomainBoard = bulletinBoardRepository.updateBulletinBoard(roomId, content,
        authorId);

    // Check if the repository operation was successful (it should return a non-null entity on success)
    if (updatedDomainBoard == null) {
      return ApiResponse.error("500"); // Internal server error if update failed internally
    }

    BulletinBoardResponse responseDto = new BulletinBoardResponse();
    responseDto.setId(updatedDomainBoard.getId());
    responseDto.setContent(updatedDomainBoard.getContent());

    UserSummaryResponse authorSummary = new UserSummaryResponse();
    authorSummary.setId(author.getId());
    authorSummary.setUserName(author.getUserName());
    authorSummary.setDateOfBirth(author.getDataOfBirth());
    authorSummary.setCity(author.getCity());
    authorSummary.setCountry(author.getCountry());
    authorSummary.setDescription(author.getDescription());
    authorSummary.setAvatarId(author.getAvatarId());

    responseDto.setAuthor(authorSummary);
    responseDto.setUpdatedAt(updatedDomainBoard.getUpdatedAt());

    return ApiResponse.success(responseDto);
  }

  public ApiResponse<Void> deleteBulletinBoard(Integer roomId) {
    if (roomId == null) {
      return ApiResponse.error("400");
    }

    try {
      if (!bulletinBoardRepository.isBulletinBoardExists(roomId)) {
        return ApiResponse.error("404");
      }
      bulletinBoardRepository.deleteBulletinBoard(roomId);
      return ApiResponse.success(null);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return ApiResponse.error("500");
    }
  }
}