package com.coactivity.controller.impl;

import com.coactivity.auth.service.AuthApplicationService;
import com.coactivity.controller.dto.request.AccountDeletionRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.AccountDeletionPreviewResponse;
import com.coactivity.controller.dto.response.ApiProblemDetail;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.service.AccountDeletionService;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.UserAvatarContent;
import com.coactivity.service.UserAvatarService;
import com.coactivity.service.UserProfileService;
import com.coactivity.auth.service.SessionInvalidationService;
import com.coactivity.security.CurrentUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(
    name = "Users",
    description = "Профиль пользователя, аватар, настройки уведомлений, заявки на вступление, роли в комнатах, удаление аккаунта."
)
public class UserControllerImpl {

  private final UserProfileService userProfileService;
  private final RoomMembershipService roomMembershipService;
  private final JoinRequestService joinRequestService;
  private final SessionInvalidationService sessionInvalidationService;
  private final AccountDeletionService accountDeletionService;
  private final AuthApplicationService authApplicationService;
  private final UserAvatarService userAvatarService;

  public UserControllerImpl(UserProfileService userProfileService,
      RoomMembershipService roomMembershipService,
      JoinRequestService joinRequestService,
      SessionInvalidationService sessionInvalidationService,
      AccountDeletionService accountDeletionService,
      AuthApplicationService authApplicationService,
      UserAvatarService userAvatarService) {
    this.userProfileService = userProfileService;
    this.roomMembershipService = roomMembershipService;
    this.joinRequestService = joinRequestService;
    this.sessionInvalidationService = sessionInvalidationService;
    this.accountDeletionService = accountDeletionService;
    this.authApplicationService = authApplicationService;
    this.userAvatarService = userAvatarService;
  }

