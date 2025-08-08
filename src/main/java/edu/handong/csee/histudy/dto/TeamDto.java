package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.*;
import java.util.List;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TeamDto {

  private Long group; // id

  private int tag;

  private List<UserDto.UserInfo> members;

  private int reports;

  private long times; // totalMinutes

  public TeamDto(
      StudyGroup group, List<StudyReport> reports, Map<User, StudyApplicant> applicantMap) {
    this.group = group.getStudyGroupId();
    this.tag = group.getTag();
    this.members =
        group.getMembers().stream()
            .map(GroupMember::getUser)
            .map(
                user -> {
                  StudyApplicant applicant = applicantMap.get(user);
                  return (applicant == null)
                      ? new UserDto.UserInfo(user)
                      : new UserDto.UserInfo(user, applicant);
                })
            .toList();
    this.reports = reports.size();
    this.times = reports.stream().mapToLong(StudyReport::getTotalMinutes).sum();
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class MatchResults {

    private List<TeamMatching> matchedTeams;

    private List<UserDto.UserInfo> unmatchedUsers;

    public MatchResults(
        List<StudyGroup> matchedStudyGroups, List<UserDto.UserInfo> unmatchedUsers) {
      this.matchedTeams = matchedStudyGroups.stream().map(TeamMatching::new).toList();
      this.unmatchedUsers = unmatchedUsers;
    }
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class TeamMatching {

    private Long id;

    private Integer tag;

    private List<UserDto.UserMatching> users;

    private List<CourseDto.CourseInfo> courses;

    public TeamMatching(StudyGroup studyGroup) {
      this.id = studyGroup.getStudyGroupId();
      this.tag = studyGroup.getTag();
      this.users =
          studyGroup.getMembers().stream()
              .map(GroupMember::getUser)
              .map(UserDto.UserMatching::new)
              .toList();
      this.courses =
          studyGroup.getCourses().stream()
              .map(enroll -> new CourseDto.CourseInfo(enroll.getCourse()))
              .toList();
    }
  }
}