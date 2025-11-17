package com.coactivity.service.dto;

/**
 * Represents user credentials extracted from an authentication token.
 *
 * @param login the user's email/login identifier
 * @param password the user's password for verification
 */
public record TokenPayload(String login, String password) {}