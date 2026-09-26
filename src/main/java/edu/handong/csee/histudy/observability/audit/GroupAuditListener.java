package edu.handong.csee.histudy.observability.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class GroupAuditListener {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onGroupMemberChanged(GroupMemberChangedEvent event) {
    AuditContext context = event.context();
    log.info("group_member_changed request_id={} actor_id={} role={} academic_term_id={}"
            + " target_user_id={} previous_group_id={} new_group_id={} result=success",
        context.requestId(), context.actorId() == null ? "unknown" : context.actorId(),
        context.role().name(), event.academicTermId(), event.targetUserId(),
        event.previousGroupId() == null ? "none" : event.previousGroupId(),
        event.newGroupId() == null ? "none" : event.newGroupId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
  public void onEmptyGroupsCleaned(EmptyGroupsCleanedEvent event) {
    AuditContext context = event.context();
    log.info("empty_groups_cleaned request_id={} actor_id={} role={} academic_term_id={}"
            + " deleted_group_count={} preserved_group_count={} result=success",
        context.requestId(), context.actorId() == null ? "unknown" : context.actorId(),
        context.role().name(), event.academicTermId(), event.deletedGroupCount(),
        event.preservedGroupCount());
  }
}
