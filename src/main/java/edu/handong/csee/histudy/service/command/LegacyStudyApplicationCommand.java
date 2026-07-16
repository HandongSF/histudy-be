package edu.handong.csee.histudy.service.command;

import java.util.List;

public record LegacyStudyApplicationCommand(
    List<String> friendStudentIds, List<Long> courseIds) {

  public LegacyStudyApplicationCommand {
    friendStudentIds = friendStudentIds == null ? List.of() : List.copyOf(friendStudentIds);
    courseIds = courseIds == null ? List.of() : List.copyOf(courseIds);
  }
}
