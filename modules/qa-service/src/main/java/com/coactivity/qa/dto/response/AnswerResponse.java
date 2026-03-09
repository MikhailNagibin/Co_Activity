package com.coactivity.qa.dto.response;

import java.time.Instant;
import java.util.List;

public record AnswerResponse(
    Integer id,
    Integer questionId,
    Integer previousAnswerId,
    String answer,
    UserSummaryResponse author,
    Instant createdAt,
    List<AnswerResponse> replies) {
}
