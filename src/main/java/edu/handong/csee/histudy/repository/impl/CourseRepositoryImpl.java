package edu.handong.csee.histudy.repository.impl;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.repository.CourseRepository;
import edu.handong.csee.histudy.repository.jpa.JpaCourseRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {
  private final JpaCourseRepository repository;

  @Override
  public List<Course> findAllByNameContainingIgnoreCase(String keyword) {
    return repository.findAllByNameContainingIgnoreCase(keyword);
  }

  @Override
  public List<Course> findAllByAcademicTermIsCurrentTrue() {
    return repository.findAllByAcademicTermIsCurrentTrue();
  }

  @Override
  public List<Course> saveAll(List<Course> entities) {
    return repository.saveAll(entities);
  }

  @Override
  public boolean existsById(Long id) {
    return repository.existsById(id);
  }

  @Override
  public boolean hasReferences(Long courseId) {
    return repository.countPreferredCourseReferences(courseId) > 0
        || repository.countGroupCourseReferences(courseId) > 0
        || repository.countStudyCourseReferences(courseId) > 0;
  }

  @Override
  public void deleteById(Long id) {
    repository.deleteById(id);
  }

  @Override
  public void deleteAllByAcademicTerm(AcademicTerm academicTerm) {
    repository.deleteAllByAcademicTerm(academicTerm);
  }

  @Override
  public Optional<Course> findById(Long id) {
    return repository.findById(id);
  }
}
