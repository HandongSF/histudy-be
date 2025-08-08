package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.StudyCourse;
import edu.handong.csee.histudy.domain.StudyParticipant;
import edu.handong.csee.histudy.domain.StudyReport;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportDto {

  private List<ReportInfo> reports;

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ReportInfo {

    public ReportInfo(StudyReport entity, Map<Long, String> imageFullPaths) {
      this.id = entity.getStudyReportId();
      this.title = entity.getTitle();
      this.content = entity.getContent();
      this.totalMinutes = entity.getTotalMinutes();
      this.participants =
          entity.getParticipants().stream()
              .map(StudyParticipant::getParticipant)
              .map(UserDto.UserBasic::new)
              .toList();
      this.courses =
          entity.getCourses().stream()
              .map(StudyCourse::getCourse)
              .map(CourseDto.BasicCourseInfo::new)
              .toList();
      this.images =
          entity.getImages().stream()
              .map(
                  img ->
                      new ImageDto(
                          img.getReportImageId(), imageFullPaths.get(img.getReportImageId())))
              .toList();
      this.regDate = entity.getLastModifiedDate().toString();
    }

    private Long id;

    private String title;

    private String content;

    private long totalMinutes;

    private List<UserDto.UserBasic> participants;

    private List<CourseDto.BasicCourseInfo> courses;

    private List<ImageDto> images;

    private String regDate;
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ReportBasic {
    public ReportBasic(StudyReport studyReport, Map<Long, String> imageMap) {
      this.id = studyReport.getStudyReportId();
      this.title = studyReport.getTitle();
      this.regDate = studyReport.getLastModifiedDate().toString();
      this.totalMinutes = studyReport.getTotalMinutes();
      this.thumbnail =
          studyReport.getImages().stream()
              .findFirst()
              .map(image -> imageMap.get(image.getReportImageId()))
              .orElse(null);
    }

    private long id;

    private String title;

    private String regDate;

    private long totalMinutes;

    private String thumbnail;
  }
}