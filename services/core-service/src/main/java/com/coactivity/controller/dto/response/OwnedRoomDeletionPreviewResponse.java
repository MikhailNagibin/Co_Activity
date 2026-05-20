package com.coactivity.controller.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnedRoomDeletionPreviewResponse {

  private Integer roomId;

  private String roomName;

  private Integer participantCount;

  private List<AccountDeletionTransferCandidateResponse> transferCandidates;
}
