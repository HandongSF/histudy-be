package edu.handong.csee.histudy.observability.audit;

public record MatchingExecutedEvent(
    AuditContext context,
    Long academicTermId,
    int applicantCount,
    int assignedCount,
    int createdGroupCount,
    long durationMs) {

  public int remainingCount() {
    return applicantCount - assignedCount;
  }

  public String result() {
    return createdGroupCount > 0 ? "success" : "no_op";
  }

  public String reasonCode() {
    if (createdGroupCount > 0) {
      return null;
    }
    return applicantCount == 0 ? "NO_UNASSIGNED_APPLICANTS" : "NO_ELIGIBLE_GROUPS";
  }
}
