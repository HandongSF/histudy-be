package edu.handong.csee.histudy.controller;

import static edu.handong.csee.histudy.support.AuthClaimsFactory.memberClaims;
import static edu.handong.csee.histudy.support.AuthClaimsFactory.userClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.handong.csee.histudy.controller.form.ReportForm;
import edu.handong.csee.histudy.dto.CourseDto;
import edu.handong.csee.histudy.dto.ReportDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.interceptor.AuthenticationInterceptor;
import edu.handong.csee.histudy.service.CourseService;
import edu.handong.csee.histudy.service.DiscordService;
import edu.handong.csee.histudy.service.ImageService;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.ReportService;
import edu.handong.csee.histudy.service.TeamService;
import edu.handong.csee.histudy.service.command.ReportCommand;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(TeamController.class)
class TeamControllerTest {

  private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthenticationInterceptor authenticationInterceptor;

  @MockitoBean private ReportService reportService;

  @MockitoBean private CourseService courseService;

  @MockitoBean private TeamService teamService;

  @MockitoBean private ImageService imageService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private DiscordService discordService;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new TeamController(
                    reportService,
                    courseService,
                    teamService,
                    imageService))
            .setControllerAdvice(new ExceptionController(discordService))
            .addInterceptors(authenticationInterceptor)
            .build();
  }

  @Test
  void 그룹원이_스터디보고서생성시_성공() throws Exception {
    // Given
    Claims claims = memberClaims("member@test.com");

    ReportForm form =
        ReportForm.builder()
            .title("1주차")
            .courses(List.of(1L))
            .content("Study content")
            .totalMinutes(120L)
            .participants(List.of(2L))
            .images(List.of("/path/to/image.jpg"))
            .build();

    ReportDto.ReportInfo reportInfo = mock(ReportDto.ReportInfo.class);
    when(reportService.createReport(any(ReportCommand.class), anyString())).thenReturn(reportInfo);

    // When
    mockMvc
        .perform(
            post("/api/team/reports")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));

    // Then
    ArgumentCaptor<ReportCommand> commandCaptor = ArgumentCaptor.forClass(ReportCommand.class);
    verify(reportService).createReport(commandCaptor.capture(), eq("member@test.com"));
    ReportCommand command = commandCaptor.getValue();
    assertThat(command.title()).isEqualTo("1주차");
    assertThat(command.content()).isEqualTo("Study content");
    assertThat(command.totalMinutes()).isEqualTo(120L);
    assertThat(command.participantIds()).containsExactly(2L);
    assertThat(command.imageUrls()).containsExactly("/path/to/image.jpg");
    assertThat(command.courseIds()).containsExactly(1L);
  }

  @Test
  void 그룹원이_보고서목록조회시_성공() throws Exception {
    Claims claims = memberClaims("member@test.com");

    List<ReportDto.ReportInfo> reports = List.of();
    when(reportService.getReports(anyString())).thenReturn(reports);

    mockMvc
        .perform(get("/api/team/reports").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 그룹원이_특정보고서조회시_성공() throws Exception {
    Claims claims = memberClaims("member@test.com");

    ReportDto.ReportInfo reportInfo = mock(ReportDto.ReportInfo.class);
    when(reportService.getReport(anyLong())).thenReturn(Optional.of(reportInfo));

    mockMvc
        .perform(get("/api/team/reports/1").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 그룹원이_없는보고서조회시_실패() throws Exception {
    Claims claims = memberClaims("member@test.com");

    when(reportService.getReport(anyLong())).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/team/reports/1").requestAttr("claims", claims))
        .andExpect(status().isNotFound());
  }

  @Test
  void 그룹원이_보고서수정시_성공() throws Exception {
    // Given
    Claims claims = memberClaims("member@test.com");

    ReportForm form =
        ReportForm.builder()
            .title("수정된 제목")
            .courses(List.of(1L))
            .content("Updated content")
            .totalMinutes(150L)
            .participants(List.of(2L))
            .images(List.of("/path/to/updated_image.jpg"))
            .build();

    when(reportService.updateReport(anyLong(), any(ReportCommand.class))).thenReturn(true);

    // When
    mockMvc
        .perform(
            patch("/api/team/reports/1")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk());

    // Then
    ArgumentCaptor<ReportCommand> commandCaptor = ArgumentCaptor.forClass(ReportCommand.class);
    verify(reportService).updateReport(eq(1L), commandCaptor.capture());
    ReportCommand command = commandCaptor.getValue();
    assertThat(command.title()).isEqualTo("수정된 제목");
    assertThat(command.content()).isEqualTo("Updated content");
    assertThat(command.totalMinutes()).isEqualTo(150L);
    assertThat(command.participantIds()).containsExactly(2L);
    assertThat(command.imageUrls()).containsExactly("/path/to/updated_image.jpg");
    assertThat(command.courseIds()).containsExactly(1L);
  }

  @Test
  void 그룹원이_없는보고서수정시_실패() throws Exception {
    Claims claims = memberClaims("member@test.com");

    ReportForm form = ReportForm.builder().build();
    when(reportService.updateReport(anyLong(), any(ReportCommand.class))).thenReturn(false);

    mockMvc
        .perform(
            patch("/api/team/reports/1")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isNotFound());
  }

  @Test
  void 그룹원이_보고서삭제시_성공() throws Exception {
    Claims claims = memberClaims("member@test.com");

    when(reportService.deleteReport(anyLong())).thenReturn(true);

    mockMvc
        .perform(delete("/api/team/reports/1").requestAttr("claims", claims))
        .andExpect(status().isOk());
  }

  @Test
  void 그룹원이_없는보고서삭제시_실패() throws Exception {
    Claims claims = memberClaims("member@test.com");

    when(reportService.deleteReport(anyLong())).thenReturn(false);

    mockMvc
        .perform(delete("/api/team/reports/1").requestAttr("claims", claims))
        .andExpect(status().isNotFound());
  }

  @Test
  void 그룹원이_선택강의목록조회시_성공() throws Exception {
    Claims claims = memberClaims("member@test.com");

    List<CourseDto.CourseInfo> courses = List.of();
    when(courseService.getTeamCourses(anyString())).thenReturn(courses);

    mockMvc
        .perform(get("/api/team/courses").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 그룹원이_팀원목록조회시_성공() throws Exception {
    Claims claims = memberClaims("member@test.com");

    List<UserDto.UserMeWithMasking> users = List.of();
    when(teamService.getTeamUsers(anyString())).thenReturn(users);

    mockMvc
        .perform(get("/api/team/users").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 그룹원이_보고서이미지업로드시_성공() throws Exception {
    Claims claims = memberClaims("member@test.com");

    MockMultipartFile image =
        new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

    String imagePath = "/path/to/image.jpg";
    when(imageService.getImagePaths(anyString(), any(), any(Optional.class))).thenReturn(imagePath);

    mockMvc
        .perform(multipart("/api/team/reports/image").file(image).requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.imagePath").value(imagePath));
  }

  @Test
  void 그룹원이_특정보고서이미지업로드시_성공() throws Exception {
    Claims claims = memberClaims("member@test.com");

    MockMultipartFile image =
        new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

    String imagePath = "/path/to/image.jpg";
    when(imageService.getImagePaths(anyString(), any(), any(Optional.class))).thenReturn(imagePath);

    mockMvc
        .perform(multipart("/api/team/reports/1/image").file(image).requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.imagePath").value(imagePath));
  }

  @Test
  void 그룹원이_용량초과_보고서이미지업로드시_실패() throws Exception {
    Claims claims = memberClaims("member@test.com");

    MockMultipartFile image =
        new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

    when(imageService.getImagePaths(anyString(), any(), any(Optional.class)))
        .thenThrow(new MaxUploadSizeExceededException(5 * 1024 * 1024));

    mockMvc
        .perform(multipart("/api/team/reports/image").file(image).requestAttr("claims", claims))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.code").value(413))
        .andExpect(jsonPath("$.error").value("Payload Too Large"))
        .andExpect(jsonPath("$.message").value("업로드 가능한 파일 용량을 초과했습니다."));
  }

  @Test
  void 권한없는사용자가_보고서작성시_실패() throws Exception {
    Claims claims = userClaims("user@test.com");

    ReportForm form = ReportForm.builder().build();

    mockMvc
        .perform(
            post("/api/team/reports")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isForbidden());
  }
}
