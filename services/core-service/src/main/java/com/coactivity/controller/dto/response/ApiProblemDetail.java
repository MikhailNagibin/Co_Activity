package com.coactivity.controller.dto.response;

import com.coactivity.service.exception.ApiFieldError;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
    name = "ApiProblemDetail",
    description = "RFC 7807 Problem Details (`application/problem+json`) с расширениями CoActivity: code, traceId, timestamp, errors."
)
public record ApiProblemDetail(
    @Schema(
        description = "URI-тип ошибки.",
        example = "urn:coactivity:error:VALIDATION_FAILED"
    )
    String type,
    @Schema(description = "Короткий заголовок ошибки.", example = "Validation failed")
    String title,
    @Schema(description = "HTTP status code.", example = "400")
    Integer status,
    @Schema(description = "Человекочитаемое описание причины.", example = "Validation failed")
    String detail,
    @Schema(description = "URI конкретного запроса.", example = "/api/rooms/42")
    String instance,
    @Schema(description = "Время формирования ответа.", format = "date-time")
    Instant timestamp,
    @Schema(description = "Код ошибки приложения.", example = "VALIDATION_FAILED")
    String code,
    @Schema(description = "ID трассировки/запроса (также приходит в заголовке).", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String traceId,
    @Schema(description = "Детализация ошибок по полям (если применимо).")
    List<ApiFieldError> errors
) {
}

