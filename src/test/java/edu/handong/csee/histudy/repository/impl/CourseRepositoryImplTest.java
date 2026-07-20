package edu.handong.csee.histudy.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.GroupCourse;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.repository.CourseRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CourseRepositoryImpl.class)
class CourseRepositoryImplTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private CourseRepository courseRepository;

  private AcademicTerm currentTerm;
  private Course course;

  @BeforeEach
  void setUp() {
    currentTerm =
        entityManager.persist(
            AcademicTerm.builder()
                .academicYear(2026)
                .semester(TermType.SUMMER)
                .isCurrent(true)
                .build());
    course =
        entityManager.persistAndFlush(
            Course.builder()
                .name("자료구조")
                .code("CSEE201")
                .professor("Kim")
                .academicTerm(currentTerm)
                .build());
  }

  @Test
  void 참조가_없는_과목을_조회하면_사용중이_아니다() {
    // Given
    // When
    boolean result = courseRepository.hasReferences(course.getCourseId());

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void 신청이_과목을_참조하면_사용중이다() {
    // Given
    User user =
        entityManager.persist(
            User.builder()
                .sub("sub-1")
                .sid("22230001")
                .email("user@histudy.com")
                .name("User")
                .role(Role.USER)
                .build());
    entityManager.persistAndFlush(
        StudyApplicant.of(currentTerm, user, List.of(), List.of(course)));

    // When
    boolean result = courseRepository.hasReferences(course.getCourseId());

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void 그룹이_과목을_참조하면_사용중이다() {
    // Given
    StudyGroup group = StudyGroup.of(1, currentTerm, List.of());
    new GroupCourse(course, group);
    entityManager.persistAndFlush(group);

    // When
    boolean result = courseRepository.hasReferences(course.getCourseId());

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void 리포트가_과목을_참조하면_사용중이다() {
    // Given
    StudyGroup group = entityManager.persist(StudyGroup.of(1, currentTerm, List.of()));
    StudyReport report =
        StudyReport.builder()
            .title("1주차")
            .content("학습 내용")
            .totalMinutes(60)
            .studyGroup(group)
            .participants(List.of())
            .images(List.of())
            .courses(List.of(course))
            .build();
    entityManager.persistAndFlush(report);

    // When
    boolean result = courseRepository.hasReferences(course.getCourseId());

    // Then
    assertThat(result).isTrue();
  }
}
