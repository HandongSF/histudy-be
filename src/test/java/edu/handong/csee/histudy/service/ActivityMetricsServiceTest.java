package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityMetricsServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private StudyApplicantRepository studyApplicantRepository;
  @Mock private StudyGroupRepository studyGroupRepository;
  @Mock private StudyReportRepository studyReportRepository;
  @Mock private AcademicTermRepository academicTermRepository;

  @InjectMocks private ActivityMetricsService activityMetricsService;

  @Test
  void 전체학기지표조회시_관리자제외전체기준으로반환() {
    // Given
    when(userRepository.countByRoleNot(Role.ADMIN)).thenReturn(10L);
    when(studyGroupRepository.count()).thenReturn(3L);
    when(studyReportRepository.sumTotalMinutes()).thenReturn(185L);
    when(studyReportRepository.count()).thenReturn(7L);

    // When
    ActivityMetricsDto result = activityMetricsService.getActivityMetrics(ActivityTerm.ALL);

    // Then
    assertThat(result.getStudyMembers()).isEqualTo(10L);
    assertThat(result.getStudyGroups()).isEqualTo(3L);
    assertThat(result.getStudyHours()).isEqualTo(3L);
    assertThat(result.getReports()).isEqualTo(7L);
    verifyNoInteractions(studyApplicantRepository, academicTermRepository);
  }

  @Test
  void 현재학기지표조회시_현재학기배정인원기준으로반환() {
    // Given
    AcademicTerm currentTerm = mock(AcademicTerm.class);
    when(academicTermRepository.findCurrentSemester()).thenReturn(Optional.of(currentTerm));
    when(studyApplicantRepository.countAssignedApplicants(currentTerm)).thenReturn(6L);
    when(studyGroupRepository.countByAcademicTerm(currentTerm)).thenReturn(2L);
    when(studyReportRepository.sumTotalMinutesByStudyGroupAcademicTerm(currentTerm)).thenReturn(150L);
    when(studyReportRepository.countByStudyGroupAcademicTerm(currentTerm)).thenReturn(4L);

    // When
    ActivityMetricsDto result = activityMetricsService.getActivityMetrics(ActivityTerm.CURRENT);

    // Then
    assertThat(result.getStudyMembers()).isEqualTo(6L);
    assertThat(result.getStudyGroups()).isEqualTo(2L);
    assertThat(result.getStudyHours()).isEqualTo(2L);
    assertThat(result.getReports()).isEqualTo(4L);
    verify(userRepository, never()).countByRole(Role.MEMBER);
  }

  @Test
  void 현재학기지표조회시_현재학기없으면_예외발생() {
    // Given
    when(academicTermRepository.findCurrentSemester()).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> activityMetricsService.getActivityMetrics(ActivityTerm.CURRENT))
        .isInstanceOf(NoCurrentTermFoundException.class);
  }
}
