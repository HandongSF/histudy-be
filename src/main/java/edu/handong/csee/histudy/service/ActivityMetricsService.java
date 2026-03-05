package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.ActivityMetricsDto;
import edu.handong.csee.histudy.dto.ActivityTerm;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.repository.AcademicTermRepository;
import edu.handong.csee.histudy.repository.StudyApplicantRepository;
import edu.handong.csee.histudy.repository.StudyGroupRepository;
import edu.handong.csee.histudy.repository.StudyReportRepository;
import edu.handong.csee.histudy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityMetricsService {

  private final UserRepository userRepository;
  private final StudyApplicantRepository studyApplicantRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final StudyReportRepository studyReportRepository;
  private final AcademicTermRepository academicTermRepository;

  public ActivityMetricsDto getActivityMetrics(ActivityTerm term) {
    if (term == ActivityTerm.CURRENT) {
      return getCurrentTermActivityMetrics();
    }
    return getAllActivityMetrics();
  }

  private ActivityMetricsDto getAllActivityMetrics() {
    long studyMembers = userRepository.countByRoleNot(Role.ADMIN);
    long studyGroups = studyGroupRepository.count();
    long totalMinutes = studyReportRepository.sumTotalMinutes();
    long studyHours = totalMinutes / 60;
    long reports = studyReportRepository.count();

    return ActivityMetricsDto.builder()
        .studyMembers(studyMembers)
        .studyGroups(studyGroups)
        .studyHours(studyHours)
        .reports(reports)
        .build();
  }

  private ActivityMetricsDto getCurrentTermActivityMetrics() {
    AcademicTerm currentTerm =
        academicTermRepository
            .findCurrentSemester()
            .orElseThrow(NoCurrentTermFoundException::new);

    long studyMembers = studyApplicantRepository.countAssignedApplicants(currentTerm);
    long studyGroups = studyGroupRepository.countByAcademicTerm(currentTerm);
    long totalMinutes = studyReportRepository.sumTotalMinutesByStudyGroupAcademicTerm(currentTerm);
    long studyHours = totalMinutes / 60;
    long reports = studyReportRepository.countByStudyGroupAcademicTerm(currentTerm);

    return ActivityMetricsDto.builder()
        .studyMembers(studyMembers)
        .studyGroups(studyGroups)
        .studyHours(studyHours)
        .reports(reports)
        .build();
  }
}
