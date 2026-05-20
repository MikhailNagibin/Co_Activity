package com.coactivity.service.event;

import com.coactivity.domain.RequestStatus;

public record JoinRequestDecisionEvent(
    Integer requesterId,
    String roomName,
    RequestStatus action) {
}
