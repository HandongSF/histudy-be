package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record GroupMemberChangedEvent(AuditContext context, Long academicTermId,
    Long targetUserId, Long previousGroupId, Long newGroupId) {
  public GroupMemberChangedEvent {
    Objects.requireNonNull(context, "audit context must not be null");
  }
}
