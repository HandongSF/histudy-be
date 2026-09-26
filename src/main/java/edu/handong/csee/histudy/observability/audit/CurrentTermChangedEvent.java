package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record CurrentTermChangedEvent(AuditContext context, Long previousTermId, Long academicTermId, boolean changed) {
  public CurrentTermChangedEvent {
    Objects.requireNonNull(context, "context must not be null");
  }
}
