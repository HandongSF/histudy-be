package edu.handong.csee.histudy.repository.impl;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.repository.StudyReportRepository;
import edu.handong.csee.histudy.repository.jpa.JpaStudyReportRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyReportRepositoryImpl implements StudyReportRepository {
  private final JpaStudyReportRepository repository;

  @Override
  public List<StudyReport> findAllByStudyGroupOrderByCreatedDateDesc(StudyGroup studyGroup) {
    return repository.findAllByStudyGroupOrderByCreatedDateDesc(studyGroup);
  }

  @Override
  public boolean existsByStudyGroup(StudyGroup studyGroup) {
    return repository.existsByStudyGroup(studyGroup);
  }

  @Override
  public Optional<StudyReport> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public void delete(StudyReport report) {
    repository.delete(report);
  }

  @Override
  public StudyReport save(StudyReport report) {
    return repository.save(report);
  }

  @Override
  public long count() {
    return repository.count();
  }

  @Override
  public long countByStudyGroupAcademicTerm(AcademicTerm academicTerm) {
    return repository.countByStudyGroupAcademicTerm(academicTerm);
  }

  @Override
  public long sumTotalMinutes() {
    return repository.sumTotalMinutes();
  }

  @Override
  public long sumTotalMinutesByStudyGroupAcademicTerm(AcademicTerm academicTerm) {
    return repository.sumTotalMinutesByStudyGroupAcademicTerm(academicTerm);
  }
}
