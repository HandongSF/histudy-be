package edu.handong.csee.histudy.observability.audit;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.CourseIdDto;
import edu.handong.csee.histudy.exception.CourseInUseException;
import edu.handong.csee.histudy.repository.impl.*;
import edu.handong.csee.histudy.service.*;
import edu.handong.csee.histudy.util.CourseCSV;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
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
    "spring.datasource.url=jdbc:h2:mem:academic-course-audit;NON_KEYWORDS=USER",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AcademicTermService.class, CourseService.class, AcademicCourseAuditListener.class,
    AcademicTermRepositoryImpl.class, CourseRepositoryImpl.class,
    UserRepositoryImpl.class, StudyGroupRepositoryImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AcademicCourseAuditListenerTest {
  private final AuditContext context = new AuditContext("request-academic", 42L, Role.ADMIN);

  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private AcademicTermService terms;
  @Autowired private CourseService courses;
  @Autowired private ApplicationEventPublisher publisher;

  private TransactionTemplate transaction;
  private ListAppender<ILoggingEvent> appender;
  private Long termId;
  private Long courseId;
  private Long userId;

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
      User user = User.builder().email(UUID.randomUUID() + "@private.example")
          .name("private-name").role(Role.USER).build();
      entityManager.persist(user);
      userId = user.getUserId();
    });
    appender = new ListAppender<>();
    appender.start();
    ((Logger) LoggerFactory.getLogger(AcademicCourseAuditListener.class)).addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger(AcademicCourseAuditListener.class)).detachAppender(appender);
    appender.stop();
    MDC.clear();
  }

  @Test
  void 학기생성을_커밋하면_한번_기록한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      terms.createAcademicTerm(2099, TermType.FALL, context);
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).singleElement().satisfies(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.INFO);
      assertThat(event.getThrowableProxy()).isNull();
      assertThat(event.getFormattedMessage()).contains("academic_term_created", "request_id=request-academic",
          "actor_id=42", "role=ADMIN", "result=success", "academic_term_id=")
          .doesNotContain("private", "@", "token");
    });
  }

  @Test
  void 현재학기를_다시선택하면_문맥을_보존하고_변경없음을_기록한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      terms.setCurrentTerm(termId, new AuditContext("second-request", null, Role.ADMIN));
      MDC.put("request_id", "unrelated");
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).singleElement().satisfies(event ->
        assertThat(event.getFormattedMessage()).contains("current_term_changed", "result=no_op",
            "reason_code=ALREADY_CURRENT", "actor_id=unknown", "request_id=second-request",
            "previous_term_id=" + termId).doesNotContain("unrelated", "request-academic"));
  }

  @Test
  void 과목교체를_커밋하면_전후건수를_기록한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      courses.replaceCourses(List.of(CourseCSV.builder().code("NEW").title("secret").professor("secret").build()), context);
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).singleElement().satisfies(event -> assertThat(event.getFormattedMessage())
        .contains("courses_replaced", "previous_count=1", "replacement_count=1", "result=success")
        .doesNotContain("secret", "reason_code"));
  }

  @Test
  void 과목삭제후_외부트랜잭션을_롤백하면_완료로그가_없다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      courses.deleteCurrentCourse(courseId, context);
      status.setRollbackOnly();
    });
    // then
    assertThat(appender.list).isEmpty();
    Course retained = transaction.execute(status -> entityManager.find(Course.class, courseId));
    assertThat(retained).isNotNull();
  }

  @Test
  void 삭제후_flush가_실패하면_완료로그가_없다() {
    // given
    // when then
    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      courses.deleteCurrentCourse(courseId, context);
      entityManager.find(User.class, userId).edit(null, "x".repeat(256));
    })).isInstanceOf(RuntimeException.class);
    assertThat(appender.list).isEmpty();
    Course retained = transaction.execute(status -> entityManager.find(Course.class, courseId));
    assertThat(retained).isNotNull();
  }

  @Test
  void 사용중인_과목삭제를_거절하면_롤백후에도_거절만_기록한다() {
    // given
    transaction.executeWithoutResult(status -> entityManager.persist(StudyApplicant.of(
        entityManager.find(AcademicTerm.class, termId), entityManager.find(User.class, userId),
        List.of(), List.of(entityManager.find(Course.class, courseId)))));
    // when then
    assertThatThrownBy(() -> courses.deleteCurrentCourse(courseId, context)).isInstanceOf(CourseInUseException.class);
    assertThat(appender.list).singleElement().satisfies(event -> {
      assertThat(event.getThrowableProxy()).isNull();
      assertThat(event.getFormattedMessage()).contains("course_deleted", "result=rejected",
          "reason_code=COURSE_IN_USE", "course_id=" + courseId).doesNotContain("private", "@", "success");
    });
  }

  @Test
  void 빈입력과_구형삭제대상부재는_변경없음을_기록한다() {
    // given
    // when
    courses.replaceCourses(List.of(), context);
    int result = courses.deleteCourse(new CourseIdDto(Long.MAX_VALUE), context);
    // then
    assertThat(result).isZero();
    assertThat(appender.list).hasSize(2);
    assertThat(appender.list.get(0).getFormattedMessage()).contains("reason_code=EMPTY_INPUT", "previous_count=unknown");
    assertThat(appender.list.get(1).getFormattedMessage()).contains("legacy=true", "reason_code=COURSE_NOT_FOUND");
  }

  @Test
  void 트랜잭션없이_완료이벤트를_발행하면_출력하지_않는다() {
    // given
    // when
    publisher.publishEvent(new AcademicTermCreatedEvent(context, termId));
    publisher.publishEvent(new CurrentTermChangedEvent(context, termId, termId, false));
    publisher.publishEvent(new CoursesReplacedEvent(context, termId, 1, 1, true));
    publisher.publishEvent(new CourseDeletedEvent(context, termId, courseId, true, false));
    // then
    assertThat(appender.list).isEmpty();
  }
}
