package com.coactivity.auth.service;

import java.util.Map;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class SessionInvalidationService {

  private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

  public SessionInvalidationService(
      FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  public void invalidateAllSessions(String principalName) {
    Map<String, ? extends Session> sessions = sessionRepository.findByIndexNameAndIndexValue(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
        principalName);
    sessions.keySet().forEach(sessionRepository::deleteById);
  }
}
