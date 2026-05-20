package com.coactivity.service;

public record UserAvatarContent(byte[] bytes, String contentType, long sizeBytes) {
}
