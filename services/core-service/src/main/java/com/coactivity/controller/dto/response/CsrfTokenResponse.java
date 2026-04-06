package com.coactivity.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsrfTokenResponse {

  private String headerName;
  private String parameterName;
  private String token;
}
