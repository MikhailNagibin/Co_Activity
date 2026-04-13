package com.coactivity.auth.controller;

import com.coactivity.auth.service.AuthApplicationService;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.PasswordChangeRequest;
import com.coactivity.controller.dto.request.PasswordResetConfirmRequest;
import com.coactivity.controller.dto.request.PasswordResetRequest;
import com.coactivity.controller.dto.request.PasswordResetVerifyRequest;
import com.coactivity.controller.dto.request.RegisterVerificationRequest;
import com.coactivity.controller.dto.request.ResendRegistrationVerificationRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.CsrfTokenResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.ApiProblemDetail;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.security.CurrentUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Аутентификация, регистрация, сессии и password reset.")
public class AuthController {

  private final AuthApplicationService authApplicationService;

  public AuthController(AuthApplicationService authApplicationService) {
    this.authApplicationService = authApplicationService;
  }

  @PostMapping("/register")
  @Operation(
      summary = "Регистрация пользователя",
      description = "Создаёт пользователя и инициирует подтверждение регистрации. Для POST требуется CSRF: сначала вызови GET /api/auth/csrf и передай заголовок X-XSRF-TOKEN.",
      parameters = {
          @Parameter(
              name = "X-XSRF-TOKEN",
              in = ParameterIn.HEADER,
              required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN)."
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = UserRegistrationRequest.class)
          )
      )
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "Пользователь создан.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RegistrationResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Ошибка валидации входных данных.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "Конфликт (например, email уже зарегистрирован).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))
      )
  })
  public ResponseEntity<RegistrationResponse> register(
      @Valid @RequestBody UserRegistrationRequest request) {
    RegistrationResponse response = authApplicationService.register(request);
    return ResponseEntity.created(URI.create("/api/users/" + response.getUserId())).body(response);
  }

  @PostMapping("/register/verify")
  @Operation(
      summary = "Подтвердить регистрацию",
      description = "Подтверждает регистрацию по коду. Для POST требуется CSRF: сначала вызови GET /api/auth/csrf и передай заголовок X-XSRF-TOKEN.",
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RegisterVerificationRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Регистрация подтверждена."),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Код/пользователь не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Конфликт (например, уже подтверждено).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> verifyRegistration(
      @Valid @RequestBody RegisterVerificationRequest request) {
    authApplicationService.verifyRegistration(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/register/resend")
  @Operation(
      summary = "Переотправить код подтверждения регистрации",
      description = "Переотправляет код подтверждения. Для POST требуется CSRF: сначала вызови GET /api/auth/csrf и передай заголовок X-XSRF-TOKEN.",
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = ResendRegistrationVerificationRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Код переотправлен."),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Пользователь/регистрация не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> resendRegistrationCode(
      @Valid @RequestBody ResendRegistrationVerificationRequest request) {
    authApplicationService.resendRegistrationCode(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/login")
  @Operation(
      summary = "Логин",
      description = "Создаёт HTTP-сессию и устанавливает session cookie. Для POST требуется CSRF: сначала вызови GET /api/auth/csrf и передай заголовок X-XSRF-TOKEN.",
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = LoginRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Успешный логин.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Неверные учётные данные.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<UserProfileResponse> login(@Valid @RequestBody LoginRequest request,
      @Parameter(hidden = true) HttpServletRequest httpRequest,
      @Parameter(hidden = true) HttpServletResponse httpResponse) {
    return ResponseEntity.ok(authApplicationService.login(request, httpRequest, httpResponse));
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Выход (logout)",
      description = "Инвалидирует текущую сессию и очищает session cookie.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Сессия завершена."),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> logout(
      @Parameter(hidden = true) HttpServletRequest request,
      @Parameter(hidden = true) HttpServletResponse response,
      @Parameter(hidden = true) Authentication authentication) {
    authApplicationService.logout(request, response, authentication);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  @Operation(
      summary = "Текущий пользователь",
      description = "Возвращает профиль текущего пользователя по активной сессии.",
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
  public ResponseEntity<UserProfileResponse> me(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    return ResponseEntity.ok(authApplicationService.me(currentUser));
  }

  @GetMapping("/csrf")
  @Operation(
      summary = "CSRF токен",
      description = "Возвращает CSRF токен и устанавливает cookie XSRF-TOKEN. Для state-changing запросов отправляй значение токена в заголовке X-XSRF-TOKEN."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "CSRF токен.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = CsrfTokenResponse.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<CsrfTokenResponse> csrf(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken == null) {
      csrfToken = (CsrfToken) request.getAttribute("_csrf");
    }
    return ResponseEntity.ok(
        new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getParameterName(),
            csrfToken.getToken()));
  }

  @PostMapping("/password/change")
  @Operation(
      summary = "Смена пароля",
      description = "Меняет пароль текущего пользователя.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = PasswordChangeRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Пароль изменён."),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Доступ запрещён/неверный текущий пароль.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> changePassword(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody PasswordChangeRequest request) {
    authApplicationService.changePassword(currentUser, request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password/reset/request")
  @Operation(
      summary = "Password reset: запросить код",
      description = "Инициирует сброс пароля и отправляет код/ссылку пользователю. Для POST требуется CSRF: сначала вызови GET /api/auth/csrf и передай заголовок X-XSRF-TOKEN.",
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = PasswordResetRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Запрос принят."),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Пользователь не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    authApplicationService.requestPasswordReset(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password/reset/verify")
  @Operation(
      summary = "Password reset: проверить код",
      description = "Проверяет корректность кода сброса пароля. Для POST требуется CSRF: сначала вызови GET /api/auth/csrf и передай заголовок X-XSRF-TOKEN.",
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = PasswordResetVerifyRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Код корректен."),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Код/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Конфликт/истёкший код.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> verifyPasswordResetCode(
      @Valid @RequestBody PasswordResetVerifyRequest request) {
    authApplicationService.verifyPasswordReset(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password/reset/confirm")
  @Operation(
      summary = "Password reset: подтвердить новый пароль",
      description = "Устанавливает новый пароль по подтверждённому коду. Для POST требуется CSRF: сначала вызови GET /api/auth/csrf и передай заголовок X-XSRF-TOKEN.",
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = PasswordResetConfirmRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Пароль обновлён."),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Код/пользователь не найдены.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Конфликт/истёкший код.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> confirmPasswordReset(
      @Valid @RequestBody PasswordResetConfirmRequest request) {
    authApplicationService.confirmPasswordReset(request);
    return ResponseEntity.noContent().build();
  }
}
