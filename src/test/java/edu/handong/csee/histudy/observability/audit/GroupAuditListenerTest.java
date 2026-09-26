package edu.handong.csee.histudy.observability.audit;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.repository.impl.*;
import edu.handong.csee.histudy.service.UserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:group-audit;NON_KEYWORDS=USER",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserService.class, GroupAuditListener.class, UserRepositoryImpl.class,
    CourseRepositoryImpl.class, AcademicTermRepositoryImpl.class,
    StudyApplicationRepositoryImpl.class, StudyGroupRepositoryImpl.class,
    StudyReportRepositoryImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GroupAuditListenerTest {
  private final AuditContext context = new AuditContext("request-edit", 42L, Role.ADMIN);

  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private UserService userService;
  @Autowired private ApplicationEventPublisher publisher;

  private TransactionTemplate transaction;
  private ListAppender<ILoggingEvent> appender;
  private Long termId;
  private Long userId;
  private Long groupId;

  @BeforeEach
  void setUp() {
    transaction = new TransactionTemplate(transactionManager);
    transaction.executeWithoutResult(status -> {
      entityManager.createQuery("update AcademicTerm a set a.isCurrent = false").executeUpdate();
      AcademicTerm term = AcademicTerm.builder().academicYear(2026)
          .semester(TermType.SPRING).isCurrent(true).build();
      entityManager.persist(term);
      termId = term.getAcademicTermId();
      User user = User.builder().email(UUID.randomUUID() + "@private.example")
          .name("private-name").sid("private-" + UUID.randomUUID()).role(Role.USER).build();
      entityManager.persist(user);
      userId = user.getUserId();
      StudyApplicant applicant = StudyApplicant.of(term, user, List.of(), List.of());
      entityManager.persist(applicant);
      StudyGroup group = StudyGroup.of(501, term, List.of(applicant));
      entityManager.persist(group);
      groupId = group.getStudyGroupId();
    });
    appender = new ListAppender<>();
    appender.start();
    ((Logger) LoggerFactory.getLogger(GroupAuditListener.class)).addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger(GroupAuditListener.class)).detachAppender(appender);
    appender.stop();
    MDC.clear();
  }

  @Test
  void 감사문맥이_없으면_편집전에_거절한다() {
    // given
    // when then
    assertThatThrownBy(() -> userService.editUser(form(null), null))
        .isInstanceOf(NullPointerException.class);
    StudyGroup retained = transaction.execute(status -> entityManager.find(StudyGroup.class, groupId));
    assertThat(retained).isNotNull();
    assertThat(appender.list).isEmpty();
    assertThatThrownBy(() -> new GroupMemberChangedEvent(null, termId, userId, groupId, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EmptyGroupsCleanedEvent(null, termId, 1, 0))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void 그룹을_해제하고_커밋하면_내부ID와_삭제건수를_기록한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      userService.editUser(form(null), context);
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).hasSize(2).allSatisfy(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.INFO);
      assertThat(event.getThrowableProxy()).isNull();
      assertThat(event.getFormattedMessage()).contains("request_id=request-edit", "actor_id=42",
          "role=ADMIN", "academic_term_id=" + termId, "result=success")
          .doesNotContain("private", "@", "501");
    });
    assertThat(appender.list.get(0).getFormattedMessage()).contains("group_member_changed",
        "target_user_id=" + userId, "previous_group_id=" + groupId, "new_group_id=none");
    assertThat(appender.list.get(1).getFormattedMessage()).contains("empty_groups_cleaned",
        "deleted_group_count=1", "preserved_group_count=0");
  }

  @Test
  void 새그룹으로_이동하면_태그가_아닌_생성된_그룹ID를_기록한다() {
    // given
    // when
    userService.editUser(form(999), context);
    // then
    Long newId = transaction.execute(status -> entityManager.createQuery(
        "select g.studyGroupId from StudyGroup g where g.academicTerm.academicTermId = :id", Long.class)
        .setParameter("id", termId).getSingleResult());
    assertThat(appender.list.get(0).getFormattedMessage()).contains("previous_group_id=" + groupId,
        "new_group_id=" + newId).doesNotContain("999");
  }

  @Test
  void 기존그룹으로_이동하면_대상그룹의_내부ID를_기록한다() {
    // given
    Long destinationId = transaction.execute(status -> {
      StudyGroup group = StudyGroup.of(777, entityManager.find(AcademicTerm.class, termId), List.of());
      entityManager.persist(group);
      return group.getStudyGroupId();
    });
    // when
    userService.editUser(form(777), context);
    // then
    assertThat(appender.list.get(0).getFormattedMessage()).contains(
        "previous_group_id=" + groupId, "new_group_id=" + destinationId);
  }

  @Test
  void 미배정_사용자를_배정하면_이전그룹을_none으로_기록한다() {
    // given
    userService.editUser(form(null), context);
    appender.list.clear();
    // when
    userService.editUser(form(888), context);
    // then
    assertThat(appender.list).singleElement().satisfies(event ->
        assertThat(event.getFormattedMessage()).contains("group_member_changed", "previous_group_id=none")
            .doesNotContain("new_group_id=none"));
  }

  @Test
  void 동일그룹을_선택하면_멤버변경을_기록하지_않는다() {
    // given
    // when
    userService.editUser(form(501), context);
    // then
    assertThat(appender.list).isEmpty();
  }

  @Test
  void 보고서가_있는_그룹을_비우면_보존건수를_기록한다() {
    // given
    transaction.executeWithoutResult(status -> {
      StudyReport report = StudyReport.builder().studyGroup(entityManager.find(StudyGroup.class, groupId))
          .title("private-report").content("private-content").totalMinutes(60)
          .participants(List.of(entityManager.find(User.class, userId)))
          .images(List.of()).courses(List.of()).build();
      entityManager.persist(report);
    });
    // when
    userService.editUser(form(null), context);
    // then
    assertThat(appender.list).hasSize(2);
    assertThat(appender.list.get(1).getFormattedMessage()).contains("deleted_group_count=0",
        "preserved_group_count=1").doesNotContain("private");
    appender.list.clear();
    userService.editUser(form(null), context);
    assertThat(appender.list).isEmpty();
  }

  @Test
  void 외부트랜잭션을_롤백하면_변경과_완료로그를_취소한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      userService.editUser(form(null), context);
      status.setRollbackOnly();
    });
    // then
    assertThat(appender.list).isEmpty();
    StudyGroup retained = transaction.execute(status -> entityManager.find(StudyGroup.class, groupId));
    assertThat(retained).isNotNull();
  }

  @Test
  void 커밋의_flush가_실패하면_완료로그가_없다() {
    // given
    // when then
    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      userService.editUser(form(null), context);
      entityManager.find(User.class, userId).edit(null, "x".repeat(256));
    })).isInstanceOf(RuntimeException.class);
    assertThat(appender.list).isEmpty();
  }

  @Test
  void 요청문맥이_변해도_이벤트의_문맥을_사용한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      userService.editUser(form(null), new AuditContext("request-second", null, Role.ADMIN));
      MDC.put("request_id", "unrelated");
    });
    // then
    assertThat(appender.list).hasSize(2).allSatisfy(event ->
        assertThat(event.getFormattedMessage()).contains("request_id=request-second", "actor_id=unknown")
            .doesNotContain("unrelated", "request-edit"));
  }

  @Test
  void 트랜잭션없이_발행하면_기록하지_않는다() {
    // given
    // when
    publisher.publishEvent(new GroupMemberChangedEvent(context, termId, userId, groupId, null));
    publisher.publishEvent(new EmptyGroupsCleanedEvent(context, termId, 1, 0));
    // then
    assertThat(appender.list).isEmpty();
  }

  private UserDto.UserEdit form(Integer tag) {
    return UserDto.UserEdit.builder().id(userId).team(tag).build();
  }
}
