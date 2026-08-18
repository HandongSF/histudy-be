package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.exception.ReportNotFoundException;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyApplicationRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyGroupRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyReportRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeUserRepository;
import edu.handong.csee.histudy.util.ImagePathMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ImageServiceTest {

  @TempDir Path tempDir;

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
  private final Course commonCourse =
      Course.builder()
          .name("자료구조")
          .code("CSEE201")
          .professor("Kim")
          .academicTerm(currentTerm)
          .build();
  private final byte[] pngBytes = createPngBytes();

  private FakeAcademicTermRepository academicTermRepository;
  private FakeUserRepository userRepository;
  private FakeStudyReportRepository studyReportRepository;
  private FakeStudyApplicationRepository studyApplicantRepository;
  private FakeStudyGroupRepository studyGroupRepository;
  private ImageService imageService;

  @BeforeEach
  void setUp() {
    academicTermRepository = new FakeAcademicTermRepository();
    userRepository = new FakeUserRepository();
    studyReportRepository = new FakeStudyReportRepository();
    studyApplicantRepository = new FakeStudyApplicationRepository();
    studyGroupRepository = new FakeStudyGroupRepository();
    ImagePathMapper imagePathMapper = new ImagePathMapper();
    ReflectionTestUtils.setField(imagePathMapper, "origin", "https://histudy.handong.edu");
    ReflectionTestUtils.setField(imagePathMapper, "imageBasePath", "/images");
    imageService =
        new ImageService(
            academicTermRepository,
            userRepository,
            studyReportRepository,
            studyApplicantRepository,
            imagePathMapper,
            studyGroupRepository);
    ReflectionTestUtils.setField(imageService, "imageBaseLocation", tempDir.toString() + "/");
  }

  @Test
  void 활동_보고서_이미지를_업로드하면_전체_이미지경로를_반환한다() throws Exception {
    // Given
    academicTermRepository.save(currentTerm);
    User member = userRepository.save(memberUser);
    Course course = commonCourse;
    StudyApplicant applicant = StudyApplicant.of(currentTerm, member, List.of(), List.of(course));
    studyGroupRepository.save(StudyGroup.of(7, currentTerm, List.of(applicant)));
    MockMultipartFile multipartFile =
        new MockMultipartFile("image", "report.png", "image/png", pngBytes);

    // When
    String result =
        imageService.getImagePaths("member@histudy.com", multipartFile, Optional.empty());

    // Then
    assertThat(result).startsWith("https://histudy.handong.edu/images/reports/");
    try (var reportFiles = Files.list(tempDir.resolve("reports"))) {
      assertThat(reportFiles.toList()).hasSize(1);
    }
  }

  @Test
  void 동일한_이미지를_업로드하면_기존_이미지경로를_재사용한다() throws Exception {
    // Given
    academicTermRepository.save(currentTerm);
    User member = userRepository.save(memberUser);
    Course course = commonCourse;
    StudyApplicant applicant = StudyApplicant.of(currentTerm, member, List.of(), List.of(course));
    StudyGroup group = studyGroupRepository.save(StudyGroup.of(7, currentTerm, List.of(applicant)));
    Files.createDirectories(tempDir.resolve("reports"));
    Files.write(tempDir.resolve("reports/existing.png"), pngBytes);
    StudyReport report =
        studyReportRepository.save(
            StudyReport.builder()
                .title("1주차")
                .content("첫 모임")
                .totalMinutes(90)
                .studyGroup(group)
                .participants(List.of(member))
                .images(List.of("reports/existing.png"))
                .courses(List.of(course))
                .build());
    MockMultipartFile multipartFile =
        new MockMultipartFile("image", "report.png", "image/png", pngBytes);

    // When
    String result =
        imageService.getImagePaths(
            "member@histudy.com", multipartFile, Optional.of(report.getStudyReportId()));

    // Then
    assertThat(result).isEqualTo("https://histudy.handong.edu/images/reports/existing.png");
    try (var reportFiles = Files.list(tempDir.resolve("reports"))) {
      assertThat(reportFiles.toList()).hasSize(1);
    }
  }

  @Test
  void 다른_그룹_보고서의_이미지는_재사용할수없다() throws Exception {
    // Given
    academicTermRepository.save(currentTerm);
    User member = userRepository.save(memberUser);
    User otherMember =
        userRepository.save(
            User.builder()
                .sub("sub-2")
                .sid("22230002")
                .email("other@histudy.com")
                .name("Other")
                .role(Role.USER)
                .build());
    StudyApplicant memberApplicant =
        StudyApplicant.of(currentTerm, member, List.of(), List.of(commonCourse));
    StudyApplicant otherApplicant =
        StudyApplicant.of(currentTerm, otherMember, List.of(), List.of(commonCourse));
    studyGroupRepository.save(StudyGroup.of(7, currentTerm, List.of(memberApplicant)));
    StudyGroup otherGroup =
        studyGroupRepository.save(StudyGroup.of(8, currentTerm, List.of(otherApplicant)));
    StudyReport otherReport =
        studyReportRepository.save(
            StudyReport.builder()
                .title("다른 그룹 보고서")
                .content("접근 불가")
                .totalMinutes(90)
                .studyGroup(otherGroup)
                .participants(List.of(otherMember))
                .images(List.of("reports/existing.png"))
                .courses(List.of(commonCourse))
                .build());
    MockMultipartFile multipartFile =
        new MockMultipartFile("image", "report.png", "image/png", pngBytes);

    // When Then
    assertThatThrownBy(
            () ->
                imageService.getImagePaths(
                    "member@histudy.com",
                    multipartFile,
                    Optional.of(otherReport.getStudyReportId())))
        .isInstanceOf(ReportNotFoundException.class);
  }

  @Test
  void 현재_학기_없이_이미지를_업로드하면_예외가_발생한다() throws Exception {
    // Given
    MockMultipartFile multipartFile =
        new MockMultipartFile("image", "report.png", "image/png", pngBytes);

    // When Then
    assertThatThrownBy(
            () -> imageService.getImagePaths("member@histudy.com", multipartFile, Optional.empty()))
        .isInstanceOf(NoCurrentTermFoundException.class);
  }

  private byte[] createPngBytes() {
    try {
      BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(image, "png", outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("failed to create image fixture", e);
    }
  }
}
