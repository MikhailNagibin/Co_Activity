package com.coactivity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.auth.domain.UserStatus;
import com.coactivity.config.SecurityConfig;
import com.coactivity.controller.dto.response.OwnershipTransferResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.domain.Role;
import com.coactivity.controller.impl.RoomControllerImpl;
import com.coactivity.domain.Category;
import com.coactivity.domain.RoomStatus;
import com.coactivity.security.CurrentUserDetailsService;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.security.RestAccessDeniedHandler;
import com.coactivity.security.RestAuthenticationEntryPoint;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.RoomImageService;
import com.coactivity.service.RoomInvitationService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.RoomService;
import com.coactivity.service.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RoomControllerImpl.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableMethodSecurity
@DisplayName("Room management controller web tests")
class RoomManagementControllerWebMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RoomService roomService;

  @MockitoBean
  private RoomMembershipService roomMembershipService;

  @MockitoBean
  private RoomInvitationService roomInvitationService;

  @MockitoBean
  private BulletinBoardService bulletinBoardService;

  @MockitoBean
  private RoomImageService roomImageService;

  @MockitoBean
  private CurrentUserDetailsService currentUserDetailsService;

  @Test
  void authenticatedOwnerCanUpdateRoom() throws Exception {
    CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");
    RoomDetailedResponse response = new RoomDetailedResponse();
    response.setId(42);
    response.setStatus(RoomStatus.COMPLETED);
    response.setIsPublic(false);
    response.setCategory(Category.ART);
    response.setName("Updated room");
    response.setDescription("Updated description");
    response.setDateOfStartEvent(Instant.parse("2025-01-01T10:00:00Z"));
    response.setDateOfEndEvent(Instant.parse("2025-01-01T12:00:00Z"));
    response.setAgeRating(18);
    response.setParticipantCount(2);
    response.setMaximumParticipants(10);
    response.setIsCurrentUserParticipant(true);
    response.setImageIds(List.of());
    response.setImages(List.of());
    response.setHasProtectedAccess(true);
    response.setChatLink("https://chat.example.com/updated");

    when(roomService.updateRoom(eq(7), eq(42), any())).thenReturn(response);

    mockMvc.perform(put("/api/rooms/42")
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": false,
                  "category": "ART",
                  "name": "Updated room",
                  "description": "Updated description",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/updated",
                  "dateOfStartEvent": "2025-01-01T10:00:00Z",
                  "dateOfEndEvent": "2025-01-01T12:00:00Z",
                  "status": "COMPLETED",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.name").value("Updated room"));

    verify(roomService).updateRoom(eq(7), eq(42), any());
  }

  @Test
  void anonymousUserCannotUpdateRoom() throws Exception {
    mockMvc.perform(put("/api/rooms/42")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": false,
                  "category": "ART",
                  "name": "Updated room",
                  "description": "Updated description",
                  "maximumNumberOfPeople": 10,
                  "status": "INACTIVE",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(roomService);
  }

  @Test
  void invalidUpdateRequestReturnsBadRequest() throws Exception {
    CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");

    mockMvc.perform(put("/api/rooms/42")
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": false,
                  "category": "ART",
                  "name": "",
                  "description": "Updated description",
                  "maximumNumberOfPeople": 1,
                  "status": "INACTIVE",
                  "ageRating": 30
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    verifyNoInteractions(roomService);
  }

  @Test
  void authenticatedModeratorCanRemoveParticipant() throws Exception {
    CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");

    mockMvc.perform(delete("/api/rooms/42/participants/11")
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()))))
        .andExpect(status().isNoContent());

    verify(roomMembershipService).removeParticipant(7, 42, 11);
  }

  @Test
  void authenticatedOwnerCanTransferOwnership() throws Exception {
    CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");
    var response = new OwnershipTransferResponse(42, 7, 11, Role.PARTICIPANT, Role.OWNER);

    when(roomMembershipService.transferOwnership(7, 42, 11)).thenReturn(response);

    mockMvc.perform(post("/api/rooms/42/ownership/transfer")
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetUserId": 11
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roomId").value(42))
        .andExpect(jsonPath("$.previousOwnerId").value(7))
        .andExpect(jsonPath("$.newOwnerId").value(11))
        .andExpect(jsonPath("$.previousOwnerNewRole").value("PARTICIPANT"))
        .andExpect(jsonPath("$.newOwnerRole").value("OWNER"));

    verify(roomMembershipService).transferOwnership(7, 42, 11);
  }

  @Test
  void invalidTransferRequestReturnsBadRequest() throws Exception {
    CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");

    mockMvc.perform(post("/api/rooms/42/ownership/transfer")
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetUserId": 0
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    verifyNoInteractions(roomMembershipService);
  }

    @Test
    void missingRoomForRemoveParticipantReturnsNotFound() throws Exception {
      CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");
    doThrow(new ResourceNotFoundException("ROOM_NOT_FOUND", "Room not found: 999"))
        .when(roomMembershipService).removeParticipant(7, 999, 11);

    mockMvc.perform(delete("/api/rooms/999/participants/11")
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Room not found: 999"));
  }

  @Test
    void missingRoomForBanListReturnsNotFound() throws Exception {
      CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");
    when(roomMembershipService.getBannedUsers(7, 999))
        .thenThrow(new ResourceNotFoundException("ROOM_NOT_FOUND", "Room not found: 999"));

    mockMvc.perform(get("/api/rooms/999/bans")
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Room not found: 999"));
  }

  private CurrentUserPrincipal principal(Integer userId, String email, String userName) {
    return new CurrentUserPrincipal(
        userId,
        email,
        email.toLowerCase(),
        userName,
        "password-hash",
        UserStatus.ACTIVE,
        Instant.now());
  }
}
