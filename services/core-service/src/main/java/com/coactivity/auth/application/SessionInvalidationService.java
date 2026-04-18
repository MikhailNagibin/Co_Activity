package com.coactivity.auth.application;

import java.util.Map;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

/**
 * Application-level helper for invalidating all sessions of a principal.
 *
 * <p>This service wraps {@link FindByIndexNameSessionRepository} and provides a single-purpose
 * operation used by authentication use cases (e.g. after a password change) to log a user out from
 * all devices.</p>
 */
@Service
public class SessionInvalidationService {

  private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

  public SessionInvalidationService(
      FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  /**
   * Deletes every active session associated with the given principal name.
   */
  public void invalidateAllSessions(String principalName) {
    Map<String, ? extends Session> sessions = sessionRepository.findByIndexNameAndIndexValue(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
        principalName);
    sessions.keySet().forEach(sessionRepository::deleteById);
  }
}
