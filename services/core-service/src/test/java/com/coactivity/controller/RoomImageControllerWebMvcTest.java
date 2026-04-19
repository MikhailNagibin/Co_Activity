package com.coactivity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.auth.domain.UserStatus;
import com.coactivity.config.SecurityConfig;
import com.coactivity.controller.impl.RoomControllerImpl;
import com.coactivity.security.CurrentUserDetailsService;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.security.RestAccessDeniedHandler;
import com.coactivity.security.RestAuthenticationEntryPoint;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.RoomImageContent;
import com.coactivity.service.RoomImageService;
import com.coactivity.service.RoomInvitationService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.RoomService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RoomControllerImpl.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableMethodSecurity
@DisplayName("Room image controller web tests")
class RoomImageControllerWebMvcTest {

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
  void publicRoomImageCanBeFetchedWithoutAuthentication() throws Exception {
    byte[] imageBytes = "room-image".getBytes(StandardCharsets.UTF_8);
    when(roomImageService.getRoomImageContent(42, 9))
        .thenReturn(new RoomImageContent(imageBytes, MediaType.IMAGE_PNG_VALUE, imageBytes.length));

    mockMvc.perform(get("/api/rooms/42/images/9"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(imageBytes));
  }

  @Test
  void authenticatedOwnerCanUploadRoomImages() throws Exception {
    CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");
    MockMultipartFile firstFile = new MockMultipartFile(
        "files",
        "cover.png",
        MediaType.IMAGE_PNG_VALUE,
        "cover".getBytes(StandardCharsets.UTF_8));
    MockMultipartFile secondFile = new MockMultipartFile(
        "files",
        "gallery.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "gallery".getBytes(StandardCharsets.UTF_8));

    when(roomImageService.uploadRoomImages(eq(7), eq(42), any()))
        .thenReturn(List.of(
            new com.coactivity.controller.dto.response.RoomImageResponse(
                10, "/api/rooms/42/images/10", 1),
            new com.coactivity.controller.dto.response.RoomImageResponse(
                11, "/api/rooms/42/images/11", 2)));

    mockMvc.perform(multipart("/api/rooms/42/images")
            .file(firstFile)
            .file(secondFile)
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[0].order").value(1))
        .andExpect(jsonPath("$[1].id").value(11))
        .andExpect(jsonPath("$[1].order").value(2));

    verify(roomImageService).uploadRoomImages(eq(7), eq(42), any());
  }

  @Test
  void authenticatedOwnerCanDeleteRoomImage() throws Exception {
    CurrentUserPrincipal principal = principal(7, "owner@example.com", "roomOwner");
    when(roomImageService.deleteRoomImage(7, 42, 10)).thenReturn(List.of());

    mockMvc.perform(delete("/api/rooms/42/images/10")
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()))))
        .andExpect(status().isOk());

    verify(roomImageService).deleteRoomImage(7, 42, 10);
  }

  @Test
  void anonymousUserCannotUploadOrDeleteRoomImages() throws Exception {
    MockMultipartFile imageFile = new MockMultipartFile(
        "files",
        "image.png",
        MediaType.IMAGE_PNG_VALUE,
        "png-image".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/rooms/42/images")
            .file(imageFile)
            .with(csrf()))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(delete("/api/rooms/42/images/10").with(csrf()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(roomImageService);
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
