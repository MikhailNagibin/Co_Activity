package com.coactivity.service.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ошибка валидации/параметра запроса (одна строка).")
public record ApiFieldError(String field, String message, String code) {
}
