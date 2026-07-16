package edu.handong.csee.histudy.service.command;

import java.util.List;

public record ReportCommand(
    String title,
    String content,
    Long totalMinutes,
    List<Long> participantIds,
    List<String> imageUrls,
    List<Long> courseIds) {

  public ReportCommand {
    participantIds = participantIds == null ? List.of() : List.copyOf(participantIds);
    imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    courseIds = courseIds == null ? List.of() : List.copyOf(courseIds);
  }
}
