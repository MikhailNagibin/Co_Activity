package com.coactivity.qa.dto.response;

import java.time.Instant;

public record UserSummaryResponse(
    Integer id,
    String userName,
    Instant dateOfBirth,
    String city,
    String country,
    String description,
    Integer avatarId) {
}
