package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.CourseDto;
import edu.handong.csee.histudy.dto.CourseIdDto;
import edu.handong.csee.histudy.exception.CourseInUseException;
import edu.handong.csee.histudy.exception.CourseNotFoundException;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.exception.StudyGroupNotFoundException;
import edu.handong.csee.histudy.exception.UserNotFoundException;
import edu.handong.csee.histudy.repository.*;
import edu.handong.csee.histudy.util.CourseCSV;
import edu.handong.csee.histudy.observability.audit.*;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
  private final ApplicationEventPublisher publisher;
  private final CourseRepository courseRepository;
  private final UserRepository userRepository;
  private final AcademicTermRepository academicTermRepository;
  private final StudyGroupRepository studyGroupRepository;

  @Transactional
  public void replaceCourses(List<CourseCSV> courseData, AuditContext context) {
    Objects.requireNonNull(context, "context must not be null");
    if (courseData.isEmpty()) {
      publisher.publishEvent(new CoursesReplacedEvent(context, null, null, 0, false));
      return;
    }
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<Course> courses = toCourses(courseData, currentTerm);
    if (courseRepository.hasReferences(currentTerm)) {
      publisher.publishEvent(new CourseChangeRejectedEvent(context, currentTerm.getAcademicTermId(), null));
      throw new CourseInUseException();
    }

    long previousCount = courseRepository.countByAcademicTerm(currentTerm);
    courseRepository.deleteAllByAcademicTerm(currentTerm);
    courseRepository.saveAll(courses);
    publisher.publishEvent(new CoursesReplacedEvent(context, currentTerm.getAcademicTermId(), previousCount, courses.size(), true));
  }

  private List<Course> toCourses(List<CourseCSV> courseData, AcademicTerm currentTerm) {
    return courseData.stream().map(csv -> csv.toCourse(currentTerm)).toList();
  }

  public List<CourseDto.CourseInfo> getCurrentCourses() {
    return courseRepository.findAllByAcademicTermIsCurrentTrue().stream()
        .map(CourseDto.CourseInfo::new)
        .toList();
  }

  public List<CourseDto.CourseInfo> search(String keyword) {
    return courseRepository.findAllByNameContainingIgnoreCase(keyword).stream()
        .map(CourseDto.CourseInfo::new)
        .toList();
  }

  public List<CourseDto.CourseInfo> getTeamCourses(String email) {
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    StudyGroup studyGroup =
        studyGroupRepository
            .findByUserAndTerm(user, currentTerm)
            .orElseThrow(StudyGroupNotFoundException::new);

    studyGroupRepository.findByUserAndTerm(user, currentTerm).orElseThrow();
    List<Course> courses = studyGroup.getCourses().stream().map(GroupCourse::getCourse).toList();

    return courses.stream().map(CourseDto.CourseInfo::new).toList();
  }

  @Transactional
  public int deleteCourse(CourseIdDto dto, AuditContext context) {
    Objects.requireNonNull(context, "context must not be null");
    Course course = courseRepository.findById(dto.getId()).orElse(null);
    if (course != null) {
      Long termId = course.getAcademicTerm() == null
          ? null : course.getAcademicTerm().getAcademicTermId();
      courseRepository.deleteById(dto.getId());
      publisher.publishEvent(new CourseDeletedEvent(context, termId, dto.getId(), true, true));
      return 1;
    }
    publisher.publishEvent(new CourseDeletedEvent(context, null, dto.getId(), false, true));
    return 0;
  }

  @Transactional
  public void deleteCurrentCourse(Long courseId, AuditContext context) {
    Objects.requireNonNull(context, "context must not be null");
    Course course =
        courseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
    if (course.getAcademicTerm() == null
        || !Boolean.TRUE.equals(course.getAcademicTerm().getIsCurrent())) {
      throw new CourseNotFoundException();
    }
    if (courseRepository.hasReferences(courseId)) {
      publisher.publishEvent(new CourseChangeRejectedEvent(context, course.getAcademicTerm().getAcademicTermId(), courseId));
      throw new CourseInUseException();
    }
    courseRepository.deleteById(courseId);
    publisher.publishEvent(new CourseDeletedEvent(context, course.getAcademicTerm().getAcademicTermId(), courseId, true, false));
  }
}
