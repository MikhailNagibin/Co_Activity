package com.coactivity.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWithAnswersResponse {

  private QuestionResponse question;

  private List<AnswerResponse> answers;
}