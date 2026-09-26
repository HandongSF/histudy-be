package edu.handong.csee.histudy.repository;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import java.util.List;
import java.util.Optional;

public interface StudyReportRepository {
  List<StudyReport> findAllByStudyGroupOrderByCreatedDateDesc(StudyGroup studyGroup);

  boolean existsByStudyGroup(StudyGroup studyGroup);

  Optional<StudyReport> findById(Long id);

  void delete(StudyReport report);

  StudyReport save(StudyReport report);

  long count();

  long countByStudyGroupAcademicTerm(AcademicTerm academicTerm);

  long sumTotalMinutes();

  long sumTotalMinutesByStudyGroupAcademicTerm(AcademicTerm academicTerm);
}
