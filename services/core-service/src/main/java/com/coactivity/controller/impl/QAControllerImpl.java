package com.coactivity.controller.impl;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.AnswerUpdateRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.ApiProblemDetail;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.security.CurrentUserPrincipal;
import com.coactivity.service.QaService;
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
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@Validated
@RequestMapping("/api/qa")
@Tag(name = "Q&A", description = "Вопросы и ответы: публичное чтение и CRUD для авторизованных пользователей.")
public class QAControllerImpl {

  private final QaService qaService;

  public QAControllerImpl(QaService qaService) {
    this.qaService = qaService;
  }

  @PostMapping("/questions")
  @Operation(
      summary = "Задать вопрос",
      description = "Создаёт вопрос от имени текущего пользователя. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = QuestionRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Вопрос создан.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = QuestionResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Категория не найдена (если используется).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<QuestionResponse> askQuestion(
    @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
    @Valid @RequestBody QuestionRequest request) {
    QuestionResponse response = qaService.askQuestion(currentUser.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/answers")
  @Operation(
      summary = "Ответить на вопрос",
      description = "Создаёт ответ от имени текущего пользователя. Требуются session cookie и CSRF.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = AnswerRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Ответ создан.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = AnswerResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Вопрос не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<AnswerResponse> answerQuestion(
    @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
    @Valid @RequestBody AnswerRequest request) {
    AnswerResponse response = qaService.answerQuestion(currentUser.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/questions/category")
  @Operation(
      summary = "Вопросы по категории (deprecated)",
      description = "Публичный endpoint: возвращает вопросы (опционально фильтруя по categoryId). Предпочтительнее использовать GET /api/qa/questions."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список вопросов.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = QuestionResponse.class)))),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<QuestionResponse>> getQuestions(
    @RequestParam(name = "categoryId", required = false) Integer categoryId) {
    List<QuestionResponse> responses = qaService.getQuestions(categoryId);
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/questions/{questionId}")
  @Operation(summary = "Вопрос с ответами", description = "Публичный endpoint.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Вопрос и ответы.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = QuestionWithAnswersResponse.class))),
      @ApiResponse(responseCode = "400", description = "Некорректный questionId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Вопрос не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<QuestionWithAnswersResponse> getQuestionWithAnswers(
      @Positive @PathVariable Integer questionId) {
    QuestionWithAnswersResponse response = qaService.getQuestionWithAnswers(questionId);
    return ResponseEntity.ok(response);
  }

  /**
   * Получает все вопросы без фильтрации по категории.
   *
   * @return список всех вопросов в системе
   */
  @GetMapping("/questions")
  @Operation(summary = "Список вопросов", description = "Публичный endpoint. Поддерживает фильтрацию по categoryId и поиск по query.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список вопросов.",
          content = @Content(mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = QuestionResponse.class)))),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<List<QuestionResponse>> getAllQuestions(
      @RequestParam(name = "categoryId", required = false) Integer categoryId,
      @RequestParam(name = "query", required = false) String query) {
    List<QuestionResponse> responses =
        query == null || query.trim().isEmpty()
            ? qaService.getQuestions(categoryId)
            : qaService.getQuestions(categoryId, query);
    return ResponseEntity.ok(responses);
  }

  @PutMapping("/questions/{questionId}")
  @Operation(
      summary = "Обновить вопрос",
      description = "Обновляет вопрос. Требуются session cookie и CSRF. Доступ: владелец вопроса.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = QuestionRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Обновлённый вопрос.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = QuestionResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не владелец).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Вопрос не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<QuestionResponse> updateQuestion(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer questionId,
      @Valid @RequestBody QuestionRequest request) {
    QuestionResponse response = qaService.updateQuestion(currentUser.getUserId(), questionId,
        request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/questions/{questionId}")
  @Operation(
      summary = "Удалить вопрос",
      description = "Удаляет вопрос. Требуются session cookie и CSRF. Доступ: владелец вопроса.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Вопрос удалён."),
      @ApiResponse(responseCode = "400", description = "Некорректный questionId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не владелец).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Вопрос не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteQuestion(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer questionId) {
    qaService.deleteQuestion(currentUser.getUserId(), questionId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/answers/{answerId}")
  @Operation(
      summary = "Обновить ответ",
      description = "Обновляет ответ. Требуются session cookie и CSRF. Доступ: владелец ответа.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = AnswerUpdateRequest.class))
      )
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Обновлённый ответ.",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = AnswerResponse.class))),
      @ApiResponse(responseCode = "400", description = "Ошибка валидации.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не владелец).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Ответ не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<AnswerResponse> updateAnswer(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer answerId,
      @Valid @RequestBody AnswerUpdateRequest request) {
    AnswerResponse response = qaService.updateAnswer(currentUser.getUserId(), answerId, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/answers/{answerId}")
  @Operation(
      summary = "Удалить ответ",
      description = "Удаляет ответ. Требуются session cookie и CSRF. Доступ: владелец ответа.",
      security = @SecurityRequirement(name = "sessionCookie"),
      parameters = {
          @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
              description = "CSRF токен (дублирует cookie XSRF-TOKEN).")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Ответ удалён."),
      @ApiResponse(responseCode = "400", description = "Некорректный answerId.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Требуется аутентификация.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав (не владелец).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Ответ не найден.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Неожиданная ошибка.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteAnswer(
      @Parameter(hidden = true) @AuthenticationPrincipal CurrentUserPrincipal currentUser,
      @Positive @PathVariable Integer answerId) {
    qaService.deleteAnswer(currentUser.getUserId(), answerId);
    return ResponseEntity.noContent().build();
  }
}
