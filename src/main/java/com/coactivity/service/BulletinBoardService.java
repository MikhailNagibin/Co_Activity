package com.coactivity.service;

import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.User;
import com.coactivity.domain.Room;
import com.coactivity.controller.dto.response.UserSummaryResponse;
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
     * This method retrieves the room and user based on the provided IDs,
     * creates a new BulletinBoard entity with the updated content,
     * persists it via the repository, and returns a response DTO.
     * </p>
     *
     * @param roomId   The unique identifier of the room whose bulletin board is to be updated.
     * @param content  The new content for the bulletin board.
     * @param authorId The unique identifier of the user updating the bulletin board.
     * @return ApiResponse containing the updated BulletinBoardResponse DTO,
     *         or an error response if the room/user does not exist or update fails.
     */
    public ApiResponse<BulletinBoardResponse> updateBulletinBoard(Integer roomId, String content, Integer authorId) {
        // Validate input parameters
        if (roomId == null || content == null || authorId == null) {
            return ApiResponse.error("400"); // Bad request if any required parameter is missing
        }

        // Fetch room and user entities
        Room room = roomRepository.getRoomById(roomId);
        User author = userRepository.getUserById(authorId);

        // Check if room and user exist
        if (room == null || author == null) {
            return ApiResponse.error("404"); // Not found if room or user doesn't exist
        }

        // Create the domain entity for the bulletin board update
        // The repository's update method likely handles the logic of updating an existing entry
        // or creating a new one if none exists for the room. Let's call it updateBulletinBoard.
        // It's assumed to return the updated BulletinBoard domain object.
        // Note: The actual method name in BulletinBoardRepositoryImpl might differ.
        // Based on the repository impl provided, it seems like it creates or updates based on room_id.
        // Let's assume it has an update method that returns the updated entity.
        // If the repository method returns void or just confirms success, you might need to fetch the updated entity separately.
        // For now, let's assume it returns the updated BulletinBoard.
        // Also, the repository method might require the author object, not just the ID.
        // Looking at the repo impl: updateBulletinBoard(Integer roomId, String content, Integer authorId)
        // It returns a BulletinBoard. Perfect.
        BulletinBoard updatedDomainBoard = bulletinBoardRepository.updateBulletinBoard(roomId, content, authorId);

        // Check if the repository operation was successful (it should return a non-null entity on success)
        if (updatedDomainBoard == null) {
            // This could happen if the internal update logic failed unexpectedly.
            // The repository might throw an exception or return null/error on failure.
            // Let's assume it returns null on failure based on typical patterns.
            // If it throws, it would be caught by the calling controller's exception handling.
            // For now, handle the case where it returns null.
            return ApiResponse.error("500"); // Internal server error if update failed internally
        }

        // Map the domain entity to the response DTO
        // Using the domain entity's fields, construct the BulletinBoardResponse DTO.
        BulletinBoardResponse responseDto = new BulletinBoardResponse();
        responseDto.setId(updatedDomainBoard.getId());
        responseDto.setContent(updatedDomainBoard.getContent());
        // The author field in BulletinBoardResponse is UserSummaryResponse
        UserSummaryResponse authorSummary = new UserSummaryResponse();
        authorSummary.setId(author.getId());
        authorSummary.setUserName(author.getUserName()); // Assuming 'name' in UserSummary comes from 'username' in User
        authorSummary.setDateOfBirth(author.getDataOfBirth());
        authorSummary.setCity(author.getCity());
        authorSummary.setCountry(author.getCountry());
        authorSummary.setDescription(author.getDescription());
        authorSummary.setAvatarId(author.getAvatarId());
        responseDto.setAuthor(authorSummary);
        responseDto.setUpdatedAt(updatedDomainBoard.getUpdatedAt());

        // Return success response with the DTO
        return ApiResponse.success(responseDto);
    }

    // Other potential methods for BulletinBoardService could be added here later:
    // e.g., getBulletinBoardByRoomId, createBulletinBoard (if separate), deleteBulletinBoard, etc.
    // For now, the spec only mentions updateBulletinBoard via the TODO comment.
}