package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record ReportChangedEvent(
    AuditContext context, Operation operation, Long academicTermId, Long groupId,
    Long reportId, int participantCount, int courseCount, int imageCount) {
  public ReportChangedEvent {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
  }

  public enum Operation {
    CREATED("report_created"), UPDATED("report_updated"), DELETED("report_deleted");
    private final String eventName;
    Operation(String eventName) { this.eventName = eventName; }
    public String eventName() { return eventName; }
  }
}
