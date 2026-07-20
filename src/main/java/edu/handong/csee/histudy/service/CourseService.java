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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
  private final CourseRepository courseRepository;
  private final UserRepository userRepository;
  private final AcademicTermRepository academicTermRepository;
  private final StudyGroupRepository studyGroupRepository;

  @Transactional
  public void replaceCourses(List<CourseCSV> courseData) {
    if (courseData.isEmpty()) {
      return;
    }
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<Course> courses = toCourses(courseData, currentTerm);

    courseRepository.deleteAllByAcademicTerm(currentTerm);
    courseRepository.saveAll(courses);
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

  public int deleteCourse(CourseIdDto dto) {
    if (courseRepository.existsById(dto.getId())) {
      courseRepository.deleteById(dto.getId());
      return 1;
    }
    return 0;
  }

  @Transactional
  public void deleteCurrentCourse(Long courseId) {
    Course course =
        courseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
    if (course.getAcademicTerm() == null
        || !Boolean.TRUE.equals(course.getAcademicTerm().getIsCurrent())) {
      throw new CourseNotFoundException();
    }
    if (courseRepository.hasReferences(courseId)) {
      throw new CourseInUseException();
    }
    courseRepository.deleteById(courseId);
  }
}
