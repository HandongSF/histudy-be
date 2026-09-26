package edu.handong.csee.histudy.observability.audit;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.repository.impl.*;
import edu.handong.csee.histudy.service.ReportService;
import edu.handong.csee.histudy.service.command.ReportCommand;
import edu.handong.csee.histudy.util.ImagePathMapper;
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
    "spring.datasource.url=jdbc:h2:mem:report-audit;NON_KEYWORDS=USER",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "custom.jwt.issuer=https://private.example", "custom.resource.path=/images"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReportService.class, ReportAuditListener.class, ImagePathMapper.class,
    AcademicTermRepositoryImpl.class, UserRepositoryImpl.class, CourseRepositoryImpl.class,
    StudyGroupRepositoryImpl.class, StudyReportRepositoryImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportAuditListenerTest {
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private ReportService service;
  @Autowired private ApplicationEventPublisher publisher;

  private TransactionTemplate transaction;
  private ListAppender<ILoggingEvent> appender;
  private AuditContext context;
  private String email;
  private Long termId;
  private Long groupId;
  private Long reportId;
  private Long otherReportId;
  private Long userId;
  private Long courseId;

  @BeforeEach
  void setUp() {
    transaction = new TransactionTemplate(transactionManager);
    transaction.executeWithoutResult(status -> {
      entityManager.createQuery("update AcademicTerm a set a.isCurrent = false").executeUpdate();
      AcademicTerm term = AcademicTerm.builder().academicYear(2026)
          .semester(TermType.SPRING).isCurrent(true).build();
      entityManager.persist(term);
      termId = term.getAcademicTermId();
      Course course = Course.builder().name("private-course").code("TEST")
          .professor("private-professor").academicTerm(term).build();
      entityManager.persist(course);
      courseId = course.getCourseId();
      email = UUID.randomUUID() + "@private.example";
      User user = User.builder().email(email).name("private-name").role(Role.MEMBER).build();
      entityManager.persist(user);
      userId = user.getUserId();
      StudyApplicant applicant = StudyApplicant.of(term, user, List.of(), List.of(course));
      StudyGroup group = StudyGroup.of(1, term, List.of(applicant));
      entityManager.persist(group);
      groupId = group.getStudyGroupId();
      StudyReport report = report(group, user, course);
      entityManager.persist(report);
      reportId = report.getStudyReportId();
      StudyGroup otherGroup = StudyGroup.of(2, term, List.of());
      entityManager.persist(otherGroup);
      StudyReport otherReport = report(otherGroup, user, course);
      entityManager.persist(otherReport);
      otherReportId = otherReport.getStudyReportId();
    });
    context = new AuditContext("report-request", userId, Role.MEMBER);
    appender = new ListAppender<>();
    appender.start();
    ((Logger) LoggerFactory.getLogger(ReportAuditListener.class)).addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger(ReportAuditListener.class)).detachAppender(appender);
    appender.stop();
    MDC.clear();
  }

  @Test
  void 생성후_커밋하면_실제_연결건수와_행위자를_한번_기록한다() {
    // given
    ReportCommand command = command();
    // when
    transaction.executeWithoutResult(status -> {
      service.createReport(command, email, context);
      assertThat(appender.list).isEmpty();
      MDC.put("request_id", "unrelated-request");
    });
    // then
    assertSuccess("report_created", 1);
    assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("unrelated-request");
  }

  @Test
  void 수정후_커밋하면_변경후_건수를_기록한다() {
    // given
    ReportCommand command = new ReportCommand(null, null, null, List.of(), List.of(), List.of());
    // when
    transaction.executeWithoutResult(status -> {
      assertThat(service.updateReport(reportId, command, email, context)).isTrue();
      assertThat(appender.list).isEmpty();
    });
    // then
    assertSuccess("report_updated", 0);
  }

  @Test
  void 삭제후_커밋하면_삭제전_건수를_기록한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      assertThat(service.deleteReport(reportId, email, context)).isTrue();
      assertThat(appender.list).isEmpty();
    });
    // then
    assertSuccess("report_deleted", 1);
    StudyReport deleted = transaction.execute(status -> entityManager.find(StudyReport.class, reportId));
    assertThat(deleted).isNull();
  }

  @Test
  void 생성수정삭제를_롤백하면_완료로그가_없고_데이터가_유지된다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      service.createReport(command(), email, context);
      service.updateReport(reportId, command(), email, context);
      service.deleteReport(reportId, email, context);
      status.setRollbackOnly();
    });
    // then
    assertThat(appender.list).isEmpty();
    String title = transaction.execute(status -> entityManager.find(StudyReport.class, reportId).getTitle());
    assertThat(title).isEqualTo("private-title");
    Long count = transaction.execute(status -> entityManager.createQuery(
        "select count(r) from StudyReport r where r.studyGroup.studyGroupId = :id", Long.class)
        .setParameter("id", groupId).getSingleResult());
    assertThat(count).isEqualTo(1);
  }

  @Test
  void 이벤트발행후_flush가_실패하면_완료로그가_없다() {
    // given
    // when then
    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      service.updateReport(reportId, command(), email, context);
      entityManager.find(User.class, userId).edit(null, "x".repeat(256));
    })).isInstanceOf(RuntimeException.class);
    assertThat(appender.list).isEmpty();
  }

  @Test
  void 없거나_타그룹인_보고서_변경거절은_롤백에도_같은사유로_기록한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      for (Long id : List.of(Long.MAX_VALUE, otherReportId)) {
        assertThat(service.updateReport(id, command(), email, context)).isFalse();
        assertThat(service.deleteReport(id, email, context)).isFalse();
      }
      status.setRollbackOnly();
    });
    // then
    assertThat(appender.list).hasSize(4).allSatisfy(event -> {
      assertThat(event.getThrowableProxy()).isNull();
      assertThat(event.getFormattedMessage()).contains("result=rejected",
          "reason_code=RESOURCE_UNAVAILABLE", "request_id=report-request")
          .doesNotContain("academic_term_id", "group_id", "participant_count", "private", "@");
    });
  }

  @Test
  void 트랜잭션없는_성공이벤트는_기록하지_않는다() {
    // given
    ReportChangedEvent event = new ReportChangedEvent(context,
        ReportChangedEvent.Operation.CREATED, termId, groupId, reportId, 1, 1, 1);
    // when
    publisher.publishEvent(event);
    // then
    assertThat(appender.list).isEmpty();
  }

  private ReportCommand command() {
    return new ReportCommand("private-updated-title", "private-content", 90L,
        List.of(userId, Long.MAX_VALUE), List.of("https://private.example/images/private.png"),
        List.of(courseId, Long.MAX_VALUE));
  }

  private StudyReport report(StudyGroup group, User user, Course course) {
    return StudyReport.builder().title("private-title").content("private-content")
        .totalMinutes(60).studyGroup(group).participants(List.of(user))
        .images(List.of("private.png")).courses(List.of(course)).build();
  }

  private void assertSuccess(String name, int count) {
    assertThat(appender.list).singleElement().satisfies(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.INFO);
      assertThat(event.getThrowableProxy()).isNull();
      assertThat(event.getFormattedMessage()).contains(name, "request_id=report-request",
          "actor_id=" + userId, "role=MEMBER", "academic_term_id=" + termId,
          "group_id=" + groupId, "report_id=", "result=success",
          "participant_count=" + count, "course_count=" + count, "image_count=" + count)
          .doesNotContain("reason_code", "private", "@", "token");
    });
  }
}
