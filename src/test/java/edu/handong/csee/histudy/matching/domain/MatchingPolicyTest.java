package edu.handong.csee.histudy.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyPartnerRequest;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MatchingPolicyTest {

  private final AcademicTerm currentTerm =
      AcademicTerm.builder().academicYear(2025).semester(TermType.SPRING).isCurrent(true).build();
  private final Course primaryCourse = createCourse(1L, "자료구조");
  private final Course secondaryCourse = createCourse(2L, "운영체제");
  private final MatchingPolicy matchingPolicy = new MatchingPolicy();

  @Test
  void 매칭하면_친구를_먼저_배정하고_남은_신청자를_과목으로_배정한다() {
    // Given
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

    // When
    List<StudyGroup> result = matchingPolicy.match(applicants, currentTerm, 10);

    // Then
    assertThat(result).extracting(StudyGroup::getTag).containsExactly(10, 11);
    assertThat(result).extracting(group -> group.getMembers().size()).containsExactly(2, 3);
    assertThat(result.get(0).getMembers())
        .extracting(applicant -> applicant.getUser().getEmail())
        .containsExactlyInAnyOrder(friendOne.getEmail(), friendTwo.getEmail());
    assertThat(result.get(1).getMembers()).containsExactlyInAnyOrderElementsOf(courseApplicants);
    assertThat(leftoverApplicants).allMatch(applicant -> !applicant.hasStudyGroup());
  }

  @Test
  void 과목이_같은_신청자가_여덟명이면_다섯명과_세명_그룹으로_나눈다() {
    // Given
    List<StudyApplicant> applicants =
        List.of(
            createApplicant(1, primaryCourse),
            createApplicant(2, primaryCourse),
            createApplicant(3, primaryCourse),
            createApplicant(4, primaryCourse),
            createApplicant(5, primaryCourse),
            createApplicant(6, primaryCourse),
            createApplicant(7, primaryCourse),
            createApplicant(8, primaryCourse));

    // When
    List<StudyGroup> result = matchingPolicy.match(applicants, currentTerm, 4);

    // Then
    assertThat(result).extracting(StudyGroup::getTag).containsExactly(4, 5);
    assertThat(result).extracting(group -> group.getMembers().size()).containsExactly(5, 3);
    assertThat(applicants).allMatch(StudyApplicant::hasStudyGroup);
  }

  @Test
  void 신청자가_없으면_그룹을_만들지_않는다() {
    // Given

    // When
    List<StudyGroup> result = matchingPolicy.match(List.of(), currentTerm, 1);

    // Then
    assertThat(result).isEmpty();
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
