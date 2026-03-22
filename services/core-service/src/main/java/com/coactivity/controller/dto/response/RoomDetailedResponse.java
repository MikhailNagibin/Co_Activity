package com.coactivity.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailedResponse extends RoomSummaryResponse {

  private Boolean hasProtectedAccess;

  private String chatLink;

  private BulletinBoardResponse bulletinBoard;
}