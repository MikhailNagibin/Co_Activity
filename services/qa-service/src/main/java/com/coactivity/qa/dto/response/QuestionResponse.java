package com.coactivity.qa.dto.response;

import com.coactivity.qa.domain.Category;

public record QuestionResponse(
    Integer id,
    Category category,
    String question,
    UserSummaryResponse author) {
}
