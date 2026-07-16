package edu.handong.csee.histudy.controller;

import static edu.handong.csee.histudy.support.AuthClaimsFactory.memberClaims;
import static edu.handong.csee.histudy.support.AuthClaimsFactory.userClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.handong.csee.histudy.controller.form.ApplyForm;
import edu.handong.csee.histudy.controller.form.ApplyFormV2;
import edu.handong.csee.histudy.domain.StudyApplicant;
import edu.handong.csee.histudy.dto.ApplyFormDto;
import edu.handong.csee.histudy.interceptor.AuthenticationInterceptor;
import edu.handong.csee.histudy.service.DiscordService;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.UserService;
import edu.handong.csee.histudy.service.command.LegacyStudyApplicationCommand;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(ApplyFormController.class)
class ApplyFormControllerTest {

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
        MockMvcBuilders.standaloneSetup(new ApplyFormController(userService))
            .setControllerAdvice(new ExceptionController(discordService))
            .addInterceptors(authenticationInterceptor)
            .build();
  }

  @Test
  void 사용자가_V1으로_스터디를_신청하면_레거시명령과_인증이메일을_서비스에_전달한다() throws Exception {
    // Given
    Claims claims = userClaims("user@test.com");

    ApplyForm form =
        ApplyForm.builder().friendIds(List.of("22500101")).courseIds(List.of(1L)).build();

    ApplyFormDto result = mock(ApplyFormDto.class);
    when(userService.apply(any(LegacyStudyApplicationCommand.class), anyString()))
        .thenReturn(result);

    // When
    mockMvc
        .perform(
            post("/api/forms")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));

    // Then
    ArgumentCaptor<LegacyStudyApplicationCommand> commandCaptor =
        ArgumentCaptor.forClass(LegacyStudyApplicationCommand.class);
    verify(userService).apply(commandCaptor.capture(), eq("user@test.com"));
    assertThat(commandCaptor.getValue())
        .isEqualTo(new LegacyStudyApplicationCommand(List.of("22500101"), List.of(1L)));
  }

  @Test
  void 사용자가_스터디신청시_성공_V2() throws Exception {
    Claims claims = userClaims("user@test.com");

    ApplyFormV2 form =
        ApplyFormV2.builder().friendIds(List.of(1L, 2L)).courseIds(List.of(1L, 2L)).build();

    StudyApplicant applicant = mock(StudyApplicant.class);
    when(applicant.getPartnerRequests()).thenReturn(List.of());
    when(applicant.getPreferredCourses()).thenReturn(List.of());

    when(userService.apply(anyList(), anyList(), anyString())).thenReturn(applicant);

    mockMvc
        .perform(
            post("/api/v2/forms")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void 권한없는사용자가_스터디신청시_실패_V1() throws Exception {
    Claims claims = memberClaims("user@test.com");

    ApplyForm form = ApplyForm.builder().friendIds(List.of()).courseIds(List.of()).build();

    mockMvc
        .perform(
            post("/api/forms")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isForbidden());
  }

  @Test
  void 권한없는사용자가_스터디신청시_실패_V2() throws Exception {
    Claims claims = memberClaims("user@test.com");

    ApplyFormV2 form = ApplyFormV2.builder().friendIds(List.of()).courseIds(List.of()).build();

    mockMvc
        .perform(
            post("/api/v2/forms")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isForbidden());
  }
}
