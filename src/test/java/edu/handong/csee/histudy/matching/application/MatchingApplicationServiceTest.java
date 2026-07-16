package edu.handong.csee.histudy.matching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyPartnerRequest;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyApplicationRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyGroupRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MatchingApplicationServiceTest {

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
    academicTermRepository = new FakeAcademicTermRepository();
    studyApplicantRepository = new FakeStudyApplicationRepository();
    studyGroupRepository = new FakeStudyGroupRepository();
    matchingApplicationService =
        new MatchingApplicationService(
            academicTermRepository, studyApplicantRepository, studyGroupRepository);
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
    matchingApplicationService.match();

    // Then
    List<StudyGroup> groups = studyGroupRepository.findAllByAcademicTerm(currentTerm);
    assertThat(groups).extracting(StudyGroup::getTag).containsExactly(1, 2);
    assertThat(groups).extracting(group -> group.getMembers().size()).containsExactly(2, 3);
    assertThat(leftoverApplicants).allMatch(applicant -> !applicant.hasStudyGroup());
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
    matchingApplicationService.match();

    // Then
    assertThat(studyGroupRepository.findAllByAcademicTerm(currentTerm))
        .extracting(StudyGroup::getTag)
        .containsExactly(7, 8);
  }

  @Test
  void 현재_학기_없이_그룹을_자동_배정하면_예외가_발생한다() {
    // Given

    // When Then
    assertThatThrownBy(matchingApplicationService::match)
        .isInstanceOf(NoCurrentTermFoundException.class);
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