  @GetMapping("/me")
  @Operation(
      summary = "Профиль текущего пользователя",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Профиль пользователя.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<UserProfileResponse> getUserProfile(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    UserProfileResponse response = userProfileService.getUserProfile(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{userId}")
  @Operation(
      summary = "Публичный профиль пользователя по ID",
      description = "Возвращает краткую информацию о пользователе.",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Публичный профиль.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = UserSummaryResponse.class))),
      @ApiResponse(responseCode = "400", description = "Некорректный userId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация (см. SecurityConfig).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Пользователь не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<UserSummaryResponse> getPublicUserProfileById(
      @Parameter(description = "ID пользователя.", example = "7")
      @Positive @PathVariable Integer userId) {
    UserSummaryResponse response = userProfileService.getPublicUserProfileById(userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{userId}/avatar")
  @Operation(
      summary = "Аватар пользователя (binary)",
      description = "Возвращает аватар пользователя как изображение. Content-Type определяется по фактическому файлу."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Изображение аватара.",
          content = {
              @Content(mediaType = "image/png",
                  schema = @Schema(type = "string", format = "binary")),
              @Content(mediaType = "image/jpeg",
                  schema = @Schema(type = "string", format = "binary")),
              @Content(mediaType = "image/webp",
                  schema = @Schema(type = "string", format = "binary"))
          }
      ),
      @ApiResponse(responseCode = "400", description = "Некорректный userId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Аватар/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "503", description = "Хранилище недоступно.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<ByteArrayResource> getUserAvatar(
      @Parameter(description = "ID пользователя.", example = "7")
      @Positive @PathVariable Integer userId) {
    UserAvatarContent avatarContent = userAvatarService.getAvatarContent(userId);
    ByteArrayResource resource = new ByteArrayResource(avatarContent.bytes());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(avatarContent.contentType()))
        .contentLength(avatarContent.sizeBytes())
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(resource);
  }

  @PutMapping("/me")
  @Operation(
      summary = "Обновить профиль текущего пользователя",
      description = "Обновляет поля профиля. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = UserProfileUpdateRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Обновлённый профиль.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<UserProfileResponse> updateUserProfile(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    userProfileService.updateUserProfile(currentUser.getUserId(), request);
    UserProfileResponse response = userProfileService.getUserProfile(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Загрузить/заменить аватар",
      description = "Загружает изображение аватара. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Профиль с обновлённым avatarUrl.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации/multipart/размер/тип файла.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "503", description = "Хранилище недоступно.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<UserProfileResponse> uploadAvatar(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Parameter(
          name = "file",
          description = "Файл-изображение (jpeg/png/webp).",
          required = true,
          content = @Content(schema = @Schema(type = "string", format = "binary"))
      )
      @RequestParam("file") MultipartFile file) {
    userAvatarService.uploadAvatar(currentUser.getUserId(), file);
    UserProfileResponse response = userProfileService.getUserProfile(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/me/avatar")
  @Operation(
      summary = "Удалить аватар",
      description = "Удаляет текущий аватар пользователя. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Аватар удалён."),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteAvatar(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    userAvatarService.deleteAvatar(currentUser.getUserId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me/notifications")
  @Operation(
      summary = "Настройки уведомлений (текущий пользователь)",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Настройки уведомлений.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = NotificationSettingsResponse.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<NotificationSettingsResponse> getNotificationSettings(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    NotificationSettingsResponse response = userProfileService.getNotificationSettings(
        currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @PutMapping("/me/notifications")
  @Operation(
      summary = "Обновить настройки уведомлений",
      description = "Сохраняет настройки уведомлений для текущего пользователя. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = NotificationSettingsRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Сохранённые настройки.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = NotificationSettingsResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<NotificationSettingsResponse> configureNotificationSettings(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody NotificationSettingsRequest request) {
    NotificationSettingsResponse response = userProfileService.configureNotificationSettings(
        currentUser.getUserId(), request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/me/deletion-preview")
  @Operation(
      summary = "Предпросмотр удаления аккаунта",
      description = "Возвращает информацию о последствиях удаления аккаунта.",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Предпросмотр.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = AccountDeletionPreviewResponse.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<AccountDeletionPreviewResponse> getDeletionPreview(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    AccountDeletionPreviewResponse response =
        accountDeletionService.getDeletionPreview(currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/me")
  @Operation(
      summary = "Удалить аккаунт (если нет комнат-владений)",
      description = "Удаляет аккаунт текущего пользователя, только если у него нет комнат, где он OWNER. Требуются session cookie и CSRF. Выполняет logout и инвалидирует сессии.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Аккаунт удалён."),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Удаление запрещено (например, есть owned rooms).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Конфликт состояния.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteAccount(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Parameter(hidden = true) Authentication authentication,
      @Parameter(hidden = true) HttpServletRequest request,
      @Parameter(hidden = true) HttpServletResponse response) {
    accountDeletionService.deleteAccountIfNoOwnedRooms(currentUser.getUserId());
    completeAccountDeletion(currentUser, authentication, request, response);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/me/deletion")
  @Operation(
      summary = "Удалить аккаунт (с owned rooms)",
      description = "Удаляет аккаунт текущего пользователя, включая сценарий с комнатами, где он OWNER (зависит от тела запроса). Требуются session cookie и CSRF. Выполняет logout и инвалидирует сессии.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = AccountDeletionRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Аккаунт удалён."),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Удаление запрещено.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Конфликт состояния.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteAccountWithOwnedRooms(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Parameter(hidden = true) Authentication authentication,
      @Parameter(hidden = true) HttpServletRequest request,
      @Parameter(hidden = true) HttpServletResponse response,
      @Valid @RequestBody AccountDeletionRequest deletionRequest) {
    accountDeletionService.deleteAccount(currentUser.getUserId(), deletionRequest);
    completeAccountDeletion(currentUser, authentication, request, response);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/rooms/{roomId}/admins/{userId}")
  @Operation(
      summary = "Назначить ADMIN в комнате",
      description = "Governance endpoint: назначает пользователю роль ADMIN в комнате. Требуются session cookie и CSRF. Доступ: room OWNER.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Роль назначена.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoleAssignmentResponse.class))),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<RoleAssignmentResponse> assignAdminRole(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    RoleAssignmentResponse response =
        roomMembershipService.assignAdminRole(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/rooms/{roomId}/admins/{userId}")
  @Operation(
      summary = "Снять роль ADMIN в комнате",
      description = "Governance endpoint: понижает ADMIN до PARTICIPANT. Требуются session cookie и CSRF. Доступ: room OWNER.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Роль изменена.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoleAssignmentResponse.class))),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<RoleAssignmentResponse> demoteAdminRole(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    RoleAssignmentResponse response =
        roomMembershipService.demoteAdminRole(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/requests/pending")
  @Operation(
      summary = "Входящие заявки на вступление (pending)",
      description = "Возвращает pending-заявки в комнаты, где текущий пользователь OWNER/ADMIN (governance).",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список заявок.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = JoinRequestResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequests(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<JoinRequestResponse> responses = joinRequestService.getPendingRequests(
        currentUser.getUserId());
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/rooms/{roomId}/requests/pending")
  @Operation(
      summary = "Pending-заявки для конкретной комнаты",
      description = "Governance endpoint: возвращает pending-заявки на вступление в комнате. Доступ: room OWNER/ADMIN.",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список заявок.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = JoinRequestResponse.class)))),
      @ApiResponse(responseCode = "400", description = "Некорректный roomId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER/ADMIN).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<JoinRequestResponse>> getPendingRequestsForRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    List<JoinRequestResponse> responses = joinRequestService.getPendingRequestsForRoom(
        currentUser.getUserId(),
        roomId);
    return ResponseEntity.ok(responses);
  }

  @PostMapping("/requests/{requestId}")
  @Operation(
      summary = "Обработать заявку на вступление",
      description = "Governance endpoint: принять/отклонить заявку на вступление. Требуются session cookie и CSRF. Query param action обязателен.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "action", in = ParameterIn.QUERY, required = true,
              description = "Действие над заявкой.", schema = @Schema(implementation = RequestStatus.class)),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Заявка обработана."),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры/значение action.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER/ADMIN).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Заявка/комната не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> processJoinRequest(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer requestId,
      @NotNull @RequestParam("action") RequestStatus action) {
    joinRequestService.processJoinRequest(currentUser.getUserId(), requestId, action);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/requests/sent")
  @Operation(
      summary = "Отправленные заявки на вступление",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список отправленных заявок.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = JoinRequestResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<JoinRequestResponse>> getSentRequests(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<JoinRequestResponse> responses = joinRequestService.getSentRequests(
        currentUser.getUserId());
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/requests/{requestId}")
  @Operation(
      summary = "Отменить отправленную заявку",
      description = "Отменяет заявку, отправленную текущим пользователем. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Заявка отменена."),
      @ApiResponse(responseCode = "400", description = "Некорректный requestId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не владелец заявки).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Заявка не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> cancelRequest(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer requestId) {
    joinRequestService.cancelRequest(currentUser.getUserId(), requestId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/banned-rooms")
  @Operation(
      summary = "Комнаты, где пользователь забанен",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список комнат.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = RoomSummaryResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<RoomSummaryResponse>> getBanRooms(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<RoomSummaryResponse> rooms = roomMembershipService.getBanRooms(currentUser.getUserId());
    return ResponseEntity.ok(rooms);
  }

  @GetMapping("/rooms/{roomId}/membership")
  @Operation(
      summary = "Проверить участие текущего пользователя в комнате",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "true если состоит, иначе false.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = Boolean.class))),
      @ApiResponse(responseCode = "400", description = "Некорректный roomId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Boolean> isUserInRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    Boolean result = roomMembershipService.isUserInRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.ok(result);
  }

  private void completeAccountDeletion(CurrentUserPrincipal currentUser,
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    authApplicationService.logout(request, response, authentication);
    sessionInvalidationService.invalidateAllSessions(currentUser.getUsername());
  }
}
