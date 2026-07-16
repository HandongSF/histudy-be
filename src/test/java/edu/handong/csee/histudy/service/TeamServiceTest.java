package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.dto.ReportDto;
import edu.handong.csee.histudy.dto.TeamDto;
import edu.handong.csee.histudy.dto.TeamRankDto;
import edu.handong.csee.histudy.dto.TeamReportDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.matching.application.MatchingApplicationService;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyApplicationRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyGroupRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyReportRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeUserRepository;
import edu.handong.csee.histudy.util.ImagePathMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TeamServiceTest {

  private final AcademicTerm currentTerm =
      AcademicTerm.builder().academicYear(2025).semester(TermType.SPRING).isCurrent(true).build();
  private final Course commonCourse = createCourse(1L, "자료구조", "CSEE201", "Kim", currentTerm);
  private final User memberUser =
      User.builder()
          .sub("sub-1")
          .sid("22230001")
          .email("member@histudy.com")
          .name("Member")
          .role(Role.USER)
          .build();
  private final User memberOneUser =
      User.builder()
          .sub("sub-2")
          .sid("22230002")
          .email("member1@histudy.com")
          .name("Member1")
          .role(Role.USER)
          .build();
  private final User memberTwoUser =
      User.builder()
          .sub("sub-3")
          .sid("22230003")
          .email("member2@histudy.com")
          .name("Member2")
          .role(Role.USER)
          .build();
  private final User teamOneUser =
      User.builder()
          .sub("sub-4")
          .sid("22230004")
          .email("team1@histudy.com")
          .name("Team1")
          .role(Role.USER)
          .build();
  private final User teamTwoUser =
      User.builder()
          .sub("sub-5")
          .sid("22230005")
          .email("team2@histudy.com")
          .name("Team2")
          .role(Role.USER)
          .build();

  private FakeStudyGroupRepository studyGroupRepository;
  private FakeUserRepository userRepository;
  private FakeAcademicTermRepository academicTermRepository;
  private FakeStudyApplicationRepository studyApplicantRepository;
  private FakeStudyReportRepository studyReportRepository;
  private ImagePathMapper imagePathMapper;
  private TeamService teamService;

  @BeforeEach
  void setUp() {
    studyGroupRepository = new FakeStudyGroupRepository();
    userRepository = new FakeUserRepository();
    academicTermRepository = new FakeAcademicTermRepository();
    studyApplicantRepository = new FakeStudyApplicationRepository();
    studyReportRepository = new FakeStudyReportRepository();
    imagePathMapper = new ImagePathMapper();
    ReflectionTestUtils.setField(imagePathMapper, "origin", "https://histudy.handong.edu");
    ReflectionTestUtils.setField(imagePathMapper, "imageBasePath", "/images");
    MatchingApplicationService matchingApplicationService =
        new MatchingApplicationService(
            academicTermRepository, studyApplicantRepository, studyGroupRepository);
    teamService =
        new TeamService(
            studyGroupRepository,
            userRepository,
            academicTermRepository,
            studyApplicantRepository,
            studyReportRepository,
            imagePathMapper,
            matchingApplicationService);
  }

  @Test
  void 그룹을_자동_배정하면_매칭애플리케이션서비스에_위임한다() {
    // Given
    MatchingApplicationService matchingApplicationService =
        mock(MatchingApplicationService.class);
    TeamService facade =
        new TeamService(
            studyGroupRepository,
            userRepository,
            academicTermRepository,
            studyApplicantRepository,
            studyReportRepository,
            imagePathMapper,
            matchingApplicationService);

    // When
    facade.matchTeam();

    // Then
    verify(matchingApplicationService).match();
  }

  @Test
  void 배정된_그룹_목록을_조회하면_보고서수와_누적시간을_함께_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User member = userRepository.save(memberUser);
    Course course = commonCourse;
    StudyApplicant applicant = StudyApplicant.of(currentTerm, member, List.of(), List.of(course));
    studyApplicantRepository.save(applicant);
    StudyGroup group = studyGroupRepository.save(StudyGroup.of(7, currentTerm, List.of(applicant)));
    studyReportRepository.save(
        StudyReport.builder()
            .title("1주차")
            .content("첫 모임")
            .totalMinutes(90)
            .studyGroup(group)
            .participants(List.of(member))
            .images(List.of("reports/report1.png"))
            .courses(List.of(course))
            .build());
    studyReportRepository.save(
        StudyReport.builder()
            .title("2주차")
            .content("둘째 모임")
            .totalMinutes(120)
            .studyGroup(group)
            .participants(List.of(member))
            .images(List.of("reports/report2.png"))
            .courses(List.of(course))
            .build());

    // When
    List<TeamDto> result = teamService.getTeams("member@histudy.com");

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTag()).isEqualTo(7);
    assertThat(result.get(0).getReports()).isEqualTo(2);
    assertThat(result.get(0).getTimes()).isEqualTo(210);
  }

  @Test
  void 같은_그룹의_멤버_정보를_조회하면_마스킹된_멤버정보를_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    Course course = commonCourse;
    User memberOne = userRepository.save(memberOneUser);
    User memberTwo = userRepository.save(memberTwoUser);
    StudyApplicant applicantOne =
        StudyApplicant.of(currentTerm, memberOne, List.of(), List.of(course));
    StudyApplicant applicantTwo =
        StudyApplicant.of(currentTerm, memberTwo, List.of(), List.of(course));
    studyApplicantRepository.saveAll(List.of(applicantOne, applicantTwo));
    studyGroupRepository.save(StudyGroup.of(3, currentTerm, List.of(applicantOne, applicantTwo)));

    // When
    List<UserDto.UserMeWithMasking> result = teamService.getTeamUsers("member1@histudy.com");

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(UserDto.UserMeWithMasking::getSid)
        .containsExactlyInAnyOrder("222****2", "222****3");
    assertThat(result).allMatch(user -> user.getTag().equals(3));
  }

  @Test
  void 그룹_랭킹을_조회하면_누적시간_내림차순으로_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    Course course = commonCourse;
    User savedTeamOneUser = userRepository.save(teamOneUser);
    User savedTeamTwoUser = userRepository.save(teamTwoUser);
    StudyApplicant applicantOne =
        StudyApplicant.of(currentTerm, savedTeamOneUser, List.of(), List.of(course));
    StudyApplicant applicantTwo =
        StudyApplicant.of(currentTerm, savedTeamTwoUser, List.of(), List.of(course));
    studyApplicantRepository.saveAll(List.of(applicantOne, applicantTwo));
    StudyGroup groupOne =
        studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(applicantOne)));
    StudyGroup groupTwo =
        studyGroupRepository.save(StudyGroup.of(2, currentTerm, List.of(applicantTwo)));
    StudyReport firstReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("A")
                .content("A")
                .totalMinutes(60)
                .studyGroup(groupOne)
                .participants(List.of(savedTeamOneUser))
                .images(List.of("reports/one.png"))
                .courses(List.of(course))
                .build());
    StudyReport secondReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("B")
                .content("B")
                .totalMinutes(180)
                .studyGroup(groupTwo)
                .participants(List.of(savedTeamTwoUser))
                .images(List.of("reports/two.png"))
                .courses(List.of(course))
                .build());
    ReflectionTestUtils.setField(
        firstReport.getImages().get(0), "createdDate", LocalDateTime.now().minusDays(1));
    ReflectionTestUtils.setField(
        secondReport.getImages().get(0), "createdDate", LocalDateTime.now());

    // When
    TeamRankDto result = teamService.getAllTeams();

    // Then
    assertThat(result.getTeams()).hasSize(2);
    assertThat(result.getTeams().get(0).getId()).isEqualTo(2);
    assertThat(result.getTeams().get(0).getTotalMinutes()).isEqualTo(180);
    assertThat(result.getTeams().get(0).getThumbnail())
        .isEqualTo("https://histudy.handong.edu/images/reports/two.png");
  }

  @Test
  void 그룹의_활동_보고서_목록을_조회하면_총시간과_보고서목록을_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    Course course = commonCourse;
    User member = userRepository.save(memberUser);
    StudyApplicant applicant = StudyApplicant.of(currentTerm, member, List.of(), List.of(course));
    studyApplicantRepository.save(applicant);
    StudyGroup group = studyGroupRepository.save(StudyGroup.of(8, currentTerm, List.of(applicant)));
    StudyReport firstReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("1주차")
                .content("스터디")
                .totalMinutes(75)
                .studyGroup(group)
                .participants(List.of(member))
                .images(List.of("reports/one.png"))
                .courses(List.of(course))
                .build());
    StudyReport secondReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("2주차")
                .content("스터디")
                .totalMinutes(45)
                .studyGroup(group)
                .participants(List.of(member))
                .images(List.of("reports/two.png"))
                .courses(List.of(course))
                .build());
    ReflectionTestUtils.setField(firstReport, "createdDate", LocalDateTime.of(2025, 3, 10, 9, 0));
    ReflectionTestUtils.setField(secondReport, "createdDate", LocalDateTime.of(2025, 3, 17, 9, 0));

    // When
    TeamReportDto result =
        teamService.getTeamReports(group.getStudyGroupId(), "member@histudy.com");

    // Then
    assertThat(result.getTag()).isEqualTo(8);
    assertThat(result.getMembers()).hasSize(1);
    assertThat(result.getTotalTime()).isEqualTo(120);
    assertThat(result.getReports()).hasSize(2);
    assertThat(result.getReports())
        .extracting(ReportDto.ReportBasic::getTitle)
        .containsExactly("2주차", "1주차");
  }

  @Test
  void 현재_학기_없이_그룹_랭킹을_조회하면_예외가_발생한다() {
    // Given

    // When Then
    assertThatThrownBy(() -> teamService.getAllTeams())
        .isInstanceOf(NoCurrentTermFoundException.class);
  }

  private Course createCourse(
      Long courseId, String name, String code, String professor, AcademicTerm academicTerm) {
    Course course =
        Course.builder()
            .name(name)
            .code(code)
            .professor(professor)
            .academicTerm(academicTerm)
            .build();
    ReflectionTestUtils.setField(course, "courseId", courseId);
    return course;
  }
}
