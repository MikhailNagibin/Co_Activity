package com.coactivity.service.dto;

/**
 * Simple immutable carrier for login (email) and password values extracted from an authentication
 * token. Lives in the service layer so controllers and services can exchange decoded credentials
 * without depending on {@code AuthenticationService}'s internal implementation.
 *
 * @param login    user's email used as the login identifier
 * @param password raw password captured from the original authentication request
 */
public record TokenPayload(String login, String password) {
}

