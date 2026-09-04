package edu.handong.csee.histudy.service;

import static edu.handong.csee.histudy.util.ImageDirectories.REPORTS;
import static org.springframework.util.ResourceUtils.isUrl;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.ReportImage;
import edu.handong.csee.histudy.domain.StudyGroup;
import edu.handong.csee.histudy.domain.StudyReport;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.exception.*;
import edu.handong.csee.histudy.repository.*;
import edu.handong.csee.histudy.util.ImagePathMapper;
import edu.handong.csee.histudy.util.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class ImageService {

  @Value("${custom.resource.location}")
  private String imageBaseLocation;

  private final AcademicTermRepository academicTermRepository;
  private final UserRepository userRepository;
  private final StudyReportRepository studyReportRepository;
  private final StudyApplicantRepository studyApplicantRepository;

  private final ImagePathMapper imagePathMapper;
  private final StudyGroupRepository studyGroupRepository;

  public String getImagePaths(
      String email, MultipartFile imageAsFormData, Optional<Long> reportIdOr) {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);
    StudyGroup studyGroup =
        studyGroupRepository
            .findByUserAndTerm(user, currentTerm)
            .orElseThrow(StudyGroupNotFoundException::new);

    if (reportIdOr.isPresent()) {
      Long id = reportIdOr.get();
      StudyReport report =
          studyReportRepository.findById(id).orElseThrow(ReportNotFoundException::new);
      if (!Objects.equals(
          report.getStudyGroup().getStudyGroupId(), studyGroup.getStudyGroupId())) {
        throw new ReportNotFoundException();
      }
      Optional<String> sameResource = getSameContent(imageAsFormData, report);

      if (sameResource.isPresent()) {
        return imagePathMapper.getFullPath(sameResource.get());
      }
    }
    int year = currentTerm.getAcademicYear();
    int semester = currentTerm.getSemester().getNumber();
    String formattedDateTime = Utils.getCurrentFormattedDateTime("yyyyMMdd_HHmmss");

    String originalName = Objects.requireNonNullElse(imageAsFormData.getOriginalFilename(), ".jpg");
    String extension = originalName.substring(originalName.lastIndexOf("."));

    // yyyy-{1|2|3|4}-group{%02d}-report_{yyyyMMdd}_{HHmmss}.{extension}
    // e.g. 2023-2-group07-report_20230923_123456.jpg
    String pathname =
        String.format(
            REPORTS + "%d-%d-group%02d-report_%s%s",
            year,
            semester,
            studyGroup.getTag(),
            formattedDateTime,
            extension);
    String savedImagePath = saveImage(imageAsFormData, pathname);

    return imagePathMapper.getFullPath(savedImagePath);
  }

  private String saveImage(MultipartFile image, String pathname) {
    try {
      File file = new File(imageBaseLocation + pathname);
      File dir = file.getParentFile();

      if (!dir.exists()) {
        dir.mkdirs();
      }
      image.transferTo(file);
      return pathname;
    } catch (IOException e) {
      throw new FileTransferException();
    }
  }

  private Optional<String> getSameContent(MultipartFile src, StudyReport report) {
    List<String> targetPaths =
        report.getImages().stream().map(ReportImage::getPath).toList();

    return targetPaths.stream()
        .filter(
            path -> {
              try {
                return (isUrl(path))
                    ? contentMatches(src, new URL(path))
                    : contentMatches(src, Path.of(imageBaseLocation + path));
              } catch (MalformedURLException e) {
                throw new RuntimeException(e);
              }
            })
        .findAny();
  }

  private boolean contentMatches(MultipartFile src, Path targetPath) {
    try {
      byte[] targetContent = Files.readAllBytes(targetPath);
      return contentMatches(src.getBytes(), targetContent);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean contentMatches(MultipartFile src, URL targetPath) {
    try (InputStream in = targetPath.openStream()) {
      byte[] targetContent = IOUtils.toByteArray(in);
      return contentMatches(src.getBytes(), targetContent);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean contentMatches(byte[] sourceContent, byte[] targetContent) {
    return Arrays.equals(sourceContent, targetContent);
  }
}
