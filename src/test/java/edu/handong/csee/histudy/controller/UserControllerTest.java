package edu.handong.csee.histudy.controller;

import static edu.handong.csee.histudy.support.AuthClaimsFactory.invalidRoleClaims;
import static edu.handong.csee.histudy.support.AuthClaimsFactory.userClaims;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.handong.csee.histudy.controller.form.UserForm;
import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.interceptor.AuthenticationInterceptor;
import edu.handong.csee.histudy.jwt.JwtPair;
import edu.handong.csee.histudy.service.DiscordService;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.UserService;
import edu.handong.csee.histudy.service.command.SignUpCommand;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(UserController.class)
class UserControllerTest {

  private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthenticationInterceptor authenticationInterceptor;

  @MockitoBean private UserService userService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private DiscordService discordService;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    mockMvc =
        MockMvcBuilders.standaloneSetup(new UserController(userService, jwtService))
            .setControllerAdvice(new ExceptionController(discordService))
            .addInterceptors(authenticationInterceptor)
            .build();
  }

  @Test
  void 사용자가_회원가입하면_요청값을_서비스명령으로_전달하고_토큰을_발급한다() throws Exception {
    // Given
    UserForm userForm = new UserForm("google-sub-123", "Test User", "user@test.com", "22500101");
    JwtPair tokens = new JwtPair(List.of("access-token", "refresh-token"));

    when(jwtService.issueToken("user@test.com", "Test User", Role.USER)).thenReturn(tokens);

    // When
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(userForm)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.isRegistered").value(true))
        .andExpect(jsonPath("$.tokenType").value("Bearer "))
        .andExpect(jsonPath("$.role").value("USER"));

    // Then
    verify(userService)
        .signUp(new SignUpCommand("google-sub-123", "Test User", "user@test.com", "22500101"));
    verify(jwtService).issueToken("user@test.com", "Test User", Role.USER);
  }

  @Test
  void 사용자가_유저검색시_성공_V1() throws Exception {
    String token = "Bearer access-token";
    Claims claims = userClaims("user@test.com");

    User user = mock(User.class);
    when(user.getEmail()).thenReturn("friend@test.com");
    when(user.getRole()).thenReturn(Role.USER);
    List<User> users = List.of(user);

    when(jwtService.extractToken(any(Optional.class))).thenReturn("access-token");
    when(jwtService.validate(anyString())).thenReturn(claims);
    when(userService.search(any(Optional.class))).thenReturn(users);

    mockMvc
        .perform(
            get("/api/users").param("search", "friend").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 사용자가_유저검색시_성공_V2() throws Exception {
    String token = "Bearer access-token";
    Claims claims = userClaims("user@test.com");

    User user = mock(User.class);
    when(user.getEmail()).thenReturn("friend@test.com");
    when(user.getRole()).thenReturn(Role.USER);
    List<User> users = List.of(user);

    when(jwtService.extractToken(any(Optional.class))).thenReturn("access-token");
    when(jwtService.validate(anyString())).thenReturn(claims);
    when(userService.search(any(Optional.class))).thenReturn(users);

    mockMvc
        .perform(
            get("/api/v2/users").param("search", "friend").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 사용자가_내정보조회시_성공() throws Exception {
    Claims claims = userClaims("user@test.com");

    UserDto.UserMe userMe = mock(UserDto.UserMe.class);
    when(userService.getUserMe(any(Optional.class))).thenReturn(userMe);

    mockMvc
        .perform(get("/api/users/me").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 사용자가_신청정보조회시_성공_V1() throws Exception {
    Claims claims = userClaims("user@test.com");

    StudyApplicant applicant = mock(StudyApplicant.class);
    when(applicant.getPartnerRequests()).thenReturn(List.of());
    when(applicant.getPreferredCourses()).thenReturn(List.of());

    when(userService.getUserInfo(anyString())).thenReturn(Optional.of(applicant));

    mockMvc
        .perform(get("/api/users/me/forms").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 사용자가_신청정보없어도_조회성공_V1() throws Exception {
    Claims claims = userClaims("user@test.com");

    when(userService.getUserInfo(anyString())).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/users/me/forms").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 사용자가_신청정보조회시_성공_V2() throws Exception {
    Claims claims = userClaims("user@test.com");

    StudyApplicant applicant = mock(StudyApplicant.class);
    when(applicant.getPartnerRequests()).thenReturn(List.of());
    when(applicant.getPreferredCourses()).thenReturn(List.of());

    when(userService.getUserInfo(anyString())).thenReturn(Optional.of(applicant));

    mockMvc
        .perform(get("/api/v2/users/me/forms").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 사용자가_신청정보없어도_조회성공_V2() throws Exception {
    Claims claims = userClaims("user@test.com");

    when(userService.getUserInfo(anyString())).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v2/users/me/forms").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 권한없는사용자가_접근시_실패() throws Exception {
    Claims claims = invalidRoleClaims("user@test.com");

    mockMvc
        .perform(get("/api/users/me").requestAttr("claims", claims))
        .andExpect(status().isForbidden());
  }

  @Test
  void 사용자가_키워드없이유저검색시_성공() throws Exception {
    String token = "Bearer access-token";
    Claims claims = userClaims("user@test.com");

    when(jwtService.extractToken(any(Optional.class))).thenReturn("access-token");
    when(jwtService.validate(anyString())).thenReturn(claims);
    when(userService.search(any(Optional.class))).thenReturn(List.of());

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }
}
