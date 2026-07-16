package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.dto.ReportDto;
import edu.handong.csee.histudy.service.command.ReportCommand;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeCourseRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyGroupRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyReportRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeUserRepository;
import edu.handong.csee.histudy.util.ImagePathMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReportServiceTest {

  private final AcademicTerm currentTerm =
      AcademicTerm.builder().academicYear(2025).semester(TermType.SPRING).isCurrent(true).build();
  private final User memberUser =
      User.builder()
          .sub("sub-1")
          .sid("22230001")
          .email("member@histudy.com")
          .name("Member")
          .role(Role.USER)
          .build();
  private final User participantUser =
      User.builder()
          .sub("sub-2")
          .sid("22230002")
          .email("participant@histudy.com")
          .name("Participant")
          .role(Role.USER)
          .build();
  private final Course primaryCourse =
      Course.builder()
          .name("자료구조")
          .code("CSEE201")
          .professor("Kim")
          .academicTerm(currentTerm)
          .build();
  private final Course secondaryCourse =
      Course.builder()
          .name("운영체제")
          .code("CSEE301")
          .professor("Lee")
          .academicTerm(currentTerm)
          .build();

  private FakeStudyReportRepository studyReportRepository;
  private FakeUserRepository userRepository;
  private FakeCourseRepository courseRepository;
  private FakeStudyGroupRepository studyGroupRepository;
  private FakeAcademicTermRepository academicTermRepository;
  private ReportService reportService;

  @BeforeEach
  void setUp() {
    studyReportRepository = new FakeStudyReportRepository();
    userRepository = new FakeUserRepository();
    courseRepository = new FakeCourseRepository();
    studyGroupRepository = new FakeStudyGroupRepository();
    academicTermRepository = new FakeAcademicTermRepository();
    ImagePathMapper imagePathMapper = new ImagePathMapper();
    ReflectionTestUtils.setField(imagePathMapper, "origin", "https://histudy.handong.edu");
    ReflectionTestUtils.setField(imagePathMapper, "imageBasePath", "/images");
    reportService =
        new ReportService(
            studyReportRepository,
            userRepository,
            courseRepository,
            studyGroupRepository,
            academicTermRepository,
            imagePathMapper);
  }

  @Test
  void 활동_보고서를_작성하면_참여자와_과목과_이미지가_저장된다() {
    // Given
    academicTermRepository.save(currentTerm);
    User savedMemberUser = userRepository.save(memberUser);
    User savedParticipantUser = userRepository.save(participantUser);
    List<Course> savedCourses = courseRepository.saveAll(List.of(primaryCourse, secondaryCourse));
    Course savedPrimaryCourse = savedCourses.get(0);
    StudyApplicant applicant =
        StudyApplicant.of(currentTerm, savedMemberUser, List.of(), List.of(savedPrimaryCourse));
    studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(applicant)));
    ReportCommand command =
        new ReportCommand(
            "1주차",
            "첫 모임",
            90L,
            List.of(savedParticipantUser.getUserId()),
            List.of("https://histudy.handong.edu/images/reports/report1.png"),
            List.of(savedPrimaryCourse.getCourseId()));

    // When
    ReportDto.ReportInfo result = reportService.createReport(command, "member@histudy.com");

    // Then
    assertThat(studyReportRepository.findAll()).hasSize(1);
    StudyReport savedReport = studyReportRepository.findAll().get(0);
    assertThat(savedReport.getTitle()).isEqualTo("1주차");
    assertThat(savedReport.getParticipants()).hasSize(1);
    assertThat(savedReport.getCourses()).hasSize(1);
    assertThat(savedReport.getImages())
        .extracting(image -> image.getPath())
        .containsExactly("reports/report1.png");
    assertThat(result.getImages()).hasSize(1);
    assertThat(result.getImages().get(0).getUrl())
        .isEqualTo("https://histudy.handong.edu/images/reports/report1.png");
  }

  @Test
  void 활동_보고서_목록을_조회하면_최신순으로_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User savedMemberUser = userRepository.save(memberUser);
    List<Course> savedCourses = courseRepository.saveAll(List.of(primaryCourse, secondaryCourse));
    Course savedPrimaryCourse = savedCourses.get(0);
    StudyApplicant applicant =
        StudyApplicant.of(currentTerm, savedMemberUser, List.of(), List.of(savedPrimaryCourse));
    StudyGroup savedStudyGroup =
        studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(applicant)));
    StudyReport first =
        studyReportRepository.save(
            StudyReport.builder()
                .title("1주차")
                .content("첫 모임")
                .totalMinutes(90)
                .studyGroup(savedStudyGroup)
                .participants(List.of(savedMemberUser))
                .images(List.of("reports/one.png"))
                .courses(List.of(savedPrimaryCourse))
                .build());
    StudyReport second =
        studyReportRepository.save(
            StudyReport.builder()
                .title("2주차")
                .content("둘째 모임")
                .totalMinutes(60)
                .studyGroup(savedStudyGroup)
                .participants(List.of(savedMemberUser))
                .images(List.of("reports/two.png"))
                .courses(List.of(savedPrimaryCourse))
                .build());
    ReflectionTestUtils.setField(first, "createdDate", java.time.LocalDateTime.now().minusDays(1));
    ReflectionTestUtils.setField(second, "createdDate", java.time.LocalDateTime.now());

    // When
    List<ReportDto.ReportInfo> result = reportService.getReports("member@histudy.com");

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).extracting(ReportDto.ReportInfo::getTitle).containsExactly("2주차", "1주차");
  }

  @Test
  void 활동_보고서_상세를_조회하면_상세_정보를_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User savedMemberUser = userRepository.save(memberUser);
    List<Course> savedCourses = courseRepository.saveAll(List.of(primaryCourse, secondaryCourse));
    Course savedPrimaryCourse = savedCourses.get(0);
    StudyApplicant applicant =
        StudyApplicant.of(currentTerm, savedMemberUser, List.of(), List.of(savedPrimaryCourse));
    StudyGroup savedStudyGroup =
        studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(applicant)));
    StudyReport savedReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("1주차")
                .content("첫 모임")
                .totalMinutes(90)
                .studyGroup(savedStudyGroup)
                .participants(List.of(savedMemberUser))
                .images(List.of("reports/one.png"))
                .courses(List.of(savedPrimaryCourse))
                .build());

    // When
    ReportDto.ReportInfo result =
        reportService.getReport(savedReport.getStudyReportId()).orElseThrow();

    // Then
    assertThat(result.getTitle()).isEqualTo("1주차");
    assertThat(result.getParticipants()).hasSize(1);
    assertThat(result.getImages()).hasSize(1);
  }

  @Test
  void 작성된_보고서를_업데이트하면_내용과_이미지가_변경된다() {
    // Given
    academicTermRepository.save(currentTerm);
    User savedMemberUser = userRepository.save(memberUser);
    List<Course> savedCourses = courseRepository.saveAll(List.of(primaryCourse, secondaryCourse));
    Course savedPrimaryCourse = savedCourses.get(0);
    Course savedSecondaryCourse = savedCourses.get(1);
    StudyApplicant applicant =
        StudyApplicant.of(
            currentTerm,
            savedMemberUser,
            List.of(),
            List.of(savedPrimaryCourse, savedSecondaryCourse));
    StudyGroup savedStudyGroup =
        studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(applicant)));
    StudyReport savedReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("1주차")
                .content("첫 모임")
                .totalMinutes(90)
                .studyGroup(savedStudyGroup)
                .participants(List.of(savedMemberUser))
                .images(List.of("reports/one.png"))
                .courses(List.of(savedPrimaryCourse))
                .build());
    ReportCommand command =
        new ReportCommand(
            "수정된 제목",
            "수정된 내용",
            120L,
            List.of(savedMemberUser.getUserId()),
            List.of("https://histudy.handong.edu/images/reports/two.png"),
            List.of(savedSecondaryCourse.getCourseId()));

    // When
    boolean updated = reportService.updateReport(savedReport.getStudyReportId(), command);

    // Then
    assertThat(updated).isTrue();
    StudyReport updatedReport =
        studyReportRepository.findById(savedReport.getStudyReportId()).orElseThrow();
    assertThat(updatedReport.getTitle()).isEqualTo("수정된 제목");
    assertThat(updatedReport.getTotalMinutes()).isEqualTo(120);
    assertThat(updatedReport.getCourses())
        .extracting(studyCourse -> studyCourse.getCourse().getName())
        .containsExactly("운영체제");
    assertThat(updatedReport.getImages())
        .extracting(image -> image.getPath())
        .containsExactly("reports/two.png");
  }

  @Test
  void 작성된_보고서를_삭제하면_보고서가_제거된다() {
    // Given
    academicTermRepository.save(currentTerm);
    User savedMemberUser = userRepository.save(memberUser);
    List<Course> savedCourses = courseRepository.saveAll(List.of(primaryCourse, secondaryCourse));
    Course savedPrimaryCourse = savedCourses.get(0);
    StudyApplicant applicant =
        StudyApplicant.of(currentTerm, savedMemberUser, List.of(), List.of(savedPrimaryCourse));
    StudyGroup savedStudyGroup =
        studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(applicant)));
    StudyReport savedReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("1주차")
                .content("첫 모임")
                .totalMinutes(90)
                .studyGroup(savedStudyGroup)
                .participants(List.of(savedMemberUser))
                .images(List.of("reports/one.png"))
                .courses(List.of(savedPrimaryCourse))
                .build());

    // When
    boolean deleted = reportService.deleteReport(savedReport.getStudyReportId());

    // Then
    assertThat(deleted).isTrue();
    assertThat(studyReportRepository.findAll()).isEmpty();
    assertThat(reportService.deleteReport(999L)).isFalse();
  }
}
