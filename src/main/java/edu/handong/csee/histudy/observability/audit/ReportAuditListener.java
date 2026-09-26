package edu.handong.csee.histudy.observability.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ReportAuditListener {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onReportChanged(ReportChangedEvent event) {
    AuditContext context = event.context();
    log.info("{} request_id={} actor_id={} role={} academic_term_id={} group_id={} report_id={}"
            + " result=success participant_count={} course_count={} image_count={}",
        event.operation().eventName(), context.requestId(),
        context.actorId() == null ? "unknown" : context.actorId(), context.role().name(),
        event.academicTermId(), event.groupId(), event.reportId(),
        event.participantCount(), event.courseCount(), event.imageCount());
  }

  @EventListener
  public void onReportChangeRejected(ReportChangeRejectedEvent event) {
    AuditContext context = event.context();
    log.info("{} request_id={} actor_id={} role={} report_id={}"
            + " result=rejected reason_code=RESOURCE_UNAVAILABLE",
        event.operation().eventName(), context.requestId(),
        context.actorId() == null ? "unknown" : context.actorId(), context.role().name(),
        event.reportId());
  }
}
