package edu.handong.csee.histudy.matching.domain;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.domain.PreferredCourse;
import edu.handong.csee.histudy.domain.Priority;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyPartnerRequest;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.util.DFS;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MatchingPolicy {

  private static final int MIN_FRIEND_GROUP_SIZE = 2;
  private static final int MIN_COURSE_GROUP_SIZE = 3;
  private static final int MAX_COURSE_GROUP_SIZE = 5;

  public List<StudyGroup> match(
      List<StudyApplicant> applicants, AcademicTerm academicTerm, int firstGroupTag) {
    if (applicants.isEmpty()) {
      return List.of();
    }

    AtomicInteger nextGroupTag = new AtomicInteger(firstGroupTag);
    List<StudyGroup> groups = new ArrayList<>();
    groups.addAll(groupByFriends(applicants, nextGroupTag, academicTerm));
    groups.addAll(groupByCoursePreference(applicants, nextGroupTag, academicTerm));
    return List.copyOf(groups);
  }

  private List<StudyGroup> groupByFriends(
      List<StudyApplicant> applicants,
      AtomicInteger nextGroupTag,
      AcademicTerm academicTerm) {
    Map<StudyApplicant, List<StudyApplicant>> friendshipMap = buildFriendshipMap(applicants);

    return new DFS<>(friendshipMap, MIN_FRIEND_GROUP_SIZE)
        .execute().stream()
            .map(
                friends ->
                    StudyGroup.of(nextGroupTag.getAndIncrement(), academicTerm, friends))
            .toList();
  }

  private Map<StudyApplicant, List<StudyApplicant>> buildFriendshipMap(
      List<StudyApplicant> applicants) {
    Map<User, StudyApplicant> userToApplicant =
        applicants.stream().collect(Collectors.toMap(StudyApplicant::getUser, Function.identity()));

    return applicants.stream()
        .filter(applicant -> !applicant.hasStudyGroup())
        .flatMap(applicant -> applicant.getPartnerRequests().stream())
        .filter(StudyPartnerRequest::isAccepted)
        .filter(request -> userToApplicant.containsKey(request.getReceiver()))
        .collect(
            Collectors.groupingBy(
                StudyPartnerRequest::getSender,
                Collectors.mapping(
                    request -> userToApplicant.get(request.getReceiver()), Collectors.toList())));
  }

  private List<StudyGroup> groupByCoursePreference(
      List<StudyApplicant> applicants,
      AtomicInteger nextGroupTag,
      AcademicTerm academicTerm) {
    List<PreferredCourse> coursePreferences =
        applicants.stream()
            .filter(applicant -> !applicant.hasStudyGroup())
            .flatMap(applicant -> applicant.getPreferredCourses().stream())
            .sorted(
                Comparator.comparingInt(PreferredCourse::getPriority)
                    .thenComparing(preference -> preference.getCourse().getCourseId()))
            .toList();

    Map<Priority, Map<Course, List<StudyApplicant>>> priorityCourseMap = new LinkedHashMap<>();
    coursePreferences.forEach(
        preference -> {
          Priority priority = Priority.of(preference.getPriority());
          Course course = preference.getCourse();

          priorityCourseMap
              .computeIfAbsent(priority, ignored -> new LinkedHashMap<>())
              .computeIfAbsent(course, ignored -> new ArrayList<>())
              .add(preference.getApplicant());
        });

    return priorityCourseMap.values().stream()
        .flatMap(courseMap -> courseMap.values().stream())
        .map(
            bucket ->
                bucket.stream().filter(applicant -> !applicant.hasStudyGroup()).toList())
        .map(
            remaining ->
                groupBySize(
                    remaining,
                    nextGroupTag,
                    academicTerm,
                    0,
                    MIN_COURSE_GROUP_SIZE,
                    MAX_COURSE_GROUP_SIZE))
        .flatMap(Collection::stream)
        .toList();
  }

  private List<StudyGroup> groupBySize(
      List<StudyApplicant> applicants,
      AtomicInteger nextGroupTag,
      AcademicTerm academicTerm,
      int startIndex,
      int minSize,
      int maxSize) {
    int remaining = applicants.size() - startIndex;
    if (remaining < minSize) {
      return List.of();
    }

    int endIndex = startIndex + Math.min(remaining, maxSize);
    StudyGroup group =
        StudyGroup.of(
            nextGroupTag.getAndIncrement(),
            academicTerm,
            applicants.subList(startIndex, endIndex));

    List<StudyGroup> groups = new ArrayList<>();
    groups.add(group);
    groups.addAll(
        groupBySize(
            applicants, nextGroupTag, academicTerm, endIndex, minSize, maxSize));
    return groups;
  }
}
