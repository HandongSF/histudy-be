package edu.handong.csee.histudy.observability.audit;

import edu.handong.csee.histudy.domain.Role;
import java.util.Objects;

public record AuditContext(String requestId, Long actorId, Role role) {
  public AuditContext {
    Objects.requireNonNull(role, "role must not be null");
    requestId = requestId == null || requestId.isBlank() ? "unknown" : requestId;
  }
}
