package com.coactivity.service.event;

import com.coactivity.domain.RequestStatus;
import com.coactivity.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class JoinRequestDecisionNotificationListener {

  private final NotificationService notificationService;

  public JoinRequestDecisionNotificationListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onJoinRequestDecision(JoinRequestDecisionEvent event) {
    if (event == null || event.requesterId() == null || event.roomName() == null
        || event.action() == null) {
      return;
    }

    if (event.action() == RequestStatus.ACCEPTED) {
      notificationService.sendMembershipAccepted(event.requesterId(), event.roomName());
      return;
    }

    if (event.action() == RequestStatus.REFUSED
        || event.action() == RequestStatus.REFUSED_WITH_BAN) {
      notificationService.sendMembershipRejected(event.requesterId(), event.roomName());
    }
  }
}
