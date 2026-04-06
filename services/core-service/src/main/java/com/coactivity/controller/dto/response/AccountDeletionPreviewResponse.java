package com.coactivity.controller.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionPreviewResponse {

  private boolean canDeleteImmediately;

  private List<OwnedRoomDeletionPreviewResponse> ownedRooms;
}
