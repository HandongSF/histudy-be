package edu.handong.csee.histudy.observability.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class StudyApplicationAuditListener {

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onApplicationChanged(StudyApplicationAuditEvent event) {
    AuditContext context = event.context();
    boolean submitted = event.action() == StudyApplicationAuditEvent.Action.SUBMITTED;
    boolean noOp = !submitted && event.applicationId() == null;
    String details = submitted
        ? " submission_type=" + (event.previousApplicationId() == null ? "new" : "resubmission")
            + " previous_application_id=" + id(event.previousApplicationId())
        : noOp ? " reason_code=NO_APPLICATION" : "";
    log.info(
        "{} request_id={} actor_id={} role={} academic_term_id={} target_user_id={}"
            + " application_id={} result={} friend_request_count={} preferred_course_count={}{}",
        submitted ? "study_application_submitted" : "study_application_deleted",
        context.requestId(), id(context.actorId()), context.role().name(),
        id(event.academicTermId()), id(event.targetUserId()), id(event.applicationId()),
        noOp ? "no_op" : "success", event.friendRequestCount(), event.preferredCourseCount(), details);
  }

  private String id(Long value) {
    return value == null ? "unknown" : value.toString();
  }
}
