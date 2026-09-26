package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record CourseDeletedEvent(AuditContext context, Long academicTermId, Long courseId, boolean deleted, boolean legacy) {
  public CourseDeletedEvent {
    Objects.requireNonNull(context, "context must not be null");
  }
}
