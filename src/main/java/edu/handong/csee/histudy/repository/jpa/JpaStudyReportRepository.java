package edu.handong.csee.histudy.repository.jpa;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaStudyReportRepository extends JpaRepository<StudyReport, Long> {

  List<StudyReport> findAllByStudyGroupOrderByCreatedDateDesc(StudyGroup studyGroup);

  boolean existsByStudyGroup(StudyGroup studyGroup);

  long countByStudyGroupAcademicTerm(AcademicTerm academicTerm);

  @Query("select coalesce(sum(r.totalMinutes), 0) from StudyReport r")
  long sumTotalMinutes();

  @Query(
      "select coalesce(sum(r.totalMinutes), 0) from StudyReport r where r.studyGroup.academicTerm = :academicTerm")
  long sumTotalMinutesByStudyGroupAcademicTerm(@Param("academicTerm") AcademicTerm academicTerm);
}
