package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record AcademicTermCreatedEvent(AuditContext context, Long academicTermId) {
  public AcademicTermCreatedEvent {
    Objects.requireNonNull(context, "context must not be null");
  }
}
