package edu.handong.csee.histudy.observability.audit;

import java.util.Objects;

public record ReportChangeRejectedEvent(
    AuditContext context, ReportChangedEvent.Operation operation, Long reportId) {
  public ReportChangeRejectedEvent {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
  }

}
