package com.coactivity.qa.dto.response;

import java.util.List;

public record QuestionWithAnswersResponse(
    QuestionResponse question,
    List<AnswerResponse> answers) {
}
