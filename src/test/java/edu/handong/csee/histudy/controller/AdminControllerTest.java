package edu.handong.csee.histudy.controller;

import static edu.handong.csee.histudy.dto.AcademicTermDto.*;
import static edu.handong.csee.histudy.support.AuthClaimsFactory.adminClaims;
import static edu.handong.csee.histudy.support.AuthClaimsFactory.userClaims;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.handong.csee.histudy.controller.form.AcademicTermForm;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.dto.AcademicTermDto;
import edu.handong.csee.histudy.dto.TeamDto;
import edu.handong.csee.histudy.dto.TeamReportDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.interceptor.AuthenticationInterceptor;
import edu.handong.csee.histudy.matching.application.MatchingApplicationService;
import edu.handong.csee.histudy.service.AcademicTermService;
import edu.handong.csee.histudy.service.DiscordService;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.TeamService;
import edu.handong.csee.histudy.service.UserService;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

  private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthenticationInterceptor authenticationInterceptor;

  @MockitoBean private TeamService teamService;

  @MockitoBean private MatchingApplicationService matchingApplicationService;

  @MockitoBean private UserService userService;

  @MockitoBean private AcademicTermService academicTermService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private DiscordService discordService;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new AdminController(
                    teamService,
                    userService,
                    academicTermService,
                    matchingApplicationService))
            .setControllerAdvice(new ExceptionController(discordService))
            .addInterceptors(authenticationInterceptor)
            .build();
  }

  @Test
  void 관리자가_그룹활동조회시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    List<TeamDto> teams = List.of();
    when(teamService.getTeams(anyString())).thenReturn(teams);

    mockMvc
        .perform(get("/api/admin/manageGroup").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 관리자가_그룹보고서조회시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    TeamReportDto reportDto = mock(TeamReportDto.class);
    when(teamService.getTeamReports(anyLong(), anyString())).thenReturn(reportDto);

    mockMvc
        .perform(get("/api/admin/groupReport/1").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 관리자가_신청유저목록조회시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    List<UserDto.UserInfo> users = List.of();
    when(userService.getAppliedUsers()).thenReturn(users);

    mockMvc
        .perform(get("/api/admin/allUsers").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 관리자가_그룹매칭실행시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    doNothing().when(matchingApplicationService).match();

    mockMvc
        .perform(post("/api/admin/team-match").requestAttr("claims", claims))
        .andExpect(status().isCreated());

    verify(matchingApplicationService).match();
  }

  @Test
  void 일반유저가_그룹매칭실행시_실패() throws Exception {
    // Given
    Claims claims = userClaims("user@test.com");

    // When Then
    mockMvc
        .perform(post("/api/admin/team-match").requestAttr("claims", claims))
        .andExpect(status().isForbidden());

    verifyNoInteractions(matchingApplicationService);
  }

  @Test
  void 관리자가_미매칭유저조회시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    List<UserDto.UserInfo> users = List.of();
    when(userService.getUnmatchedUsers()).thenReturn(users);

    mockMvc
        .perform(get("/api/admin/unmatched-users").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 관리자가_유저지원폼삭제시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    doNothing().when(userService).deleteUserForm(anyString());

    mockMvc
        .perform(delete("/api/admin/form").requestAttr("claims", claims).param("sid", "22500101"))
        .andExpect(status().isOk());
  }

  @Test
  void 관리자가_유저정보수정시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    UserDto.UserEdit form = mock(UserDto.UserEdit.class);
    doNothing().when(userService).editUser(any(UserDto.UserEdit.class));

    mockMvc
        .perform(
            post("/api/admin/edit-user")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk());
  }

  @Test
  void 관리자가_미배정유저목록조회시_성공() throws Exception {
    Claims claims = adminClaims("admin@test.com");

    List<UserDto.UserInfo> users = List.of();
    when(userService.getAppliedWithoutGroup()).thenReturn(users);

    mockMvc
        .perform(get("/api/admin/users/unassigned").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 일반유저가_관리자API접근시_실패() throws Exception {
    Claims claims = userClaims("user@test.com");

    mockMvc
        .perform(get("/api/admin/manageGroup").requestAttr("claims", claims))
        .andExpect(status().isForbidden());
  }

  @Test
  void 관리자가_학기생성시_성공() throws Exception {
    // Given
    Claims claims = adminClaims("admin@test.com");

    AcademicTermForm form = new AcademicTermForm(2025, TermType.SPRING);
    doNothing().when(academicTermService).createAcademicTerm(2025, TermType.SPRING);

    // When & Then
    mockMvc
        .perform(
            post("/api/admin/academicTerm")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isCreated());

    verify(academicTermService).createAcademicTerm(2025, TermType.SPRING);
  }

  @Test
  void 비관리자가_학기생성시_실패() throws Exception {
    // Given
    Claims claims = userClaims("user@test.com");

    AcademicTermForm form = new AcademicTermForm(2025, TermType.SPRING);

    // When & Then
    mockMvc
        .perform(
            post("/api/admin/academicTerm")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isForbidden());

    verify(academicTermService, never()).createAcademicTerm(any(), any());
  }

  @Test
  void 관리자가_학기목록조회시_성공() throws Exception {
    // Given
    Claims claims = adminClaims("admin@test.com");

    List<AcademicTermItem> termItems =
        List.of(
            new AcademicTermItem(1L, 2024, TermType.FALL, false),
            new AcademicTermItem(2L, 2025, TermType.SPRING, true));
    AcademicTermDto response = new AcademicTermDto(termItems);
    when(academicTermService.getAllAcademicTerms()).thenReturn(response);

    // When & Then
    mockMvc
        .perform(get("/api/admin/academicTerm").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.academicTerms").isArray())
        .andExpect(jsonPath("$.academicTerms.length()").value(2))
        .andDo(MockMvcResultHandlers.print());

    verify(academicTermService).getAllAcademicTerms();
  }

  @Test
  void 비관리자가_학기목록조회시_실패() throws Exception {
    // Given
    Claims claims = userClaims("user@test.com");

    // When & Then
    mockMvc
        .perform(get("/api/admin/academicTerm").requestAttr("claims", claims))
        .andExpect(status().isForbidden());

    verify(academicTermService, never()).getAllAcademicTerms();
  }

  @Test
  void 관리자가_현재학기설정시_성공() throws Exception {
    // Given
    Claims claims = adminClaims("admin@test.com");

    Long termId = 1L;
    doNothing().when(academicTermService).setCurrentTerm(termId);

    // When & Then
    mockMvc
        .perform(
            patch("/api/admin/academicTerm/{id}/current", termId).requestAttr("claims", claims))
        .andExpect(status().isOk());

    verify(academicTermService).setCurrentTerm(termId);
  }

  @Test
  void 비관리자가_현재학기설정시_실패() throws Exception {
    // Given
    Claims claims = userClaims("user@test.com");

    Long termId = 1L;

    // When & Then
    mockMvc
        .perform(
            patch("/api/admin/academicTerm/{id}/current", termId).requestAttr("claims", claims))
        .andExpect(status().isForbidden());

    verify(academicTermService, never()).setCurrentTerm(any());
  }

  @Test
  void 학기생성시_잘못된_JSON형식일때_실패() throws Exception {
    // Given
    Claims claims = adminClaims("admin@test.com");

    String invalidJson = "{\"year\": \"invalid\", \"term\": \"SPRING\"}";

    // When & Then
    mockMvc
        .perform(
            post("/api/admin/academicTerm")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(invalidJson))
        .andExpect(status().isBadRequest());

    verify(academicTermService, never()).createAcademicTerm(any(), any());
  }
}
