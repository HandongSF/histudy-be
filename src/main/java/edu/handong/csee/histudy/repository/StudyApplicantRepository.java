package edu.handong.csee.histudy.repository;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.User;
import java.util.List;
import java.util.Optional;

public interface StudyApplicantRepository {
  Optional<StudyApplicant> findByUserAndTerm(User applicant, AcademicTerm currentTerm);

  List<StudyApplicant> findUnassignedApplicants(AcademicTerm currentTerm);

  List<StudyApplicant> findAssignedApplicants(AcademicTerm currentTerm);

  long countAssignedApplicants(AcademicTerm currentTerm);

  List<StudyApplicant> findAllByTerm(AcademicTerm currentTerm);

  List<StudyApplicant> findAllByStudyGroup(StudyGroup group);

  StudyApplicant save(StudyApplicant entity);

  List<StudyApplicant> saveAll(Iterable<StudyApplicant> entities);

  void delete(StudyApplicant applicant);
}
