package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record EmptyGroupsCleanedEvent(AuditContext context, Long academicTermId,
    int deletedGroupCount, int preservedGroupCount) {
  public EmptyGroupsCleanedEvent {
    Objects.requireNonNull(context, "audit context must not be null");
  }
}
