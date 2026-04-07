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

import com.coactivity.auth.model.UserStatus;
import com.coactivity.auth.service.AuthApplicationService;
import com.coactivity.auth.service.SessionInvalidationService;
import com.coactivity.config.SecurityConfig;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.security.CurrentUserDetailsService;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.security.RestAccessDeniedHandler;
import com.coactivity.security.RestAuthenticationEntryPoint;
import com.coactivity.service.AccountDeletionService;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.UserAvatarContent;
import com.coactivity.service.UserAvatarService;
import com.coactivity.service.UserProfileService;
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

@WebMvcTest(controllers = UserControllerImpl.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableMethodSecurity
@DisplayName("User avatar controller web tests")
class UserAvatarControllerWebMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserProfileService userProfileService;

  @MockitoBean
  private RoomMembershipService roomMembershipService;

  @MockitoBean
  private JoinRequestService joinRequestService;

  @MockitoBean
  private SessionInvalidationService sessionInvalidationService;

  @MockitoBean
  private AccountDeletionService accountDeletionService;

  @MockitoBean
  private AuthApplicationService authApplicationService;

  @MockitoBean
  private UserAvatarService userAvatarService;

  @MockitoBean
  private CurrentUserDetailsService currentUserDetailsService;

  @Test
  void publicUserAvatarCanBeFetchedWithoutAuthentication() throws Exception {
    byte[] avatarBytes = "avatar-bytes".getBytes(StandardCharsets.UTF_8);
    when(userAvatarService.getAvatarContent(7))
        .thenReturn(new UserAvatarContent(avatarBytes, MediaType.IMAGE_PNG_VALUE, avatarBytes.length));

    mockMvc.perform(get("/api/users/7/avatar"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(avatarBytes));
  }

  @Test
  void authenticatedUserCanUploadAvatarViaMultipartPut() throws Exception {
    CurrentUserPrincipal principal = principal(7, "avatar@example.com", "avatarUser");
    UserProfileResponse profileResponse = new UserProfileResponse(
        7,
        "avatar@example.com",
        "avatarUser",
        null,
        null,
        null,
        null,
        null,
        "/api/users/7/avatar",
        List.of());
    MockMultipartFile avatarFile = new MockMultipartFile(
        "file",
        "avatar.png",
        MediaType.IMAGE_PNG_VALUE,
        "png-avatar".getBytes(StandardCharsets.UTF_8));

    when(userProfileService.getUserProfile(7)).thenReturn(profileResponse);

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(avatarFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .with(csrf())
            .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("/api/users/7/avatar"));

    verify(userAvatarService).uploadAvatar(eq(7), any());
    verify(userProfileService).getUserProfile(7);
  }

  @Test
  void anonymousUserCannotUploadOrDeleteAvatar() throws Exception {
    MockMultipartFile avatarFile = new MockMultipartFile(
        "file",
        "avatar.png",
        MediaType.IMAGE_PNG_VALUE,
        "png-avatar".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(avatarFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .with(csrf()))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(delete("/api/users/me/avatar").with(csrf()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(userAvatarService);
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
