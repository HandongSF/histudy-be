package edu.handong.csee.histudy.observability.audit;

import edu.handong.csee.histudy.domain.Role;

public record AuditContext(String requestId, Long actorId, Role role) {}
