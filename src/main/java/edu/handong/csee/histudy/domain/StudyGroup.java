package edu.handong.csee.histudy.domain;

import jakarta.persistence.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyGroup extends BaseTime {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer tag;

  @ManyToOne(fetch = FetchType.LAZY)
  private AcademicTerm academicTerm;

  @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GroupMember> members = new ArrayList<>();

  @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GroupCourse> courses = new ArrayList<>();

  public static StudyGroup of(
      AtomicInteger tag, AcademicTerm current, StudyApplicant sender, StudyApplicant receiver) {
    if (sender.getStudyGroup() != null && receiver.getStudyGroup() != null) {
      // a -> b or a <- b
      assert sender.getStudyGroup().equals(receiver.getStudyGroup());
      return sender.getStudyGroup();
    } else if (sender.getStudyGroup() != null) {
      // (a <-> b) -> c
      return sender.getStudyGroup().assignMembers(receiver);
    } else if (receiver.getStudyGroup() != null) {
      // (a <-> b) <- c
      return receiver.getStudyGroup().assignMembers(sender);
    }
    return new StudyGroup(tag.getAndIncrement(), current, sender, receiver);
  }

  public static StudyGroup of(Integer tag, AcademicTerm current, List<StudyApplicant> applicants) {
    return new StudyGroup(tag, current, applicants.toArray(StudyApplicant[]::new));
  }

  public static StudyGroup of(Integer tag, AcademicTerm current) {
    return new StudyGroup(tag, current);
  }

  protected StudyGroup(Integer tag, AcademicTerm academicTerm, StudyApplicant... applicants) {
    this.tag = tag;
    this.academicTerm = academicTerm;
    assignMembers(applicants);
  }

  protected StudyGroup(Integer tag, AcademicTerm academicTerm) {
    this.tag = tag;
    this.academicTerm = academicTerm;
  }

  private List<Course> findCommonCourses(StudyApplicant... applicants) {
    if (!this.courses.isEmpty()) {
      this.courses.clear();
    }
    Map<Course, Long> courseCountMap =
        Arrays.stream(applicants)
            .flatMap(form -> form.getPreferredCourses().stream())
            .map(PreferredCourse::getCourse)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    List<Course> commonCourses =
        courseCountMap.entrySet().stream()
            .filter(entry -> entry.getValue() >= 2)
            .sorted(Map.Entry.<Course, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();

    return (commonCourses.isEmpty())
        ? Arrays.stream(applicants)
            .map(StudyApplicant::getPreferredCourses)
            .flatMap(Collection::stream)
            .map(PreferredCourse::getCourse)
            .toList()
        : commonCourses;
  }

  public StudyGroup assignMembers(StudyApplicant... applicants) {
    Arrays.stream(applicants)
        .forEach(
            form -> {
              if (isInSameGroup(form)) {
                return;
              } else if (isAlreadyInOtherGroup(form)) {
                form.leaveGroup();
              }
              GroupMember.of(this, form);
            });
    assignCommonCourses(applicants);
    return this;
  }

  public void removeMember(User member) {
    this.members.stream()
        .filter(_member -> _member.getUser().equals(member))
        .findFirst()
        .ifPresent(GroupMember::remove);
  }

  private boolean isAlreadyInOtherGroup(StudyApplicant applicant) {
    return applicant != null && !applicant.getStudyGroup().equals(this);
  }

  public boolean isInSameGroup(StudyApplicant applicant) {
    return applicant != null && applicant.getStudyGroup().equals(this);
  }

  protected void assignCommonCourses(StudyApplicant... applicants) {
    if (this.members.isEmpty()) {
      this.courses.clear();
      this.tag = -1;
      return;
    }
    findCommonCourses(applicants).stream()
        .filter(this::isNotInGroupCourse)
        .forEach(course -> new GroupCourse(this, course));
  }

  private boolean isNotInGroupCourse(Course course) {
    return this.courses.stream().map(GroupCourse::getCourse).noneMatch(c -> c.equals(course));
  }
}
