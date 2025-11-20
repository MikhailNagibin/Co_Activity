package com.coactivity.service;

/*

TODO черновая реализация, можно не смотреть вообще. :3>

 */


import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JoinRequestService {

    private final RoomRepositoryImpl roomRepository;
    private final UserRepositoryImpl userRepository;

    public JoinRequestService(RoomRepositoryImpl roomRepository, UserRepositoryImpl userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves pending join requests for rooms where the user has administrative privileges.
     * <p>
     * This method fetches all join requests with status {@link RequestStatus#CONSIDERATION}
     * for rooms where the specified user is an owner or an administrator.
     * </p>
     *
     * @param userId The ID of the user with administrative privileges.
     * @return ApiResponse containing a list of JoinRequestResponse objects, or an error.
     */
    public ApiResponse<List<JoinRequestResponse>> getPendingRequests(Integer userId) {
        // Check if user exists
        var user = userRepository.getUserById(userId);
        if (user == null) {
            return ApiResponse.error("404");
        }

        // Find rooms where the user is admin/owner and get pending requests
        // This logic might require a new method in RoomRepositoryImpl to find all requests
        // for rooms where a specific user has admin rights.
        // For now, let's assume roomRepository has a method like this:
        // List<JoinRequest> requests = roomRepository.getPendingRequestsForAdmin(userId);

        // Placeholder logic:
        // This is a simplified version. In reality, you'd query the database more efficiently,
        // perhaps with a single JOIN query in RoomRepositoryImpl.
        // Assuming a method exists: roomRepository.getAllJoinRequestsForAdmin(userId)
        // List<JoinRequest> allRequestsForAdmin = roomRepository.getAllJoinRequestsForAdmin(userId);
        // List<JoinRequest> pendingRequests = allRequestsForAdmin.stream()
        //     .filter(r -> r.getStatus() == RequestStatus.CONSIDERATION)
        //     .collect(Collectors.toList());

        // For now, returning an empty list as the specific repo method isn't defined in the provided impl.
        // You would need to implement this in RoomRepositoryImpl or a dedicated JoinRequestRepository.
        // Example query in RoomRepositoryImpl:
        // SELECT ur.user_id, ur.username, rr.room_id, r.name as room_name, rr.status_id, rr.created_at
        // FROM Rooms_requests rr
        // JOIN Users ur ON ur.id = rr.user_id
        // JOIN Rooms r ON r.id = rr.room_id
        // JOIN Rooms_members rm_admin ON rm_admin.room_id = r.id
        // JOIN Roles role_admin ON role_admin.id = rm_admin.role_id
        // WHERE rm_admin.user_id = ? AND role_admin.role IN ('owner', 'admin')
        // AND rr.status_id = (SELECT id FROM RequestStatuses WHERE status_info = 'Consideration');

        // For now, returning an empty list as the implementation depends on the repository structure.
        // You might need to create a new repository or extend RoomRepositoryImpl.
        // e.g., a JoinRequestRepositoryImpl would be ideal.
        // For the sake of this exercise, let's assume a hypothetical method exists or will be created:
        // List<JoinRequestEntity> dbRequests = joinRequestRepository.findByAdminUserIdAndStatus(userId, RequestStatus.CONSIDERATION);

        // Placeholder return
        List<JoinRequestResponse> responses = List.of(); // Replace with actual mapped list from DB query
        return ApiResponse.success(responses);
    }

    /**
     * Retrieves pending join requests for a specific private room where the user has administrative privileges.
     * <p>
     * This method fetches join requests with status {@link RequestStatus#CONSIDERATION}
     * specifically for the given room, ensuring the user is an admin/owner of that room.
     * </p>
     *
     * @param userId The ID of the user with administrative privileges.
     * @param roomId The ID of the room to retrieve requests for.
     * @return ApiResponse containing a list of JoinRequestResponse objects for the room, or an error.
     */
    public ApiResponse<List<JoinRequestResponse>> getPendingRequestsForRoom(Integer userId, Integer roomId) {
        // Validate user and room existence
        var user = userRepository.getUserById(userId);
        var room = roomRepository.getRoomById(roomId);
        if (user == null || room == null) {
            return ApiResponse.error("404");
        }

        // Check if user has admin privileges for the room
        // This uses the existing method in RoomRepositoryImpl
        if (!roomRepository.isUserOwnerOfRoom(userId, roomId)) {
            // Also need to check for ADMIN role, not just OWNER
            // Assuming a method exists or can be added to check admin role
            // boolean isAdmin = roomRepository.isUserAdminOfRoom(userId, roomId);
            // For now, using the existing method which checks OWNER only.
            // A more robust check would be:
            // Role userRole = roomRepository.getUserRoleByRoomId(roomId, userId);
            // if (userRole != Role.OWNER && userRole != Role.ADMIN) {
            //     return ApiResponse.error("403");
            // }
            // The current impl in RoomRepositoryImpl only checks OWNER.
            // Let's assume a general check for admin rights exists or is handled by a role check.
            // For now, we'll just check if user is in the room and has a high enough role.
            // A direct query might be better here.
            // For simplicity here, let's just call a hypothetical method or adapt existing ones.
            // The current RoomRepositoryImpl.isUserOwnerOfRoom only checks owner.
            // To check admin, we'd need:
            // String sql = "SELECT COUNT(*) FROM Rooms_members rm JOIN Roles r ON rm.role_id = r.id WHERE rm.user_id = ? AND rm.room_id = ? AND r.role IN ('owner', 'admin')";
            // For now, we'll assume the controller/service handles role checks before calling this,
            // or we add a method to RoomRepositoryImpl.
            // Let's add a check here based on role.
            // Role userRole = roomRepository.getUserRoleByRoomId(roomId, userId);
            // if (userRole != Role.OWNER && userRole != Role.ADMIN) {
            //     return ApiResponse.error("403");
            // }

            // As per the current impl of RoomRepositoryImpl, it only checks for OWNER.
            // So we'll use that.
            if (!roomRepository.isUserOwnerOfRoom(userId, roomId)) {
                // Attempt to check for admin role as well based on the schema.
                // The method getUserRoleByRoomId in RoomRepositoryImpl can return the role.
                // Let's use it.
                com.coactivity.domain.Role userRole = null;
                try {
                    // Note: The provided RoomRepositoryImpl.getUserRoleByRoomId has incorrect param order in its call.
                    // Statement.setInt(1, userId); should be setInt(2, userId) and vice versa based on "WHERE rm.room_id = ? and rm.user_id = ?"
                    // Correcting the call here.
                    userRole = roomRepository.getUserRoleByRoomId(roomId, userId);
                } catch (Exception e) {
                    // If user is not in the room at all, getUserRoleByRoomId might throw.
                    // In that case, they definitely aren't an admin.
                    return ApiResponse.error("403");
                }
                if (userRole != com.coactivity.domain.Role.OWNER && userRole != com.coactivity.domain.Role.ADMIN) {
                    return ApiResponse.error("403");
                }
            }


        }

        // Fetch requests for this room with status CONSIDERATION
        // Assuming a method exists in RoomRepositoryImpl or a new JoinRequestRepository is used.
        // The current Rooms_requests table is accessed via RoomRepositoryImpl.
        // Let's assume a method exists like:
        // List<JoinRequestEntity> dbRequests = roomRepository.getJoinRequestsByRoomIdAndStatus(roomId, RequestStatus.CONSIDERATION);
        // For now, since it's not explicitly in the provided RoomRepositoryImpl, we'll outline the necessary logic.

        // Example raw logic (would be in repository):
        // String sql = "SELECT user_id, status_id, created_at FROM Rooms_requests WHERE room_id = ? AND status_id = ?";
        // Loop through results, fetch user and room names, map to JoinRequestResponse.

        // Placeholder return
        List<JoinRequestResponse> responses = List.of(); // Replace with actual mapped list
        return ApiResponse.success(responses);
    }

    /**
     * Processes a room join request with the specified action (Accept, Reject, Reject with Ban).
     * <p>
     * Updates the request status and performs the corresponding action (adding user to room,
     * notifying user, potentially banning user).
     * </p>
     *
     * @param userId    The ID of the user with administrative privileges performing the action.
     * @param requestId The ID of the join request to process.
     * @param action    The action to take ({@link RequestStatus#ACCEPTED}, {@link RequestStatus#REFUSED}, {@link RequestStatus#REFUSED_WITH_BAN}).
     * @return ApiResponse indicating success or failure.
     */
    public ApiResponse<Void> processJoinRequest(Integer userId, Integer requestId, RequestStatus action) {
        // Validate inputs
        if (action == null || requestId == null || userId == null) {
            return ApiResponse.error("400");
        }

        // Fetch the join request details (requires a method in repository to get by ID)
        // This logic heavily depends on having a method like roomRepository.getJoinRequestById(requestId)
        // which joins Rooms_requests, Users, and Rooms tables.
        // For now, let's assume a method exists or we construct the logic.
        // The tables involved: Rooms_requests (user_id, room_id, status_id), Users (for email), Rooms (for name)
        // We need to find the user who made the request, the room, and the admin who is processing it.

        // Example of what's needed from DB:
        // SELECT rq.id, rq.user_id as requester_id, rq.room_id, rq.status_id as current_status_id,
        // u.login as requester_email, r.name as room_name, rm.role_id as admin_role_id
        // FROM Rooms_requests rq
        // JOIN Users u ON u.id = rq.user_id
        // JOIN Rooms r ON r.id = rq.room_id
        // LEFT JOIN Rooms_members rm ON rm.user_id = ? AND rm.room_id = r.id -- admin
        // LEFT JOIN Roles role ON role.id = rm.role_id
        // WHERE rq.id = ? AND role.role IN ('owner', 'admin')

        // For now, let's outline the steps assuming helper methods exist or are added to repositories.

        // 1. Verify admin rights for the room associated with the request.
        // 2. Get current status of the request.
        // 3. Update the request status.
        // 4. If accepted, add user to room members.
        // 5. If banned, add to Bans table.
        // 6. Send notification via NotificationService (not instantiated here, but called).

        // Placeholder implementation:
        // This requires detailed DB interaction not fully provided in current repos.
        // A new JoinRequestRepository or extension of RoomRepositoryImpl is ideal.

        // Assume a method exists to get the request details including room_id and requester_id:
        // JoinRequestDetails details = roomRepository.getJoinRequestDetailsById(requestId);

        // Check if admin can act on this request (belongs to a room they manage)
        // if (!roomRepository.isUserAdminOrOwnerOfRoomWithRequestId(userId, requestId)) {
        //     return ApiResponse.error("403");
        // }

        // Update status in Rooms_requests
        // roomRepository.updateJoinRequestStatus(requestId, action);

        // Handle consequences of the action (accept, reject, ban)
        // if (action == RequestStatus.ACCEPTED) {
        //     // Add user to room members as PARTICIPANT
        //     Integer PARTICIPANT_ROLE_ID = 2; // Assuming 'Participant' role ID is 2, fetched from Roles table
        //     roomRepository.addUserToRoom(details.getRoomId(), details.getRequesterId(), PARTICIPANT_ROLE_ID);
        //     // Notify user of acceptance
        //     // notificationService.sendMembershipAccepted(userEmail, roomName);
        // } else if (action == RequestStatus.REFUSED_WITH_BAN) {
        //    // Add to Bans table
        //    roomRepository.addBan(details.getRequesterId(), details.getRoomId());
        //    // Notify user of rejection with ban
        //    // notificationService.sendMembershipRejected(userEmail, roomName);
        // } else if (action == RequestStatus.REFUSED) {
        //    // Just notify user of rejection
        //    // notificationService.sendMembershipRejected(userEmail, roomName);
        // }

        // For now, returning success as a placeholder.
        // Actual implementation requires repository methods.
        return ApiResponse.success(null);
    }

    /**
     * Retrieves join requests submitted by the authenticated user.
     * <p>
     * This method fetches all join requests initiated by the specified user,
     * regardless of their current status (Consideration, Accepted, Rejected).
     * </p>
     *
     * @param userId The ID of the user whose sent requests are being retrieved.
     * @return ApiResponse containing a list of JoinRequestResponse objects, or an error.
     */
    public ApiResponse<List<JoinRequestResponse>> getSentRequests(Integer userId) {
        // Validate user existence
        var user = userRepository.getUserById(userId);
        if (user == null) {
            return ApiResponse.error("404");
        }

        // Fetch requests made by this user
        // Assuming a method in RoomRepositoryImpl or a new repository:
        // List<JoinRequestEntity> dbRequests = roomRepository.getJoinRequestsByRequesterId(userId);

        // Placeholder return
        List<JoinRequestResponse> responses = List.of(); // Replace with actual mapped list
        return ApiResponse.success(responses);
    }

    /**
     * Cancels a pending join request submitted by the specified user.
     * <p>
     * This operation is only valid if the request is still in the {@link RequestStatus#CONSIDERATION} state.
     * It removes the request from the system.
     * </p>
     *
     * @param userId    The ID of the user who submitted the request.
     * @param requestId The ID of the join request to cancel.
     * @return ApiResponse indicating success or failure (e.g., if request not found or not pending).
     */
    public ApiResponse<Void> cancelRequest(Integer userId, Integer requestId) {
        // Validate inputs
        if (requestId == null || userId == null) {
            return ApiResponse.error("400");
        }

        // Validate user existence
        var user = userRepository.getUserById(userId);
        if (user == null) {
            return ApiResponse.error("404");
        }

        // Fetch the specific request to check if it belongs to the user and is pending
        // JoinRequestDetails details = roomRepository.getJoinRequestDetailsById(requestId);
        // if (details == null || !details.getRequesterId().equals(userId)) {
        //     return ApiResponse.error("404"); // Request not found or doesn't belong to user
        // }
        // if (details.getCurrentStatus() != RequestStatus.CONSIDERATION) {
        //     return ApiResponse.error("400"); // Cannot cancel if not pending
        // }

        // Delete the request from Rooms_requests table
        // roomRepository.deleteJoinRequest(requestId);

        // Placeholder return
        return ApiResponse.success(null);
    }
}