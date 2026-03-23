package com.coactivity.controller.dto.response;

import com.coactivity.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

  private Integer id;

  private Category category;

  private String question;

  private UserSummaryResponse author;
}
