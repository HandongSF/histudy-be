package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.AcademicTermForm;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.AcademicTermDto;
import edu.handong.csee.histudy.dto.TeamDto;
import edu.handong.csee.histudy.dto.TeamIdDto;
import edu.handong.csee.histudy.dto.TeamReportDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.service.AcademicTermService;
import edu.handong.csee.histudy.service.TeamService;
import edu.handong.csee.histudy.service.UserService;
import io.jsonwebtoken.Claims;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
  private final TeamService teamService;
  private final UserService userService;
  private final AcademicTermService academicTermService;

  /**
   * Returns the teams managed by the authenticated admin.
   *
   * Only accessible to users with the ADMIN role; otherwise a ForbiddenException is thrown.
   *
   * @param claims JWT claims extracted from the request; the subject (email) identifies the admin used to scope results
   * @return ResponseEntity containing a list of TeamDto (HTTP 200 OK)
   * @throws ForbiddenException if the caller is not authorized as ADMIN
   */
  @GetMapping(value = "/manageGroup")
  public ResponseEntity<List<TeamDto>> getTeams(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      String email = claims.getSubject();
      return ResponseEntity.ok(teamService.getTeams(email));
    }
    throw new ForbiddenException();
  }

  @Deprecated
  @DeleteMapping("/group")
  public ResponseEntity<Integer> deleteTeam(
      @RequestBody TeamIdDto dto, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      return ResponseEntity.ok(teamService.deleteTeam(dto, claims.getSubject()));
    }
    throw new ForbiddenException();
  }

  @GetMapping("/groupReport/{id}")
  public ResponseEntity<TeamReportDto> getTeamReports(
      @PathVariable(name = "id") long id, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      TeamReportDto res = teamService.getTeamReports(id, claims.getSubject());
      return ResponseEntity.ok(res);
    }
    throw new ForbiddenException();
  }

  /**
   * 스터디 신청한 유저 목록 조회(신청O 그룹?)
   *
   * <p>그룹 배정 여부와 관계 없이 스터디를 신청한 유저 목록을 표시한다
   *
   * @param claims 토큰 페이로드
   * @return 스터디 신청한 유저 목록
   */
  @GetMapping("/allUsers")
  public ResponseEntity<List<UserDto.UserInfo>> getAppliedUsers(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      return ResponseEntity.ok(userService.getAppliedUsers());
    }
    throw new ForbiddenException();
  }

  @PostMapping("/team-match")
  public ResponseEntity<Void> matchTeam(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      teamService.matchTeam();
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    throw new ForbiddenException();
  }

  /**
   * 그룹 미배정 학생 목록 조회(신청? 그룹X)
   *
   * <p>스터디 신청 여부와 관계 없이 가입된 유저 중에서 그룹이 배정되지 않은 유저 목록을 표시한다
   *
   * @param claims 토큰 페이로드
   * @return 그룹 미배정 학생 목록
   */
  @GetMapping("/unmatched-users")
  public ResponseEntity<List<UserDto.UserInfo>> getUnmatchedUsers(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      return ResponseEntity.ok(userService.getUnmatchedUsers());
    }
    throw new ForbiddenException();
  }

  @DeleteMapping("/form")
  public void deleteForm(@RequestParam String sid, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      userService.deleteUserForm(sid);
      return;
    }
    throw new ForbiddenException();
  }

  @PostMapping("/edit-user")
  public void editUser(@RequestBody UserDto.UserEdit form, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      userService.editUser(form);
      return;
    }
    throw new ForbiddenException();
  }

  /**
   * Get users who applied for study but are not assigned to any group.
   *
   * Returns the list of applicants who have submitted a study application but remain unassigned to a group.
   *
   * @param claims JWT claims extracted from the request (used for authorization)
   * @return a list of user info DTOs for applicants without a group
   * @throws ForbiddenException if the caller is not authorized as ADMIN
   */
  @GetMapping("/users/unassigned")
  public ResponseEntity<List<UserDto.UserInfo>> unassignedUser(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      return ResponseEntity.ok(userService.getAppliedWithoutGroup());
    }
    throw new ForbiddenException();
  }

  /**
   * Creates a new academic term. Requires ADMIN authorization.
   *
   * If the request is authorized as an admin, delegates to AcademicTermService to create
   * the term and returns HTTP 201 Created with no body. If not authorized, a
   * ForbiddenException is thrown.
   *
   * @param form   the academic term data to create
   * @param claims JWT claims from the request used to verify ADMIN role
   * @return a ResponseEntity with HTTP 201 Created and no body
   * @throws ForbiddenException if the caller is not authorized as ADMIN
   */
  @PostMapping("/academicTerm")
  public ResponseEntity<Void> createAcademicTerm(
      @RequestBody AcademicTermForm form, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      academicTermService.createAcademicTerm(form);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    throw new ForbiddenException();
  }

  /**
   * Retrieve all academic terms.
   *
   * <p>Requires an authenticated user with the ADMIN role; otherwise a {@link ForbiddenException} is thrown.
   *
   * @return a 200 OK response containing the current list of academic terms as an {@link AcademicTermDto}
   * @throws ForbiddenException if the requester is not authorized as ADMIN
   */
  @GetMapping("/academicTerm")
  public ResponseEntity<AcademicTermDto> getAllAcademicTerms(@RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      return ResponseEntity.ok(academicTermService.getAllAcademicTerms());
    }
    throw new ForbiddenException();
  }

  /**
   * Marks the academic term identified by the given id as the current term.
   *
   * This endpoint requires the caller to have the ADMIN role; otherwise a
   * ForbiddenException is thrown.
   *
   * @param id the id of the academic term to set as current
   * @param claims JWT claims of the requester (used for authorization)
   * @return a 200 OK response with no body on success
   * @throws ForbiddenException if the requester is not authorized as ADMIN
   */
  @PatchMapping("/academicTerm/{id}/current")
  public ResponseEntity<Void> setCurrentTerm(
      @PathVariable Long id, @RequestAttribute Claims claims) {
    if (Role.isAuthorized(claims, Role.ADMIN)) {
      academicTermService.setCurrentTerm(id);
      return ResponseEntity.ok().build();
    }
    throw new ForbiddenException();
  }
}
