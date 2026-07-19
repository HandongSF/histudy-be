package edu.handong.csee.histudy.repository.jpa;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCourseRepository extends JpaRepository<Course, Long> {

  @Query(
      "select c from Course c "
          + "where lower(c.name) "
          + "like lower(concat('%', :keyword, '%')) "
          + "and c.academicTerm.isCurrent = true")
  List<Course> findAllByNameContainingIgnoreCase(@Param("keyword") String keyword);

  List<Course> findAllByAcademicTermIsCurrentTrue();

  @Query("select count(pc) from PreferredCourse pc where pc.course.courseId = :courseId")
  long countPreferredCourseReferences(@Param("courseId") Long courseId);

  @Query("select count(gc) from GroupCourse gc where gc.course.courseId = :courseId")
  long countGroupCourseReferences(@Param("courseId") Long courseId);

  @Query("select count(sc) from StudyCourse sc where sc.course.courseId = :courseId")
  long countStudyCourseReferences(@Param("courseId") Long courseId);

  void deleteAllByAcademicTerm(AcademicTerm academicTerm);
}
