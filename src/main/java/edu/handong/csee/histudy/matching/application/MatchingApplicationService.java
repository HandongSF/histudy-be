package edu.handong.csee.histudy.matching.application;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.matching.domain.MatchingPolicy;
import edu.handong.csee.histudy.repository.AcademicTermRepository;
import edu.handong.csee.histudy.repository.StudyApplicantRepository;
import edu.handong.csee.histudy.repository.StudyGroupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MatchingApplicationService {

  private final AcademicTermRepository academicTermRepository;
  private final StudyApplicantRepository studyApplicantRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final MatchingPolicy matchingPolicy = new MatchingPolicy();

  public void match() {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<StudyApplicant> applicants =
        studyApplicantRepository.findUnassignedApplicants(currentTerm);

    if (applicants.isEmpty()) {
      return;
    }

    int latestGroupTag = studyGroupRepository.countMaxTag(currentTerm).orElse(0);
    List<StudyGroup> matchedGroups =
        matchingPolicy.match(applicants, currentTerm, latestGroupTag + 1);

    if (!matchedGroups.isEmpty()) {
      studyGroupRepository.saveAll(matchedGroups);
    }
  }
}
