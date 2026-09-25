package edu.handong.csee.histudy.matching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyPartnerRequest;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.observability.audit.AuditContext;
import edu.handong.csee.histudy.observability.audit.MatchingExecutedEvent;
import edu.handong.csee.histudy.repository.StudyGroupRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyApplicationRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyGroupRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MatchingApplicationServiceTest {

  private final AuditContext context = new AuditContext("request-match", 42L, Role.ADMIN);
  private final List<MatchingExecutedEvent> events = new ArrayList<>();
  private final AcademicTerm currentTerm =
      AcademicTerm.builder().academicYear(2025).semester(TermType.SPRING).isCurrent(true).build();
  private final Course primaryCourse = createCourse(1L, "자료구조");
  private final Course secondaryCourse = createCourse(2L, "운영체제");

  private FakeAcademicTermRepository academicTermRepository;
  private FakeStudyApplicationRepository studyApplicantRepository;
  private FakeStudyGroupRepository studyGroupRepository;
  private MatchingApplicationService matchingApplicationService;

  @BeforeEach
  void setUp() {
    events.clear();
    academicTermRepository = new FakeAcademicTermRepository();
    studyApplicantRepository = new FakeStudyApplicationRepository();
    studyGroupRepository = new FakeStudyGroupRepository();
    matchingApplicationService =
        new MatchingApplicationService(
            academicTermRepository, studyApplicantRepository, studyGroupRepository,
            event -> events.add((MatchingExecutedEvent) event));
  }

  @Test
  void 그룹을_자동_배정하면_친구_우선과_과목_우선_정책결과를_저장한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User friendOne = createUser(1);
    User friendTwo = createUser(2);
    StudyApplicant friendOneApplicant =
        StudyApplicant.of(currentTerm, friendOne, List.of(friendTwo), List.of(primaryCourse));
    StudyApplicant friendTwoApplicant =
        StudyApplicant.of(currentTerm, friendTwo, List.of(), List.of(primaryCourse));
    friendOneApplicant.changeStatusIfReceivedBy(friendTwo, StudyPartnerRequest::accept);

    List<StudyApplicant> courseApplicants =
        List.of(
            createApplicant(3, primaryCourse),
            createApplicant(4, primaryCourse),
            createApplicant(5, primaryCourse));
    List<StudyApplicant> leftoverApplicants =
        List.of(createApplicant(6, secondaryCourse), createApplicant(7, secondaryCourse));

    List<StudyApplicant> applicants = new ArrayList<>();
    applicants.add(friendOneApplicant);
    applicants.add(friendTwoApplicant);
    applicants.addAll(courseApplicants);
    applicants.addAll(leftoverApplicants);
    studyApplicantRepository.saveAll(applicants);

    // When
    matchingApplicationService.match(context);

    // Then
    List<StudyGroup> groups = studyGroupRepository.findAllByAcademicTerm(currentTerm);
    assertThat(groups).extracting(StudyGroup::getTag).containsExactly(1, 2);
    assertThat(groups).extracting(group -> group.getMembers().size()).containsExactly(2, 3);
    assertThat(leftoverApplicants).allMatch(applicant -> !applicant.hasStudyGroup());
    assertThat(events).singleElement().satisfies(event -> {
      assertThat(event.context()).isEqualTo(context);
      assertThat(event.academicTermId()).isEqualTo(currentTerm.getAcademicTermId());
      assertThat(event.applicantCount()).isEqualTo(7);
      assertThat(event.assignedCount()).isEqualTo(5);
      assertThat(event.remainingCount()).isEqualTo(2);
      assertThat(event.createdGroupCount()).isEqualTo(2);
      assertThat(event.result()).isEqualTo("success");
      assertThat(event.reasonCode()).isNull();
      assertThat(event.durationMs()).isNotNegative();
    });
  }

  @Test
  void 기존_그룹태그가_있으면_다음_번호부터_새_그룹을_저장한다() {
    // Given
    academicTermRepository.save(currentTerm);
    StudyApplicant existingApplicant = createApplicant(1, primaryCourse);
    studyApplicantRepository.save(existingApplicant);
    studyGroupRepository.save(StudyGroup.of(7, currentTerm, List.of(existingApplicant)));

    List<StudyApplicant> newApplicants =
        List.of(
            createApplicant(2, primaryCourse),
            createApplicant(3, primaryCourse),
            createApplicant(4, primaryCourse));
    studyApplicantRepository.saveAll(newApplicants);

    // When
    matchingApplicationService.match(context);

    // Then
    assertThat(studyGroupRepository.findAllByAcademicTerm(currentTerm))
        .extracting(StudyGroup::getTag)
        .containsExactly(7, 8);
    assertThat(events).singleElement().satisfies(event -> {
      assertThat(event.applicantCount()).isEqualTo(3);
      assertThat(event.assignedCount()).isEqualTo(3);
      assertThat(event.createdGroupCount()).isEqualTo(1);
    });
  }

  @Test
  void 현재_학기_없이_그룹을_자동_배정하면_예외가_발생한다() {
    // Given

    // When Then
    assertThatThrownBy(() -> matchingApplicationService.match(context))
        .isInstanceOf(NoCurrentTermFoundException.class);
    assertThat(events).isEmpty();
  }

  @Test
  void 신청자가_없으면_변경없음_이벤트를_발행한다() {
    // given
    academicTermRepository.save(currentTerm);
    // when
    matchingApplicationService.match(context);
    // then
    assertThat(events).singleElement().satisfies(event -> {
      assertThat(event.result()).isEqualTo("no_op");
      assertThat(event.reasonCode()).isEqualTo("NO_UNASSIGNED_APPLICANTS");
      assertThat(event.applicantCount()).isZero();
      assertThat(event.assignedCount()).isZero();
      assertThat(event.remainingCount()).isZero();
      assertThat(event.createdGroupCount()).isZero();
    });
  }

  @Test
  void 그룹구성이_불가능하면_미배정인원과_사유를_기록한다() {
    // given
    academicTermRepository.save(currentTerm);
    studyApplicantRepository.saveAll(List.of(createApplicant(1, primaryCourse), createApplicant(2, primaryCourse)));
    // when
    matchingApplicationService.match(context);
    // then
    assertThat(events).singleElement().satisfies(event -> {
      assertThat(event.result()).isEqualTo("no_op");
      assertThat(event.reasonCode()).isEqualTo("NO_ELIGIBLE_GROUPS");
      assertThat(event.applicantCount()).isEqualTo(2);
      assertThat(event.remainingCount()).isEqualTo(2);
      assertThat(event.assignedCount()).isZero();
      assertThat(event.createdGroupCount()).isZero();
    });
  }

  @Test
  void 그룹저장이_실패하면_완료이벤트를_발행하지_않는다() {
    // given
    academicTermRepository.save(currentTerm);
    studyApplicantRepository.saveAll(List.of(
        createApplicant(1, primaryCourse), createApplicant(2, primaryCourse),
        createApplicant(3, primaryCourse)));
    StudyGroupRepository failingRepository = mock(StudyGroupRepository.class);
    RuntimeException failure = new IllegalStateException("save failed");
    when(failingRepository.saveAll(any())).thenThrow(failure);
    MatchingApplicationService service = new MatchingApplicationService(
        academicTermRepository, studyApplicantRepository, failingRepository,
        event -> events.add((MatchingExecutedEvent) event));
    // when then
    assertThatThrownBy(() -> service.match(context)).isSameAs(failure);
    assertThat(events).isEmpty();
  }

  private StudyApplicant createApplicant(int sequence, Course course) {
    return StudyApplicant.of(currentTerm, createUser(sequence), List.of(), List.of(course));
  }

  private User createUser(int sequence) {
    return User.builder()
        .sub("sub-" + sequence)
        .sid("2223%04d".formatted(sequence))
        .email("user%d@histudy.com".formatted(sequence))
        .name("User" + sequence)
        .role(Role.USER)
        .build();
  }

  private Course createCourse(Long courseId, String name) {
    Course course =
        Course.builder()
            .name(name)
            .code("CSEE" + courseId)
            .professor("Professor")
            .academicTerm(currentTerm)
            .build();
    ReflectionTestUtils.setField(course, "courseId", courseId);
    return course;
  }
}
