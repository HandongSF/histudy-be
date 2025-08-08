package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.UserForm;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.ApplyFormDto;
import edu.handong.csee.histudy.dto.CourseDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.jwt.JwtPair;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.UserService;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final JwtService jwtService;

  @PostMapping("/api/users")
  public ResponseEntity<UserDto.UserLogin> createUser(@RequestBody UserForm userForm) {
    userService.signUp(userForm);
    JwtPair tokens = jwtService.issueToken(userForm.getEmail(), userForm.getName(), Role.USER);

    return ResponseEntity.ok(
        UserDto.UserLogin.builder()
            .isRegistered(true)
            .tokenType("Bearer ")
            .tokens(tokens)
            .role(Role.USER.name())
            .build());
  }

  /**
   * 스터디 그룹 신청 단계에서 같이 스터디할 유저를 검색하는 API
   *
   * @param keyword 검색 키워드: 이름 또는 학번 또는 이메일
   * @param header 액세스 토큰
   * @return 유저 목록
   * @see #searchUserWithMasking(Optional, Optional)
   * @deprecated 마스킹된 유저 정보를 반환하는 v2 API를 사용할 것
   */
  @Deprecated
  @GetMapping("/api/users")
  public ResponseEntity<UserDto> searchUser(
      @RequestParam(name = "search") Optional<String> keyword,
      @RequestHeader(HttpHeaders.AUTHORIZATION) Optional<String> header) {
    String token = jwtService.extractToken(header);
    Claims claims = jwtService.validate(token);
    String email = claims.getSubject();

    List<UserDto.UserMatching> users =
        userService.search(keyword).stream()
            .filter(Role::isNotAdmin)
            .filter(u -> !u.getEmail().equals(email))
            .map(UserDto.UserMatching::new)
            .toList();

    return ResponseEntity.ok(new UserDto(users));
  }

  /**
   * 스터디 그룹 신청 단계에서 같이 스터디할 유저를 검색하는 API
   *
   * <p>원래 토큰 검증은 인터셉터에서 처리하고 있으나, HTTP 메서드만 다르고 동일한 URI를 가지는 회원가입 API와 요청을 구분하기가 번거로워서 이 API에 한해서만
   * 컨트롤러에 검증을 위임하였다.
   *
   * @param keyword 검색 키워드: 이름 또는 학번 또는 이메일
   * @param header 액세스 토큰
   * @return 유저 목록
   */
  @GetMapping("/api/v2/users")
  public ResponseEntity<UserDto> searchUserWithMasking(
      @RequestParam(name = "search") Optional<String> keyword,
      @RequestHeader(HttpHeaders.AUTHORIZATION) Optional<String> header) {
    String token = jwtService.extractToken(header);
    Claims claims = jwtService.validate(token);
    String email = claims.getSubject();

    List<UserDto.UserMatchingWithMasking> users =
        userService.search(keyword).stream()
            .filter(Role::isNotAdmin)
            .filter(u -> !u.getEmail().equals(email))
            .map(UserDto.UserMatchingWithMasking::new)
            .toList();

    return ResponseEntity.ok(new UserDto(users));
  }

  @GetMapping("/api/users/me")
  public ResponseEntity<UserDto.UserMe> getMyInfo(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.values())) {
      UserDto.UserMe info = userService.getUserMe(Optional.ofNullable(claims.getSubject()));
      return ResponseEntity.ok(info);
    }
    throw new ForbiddenException();
  }

  /**
   * 나의 스터디 신청 정보를 조회하는 API
   *
   * @param claims 토큰 페이로드
   * @return 스터디 신청 내역
   * @see #getMyApplicationFormWithMasking(Claims)
   * @deprecated 마스킹된 유저 정보를 반환하는 v2 API를 사용할 것
   */
  @Deprecated
  @GetMapping("/api/users/me/forms")
  public ResponseEntity<ApplyFormDto> getMyApplicationForm(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.USER)) {
      Optional<StudyApplicant> formOr = userService.getUserInfo(claims.getSubject());

      return ResponseEntity.ok(
          formOr
              .map(ApplyFormDto::new)
              .orElseGet(() -> new ApplyFormDto(Collections.emptyList(), Collections.emptyList())));
    }
    throw new ForbiddenException();
  }

  /**
   * 나의 스터디 신청 정보를 조회하는 API
   *
   * <p>스터디 신청시 선택한 유저 정보와 강의 목록을 보여준다. 이때 유저 정보(학번)는 마스킹 처리한다.
   *
   * @param claims 토큰 페이로드
   * @return 스터디 신청 내역
   */
  @GetMapping("/api/v2/users/me/forms")
  public ResponseEntity<ApplyFormDto> getMyApplicationFormWithMasking(
      @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.USER)) {
      Optional<StudyApplicant> formOr = userService.getUserInfo(claims.getSubject());

      return ResponseEntity.ok(
          formOr
              .map(
                  form ->
                      new ApplyFormDto(
                          form.getPartnerRequests().stream()
                              .map(StudyPartnerRequest::getReceiver)
                              .map(UserDto.UserBasicWithMasking::new)
                              .toList(),
                          form.getPreferredCourses().stream()
                              .sorted(Comparator.comparing(PreferredCourse::getPriority))
                              .map(PreferredCourse::getCourse)
                              .map(CourseDto.CourseInfo::new)
                              .toList()))
              .orElseGet(() -> new ApplyFormDto(Collections.emptyList(), Collections.emptyList())));
    }
    throw new ForbiddenException();
  }
}
