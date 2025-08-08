package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.ApplyForm;
import edu.handong.csee.histudy.controller.form.ApplyFormV2;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.ApplyFormDto;
import edu.handong.csee.histudy.dto.CourseDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.service.UserService;
import io.jsonwebtoken.Claims;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplyFormController {

  private final UserService userService;

  /**
   * 스터디 신청 정보를 등록하는 API
   *
   * @param form 신청 정보(같이 하고 싶은 학생 목록, 강의 목록)
   * @param claims 토큰 페이로드
   * @return 신청 내역
   * @see #applyForStudy(ApplyFormV2, Claims)
   * @deprecated 신청한 학생 목록을 보낼 때 더 이상 학번 정보를 보낼 수 없기 때문에 사용하지 않음
   */
  @Deprecated
  @PostMapping("/api/forms")
  public ResponseEntity<ApplyFormDto> applyForStudy(
      @RequestBody ApplyForm form, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.USER)) {
      return ResponseEntity.ok(userService.apply(form, claims.getSubject()));
    }
    throw new ForbiddenException();
  }

  /**
   * 스터디 신청 정보를 등록하는 API (v2)
   *
   * <p>스터디 신청 단계에서 같이할 학생과 강의를 선택하여 신청 정보를 등록한다.
   *
   * @param form 신청 정보(같이 하고 싶은 학생 목록, 강의 목록)
   * @param claims 토큰 페이로드
   * @return 신청 내역
   */
  @PostMapping("/api/v2/forms")
  public ResponseEntity<ApplyFormDto> applyForStudy(
      @RequestBody ApplyFormV2 form, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.USER)) {
      StudyApplicant submittedForm =
          userService.apply(form.getFriendIds(), form.getCourseIds(), claims.getSubject());

      return ResponseEntity.ok(
          new ApplyFormDto(
              submittedForm.getPartnerRequests().stream()
                  .map(StudyPartnerRequest::getReceiver)
                  .map(UserDto.UserBasicWithMasking::new)
                  .toList(),
              submittedForm.getPreferredCourses().stream()
                  .sorted(Comparator.comparing(PreferredCourse::getPriority))
                  .map(PreferredCourse::getCourse)
                  .map(CourseDto.CourseInfo::new)
                  .toList()));
    }
    throw new ForbiddenException();
  }
}
