package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.CourseDto;
import edu.handong.csee.histudy.dto.CourseIdDto;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.service.CourseService;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

  private final CourseService courseService;

  @PostMapping(consumes = {"multipart/form-data"})
  public ResponseEntity<Void> importCourses(
      @RequestParam("file") MultipartFile file, @RequestAttribute Claims claims)
      throws IOException {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      if (file.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
      }
      courseService.readCourseCSV(file);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    throw new ForbiddenException();
  }

  @PostMapping("/delete")
  public int deleteCourse(@RequestBody CourseIdDto dto, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      return courseService.deleteCourse(dto);
    }
    throw new ForbiddenException();
  }

  @GetMapping
  public ResponseEntity<CourseDto> getCourses(
      @RequestParam(name = "search", required = false) String keyword,
      @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN, Role.USER)) {
      List<CourseDto.CourseInfo> courses =
          (keyword == null) ? courseService.getCurrentCourses() : courseService.search(keyword);

      return ResponseEntity.ok(new CourseDto(courses));
    }
    throw new ForbiddenException();
  }
}
