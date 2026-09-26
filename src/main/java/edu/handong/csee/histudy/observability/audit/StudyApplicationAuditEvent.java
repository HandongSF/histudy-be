package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record StudyApplicationAuditEvent(
    AuditContext context, Action action, Long academicTermId, Long targetUserId,
    Long applicationId, Long previousApplicationId, int friendRequestCount,
    int preferredCourseCount) {

  public enum Action { SUBMITTED, DELETED }

  public StudyApplicationAuditEvent {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(action, "action must not be null");
  }
}
