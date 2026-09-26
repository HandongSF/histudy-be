package edu.handong.csee.histudy.service.repository.fake;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.repository.StudyReportRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class FakeStudyReportRepository implements StudyReportRepository {

  private final List<StudyReport> store = new ArrayList<>();
  private Long sequence = 1L;
  private Long imageSequence = 1L;
  private Long participantSequence = 1L;
  private Long courseSequence = 1L;

  @Override
  public List<StudyReport> findAllByStudyGroupOrderByCreatedDateDesc(StudyGroup studyGroup) {
    return store.stream()
        .filter(r -> r.getStudyGroup().equals(studyGroup))
        .sorted(Comparator.comparing(StudyReport::getCreatedDate).reversed())
        .toList();
  }

  @Override
  public boolean existsByStudyGroup(StudyGroup studyGroup) {
    return store.stream().anyMatch(report -> report.getStudyGroup().equals(studyGroup));
  }

  @Override
  public Optional<StudyReport> findById(Long id) {
    return store.stream().filter(report -> report.getStudyReportId().equals(id)).findFirst();
  }

  @Override
  public void delete(StudyReport report) {
    store.removeIf(r -> r.equals(report));
  }

  @Override
  public StudyReport save(StudyReport report) {
    if (report.getStudyReportId() == null) {
      ReflectionTestUtils.setField(report, "studyReportId", sequence++);
      ReflectionTestUtils.setField(report, "createdDate", LocalDateTime.now());
    }
    report
        .getImages()
        .forEach(
            image -> {
              if (image.getReportImageId() == null) {
                ReflectionTestUtils.setField(image, "reportImageId", imageSequence++);
                ReflectionTestUtils.setField(image, "createdDate", LocalDateTime.now());
              }
              ReflectionTestUtils.setField(image, "lastModifiedDate", LocalDateTime.now());
            });
    report
        .getParticipants()
        .forEach(
            participant -> {
              if (ReflectionTestUtils.getField(participant, "studyParticipantId") == null) {
                ReflectionTestUtils.setField(
                    participant, "studyParticipantId", participantSequence++);
                ReflectionTestUtils.setField(participant, "createdDate", LocalDateTime.now());
              }
              ReflectionTestUtils.setField(participant, "lastModifiedDate", LocalDateTime.now());
            });
    report
        .getCourses()
        .forEach(
            studyCourse -> {
              if (ReflectionTestUtils.getField(studyCourse, "studyCourseId") == null) {
                ReflectionTestUtils.setField(studyCourse, "studyCourseId", courseSequence++);
                ReflectionTestUtils.setField(studyCourse, "createdDate", LocalDateTime.now());
              }
              ReflectionTestUtils.setField(studyCourse, "lastModifiedDate", LocalDateTime.now());
            });
    ReflectionTestUtils.setField(report, "lastModifiedDate", LocalDateTime.now());
    store.removeIf(existing -> existing.getStudyReportId().equals(report.getStudyReportId()));
    store.add(report);
    return report;
  }

  @Override
  public long count() {
    return store.size();
  }

  @Override
  public long countByStudyGroupAcademicTerm(AcademicTerm academicTerm) {
    return store.stream()
        .filter(report -> report.getStudyGroup().getAcademicTerm().equals(academicTerm))
        .count();
  }

  @Override
  public long sumTotalMinutes() {
    return store.stream().mapToLong(StudyReport::getTotalMinutes).sum();
  }

  @Override
  public long sumTotalMinutesByStudyGroupAcademicTerm(AcademicTerm academicTerm) {
    return store.stream()
        .filter(report -> report.getStudyGroup().getAcademicTerm().equals(academicTerm))
        .mapToLong(StudyReport::getTotalMinutes)
        .sum();
  }

  public List<StudyReport> findAll() {
    return new ArrayList<>(store);
  }
}
