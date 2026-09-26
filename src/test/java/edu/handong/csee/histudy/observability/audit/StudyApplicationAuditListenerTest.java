package edu.handong.csee.histudy.observability.audit;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.repository.impl.*;
import edu.handong.csee.histudy.service.UserService;
import edu.handong.csee.histudy.service.command.LegacyStudyApplicationCommand;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:application-audit;NON_KEYWORDS=USER",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserService.class, StudyApplicationAuditListener.class, UserRepositoryImpl.class,
    CourseRepositoryImpl.class, AcademicTermRepositoryImpl.class,
    StudyApplicationRepositoryImpl.class, StudyGroupRepositoryImpl.class, StudyReportRepositoryImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StudyApplicationAuditListenerTest {
  private final AuditContext admin = new AuditContext("admin-delete", 999L, Role.ADMIN);

  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private UserService service;
  @Autowired private ApplicationEventPublisher publisher;

  private TransactionTemplate transaction;
  private ListAppender<ILoggingEvent> appender;
  private Long userId;
  private Long termId;
  private Long courseId;
  private String email;
  private String sid;
  private AuditContext userContext;

  @BeforeEach
  void setUp() {
    transaction = new TransactionTemplate(transactionManager);
    transaction.executeWithoutResult(status -> {
      entityManager.createQuery("update AcademicTerm a set a.isCurrent = false").executeUpdate();
      AcademicTerm term = AcademicTerm.builder().academicYear(2026)
          .semester(TermType.SPRING).isCurrent(true).build();
      entityManager.persist(term);
      termId = term.getAcademicTermId();
      email = UUID.randomUUID() + "@private.example";
      sid = UUID.randomUUID().toString();
      User user = User.builder().email(email).sid(sid).name("private-name").role(Role.USER).build();
      entityManager.persist(user);
      userId = user.getUserId();
      Course course = Course.builder().name("private-course").code("TEST")
          .professor("private-professor").academicTerm(term).build();
      entityManager.persist(course);
      courseId = course.getCourseId();
    });
    userContext = new AuditContext("application-submit", userId, Role.USER);
    appender = new ListAppender<>();
    appender.start();
    ((Logger) LoggerFactory.getLogger(StudyApplicationAuditListener.class)).addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger(StudyApplicationAuditListener.class)).detachAppender(appender);
    appender.stop();
  }

  @Test
  void V1신규신청을_커밋하면_개인정보없이_한번_기록한다() {
    // given when
    transaction.executeWithoutResult(status -> {
      service.apply(new LegacyStudyApplicationCommand(List.of(), List.of(courseId)), email, userContext);
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).singleElement().satisfies(event -> {
      assertThat(event.getFormattedMessage()).contains("study_application_submitted",
          "request_id=application-submit", "actor_id=" + userId, "target_user_id=" + userId,
          "academic_term_id=" + termId, "result=success", "submission_type=new",
          "friend_request_count=0", "preferred_course_count=1")
          .doesNotContain(email, sid, "private", "reason_code");
      assertThat(event.getThrowableProxy()).isNull();
    });
  }

  @Test
  void V2재신청을_커밋하면_내부삭제는_중복기록하지_않는다() {
    // given
    StudyApplicant initial = service.apply(List.of(), List.of(courseId), email, userContext);
    appender.list.clear();
    // when
    transaction.executeWithoutResult(status -> {
      service.apply(List.of(), List.of(), email, userContext);
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).singleElement().satisfies(event ->
        assertThat(event.getFormattedMessage()).contains("study_application_submitted",
            "submission_type=resubmission", "previous_application_id=" + initial.getStudyApplicantId(),
            "preferred_course_count=0").doesNotContain("study_application_deleted"));
  }

  @Test
  void 관리자삭제는_행위자와_대상을_구분하고_없는신청은_변경없음으로_기록한다() {
    // given
    service.apply(List.of(), List.of(courseId), email, userContext);
    appender.list.clear();
    // when
    transaction.executeWithoutResult(status -> {
      service.deleteUserForm(sid, admin);
      assertThat(appender.list).isEmpty();
    });
    service.deleteUserForm(sid, admin);
    // then
    assertThat(appender.list).hasSize(2);
    assertThat(appender.list.get(0).getFormattedMessage()).contains("study_application_deleted",
        "actor_id=999", "target_user_id=" + userId, "result=success", "preferred_course_count=1")
        .doesNotContain(email, sid, "private");
    assertThat(appender.list.get(1).getFormattedMessage()).contains("result=no_op", "reason_code=NO_APPLICATION");
  }

  @Test
  void 재신청을_롤백하면_기존신청을_보존하고_완료로그가_없다() {
    // given
    StudyApplicant initial = service.apply(List.of(), List.of(courseId), email, userContext);
    appender.list.clear();
    // when
    transaction.executeWithoutResult(status -> {
      service.apply(List.of(), List.of(), email, userContext);
      status.setRollbackOnly();
    });
    // then
    assertThat(appender.list).isEmpty();
    transaction.executeWithoutResult(status ->
        assertThat(entityManager.find(StudyApplicant.class, initial.getStudyApplicantId())).isNotNull());
  }

  @Test
  void 삭제후_커밋_flush가_실패하면_완료로그가_없고_신청은_보존된다() {
    // given
    StudyApplicant initial = service.apply(List.of(), List.of(courseId), email, userContext);
    appender.list.clear();
    // when then
    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      service.deleteUserForm(sid, admin);
      entityManager.find(User.class, userId).edit(null, "x".repeat(256));
    })).isInstanceOf(RuntimeException.class);
    assertThat(appender.list).isEmpty();
    transaction.executeWithoutResult(status ->
        assertThat(entityManager.find(StudyApplicant.class, initial.getStudyApplicantId())).isNotNull());
  }

  @Test
  void 그룹배정된_신청삭제가_실패하면_완료로그가_없다() {
    // given
    StudyApplicant initial = service.apply(List.of(), List.of(courseId), email, userContext);
    transaction.executeWithoutResult(status -> {
      StudyApplicant applicant = entityManager.find(StudyApplicant.class, initial.getStudyApplicantId());
      entityManager.persist(StudyGroup.of(1, applicant.getAcademicTerm(), List.of(applicant)));
    });
    appender.list.clear();
    // when then
    assertThatThrownBy(() -> service.deleteUserForm(sid, admin)).isInstanceOf(IllegalStateException.class);
    assertThat(appender.list).isEmpty();
  }

  @Test
  void 트랜잭션밖_이벤트는_기록하지_않는다() {
    // given when
    publisher.publishEvent(new StudyApplicationAuditEvent(userContext,
        StudyApplicationAuditEvent.Action.SUBMITTED, termId, userId, 1L, null, 0, 0));
    // then
    assertThat(appender.list).isEmpty();
  }
}
