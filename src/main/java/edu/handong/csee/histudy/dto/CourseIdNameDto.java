package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.Course;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseIdNameDto {

    private long id;

    private String name;

    public CourseIdNameDto(Course course) {
        this.id = course.getCourseId();
        this.name = course.getName();
    }
}
