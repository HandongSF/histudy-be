package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record CoursesReplacedEvent(AuditContext context, Long academicTermId, Long previousCount, int replacementCount, boolean changed) {
  public CoursesReplacedEvent {
    Objects.requireNonNull(context, "context must not be null");
  }
}
