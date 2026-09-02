package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.CourseDto;
import edu.handong.csee.histudy.dto.CourseIdDto;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.exception.InvalidCourseCsvException;
import edu.handong.csee.histudy.service.CourseService;
import edu.handong.csee.histudy.util.CSVResolver;
import edu.handong.csee.histudy.util.CourseCSV;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

  private static final Set<String> CSV_CONTENT_TYPES =
      Set.of("text/csv", "application/csv", "application/vnd.ms-excel");

  private final CourseService courseService;

  @PostMapping(consumes = {"multipart/form-data"})
  public ResponseEntity<Void> importCourses(
      @RequestParam("file") MultipartFile file, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      if (file.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
      }
      validateCsvFile(file);
      List<CourseCSV> courseData = CSVResolver.of(file).resolve();
      courseService.replaceCourses(courseData);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    throw new ForbiddenException();
  }

  private void validateCsvFile(MultipartFile file) {
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new InvalidCourseCsvException("CSV 파일만 업로드할 수 있습니다.");
    }

    String contentType = file.getContentType();
    if (contentType != null
        && !CSV_CONTENT_TYPES.contains(contentType.split(";", 2)[0].toLowerCase(Locale.ROOT))) {
      throw new InvalidCourseCsvException("CSV 파일만 업로드할 수 있습니다.");
    }
  }

  @PostMapping("/delete")
  public int deleteCourse(@RequestBody CourseIdDto dto, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      return courseService.deleteCourse(dto);
    }
    throw new ForbiddenException();
  }

  @DeleteMapping("/{courseId}")
  public ResponseEntity<Void> deleteCurrentCourse(
      @PathVariable Long courseId, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      courseService.deleteCurrentCourse(courseId);
      return ResponseEntity.noContent().build();
    }
    throw new ForbiddenException();
  }

  @GetMapping
  public ResponseEntity<CourseDto> getCourses(
      @RequestParam(name = "search", required = false) String keyword,
      @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN, Role.USER)) {
      List<CourseDto.CourseInfo> courses =
          (keyword == null || keyword.isBlank())
              ? courseService.getCurrentCourses()
              : courseService.search(keyword.trim());

      return ResponseEntity.ok(new CourseDto(courses));
    }
    throw new ForbiddenException();
  }
}
