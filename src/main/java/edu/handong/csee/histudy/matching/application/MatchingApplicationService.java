package edu.handong.csee.histudy.matching.application;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.matching.domain.MatchingPolicy;
import edu.handong.csee.histudy.observability.audit.AuditContext;
import edu.handong.csee.histudy.observability.audit.MatchingExecutedEvent;
import edu.handong.csee.histudy.repository.AcademicTermRepository;
import edu.handong.csee.histudy.repository.StudyApplicantRepository;
import edu.handong.csee.histudy.repository.StudyGroupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MatchingApplicationService {

  private final AcademicTermRepository academicTermRepository;
  private final StudyApplicantRepository studyApplicantRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final MatchingPolicy matchingPolicy = new MatchingPolicy();

  public void match(AuditContext context) {
    long startedAt = System.nanoTime();
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<StudyApplicant> applicants =
        studyApplicantRepository.findUnassignedApplicants(currentTerm);

    List<StudyGroup> matchedGroups = List.of();
    if (!applicants.isEmpty()) {
      int latestGroupTag = studyGroupRepository.countMaxTag(currentTerm).orElse(0);
      matchedGroups = matchingPolicy.match(applicants, currentTerm, latestGroupTag + 1);
    }

    if (!matchedGroups.isEmpty()) {
      studyGroupRepository.saveAll(matchedGroups);
    }
    int assignedCount = matchedGroups.stream().mapToInt(group -> group.getMembers().size()).sum();
    eventPublisher.publishEvent(
        new MatchingExecutedEvent(
            context,
            currentTerm.getAcademicTermId(),
            applicants.size(),
            assignedCount,
            matchedGroups.size(),
            (System.nanoTime() - startedAt) / 1_000_000));
  }
}
