package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.PreferredCourse;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyPartnerRequest;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.jwt.JwtPair;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDto {

  private List<? extends UserMatching> users;

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @Deprecated
  public static class UserMatching {

    private String name;

    private String sid;

    private String email;

    public UserMatching(User user) {
      this.name = user.getName();
      this.sid = user.getSid();
      this.email = user.getEmail();
    }
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class UserMatchingWithMasking extends UserMatching {

    private Long id;

    private String name;

    private String sid;

    public UserMatchingWithMasking(User user) {
      this.id = user.getUserId();
      this.name = user.getName();
      this.sid = user.getSidWithMasking();
    }
  }

  @Builder
  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class UserLogin {
    private Boolean isRegistered;

    private String tokenType;

    private JwtPair tokens;

    private String role;
  }

  @Builder
  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class UserInfo {

    private Long id;

    private String name;

    private String sid;

    private Integer group;

    private String email;

    private List<UserBasic> friends;

    private List<CourseDto.BasicCourseInfo> courses;

    public UserInfo(User user, StudyApplicant applicant) {
      this.id = user.getUserId();
      this.name = user.getName();
      this.sid = user.getSid();
      this.email = user.getEmail();
      this.group = (applicant.getStudyGroup() == null) ? null : applicant.getStudyGroup().getTag();
      this.friends =
          (applicant.getPartnerRequests() == null)
              ? Collections.emptyList()
              : applicant.getPartnerRequests().stream()
                  .filter(StudyPartnerRequest::isAccepted)
                  .map(StudyPartnerRequest::getReceiver)
                  .map(UserBasic::new)
                  .toList();
      this.courses =
          (applicant.getPreferredCourses() == null)
              ? Collections.emptyList()
              : applicant.getPreferredCourses().stream()
                  .sorted(Comparator.comparing(PreferredCourse::getPriority))
                  .map(c -> new CourseDto.BasicCourseInfo(c.getCourse()))
                  .toList();
    }

    public UserInfo(User user) {
      this.id = user.getUserId();
      this.name = user.getName();
      this.sid = user.getSid();
      this.email = user.getEmail();
      this.group = null;
      this.friends = Collections.emptyList();
      this.courses = Collections.emptyList();
    }
  }

  @Builder
  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class UserBasic {

    private Long id;

    private String name;

    private String sid;

    public UserBasic(User user) {
      this.id = user.getUserId();
      this.sid = user.getSid();
      this.name = user.getName();
    }
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class UserBasicWithMasking extends UserBasic {

    private Long id;

    private String name;

    private String sid;

    public UserBasicWithMasking(User user) {
      this.id = user.getUserId();
      this.sid = user.getSidWithMasking();
      this.name = user.getName();
    }
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class UserMe {
    private Long id;

    private String name;

    private String sid;

    private String email;

    public UserMe(User user) {
      this.id = user.getUserId();
      this.sid = user.getSid();
      this.name = user.getName();
      this.email = user.getEmail();
    }
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class UserMeWithMasking extends UserMe {
    private Long id;

    private String name;

    private String sid;

    private String email;

    private Integer tag;

    public UserMeWithMasking(User user, int tag) {
      this.id = user.getUserId();
      this.sid = user.getSidWithMasking();
      this.name = user.getName();
      this.email = user.getEmail();
      this.tag = tag;
    }
  }

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserEdit {

    private Long id;

    private Integer team;

    private String name;

    private String sid;
  }
}