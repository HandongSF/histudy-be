package edu.handong.csee.histudy.repository.jpa;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaStudyApplicantRepository extends JpaRepository<StudyApplicant, Long> {

  @Query(
      "select s from StudyApplicant s left join fetch s.preferredCourses "
          + "where s.user = :applicant and s.academicTerm = :currentTerm")
  Optional<StudyApplicant> findByUserAndTerm(User applicant, AcademicTerm currentTerm);

  @Query(
      "select s from StudyApplicant s left join fetch s.preferredCourses "
          + "where s.academicTerm = :currentTerm and s.studyGroup is null")
  List<StudyApplicant> findUnassignedApplicants(AcademicTerm currentTerm);

  @Query(
      "select s from StudyApplicant s join fetch s.user "
          + "where s.academicTerm = :currentTerm and s.studyGroup is not null")
  List<StudyApplicant> findAssignedApplicants(AcademicTerm currentTerm);

  @Query(
      "select count(s) from StudyApplicant s "
          + "where s.academicTerm = :currentTerm and s.studyGroup is not null")
  long countAssignedApplicants(@Param("currentTerm") AcademicTerm currentTerm);

  @Query(
      "select s from StudyApplicant s left join fetch s.preferredCourses "
          + "where s.academicTerm = :currentTerm")
  List<StudyApplicant> findAllByTerm(AcademicTerm currentTerm);

  @Query(
      "select s from StudyApplicant s left join fetch s.preferredCourses "
          + "where s.studyGroup = :studyGroup")
  List<StudyApplicant> findAllByStudyGroup(@Param("studyGroup") StudyGroup group);
}
