package edu.handong.csee.histudy.service.repository.fake;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.repository.CourseRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.test.util.ReflectionTestUtils;

public class FakeCourseRepository implements CourseRepository {

  private final List<Course> store = new ArrayList<>();
  private final Set<Long> referencedCourseIds = new HashSet<>();
  private Long sequence = 1L;

  @Override
  public List<Course> findAllByNameContainingIgnoreCase(String keyword) {
    return store.stream()
        .filter(course -> course.getName().toLowerCase().contains(keyword.toLowerCase()))
        .collect(Collectors.toList());
  }

  @Override
  public List<Course> findAllByAcademicTermIsCurrentTrue() {
    return store.stream().filter(c -> c.getAcademicTerm().getIsCurrent()).toList();
  }

  @Override
  public List<Course> saveAll(List<Course> entities) {
    entities.forEach(
        course -> {
          if (course.getCourseId() == null) {
            ReflectionTestUtils.setField(course, "courseId", sequence++);
          }
          store.removeIf(existing -> existing.getCourseId().equals(course.getCourseId()));
          store.add(course);
        });
    return entities;
  }

  @Override
  public boolean existsById(Long id) {
    return store.stream().anyMatch(c -> c.getCourseId().equals(id));
  }

  @Override
  public boolean hasReferences(Long courseId) {
    return referencedCourseIds.contains(courseId);
  }

  @Override
  public void deleteById(Long id) {
    store.removeIf(c -> c.getCourseId().equals(id));
  }

  @Override
  public void deleteAllByAcademicTerm(AcademicTerm academicTerm) {
    store.removeIf(c -> c.getAcademicTerm().equals(academicTerm));
  }

  @Override
  public Optional<Course> findById(Long id) {
    return store.stream().filter(c -> c.getCourseId().equals(id)).findFirst();
  }

  public List<Course> findAll() {
    return new ArrayList<>(store);
  }

  public void markReferenced(Long courseId) {
    referencedCourseIds.add(courseId);
  }
}
