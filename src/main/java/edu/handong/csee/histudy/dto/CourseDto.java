package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.Course;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseDto {

  private List<CourseInfo> courses;

  @AllArgsConstructor
  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class CourseInfo {

    private Long id;

    private String name;

    private String prof;

    private String code;

    private int year;

    private int semester;

    public CourseInfo(Course course) {
      this.id = course.getCourseId();
      this.name = course.getName();
      this.prof = course.getProfessor();
      this.code = course.getCode();
      this.year = course.getAcademicTerm().getAcademicYear();
      this.semester = course.getAcademicTerm().getSemester().getNumber();
    }
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class BasicCourseInfo {

    private Long id;

    private String name;

    private String prof;

    public BasicCourseInfo(Course course) {
      this.id = course.getCourseId();
      this.name = course.getName();
      this.prof = course.getProfessor();
    }
  }
}