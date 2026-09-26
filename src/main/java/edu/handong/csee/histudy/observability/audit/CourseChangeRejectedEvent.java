package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record CourseChangeRejectedEvent(AuditContext context, Long academicTermId, Long courseId) {
  public CourseChangeRejectedEvent {
    Objects.requireNonNull(context, "context must not be null");
  }
}
