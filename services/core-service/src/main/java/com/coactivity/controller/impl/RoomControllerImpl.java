package com.coactivity.controller.impl;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.OwnershipTransferRequest;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.request.RoomUpdateRequest;
import com.coactivity.controller.dto.response.ApiProblemDetail;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.OwnershipTransferResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomImageResponse;
import com.coactivity.controller.dto.response.RoomMembershipStatusResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Role;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.RoomImageContent;
import com.coactivity.service.RoomImageService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.RoomService;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rooms")
@Validated
@Tag(
    name = "Rooms",
    description = "Комнаты: поиск/просмотр, участие, управление комнатой и governance (участники/баны/ownership), изображения комнаты."
)
public class RoomControllerImpl {

  private final RoomService roomService;
  private final RoomMembershipService roomMembershipService;
  private final BulletinBoardService bulletinBoardService;
  private final RoomImageService roomImageService;

  public RoomControllerImpl(RoomService roomService,
      RoomMembershipService roomMembershipService,
      BulletinBoardService bulletinBoardService,
      RoomImageService roomImageService) {
    this.roomService = roomService;
    this.roomMembershipService = roomMembershipService;
    this.bulletinBoardService = bulletinBoardService;
    this.roomImageService = roomImageService;
  }

  @PostMapping("/createRoom")
  @Operation(
      summary = "Создать комнату",
      description = "Создаёт новую комнату. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoomCreationRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Комната создана.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoomCreationResponse.class))),
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
  public ResponseEntity<RoomCreationResponse> createRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody RoomCreationRequest request) {
    RoomCreationResponse response = roomService.createRoom(currentUser.getUserId(), request);
    URI location = Objects.requireNonNull(response.getRoomId() != null
        ? URI.create("/api/rooms/" + response.getRoomId())
        : URI.create("/api/rooms"));
    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{roomId}")
  @Operation(
      summary = "Обновить комнату",
      description = "Governance endpoint: обновляет параметры комнаты. Доступ: room OWNER. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoomUpdateRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Обновлённая комната.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoomDetailedResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<RoomDetailedResponse> updateRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Valid @RequestBody RoomUpdateRequest request) {
    RoomDetailedResponse response =
        roomService.updateRoom(currentUser.getUserId(), roomId, request);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{roomId}/bulletin")
  @Operation(
      summary = "Обновить bulletin board",
      description = "Governance endpoint: обновляет содержимое bulletin board. Доступ: room OWNER/ADMIN. Требуются session cookie и CSRF. Рекомендуемый Content-Type: text/plain.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Bulletin board обновлён.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = BulletinBoardResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации/пустой контент.",
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
  public ResponseEntity<BulletinBoardResponse> updateBulletinBoard(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @NotBlank @RequestBody String newContent) {
    BulletinBoardResponse response =
        bulletinBoardService.updateBulletinBoard(roomId, newContent, currentUser.getUserId());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/bulletin")
  @Operation(
      summary = "Удалить bulletin board",
      description = "Governance endpoint: удаляет bulletin board. Доступ: room OWNER/ADMIN. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Bulletin board удалён."),
      @ApiResponse(responseCode = "400", description = "Некорректный roomId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER/ADMIN).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/board не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteBulletinBoard(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    bulletinBoardService.deleteBulletinBoard(roomId, currentUser.getUserId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @Operation(
      summary = "Список комнат",
      description = "Публичный endpoint. Можно вызывать анонимно. Если передана сессия, ответ может содержать user-specific поля (например, membership-related)."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список комнат.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = RoomSummaryResponse.class)))),
      @ApiResponse(responseCode = "400", description = "Некорректные query параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<RoomSummaryResponse>> getRooms(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @ParameterObject @Valid @ModelAttribute RoomFilter filter,
      @Parameter(
          name = "sortBy",
          in = ParameterIn.QUERY,
          required = false,
          description = "Сортировка результатов.",
          schema = @Schema(implementation = RoomSort.class)
      )
      @RequestParam(name = "sortBy", required = false) RoomSort sortBy) {
    Integer currentUserId = currentUser != null ? currentUser.getUserId() : null;
    List<RoomSummaryResponse> rooms = roomService.getRooms(currentUserId, filter, sortBy);
    return ResponseEntity.ok(rooms);
  }

  @GetMapping("/{roomId}")
  @Operation(
      summary = "Комната по ID",
      description = "Публичный endpoint. Можно вызывать анонимно. Если передана сессия, ответ может содержать дополнительные поля/флаги."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Комната.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoomDetailedResponse.class))),
      @ApiResponse(responseCode = "400", description = "Некорректный roomId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<RoomDetailedResponse> getRoomById(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    Integer currentUserId = currentUser != null ? currentUser.getUserId() : null;
    RoomDetailedResponse response = roomService.getRoomById(roomId, currentUserId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{roomId}/images/{imageId}")
  @Operation(
      summary = "Изображение комнаты (binary)",
      description = "Публичный endpoint. Возвращает изображение комнаты как файл. Content-Type определяется по фактическому файлу."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Изображение.",
          content = {
              @Content(mediaType = "image/png",
                  schema = @Schema(type = "string", format = "binary")),
              @Content(mediaType = "image/jpeg",
                  schema = @Schema(type = "string", format = "binary")),
              @Content(mediaType = "image/webp",
                  schema = @Schema(type = "string", format = "binary"))
          }
      ),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/изображение не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "503", description = "Хранилище недоступно.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<ByteArrayResource> getRoomImage(
      @Parameter(description = "ID комнаты.", example = "42")
      @Positive @PathVariable Integer roomId,
      @Parameter(description = "ID изображения.", example = "10")
      @Positive @PathVariable Integer imageId) {
    RoomImageContent imageContent = roomImageService.getRoomImageContent(roomId, imageId);
    ByteArrayResource resource = new ByteArrayResource(imageContent.bytes());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(imageContent.contentType()))
        .contentLength(imageContent.sizeBytes())
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(resource);
  }

  @GetMapping("/me")
  @Operation(
      summary = "Комнаты текущего пользователя",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список комнат пользователя.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = RoomDetailedResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<RoomDetailedResponse>> getUserRooms(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    List<RoomDetailedResponse> rooms = roomMembershipService.getUserRooms(currentUser.getUserId());
    return ResponseEntity.ok(rooms);
  }

  @PostMapping("/{roomId}/join")
  @Operation(
      summary = "Вступить в комнату",
      description = "Добавляет текущего пользователя в комнату (если это разрешено правилами комнаты). Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Пользователь вступил в комнату."),
      @ApiResponse(responseCode = "400", description = "Некорректный roomId/состояние.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Вступление запрещено (например, пользователь забанен).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> joinRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    roomMembershipService.joinRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{roomId}/leave")
  @Operation(
      summary = "Покинуть комнату",
      description = "Удаляет текущего пользователя из комнаты. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Пользователь покинул комнату."),
      @ApiResponse(responseCode = "400", description = "Некорректный roomId/состояние.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Операция запрещена (например, OWNER не может выйти).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> leaveRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    roomMembershipService.leaveRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{roomId}")
  @Operation(
      summary = "Удалить комнату",
      description = "Governance endpoint: удаляет комнату. Доступ: room OWNER. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Комната удалена."),
      @ApiResponse(responseCode = "400", description = "Некорректный roomId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    roomService.deleteRoom(currentUser.getUserId(), roomId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Загрузить изображения комнаты",
      description = "Governance endpoint: загружает до нескольких изображений комнаты (multipart). Доступ: room OWNER. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Текущий список изображений комнаты.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = RoomImageResponse.class)))),
      @ApiResponse(responseCode = "400", description = "Ошибка multipart/размер/тип/количество файлов.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната не найдена.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "503", description = "Хранилище недоступно.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<RoomImageResponse>> uploadRoomImages(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Parameter(
          name = "files",
          description = "Массив файлов-изображений (jpeg/png/webp).",
          required = true,
          content = @Content(
              array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
          )
      )
      @RequestParam("files") MultipartFile[] files) {
    List<RoomImageResponse> response =
        roomImageService.uploadRoomImages(currentUser.getUserId(), roomId, files);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/images/{imageId}")
  @Operation(
      summary = "Удалить изображение комнаты",
      description = "Governance endpoint: удаляет конкретное изображение комнаты. Доступ: room OWNER. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "roomId", in = ParameterIn.PATH, required = true,
              description = "ID комнаты.", example = "42"),
          @Parameter(name = "imageId", in = ParameterIn.PATH, required = true,
              description = "ID изображения.", example = "10"),
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Текущий список изображений комнаты.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = RoomImageResponse.class)))),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/изображение не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<RoomImageResponse>> deleteRoomImage(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer imageId) {
    List<RoomImageResponse> response =
        roomImageService.deleteRoomImage(currentUser.getUserId(), roomId, imageId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{roomId}/participants")
  @Operation(
      summary = "Участники комнаты",
      description = "Governance endpoint: возвращает список участников комнаты. Доступ: room OWNER/ADMIN.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "role", in = ParameterIn.QUERY, required = false,
              description = "Фильтр по роли.", schema = @Schema(implementation = Role.class))
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список участников.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = RoomParticipantResponse.class)))),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
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
  public ResponseEntity<List<RoomParticipantResponse>> getRoomParticipants(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @RequestParam(name = "role", required = false) Role roleFilter) {
    List<RoomParticipantResponse> participants =
        roomMembershipService.getRoomParticipants(currentUser.getUserId(), roomId, roleFilter);

    return ResponseEntity.ok(participants);
  }

  @GetMapping("/{roomId}/participants/{userId}")
  @Operation(
      summary = "Проверка участия конкретного пользователя в комнате",
      description = "Governance endpoint: проверяет membership пользователя userId в комнате. Доступ: room OWNER/ADMIN.",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Результат проверки.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = MembershipVerificationResponse.class))),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER/ADMIN).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<MembershipVerificationResponse> isUserInRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    MembershipVerificationResponse response =
        roomMembershipService.verifyUserMembership(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{roomId}/membership/status")
  @Operation(
      summary = "Membership status текущего пользователя",
      description = "Возвращает статус участия текущего пользователя в комнате.",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Статус участия.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RoomMembershipStatusResponse.class))),
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
  public ResponseEntity<RoomMembershipStatusResponse> getCurrentUserMembershipStatus(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    RoomMembershipStatusResponse response =
        roomMembershipService.getCurrentUserMembershipStatus(currentUser.getUserId(), roomId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/participants/{userId}")
  @Operation(
      summary = "Удалить участника из комнаты",
      description = "Governance endpoint: удаляет участника userId из комнаты. Доступ: room OWNER/ADMIN. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Участник удалён."),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав/нельзя удалить указанную роль.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> removeParticipant(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    roomMembershipService.removeParticipant(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{roomId}/bans/{userId}")
  @Operation(
      summary = "Забанить пользователя в комнате",
      description = "Governance endpoint: банит пользователя userId в комнате. Доступ: room OWNER/ADMIN. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Пользователь забанен."),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав/нельзя забанить указанную роль.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> banUser(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    roomMembershipService.banUser(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{roomId}/bans")
  @Operation(
      summary = "Список забаненных пользователей",
      description = "Governance endpoint: возвращает забаненных пользователей в комнате. Доступ: room OWNER/ADMIN.",
      security = @SecurityRequirement(name = "sessionCookie")
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список пользователей.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = UserSummaryResponse.class)))),
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
  public ResponseEntity<List<UserSummaryResponse>> getBannedUsers(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId) {
    List<UserSummaryResponse> response =
        roomMembershipService.getBannedUsers(currentUser.getUserId(), roomId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{roomId}/bans/{userId}")
  @Operation(
      summary = "Разбанить пользователя в комнате",
      description = "Governance endpoint: убирает бан пользователя userId. Доступ: room OWNER/ADMIN. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Пользователь разбанен."),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не OWNER/ADMIN).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Комната/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> unbanUser(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Positive @PathVariable Integer userId) {
    roomMembershipService.unbanUser(currentUser.getUserId(), roomId, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{roomId}/ownership/transfer")
  @Operation(
      summary = "Передать ownership комнаты",
      description = "Governance endpoint: передаёт роль OWNER пользователю targetUserId. Доступ: текущий OWNER. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = OwnershipTransferRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Ownership передан.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = OwnershipTransferResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
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
      @ApiResponse(responseCode = "409", description = "Конфликт состояния.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<OwnershipTransferResponse> transferOwnership(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer roomId,
      @Valid @RequestBody OwnershipTransferRequest request) {
    OwnershipTransferResponse response =
        roomMembershipService.transferOwnership(currentUser.getUserId(), roomId,
            request.getTargetUserId());
    return ResponseEntity.ok(response);
  }
}
