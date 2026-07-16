package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.*;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.exception.UserNotFoundException;
import edu.handong.csee.histudy.matching.domain.MatchingPolicy;
import edu.handong.csee.histudy.repository.*;
import edu.handong.csee.histudy.repository.StudyApplicantRepository;
import edu.handong.csee.histudy.util.ImagePathMapper;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {
  private final StudyGroupRepository studyGroupRepository;
  private final UserRepository userRepository;
  private final AcademicTermRepository academicTermRepository;
  private final StudyApplicantRepository studyApplicantRepository;
  private final StudyReportRepository studyReportRepository;

  private final ImagePathMapper imagePathMapper;
  private final MatchingPolicy matchingPolicy = new MatchingPolicy();

  public List<TeamDto> getTeams(String email) {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<StudyGroup> groups = studyGroupRepository.findAllByAcademicTerm(currentTerm);

    return groups.stream()
        .map(
            group -> {
              List<StudyReport> reports =
                  studyReportRepository.findAllByStudyGroupOrderByCreatedDateDesc(group);
              List<StudyApplicant> applicants = studyApplicantRepository.findAllByStudyGroup(group);

              Map<User, StudyApplicant> applicantMap =
                  applicants.stream()
                      .collect(Collectors.toMap(StudyApplicant::getUser, Function.identity()));

              return new TeamDto(group, reports, applicantMap);
            })
        .toList();
  }

  public TeamReportDto getTeamReports(long id, String email) {
    StudyGroup studyGroup = studyGroupRepository.findById(id).orElseThrow();
    List<UserDto.UserBasic> users =
        studyGroup.getMembers().stream()
            .map(StudyApplicant::getUser)
            .map(UserDto.UserBasic::new)
            .toList();

    List<StudyReport> studyReports =
        studyReportRepository.findAllByStudyGroupOrderByCreatedDateDesc(studyGroup);
    List<ReportDto.ReportBasic> reports =
        studyReports.stream()
            .map(
                report -> {
                  Map<Long, String> imgFullPaths =
                      imagePathMapper.parseImageToMapWithFullPath(report.getImages());
                  return new ReportDto.ReportBasic(report, imgFullPaths);
                })
            .toList();

    return new TeamReportDto(
        studyGroup.getStudyGroupId(),
        studyGroup.getTag(),
        users,
        calculateTotalMinutes(studyReports),
        reports);
  }

  private long calculateTotalMinutes(List<StudyReport> reports) {
    return reports.stream().mapToLong(StudyReport::getTotalMinutes).sum();
  }

  public List<UserDto.UserMeWithMasking> getTeamUsers(String email) {
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    StudyGroup studyGroup = studyGroupRepository.findByUserAndTerm(user, currentTerm).orElseThrow();

    return studyGroup.getMembers().stream()
        .map(StudyApplicant::getUser)
        .map(_user -> new UserDto.UserMeWithMasking(_user, studyGroup.getTag()))
        .toList();
  }

  public TeamRankDto getAllTeams() {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<StudyGroup> currentStudyGroups = studyGroupRepository.findAllByAcademicTerm(currentTerm);

    List<TeamRankDto.TeamInfo> teams =
        currentStudyGroups.stream()
            .map(
                group -> {
                  List<StudyReport> reports =
                      studyReportRepository.findAllByStudyGroupOrderByCreatedDateDesc(group);

                  String path =
                      reports.stream()
                          .findFirst()
                          .flatMap(
                              report ->
                                  report.getImages().stream()
                                      .max(Comparator.comparing(ReportImage::getCreatedDate))
                                      .map(ReportImage::getPath))
                          .orElse(null);
                  String fullPath = imagePathMapper.getFullPath(path);

                  return new TeamRankDto.TeamInfo(group, reports, fullPath);
                })
            .sorted(Comparator.comparing(TeamRankDto.TeamInfo::getTotalMinutes).reversed())
            .toList();
    return new TeamRankDto(teams);
  }

  public void matchTeam() {
    AcademicTerm current =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<StudyApplicant> allApplicants = studyApplicantRepository.findUnassignedApplicants(current);

    if (allApplicants.isEmpty()) {
      return;
    }

    int latestGroupTag = studyGroupRepository.countMaxTag(current).orElse(0);
    List<StudyGroup> allMatchedGroups =
        matchingPolicy.match(allApplicants, current, latestGroupTag + 1);

    if (!allMatchedGroups.isEmpty()) {
      studyGroupRepository.saveAll(allMatchedGroups);
    }
  }
}
