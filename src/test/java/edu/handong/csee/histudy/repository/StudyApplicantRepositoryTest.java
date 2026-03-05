package edu.handong.csee.histudy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.repository.jpa.JpaStudyApplicantRepository;
import edu.handong.csee.histudy.repository.jpa.JpaStudyGroupRepository;
import edu.handong.csee.histudy.support.BaseRepositoryTest;
import edu.handong.csee.histudy.support.TestDataFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StudyApplicantRepositoryTest extends BaseRepositoryTest {

  @Autowired private JpaStudyApplicantRepository studyApplicantRepository;
  @Autowired private JpaStudyGroupRepository studyGroupRepository;

  @Test
  void 사용자와학기로조회시_해당신청서반환() {
    // Given
    StudyApplicant applicant =
        TestDataFactory.createStudyApplicant(currentTerm, user1, List.of(user2), List.of(course1));
    studyApplicantRepository.save(applicant);

    // When
    Optional<StudyApplicant> result =
        studyApplicantRepository.findByUserAndTerm(user1, currentTerm);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getUser()).isEqualTo(user1);
    assertThat(result.get().getAcademicTerm()).isEqualTo(currentTerm);
  }

  @Test
  void 존재하지않는사용자학기조회시_빈결과반환() {
    // When
    Optional<StudyApplicant> result =
        studyApplicantRepository.findByUserAndTerm(user1, currentTerm);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void 미배정신청자조회시_배정되지않은신청자목록반환() {
    // Given
    StudyApplicant unassignedApplicant1 =
        TestDataFactory.createStudyApplicant(currentTerm, user1, List.of(user2), List.of(course1));
    StudyApplicant unassignedApplicant2 =
        TestDataFactory.createStudyApplicant(currentTerm, user2, List.of(user1), List.of(course1));

    studyApplicantRepository.save(unassignedApplicant1);
    studyApplicantRepository.save(unassignedApplicant2);

    // Create a study group and assign one applicant
    StudyGroup studyGroup = StudyGroup.of(1, currentTerm, List.of(unassignedApplicant2));
    studyGroupRepository.save(studyGroup);

    // When
    List<StudyApplicant> unassignedApplicants =
        studyApplicantRepository.findUnassignedApplicants(currentTerm);

    // Then
    assertThat(unassignedApplicants).hasSize(1);
    assertThat(unassignedApplicants.get(0).getUser()).isEqualTo(user1);
    assertThat(unassignedApplicants.get(0).hasStudyGroup()).isFalse();
  }

  @Test
  void 배정신청자조회시_배정된신청자목록반환() {
    // Given
    StudyApplicant applicant1 =
        TestDataFactory.createStudyApplicant(currentTerm, user1, List.of(user2), List.of(course1));
    StudyApplicant applicant2 =
        TestDataFactory.createStudyApplicant(currentTerm, user2, List.of(user1), List.of(course1));

    studyApplicantRepository.save(applicant1);
    studyApplicantRepository.save(applicant2);

    StudyGroup studyGroup = StudyGroup.of(1, currentTerm, List.of(applicant1, applicant2));
    studyGroupRepository.save(studyGroup);

    // When
    List<StudyApplicant> assignedApplicants =
        studyApplicantRepository.findAssignedApplicants(currentTerm);

    // Then
    assertThat(assignedApplicants).hasSize(2);
    assertThat(assignedApplicants).allMatch(StudyApplicant::hasStudyGroup);
  }

  @Test
  void 배정신청자수조회시_현재학기배정인원만반환() {
    // Given
    StudyApplicant currentAssigned =
        TestDataFactory.createStudyApplicant(currentTerm, user1, List.of(user2), List.of(course1));
    StudyApplicant currentUnassigned =
        TestDataFactory.createStudyApplicant(currentTerm, user2, List.of(user1), List.of(course1));
    StudyApplicant pastAssigned =
        TestDataFactory.createStudyApplicant(pastTerm, user3, List.of(), List.of(course2));

    studyApplicantRepository.saveAll(List.of(currentAssigned, currentUnassigned, pastAssigned));

    studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(currentAssigned)));
    studyGroupRepository.save(StudyGroup.of(2, pastTerm, List.of(pastAssigned)));

    // When
    long assignedCount = studyApplicantRepository.countAssignedApplicants(currentTerm);

    // Then
    assertThat(assignedCount).isEqualTo(1L);
  }

  @Test
  void 특정학기신청자조회시_해당학기신청자목록반환() {
    // Given
    StudyApplicant currentApplicant =
        TestDataFactory.createStudyApplicant(currentTerm, user1, List.of(user2), List.of(course1));
    StudyApplicant pastApplicant =
        TestDataFactory.createStudyApplicant(pastTerm, user2, List.of(user1), List.of(course2));

    studyApplicantRepository.save(currentApplicant);
    studyApplicantRepository.save(pastApplicant);

    // When
    List<StudyApplicant> currentTermApplicants =
        studyApplicantRepository.findAllByTerm(currentTerm);
    List<StudyApplicant> pastTermApplicants = studyApplicantRepository.findAllByTerm(pastTerm);

    // Then
    assertThat(currentTermApplicants).hasSize(1);
    assertThat(currentTermApplicants.get(0).getAcademicTerm()).isEqualTo(currentTerm);

    assertThat(pastTermApplicants).hasSize(1);
    assertThat(pastTermApplicants.get(0).getAcademicTerm()).isEqualTo(pastTerm);
  }

  @Test
  void 스터디그룹별신청자조회시_해당그룹신청자목록반환() {
    // Given
    StudyApplicant applicant1 =
        TestDataFactory.createStudyApplicant(currentTerm, user1, List.of(user2), List.of(course1));
    StudyApplicant applicant2 =
        TestDataFactory.createStudyApplicant(currentTerm, user2, List.of(user1), List.of(course1));
    StudyApplicant applicant3 =
        TestDataFactory.createStudyApplicant(currentTerm, user3, List.of(), List.of(course2));

    studyApplicantRepository.save(applicant1);
    studyApplicantRepository.save(applicant2);
    studyApplicantRepository.save(applicant3);

    StudyGroup studyGroup1 = StudyGroup.of(1, currentTerm, List.of(applicant1, applicant2));
    StudyGroup studyGroup2 = StudyGroup.of(2, currentTerm, List.of(applicant3));

    studyGroupRepository.save(studyGroup1);
    studyGroupRepository.save(studyGroup2);

    // When
    List<StudyApplicant> group1Applicants =
        studyApplicantRepository.findAllByStudyGroup(studyGroup1);
    List<StudyApplicant> group2Applicants =
        studyApplicantRepository.findAllByStudyGroup(studyGroup2);

    // Then
    assertThat(group1Applicants).hasSize(2);
    assertThat(group1Applicants).extracting("user").containsExactlyInAnyOrder(user1, user2);

    assertThat(group2Applicants).hasSize(1);
    assertThat(group2Applicants.get(0).getUser()).isEqualTo(user3);
  }

  @Test
  void 새신청서저장시_저장된신청서반환() {
    // Given
    StudyApplicant applicant =
        TestDataFactory.createStudyApplicant(
            currentTerm, user1, List.of(user2, user3), List.of(course1, course2));

    // When
    StudyApplicant savedApplicant = studyApplicantRepository.save(applicant);

    // Then
    assertThat(savedApplicant.getStudyApplicantId()).isNotNull();
    assertThat(savedApplicant.getUser()).isEqualTo(user1);
    assertThat(savedApplicant.getAcademicTerm()).isEqualTo(currentTerm);
    assertThat(savedApplicant.getPartnerRequests()).hasSize(2);
    assertThat(savedApplicant.getPreferredCourses()).hasSize(2);
  }

  @Test
  void 신청서삭제시_삭제성공() {
    // Given
    StudyApplicant applicant =
        TestDataFactory.createStudyApplicant(currentTerm, user1, List.of(user2), List.of(course1));
    StudyApplicant savedApplicant = studyApplicantRepository.save(applicant);

    // When
    studyApplicantRepository.delete(savedApplicant);
    flushAndClear();

    // Then
    Optional<StudyApplicant> deletedApplicant =
        studyApplicantRepository.findByUserAndTerm(user1, currentTerm);
    assertThat(deletedApplicant).isEmpty();
  }

  @Test
  void 신청자없는학기조회시_모든조회빈결과반환() {
    // Given - 다른 학기 신청자만 존재
    StudyApplicant pastApplicant =
        TestDataFactory.createStudyApplicant(pastTerm, user1, List.of(user2), List.of(course1));
    studyApplicantRepository.save(pastApplicant);

    // When
    List<StudyApplicant> currentTermApplicants =
        studyApplicantRepository.findAllByTerm(currentTerm);
    List<StudyApplicant> unassignedApplicants =
        studyApplicantRepository.findUnassignedApplicants(currentTerm);
    List<StudyApplicant> assignedApplicants =
        studyApplicantRepository.findAssignedApplicants(currentTerm);

    // Then
    assertThat(currentTermApplicants).isEmpty();
    assertThat(unassignedApplicants).isEmpty();
    assertThat(assignedApplicants).isEmpty();
  }

  @Test
  void 복수친구요청포함신청서저장시_모든요청저장성공() {
    // Given
    StudyApplicant applicant =
        TestDataFactory.createStudyApplicant(
            currentTerm, user1, List.of(user2, user3), List.of(course1, course2));

    // When
    StudyApplicant savedApplicant = studyApplicantRepository.save(applicant);

    // Then
    assertThat(savedApplicant.getPartnerRequests()).hasSize(2);
    assertThat(savedApplicant.getRequestedUsers()).containsExactlyInAnyOrder(user2, user3);
    assertThat(savedApplicant.getPreferredCourses()).hasSize(2);
  }
}
