package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.PreferredCourse;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyPartnerRequest;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplyFormDto {

  private List<? extends UserDto.UserBasic> friends;

  private List<CourseDto.CourseInfo> courses;

  @Deprecated
  public ApplyFormDto(StudyApplicant applicant) {
    this.friends =
        applicant.getPartnerRequests().stream()
            .map(StudyPartnerRequest::getReceiver)
            .map(UserDto.UserBasic::new)
            .toList();
    this.courses =
        applicant.getPreferredCourses().stream()
            .sorted(Comparator.comparing(PreferredCourse::getPriority))
            .map(PreferredCourse::getCourse)
            .map(CourseDto.CourseInfo::new)
            .toList();
  }
}
