package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.ReportDto;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.exception.ReportNotFoundException;
import edu.handong.csee.histudy.exception.UserNotFoundException;
import edu.handong.csee.histudy.repository.*;
import edu.handong.csee.histudy.service.command.ReportCommand;
import edu.handong.csee.histudy.util.ImagePathMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
  private final StudyReportRepository studyReportRepository;
  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final AcademicTermRepository academicTermRepository;

  private final ImagePathMapper imagePathMapper;

  public ReportDto.ReportInfo createReport(ReportCommand command, String email) {
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    StudyGroup studyGroup = studyGroupRepository.findByUserAndTerm(user, currentTerm).orElseThrow();

    List<User> participants =
        command.participantIds().stream()
            .map(userRepository::findById)
            .flatMap(Optional::stream)
            .toList();

    List<Course> courses =
        command.courseIds().stream()
            .map(courseRepository::findById)
            .flatMap(Optional::stream)
            .toList();

    // parse image path to filename
    // /path/to/image.png -> image.png
    List<String> imageFilenames = imagePathMapper.extractFilename(command.imageUrls());

    StudyReport report =
        StudyReport.builder()
            .title(command.title())
            .content(command.content())
            .totalMinutes(command.totalMinutes())
            .studyGroup(studyGroup)
            .participants(participants)
            .images(imageFilenames)
            .courses(courses)
            .build();

    StudyReport saved = studyReportRepository.save(report);
    Map<Long, String> imgFullPaths = imagePathMapper.parseImageToMapWithFullPath(saved.getImages());
    return new ReportDto.ReportInfo(saved, imgFullPaths);
  }

  public List<ReportDto.ReportInfo> getReports(String email) {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);
    StudyGroup studyGroup = studyGroupRepository.findByUserAndTerm(user, currentTerm).orElseThrow();
    List<StudyReport> studyReports =
        studyReportRepository.findAllByStudyGroupOrderByCreatedDateDesc(studyGroup);

    return studyReports.stream()
        .map(
            report -> {
              Map<Long, String> imgFullPaths =
                  imagePathMapper.parseImageToMapWithFullPath(report.getImages());
              return new ReportDto.ReportInfo(report, imgFullPaths);
            })
        .toList();
  }

  public boolean updateReport(Long reportId, ReportCommand command) {
    List<User> participants =
        command.participantIds().stream()
            .map(userRepository::findById)
            .flatMap(Optional::stream)
            .toList();

    List<Course> courses =
        command.courseIds().stream()
            .map(courseRepository::findById)
            .flatMap(Optional::stream)
            .toList();

    StudyReport targetReport =
        studyReportRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);

    // parse image path to filename
    // /path/to/image.png -> image.png
    List<String> imageFilenames = imagePathMapper.extractFilename(command.imageUrls());
    targetReport.update(
        command.title(),
        command.content(),
        command.totalMinutes(),
        imageFilenames,
        participants,
        courses);

    return true;
  }

  public Optional<ReportDto.ReportInfo> getReport(Long reportId) {
    return studyReportRepository
        .findById(reportId)
        .map(
            report -> {
              Map<Long, String> imgFullPaths =
                  imagePathMapper.parseImageToMapWithFullPath(report.getImages());
              return new ReportDto.ReportInfo(report, imgFullPaths);
            });
  }

  public boolean deleteReport(Long reportId) {
    Optional<StudyReport> reportOr = studyReportRepository.findById(reportId);

    if (reportOr.isEmpty()) {
      return false;
    } else {
      studyReportRepository.delete(reportOr.get());
      return true;
    }
  }
}
