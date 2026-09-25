package edu.handong.csee.histudy.observability.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class MatchingAuditListener {

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onMatchingExecuted(MatchingExecutedEvent event) {
    AuditContext context = event.context();
    String reason = event.reasonCode() == null ? "" : " reason_code=" + event.reasonCode();
    log.info(
        "matching_executed request_id={} actor_id={} role={} academic_term_id={} result={}"
            + " applicant_count={} assigned_count={} remaining_count={} created_group_count={}"
            + " duration_ms={}{}",
        context.requestId(),
        context.actorId() == null ? "unknown" : context.actorId(),
        context.role().name(),
        event.academicTermId(),
        event.result(),
        event.applicantCount(),
        event.assignedCount(),
        event.remainingCount(),
        event.createdGroupCount(),
        event.durationMs(),
        reason);
  }
}
