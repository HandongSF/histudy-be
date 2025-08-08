package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.GroupMember;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.domain.User;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamRankDto {

  private List<TeamInfo> teams;

  public TeamRankDto(List<TeamInfo> teamInfos) {
    this.teams = teamInfos;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class TeamInfo {
    private int id;

    private List<String> members;

    private int reports;

    private long totalMinutes;

    private String thumbnail;

    public TeamInfo(StudyGroup studyGroup, List<StudyReport> reports, String imgPath) {
      this.id = studyGroup.getTag();
      this.members =
          studyGroup.getMembers().stream().map(GroupMember::getUser).map(User::getName).toList();
      this.reports = reports.size();
      this.totalMinutes = reports.stream().mapToLong(StudyReport::getTotalMinutes).sum();
      this.thumbnail = imgPath;
    }
  }
}