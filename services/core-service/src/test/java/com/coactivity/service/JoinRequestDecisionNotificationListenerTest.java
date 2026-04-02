package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.coactivity.domain.RequestStatus;
import com.coactivity.service.event.JoinRequestDecisionEvent;
import com.coactivity.service.event.JoinRequestDecisionNotificationListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(JoinRequestDecisionNotificationListenerTest.Config.class)
@DisplayName("Join request decision notification listener tests")
class JoinRequestDecisionNotificationListenerTest {

  @Configuration
  @EnableTransactionManagement
  static class Config {

    @Bean
    NotificationService notificationService() {
      return Mockito.mock(NotificationService.class);
    }

    @Bean
    JoinRequestDecisionNotificationListener joinRequestDecisionNotificationListener(
        NotificationService notificationService) {
      return new JoinRequestDecisionNotificationListener(notificationService);
    }

    @Bean
    PlatformTransactionManager transactionManager() {
      return new TestPlatformTransactionManager();
    }
  }

  private static final class TestPlatformTransactionManager
      extends AbstractPlatformTransactionManager {

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
      // No resource binding is needed. The parent class handles synchronization callbacks.
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
      // No-op: we only need transaction synchronization for the test.
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
      // No-op: rollback semantics are enough for listener verification.
    }
  }

  @org.springframework.beans.factory.annotation.Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  @org.springframework.beans.factory.annotation.Autowired
  private NotificationService notificationService;

  @org.springframework.beans.factory.annotation.Autowired
  private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    reset(notificationService);
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Test
  void publishesAcceptedNotificationOnlyAfterCommit() {
    transactionTemplate.executeWithoutResult(status ->
        applicationEventPublisher.publishEvent(
            new JoinRequestDecisionEvent(1, "Chess Club", RequestStatus.ACCEPTED)));

    verify(notificationService).sendMembershipAccepted(1, "Chess Club");
  }

  @Test
  void skipsNotificationWhenTransactionRollsBack() {
    assertThrows(IllegalStateException.class, () ->
        transactionTemplate.executeWithoutResult(status -> {
          applicationEventPublisher.publishEvent(
              new JoinRequestDecisionEvent(2, "Chess Club", RequestStatus.REFUSED));
          throw new IllegalStateException("rollback");
        }));

    verifyNoInteractions(notificationService);
  }

  @Test
  void fallbackExecutionPublishesImmediatelyWithoutTransaction() {
    applicationEventPublisher.publishEvent(
        new JoinRequestDecisionEvent(3, "Chess Club", RequestStatus.REFUSED_WITH_BAN));

    verify(notificationService).sendMembershipRejected(3, "Chess Club");
  }
}
