package edu.handong.csee.histudy.repository.impl;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.repository.StudyApplicantRepository;
import edu.handong.csee.histudy.repository.jpa.JpaStudyApplicantRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyApplicationRepositoryImpl implements StudyApplicantRepository {
  private final JpaStudyApplicantRepository repository;

  @Override
  public Optional<StudyApplicant> findByUserAndTerm(User applicant, AcademicTerm currentTerm) {
    return repository.findByUserAndTerm(applicant, currentTerm);
  }

  @Override
  public List<StudyApplicant> findUnassignedApplicants(AcademicTerm currentTerm) {
    return repository.findUnassignedApplicants(currentTerm);
  }

  @Override
  public List<StudyApplicant> findAssignedApplicants(AcademicTerm currentTerm) {
    return repository.findAssignedApplicants(currentTerm);
  }

  @Override
  public long countAssignedApplicants(AcademicTerm currentTerm) {
    return repository.countAssignedApplicants(currentTerm);
  }

  @Override
  public List<StudyApplicant> findAllByTerm(AcademicTerm currentTerm) {
    return repository.findAllByTerm(currentTerm);
  }

  @Override
  public List<StudyApplicant> findAllByStudyGroup(StudyGroup group) {
    return repository.findAllByStudyGroup(group);
  }

  @Override
  public StudyApplicant save(StudyApplicant applicant) {
    return repository.save(applicant);
  }

  @Override
  public List<StudyApplicant> saveAll(Iterable<StudyApplicant> entities) {
    return repository.saveAll(entities);
  }

  @Override
  public void delete(StudyApplicant applicant) {
    repository.delete(applicant);
  }
}
