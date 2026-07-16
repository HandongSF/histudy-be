package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.handong.csee.histudy.controller.form.ApplyForm;
import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.RequestStatus;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.dto.ApplyFormDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.exception.UserAlreadyExistsException;
import edu.handong.csee.histudy.service.command.SignUpCommand;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeCourseRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyApplicationRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeStudyGroupRepository;
import edu.handong.csee.histudy.service.repository.fake.FakeUserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class UserServiceTest {

  private final AcademicTerm currentTerm =
      AcademicTerm.builder().academicYear(2025).semester(TermType.SPRING).isCurrent(true).build();
  private final User applicantUser =
      User.builder()
          .sub("sub-1")
          .sid("22230001")
          .email("applicant@histudy.com")
          .name("Applicant")
          .role(Role.USER)
          .build();
  private final User partnerUser =
      User.builder()
          .sub("sub-2")
          .sid("22230002")
          .email("partner@histudy.com")
          .name("Partner")
          .role(Role.USER)
          .build();
  private final User firstPartnerUser =
      User.builder()
          .sub("sub-3")
          .sid("22230002")
          .email("first@histudy.com")
          .name("First")
          .role(Role.USER)
          .build();
  private final User secondPartnerUser =
      User.builder()
          .sub("sub-4")
          .sid("22230003")
          .email("second@histudy.com")
          .name("Second")
          .role(Role.USER)
          .build();
  private final User meUser =
      User.builder()
          .sub("sub-5")
          .sid("22230001")
          .email("me@histudy.com")
          .name("Me")
          .role(Role.USER)
          .build();
  private final User groupedUser =
      User.builder()
          .sub("sub-6")
          .sid("22230006")
          .email("grouped@histudy.com")
          .name("Grouped")
          .role(Role.USER)
          .build();
  private final User applicantOnlyUser =
      User.builder()
          .sub("sub-7")
          .sid("22230007")
          .email("applicant-only@histudy.com")
          .name("ApplicantOnly")
          .role(Role.USER)
          .build();
  private final User noFormUser =
      User.builder()
          .sub("sub-8")
          .sid("22230008")
          .email("plain@histudy.com")
          .name("Plain")
          .role(Role.USER)
          .build();
  private final User ungroupedUser =
      User.builder()
          .sub("sub-9")
          .sid("22230009")
          .email("ungrouped@histudy.com")
          .name("Ungrouped")
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
  private final SignUpCommand signUpCommand =
      new SignUpCommand("sub-10", "Alice", "alice@histudy.com", "22230010");

  private FakeUserRepository userRepository;
  private FakeCourseRepository courseRepository;
  private FakeStudyGroupRepository studyGroupRepository;
  private FakeAcademicTermRepository academicTermRepository;
  private FakeStudyApplicationRepository studyApplicantRepository;
  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = new FakeUserRepository();
    courseRepository = new FakeCourseRepository();
    studyGroupRepository = new FakeStudyGroupRepository();
    academicTermRepository = new FakeAcademicTermRepository();
    studyApplicantRepository = new FakeStudyApplicationRepository();
    userService =
        new UserService(
            userRepository,
            courseRepository,
            studyGroupRepository,
            academicTermRepository,
            studyApplicantRepository);
  }

  @Test
  void 검색어_없이_같이할_사람을_검색하면_전체를_학번순으로_조회한다() {
    // Given
    userRepository.save(
        User.builder()
            .sub("sub-2")
            .sid("22230002")
            .email("second@histudy.com")
            .name("Second")
            .role(Role.USER)
            .build());
    userRepository.save(
        User.builder()
            .sub("sub-1")
            .sid("22230001")
            .email("first@histudy.com")
            .name("First")
            .role(Role.USER)
            .build());

    // When
    List<User> result = userService.search(Optional.of(" "));

    // Then
    assertThat(result).extracting(User::getSid).containsExactly("22230001", "22230002");
    assertThat(result).isEqualTo(userRepository.findAll(Sort.by(Sort.Direction.ASC, "sid")));
  }

  @Test
  void 회원가입하면_명령의_사용자정보를_USER_권한으로_저장한다() {
    // Given
    // When
    userService.signUp(signUpCommand);

    // Then
    User savedUser = userRepository.findUserBySub("sub-10").orElseThrow();
    assertThat(savedUser.getSub()).isEqualTo("sub-10");
    assertThat(savedUser.getName()).isEqualTo("Alice");
    assertThat(savedUser.getEmail()).isEqualTo("alice@histudy.com");
    assertThat(savedUser.getSid()).isEqualTo("22230010");
    assertThat(savedUser.getRole()).isEqualTo(Role.USER);
  }

  @Test
  void 이미_존재하는_유저로_회원가입하면_예외가_발생한다() {
    // Given
    userRepository.save(
        User.builder()
            .sub("sub-10")
            .sid("22230010")
            .email("alice@histudy.com")
            .name("Alice")
            .role(Role.USER)
            .build());

    // When Then
    assertThatThrownBy(() -> userService.signUp(signUpCommand))
        .isInstanceOf(UserAlreadyExistsException.class);
  }

  @Test
  void 서로_신청한_친구로_스터디를_신청하면_친구요청이_수락상태가_된다() {
    // Given
    academicTermRepository.save(currentTerm);
    User applicant = userRepository.save(applicantUser);
    User partner = userRepository.save(partnerUser);
    Course course = courseRepository.saveAll(List.of(primaryCourse)).get(0);
    userService.apply(
        ApplyForm.builder()
            .friendIds(List.of("22230001"))
            .courseIds(List.of(course.getCourseId()))
            .build(),
        "partner@histudy.com");

    // When
    ApplyFormDto result =
        userService.apply(
            ApplyForm.builder()
                .friendIds(List.of("22230002"))
                .courseIds(List.of(course.getCourseId()))
                .build(),
            "applicant@histudy.com");

    // Then
    StudyApplicant applicantForm =
        studyApplicantRepository.findByUserAndTerm(applicant, currentTerm).orElseThrow();
    StudyApplicant partnerForm =
        studyApplicantRepository.findByUserAndTerm(partner, currentTerm).orElseThrow();
    assertThat(applicantForm.getPartnerRequests())
        .singleElement()
        .extracting(request -> request.getRequestStatus())
        .isEqualTo(RequestStatus.ACCEPTED);
    assertThat(partnerForm.getPartnerRequests())
        .singleElement()
        .extracting(request -> request.getRequestStatus())
        .isEqualTo(RequestStatus.ACCEPTED);
    assertThat(result.getFriends()).hasSize(1);
    assertThat(result.getCourses()).hasSize(1);
  }

  @Test
  void 스터디_신청을_수정하면_기존_신청을_교체한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User applicant = userRepository.save(applicantUser);
    User firstPartner = userRepository.save(firstPartnerUser);
    User secondPartner = userRepository.save(secondPartnerUser);
    Course firstCourse = courseRepository.saveAll(List.of(primaryCourse, secondaryCourse)).get(0);
    Course secondCourse = courseRepository.findAll().get(1);
    userService.apply(
        ApplyForm.builder()
            .friendIds(List.of("22230002"))
            .courseIds(List.of(firstCourse.getCourseId()))
            .build(),
        "applicant@histudy.com");

    // When
    userService.apply(
        ApplyForm.builder()
            .friendIds(List.of("22230003"))
            .courseIds(List.of(secondCourse.getCourseId()))
            .build(),
        "applicant@histudy.com");

    // Then
    StudyApplicant applicantForm =
        studyApplicantRepository.findByUserAndTerm(applicant, currentTerm).orElseThrow();
    assertThat(studyApplicantRepository.findAllByTerm(currentTerm)).hasSize(1);
    assertThat(applicantForm.getRequestedUsers()).containsExactly(secondPartner);
    assertThat(applicantForm.getPreferredCourses())
        .extracting(preferred -> preferred.getCourse())
        .containsExactly(secondCourse);
    assertThat(applicantForm.getRequestedUsers()).doesNotContain(firstPartner);
  }

  @Test
  void 그룹이_배정된_스터디_신청을_수정하면_예외가_발생한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User applicant = userRepository.save(applicantUser);
    Course course = courseRepository.saveAll(List.of(primaryCourse)).get(0);
    StudyApplicant applicantForm =
        studyApplicantRepository.save(
            StudyApplicant.of(currentTerm, applicant, List.of(), List.of(course)));
    studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(applicantForm)));

    // When Then
    assertThatThrownBy(
            () ->
                userService.apply(
                    ApplyForm.builder()
                        .friendIds(List.of())
                        .courseIds(List.of(course.getCourseId()))
                        .build(),
                    "applicant@histudy.com"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void 현재_학기_없이_스터디를_신청하면_예외가_발생한다() {
    // Given
    userRepository.save(applicantUser);
    ApplyForm form = ApplyForm.builder().friendIds(List.of()).courseIds(List.of()).build();

    // When Then
    assertThatThrownBy(() -> userService.apply(form, "applicant@histudy.com"))
        .isInstanceOf(NoCurrentTermFoundException.class);
  }

  @Test
  void 내_정보를_조회하면_기본_정보를_반환한다() {
    // Given
    userRepository.save(meUser);

    // When
    UserDto.UserMe result = userService.getUserMe(Optional.of("me@histudy.com"));

    // Then
    assertThat(result.getName()).isEqualTo("Me");
    assertThat(result.getEmail()).isEqualTo("me@histudy.com");
    assertThat(result.getSid()).isEqualTo("22230001");
  }

  @Test
  void 내가_제출한_스터디_신청_내역을_조회하면_친구와_과목정보를_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User applicant = userRepository.save(applicantUser);
    User partner = userRepository.save(partnerUser);
    List<Course> courses = courseRepository.saveAll(List.of(primaryCourse, secondaryCourse));
    studyApplicantRepository.save(
        StudyApplicant.of(currentTerm, applicant, List.of(partner), courses));

    // When
    StudyApplicant result = userService.getUserInfo("applicant@histudy.com").orElseThrow();

    // Then
    assertThat(result.getRequestedUsers()).containsExactly(partner);
    assertThat(result.getPreferredCourses())
        .extracting(preferred -> preferred.getCourse())
        .containsExactlyInAnyOrderElementsOf(courses);
  }

  @Test
  void 내가_제출한_스터디_신청_내역이_없으면_빈_값을_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    userRepository.save(applicantUser);

    // When
    Optional<StudyApplicant> result = userService.getUserInfo("applicant@histudy.com");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void 그룹이_배정되지_않은_전체_유저_목록을_조회하면_미배정_유저를_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User savedGroupedUser = userRepository.save(groupedUser);
    User savedApplicantOnlyUser = userRepository.save(applicantOnlyUser);
    User savedNoFormUser = userRepository.save(noFormUser);
    Course course = courseRepository.saveAll(List.of(primaryCourse)).get(0);
    StudyApplicant groupedApplicant =
        StudyApplicant.of(currentTerm, savedGroupedUser, List.of(), List.of(course));
    StudyApplicant applicantOnly =
        StudyApplicant.of(currentTerm, savedApplicantOnlyUser, List.of(), List.of(course));
    studyApplicantRepository.save(groupedApplicant);
    studyApplicantRepository.save(applicantOnly);
    studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(groupedApplicant)));

    // When
    List<UserDto.UserInfo> result = userService.getUnmatchedUsers();

    // Then
    assertThat(result)
        .extracting(UserDto.UserInfo::getEmail)
        .containsExactly("applicant-only@histudy.com", "plain@histudy.com");
    assertThat(result)
        .filteredOn(user -> user.getEmail().equals(savedNoFormUser.getEmail()))
        .singleElement()
        .extracting(UserDto.UserInfo::getGroup)
        .isNull();
  }

  @Test
  void 특정_유저의_스터디_신청을_삭제하면_현재_학기_신청이_제거된다() {
    // Given
    academicTermRepository.save(currentTerm);
    User applicant = userRepository.save(applicantUser);
    Course course = courseRepository.saveAll(List.of(primaryCourse)).get(0);
    studyApplicantRepository.save(
        StudyApplicant.of(currentTerm, applicant, List.of(), List.of(course)));

    // When
    userService.deleteUserForm("22230001");

    // Then
    assertThat(studyApplicantRepository.findAllByTerm(currentTerm)).isEmpty();
  }

  @Test
  void 신청했지만_아직_그룹이_배정되지_않은_유저_목록을_조회하면_미배정_신청자만_반환한다() {
    // Given
    academicTermRepository.save(currentTerm);
    User savedGroupedUser = userRepository.save(groupedUser);
    User savedUngroupedUser = userRepository.save(ungroupedUser);
    Course course = courseRepository.saveAll(List.of(primaryCourse)).get(0);
    StudyApplicant groupedApplicant =
        StudyApplicant.of(currentTerm, savedGroupedUser, List.of(), List.of(course));
    StudyApplicant ungroupedApplicant =
        StudyApplicant.of(currentTerm, savedUngroupedUser, List.of(), List.of(course));
    studyApplicantRepository.save(groupedApplicant);
    studyApplicantRepository.save(ungroupedApplicant);
    studyGroupRepository.save(StudyGroup.of(1, currentTerm, List.of(groupedApplicant)));

    // When
    List<UserDto.UserInfo> result = userService.getAppliedWithoutGroup();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmail()).isEqualTo(savedUngroupedUser.getEmail());
  }
}
