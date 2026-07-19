package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.dto.CourseDto;
import edu.handong.csee.histudy.dto.CourseIdDto;
import edu.handong.csee.histudy.exception.CourseInUseException;
import edu.handong.csee.histudy.exception.CourseNotFoundException;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.exception.StudyGroupNotFoundException;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeCourseRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyGroupRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeUserRepository;
import edu.handong.csee.histudy.util.CourseCSV;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CourseServiceTest {

  private final AcademicTerm currentTerm =
      AcademicTerm.builder().academicYear(2025).semester(TermType.SPRING).isCurrent(true).build();
  private final AcademicTerm previousTerm =
      AcademicTerm.builder().academicYear(2024).semester(TermType.FALL).isCurrent(false).build();
  private final User memberUser =
      User.builder()
          .sub("sub-1")
          .sid("22230001")
          .email("member@histudy.com")
          .name("Member")
          .role(Role.USER)
          .build();
  private final Course currentCourse =
      Course.builder()
          .name("자료구조")
          .code("CSEE201")
          .professor("Kim")
          .academicTerm(currentTerm)
          .build();
  private final Course previousCourse =
      Course.builder()
          .name("운영체제")
          .code("CSEE301")
          .professor("Lee")
          .academicTerm(previousTerm)
          .build();
  private final List<CourseCSV> replacementCsvData =
      List.of(
          CourseCSV.builder().code("CSEE101").title("프로그래밍입문").professor("Kim").build(),
          CourseCSV.builder().code("CSEE102").title("이산수학").professor("Lee").build());

  private FakeCourseRepository courseRepository;
  private FakeUserRepository userRepository;
  private FakeAcademicTermRepository academicTermRepository;
  private FakeStudyGroupRepository studyGroupRepository;
  private CourseService courseService;

  @BeforeEach
  void setUp() {
    courseRepository = new FakeCourseRepository();
    userRepository = new FakeUserRepository();
    academicTermRepository = new FakeAcademicTermRepository();
    studyGroupRepository = new FakeStudyGroupRepository();
    courseService =
        new CourseService(
            courseRepository, userRepository, academicTermRepository, studyGroupRepository);
  }

  @Test
  void 현재_학기_과목_목록을_조회하면_현재_학기_과목만_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    academicTermRepository.save(previousTerm);
    courseRepository.saveAll(List.of(currentCourse, previousCourse));

    // When
    List<CourseDto.CourseInfo> result = courseService.getCurrentCourses();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("자료구조");
  }

  @Test
  void 과목명을_검색하면_검색어와_일치하는_과목을_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    courseRepository.saveAll(
        List.of(
            Course.builder()
                .name("Algorithms")
                .code("CSEE221")
                .professor("Kim")
                .academicTerm(currentTerm)
                .build(),
            Course.builder()
                .name("Database")
                .code("CSEE223")
                .professor("Park")
                .academicTerm(currentTerm)
                .build()));

    // When
    List<CourseDto.CourseInfo> result = courseService.search("algo");

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Algorithms");
  }

  @Test
  void 과목_CSV로_교체하면_기존_과목을_대체한다() {
    // Given
    academicTermRepository.save(currentTerm);
    courseRepository.saveAll(
        List.of(
            Course.builder()
                .name("Old Course")
                .code("OLD101")
                .professor("Old")
                .academicTerm(currentTerm)
                .build()));

    // When
    courseService.replaceCourses(replacementCsvData);

    // Then
    assertThat(courseRepository.findAll()).hasSize(2);
    assertThat(courseRepository.findAll())
        .extracting(Course::getName)
        .containsExactlyInAnyOrder("프로그래밍입문", "이산수학");
    assertThat(courseRepository.findAll())
        .allMatch(course -> course.getAcademicTerm().equals(currentTerm));
  }

  @Test
  void 현재_학기_없이_과목_CSV로_교체하면_예외가_발생한다() {
    // Given
    // When Then
    assertThatThrownBy(() -> courseService.replaceCourses(replacementCsvData))
        .isInstanceOf(NoCurrentTermFoundException.class);
  }

  @Test
  void 그룹_과목_정보를_조회하면_같은_그룹의_과목을_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User user = userRepository.save(memberUser);
    Course commonCourse = courseRepository.saveAll(List.of(currentCourse)).get(0);
    StudyApplicant applicant =
        StudyApplicant.of(currentTerm, user, List.of(), List.of(commonCourse));
    StudyGroup group = StudyGroup.of(1, currentTerm, List.of(applicant));
    studyGroupRepository.save(group);

    // When
    List<CourseDto.CourseInfo> result = courseService.getTeamCourses("member@histudy.com");

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("자료구조");
  }

  @Test
  void 그룹이_없는_유저가_그룹_과목_정보를_조회하면_예외가_발생한다() {
    // Given
    academicTermRepository.save(currentTerm);
    userRepository.save(memberUser);

    // When Then
    assertThatThrownBy(() -> courseService.getTeamCourses("member@histudy.com"))
        .isInstanceOf(StudyGroupNotFoundException.class);
  }

  @Test
  void 등록된_과목을_삭제하면_과목이_제거된다() {
    // Given
    academicTermRepository.save(currentTerm);
    Course savedCourse = courseRepository.saveAll(List.of(currentCourse)).get(0);

    // When
    int result = courseService.deleteCourse(new CourseIdDto(savedCourse.getCourseId()));

    // Then
    assertThat(result).isEqualTo(1);
    assertThat(courseRepository.findAll()).isEmpty();
  }

  @Test
  void 현재_학기_미사용_과목을_삭제하면_과목이_제거된다() {
    // Given
    Course savedCourse = courseRepository.saveAll(List.of(currentCourse)).get(0);

    // When
    courseService.deleteCurrentCourse(savedCourse.getCourseId());

    // Then
    assertThat(courseRepository.findAll()).isEmpty();
  }

  @Test
  void 없는_과목을_삭제하면_예외가_발생한다() {
    // Given
    // When Then
    assertThatThrownBy(() -> courseService.deleteCurrentCourse(999L))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void 과거_학기_과목을_삭제하면_예외가_발생한다() {
    // Given
    Course savedCourse = courseRepository.saveAll(List.of(previousCourse)).get(0);

    // When Then
    assertThatThrownBy(() -> courseService.deleteCurrentCourse(savedCourse.getCourseId()))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void 사용중인_과목을_삭제하면_예외가_발생한다() {
    // Given
    Course savedCourse = courseRepository.saveAll(List.of(currentCourse)).get(0);
    courseRepository.markReferenced(savedCourse.getCourseId());

    // When Then
    assertThatThrownBy(() -> courseService.deleteCurrentCourse(savedCourse.getCourseId()))
        .isInstanceOf(CourseInUseException.class)
        .hasMessage("사용 중인 강의는 삭제할 수 없습니다.");
    assertThat(courseRepository.findAll()).containsExactly(savedCourse);
  }
}
