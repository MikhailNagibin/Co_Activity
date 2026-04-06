package com.coactivity.auth.controller;

import com.coactivity.auth.service.AuthApplicationService;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.PasswordChangeRequest;
import com.coactivity.controller.dto.request.RegisterVerificationRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.CsrfTokenResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.security.CurrentUserPrincipal;
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
public class AuthController {

  private final AuthApplicationService authApplicationService;

  public AuthController(AuthApplicationService authApplicationService) {
    this.authApplicationService = authApplicationService;
  }

  @PostMapping("/register")
  public ResponseEntity<RegistrationResponse> register(
      @Valid @RequestBody UserRegistrationRequest request) {
    RegistrationResponse response = authApplicationService.register(request);
    return ResponseEntity.created(URI.create("/api/users/" + response.getUserId())).body(response);
  }

  @PostMapping("/register/verify")
  public ResponseEntity<Void> verifyRegistration(
      @Valid @RequestBody RegisterVerificationRequest request) {
    authApplicationService.verifyRegistration(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/login")
  public ResponseEntity<UserProfileResponse> login(@Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    return ResponseEntity.ok(authApplicationService.login(request, httpRequest, httpResponse));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {
    authApplicationService.logout(request, response, authentication);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> me(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser) {
    return ResponseEntity.ok(authApplicationService.me(currentUser));
  }

  @GetMapping("/csrf")
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
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Valid @RequestBody PasswordChangeRequest request) {
    authApplicationService.changePassword(currentUser, request);
    return ResponseEntity.noContent().build();
  }
}
