package edu.handong.csee.histudy.observability.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class AcademicCourseAuditListener {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onCreated(AcademicTermCreatedEvent event) {
    log.info("academic_term_created {} academic_term_id={} result=success", context(event.context()), event.academicTermId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onCurrentChanged(CurrentTermChangedEvent event) {
    log.info("current_term_changed {} previous_term_id={} academic_term_id={} result={}{}",
        context(event.context()), value(event.previousTermId()), event.academicTermId(),
        event.changed() ? "success" : "no_op", event.changed() ? "" : " reason_code=ALREADY_CURRENT");
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onReplaced(CoursesReplacedEvent event) {
    log.info("courses_replaced {} academic_term_id={} previous_count={} replacement_count={} result={}{}",
        context(event.context()), value(event.academicTermId()), value(event.previousCount()), event.replacementCount(),
        event.changed() ? "success" : "no_op", event.changed() ? "" : " reason_code=EMPTY_INPUT");
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onDeleted(CourseDeletedEvent event) {
    log.info("course_deleted {} academic_term_id={} course_id={} legacy={} result={}{}",
        context(event.context()), value(event.academicTermId()), event.courseId(), event.legacy(),
        event.deleted() ? "success" : "no_op", event.deleted() ? "" : " reason_code=COURSE_NOT_FOUND");
  }

  @EventListener
  public void onRejected(CourseChangeRejectedEvent event) {
    log.info("{} {} academic_term_id={}{} result=rejected reason_code=COURSE_IN_USE",
        event.courseId() == null ? "courses_replaced" : "course_deleted", context(event.context()),
        value(event.academicTermId()), event.courseId() == null ? "" : " course_id=" + event.courseId());
  }

  private String context(AuditContext context) {
    return "request_id=" + context.requestId() + " actor_id=" + value(context.actorId())
        + " role=" + context.role().name();
  }

  private Object value(Object value) {
    return value == null ? "unknown" : value;
  }
}
