package edu.handong.csee.histudy.observability.audit;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.matching.application.MatchingApplicationService;
import edu.handong.csee.histudy.repository.impl.AcademicTermRepositoryImpl;
import edu.handong.csee.histudy.repository.impl.StudyApplicationRepositoryImpl;
import edu.handong.csee.histudy.repository.impl.StudyGroupRepositoryImpl;
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
    "spring.datasource.url=jdbc:h2:mem:matching-audit;NON_KEYWORDS=USER",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MatchingApplicationService.class, MatchingAuditListener.class,
    AcademicTermRepositoryImpl.class, StudyApplicationRepositoryImpl.class,
    StudyGroupRepositoryImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MatchingAuditListenerTest {
  private final AuditContext context = new AuditContext("request-match", 42L, Role.ADMIN);

  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private MatchingApplicationService matchingService;
  @Autowired private ApplicationEventPublisher publisher;

  private TransactionTemplate transaction;
  private ListAppender<ILoggingEvent> appender;
  private Long termId;
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
      for (int i = 0; i < 3; i++) {
        User user = User.builder().email(UUID.randomUUID() + "@private.example")
            .name("private-name").role(Role.USER).build();
        entityManager.persist(user);
        userId = user.getUserId();
        entityManager.persist(StudyApplicant.of(term, user, List.of(), List.of(course)));
      }
    });
    appender = new ListAppender<>();
    appender.start();
    ((Logger) LoggerFactory.getLogger(MatchingAuditListener.class)).addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger(MatchingAuditListener.class)).detachAppender(appender);
    appender.stop();
    MDC.clear();
  }

  @Test
  void 매칭을_커밋하면_완료로그를_한번_기록한다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      matchingService.match(context);
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).singleElement().satisfies(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.INFO);
      assertThat(event.getThrowableProxy()).isNull();
      assertThat(event.getFormattedMessage()).contains(
          "matching_executed", "request_id=request-match", "actor_id=42", "role=ADMIN",
          "academic_term_id=" + termId, "result=success", "applicant_count=3",
          "assigned_count=3", "remaining_count=0", "created_group_count=1")
          .containsPattern("duration_ms=\\d+")
          .doesNotContain("reason_code", "private", "@", "token");
    });
  }

  @Test
  void 이벤트_발행후_외부트랜잭션을_롤백하면_완료로그가_없다() {
    // given
    // when
    transaction.executeWithoutResult(status -> {
      matchingService.match(context);
      status.setRollbackOnly();
    });
    // then
    assertThat(appender.list).isEmpty();
    assertNoGroups();
  }

  @Test
  void 이벤트_발행후_커밋의_flush가_실패하면_완료로그가_없다() {
    // given
    // when then
    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      matchingService.match(context);
      assertThat(appender.list).isEmpty();
      entityManager.find(User.class, userId).edit(null, "x".repeat(256));
    })).isInstanceOf(RuntimeException.class);
    assertThat(appender.list).isEmpty();
    assertNoGroups();
  }

  @Test
  void 요청문맥이_바뀌어도_발행시점의_값과_변경없음_사유를_기록한다() {
    // given
    matchingService.match(context);
    appender.list.clear();
    // when
    transaction.executeWithoutResult(status -> {
      matchingService.match(new AuditContext("request-second", null, Role.ADMIN));
      MDC.put("request_id", "unrelated-request");
      assertThat(appender.list).isEmpty();
    });
    // then
    assertThat(appender.list).singleElement().satisfies(event ->
        assertThat(event.getFormattedMessage()).contains(
            "request_id=request-second", "actor_id=unknown", "result=no_op",
            "reason_code=NO_UNASSIGNED_APPLICANTS", "applicant_count=0",
            "assigned_count=0", "remaining_count=0", "created_group_count=0")
            .doesNotContain("unrelated-request", "request-match"));
  }

  @Test
  void 트랜잭션없이_이벤트를_발행하면_완료로그가_없다() {
    // given
    MatchingExecutedEvent event = new MatchingExecutedEvent(context, termId, 3, 3, 1, 1);
    // when
    publisher.publishEvent(event);
    // then
    assertThat(appender.list).isEmpty();
  }

  private void assertNoGroups() {
    Long count = transaction.execute(status -> entityManager.createQuery(
        "select count(g) from StudyGroup g where g.academicTerm.academicTermId = :termId", Long.class)
        .setParameter("termId", termId).getSingleResult());
    assertThat(count).isZero();
  }
}
