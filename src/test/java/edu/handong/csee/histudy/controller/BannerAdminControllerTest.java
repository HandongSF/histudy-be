package edu.handong.csee.histudy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.handong.csee.histudy.controller.form.BannerReorderForm;
import edu.handong.csee.histudy.domain.Banner;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.BannerDto;
import edu.handong.csee.histudy.interceptor.AuthenticationInterceptor;
import edu.handong.csee.histudy.service.BannerService;
import edu.handong.csee.histudy.service.DiscordService;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.command.BannerCommand;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(BannerAdminController.class)
class BannerAdminControllerTest {

  private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthenticationInterceptor authenticationInterceptor;

  @MockitoBean private BannerService bannerService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private DiscordService discordService;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    mockMvc =
        MockMvcBuilders.standaloneSetup(new BannerAdminController(bannerService))
            .setControllerAdvice(new ExceptionController(discordService))
            .addInterceptors(authenticationInterceptor)
            .build();
  }

  @Test
  void 관리자가_배너목록조회시_성공() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.ADMIN.name());

    BannerDto.AdminBannerInfo info = createAdminBannerInfo(1L);
    when(bannerService.getAdminBanners()).thenReturn(List.of(info));

    // When & Then
    mockMvc
        .perform(get("/api/admin/banners").requestAttr("claims", claims))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  void 관리자가_배너생성시_성공() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.ADMIN.name());

    MockMultipartFile image =
        new MockMultipartFile("image", "banner.png", "image/png", "banner".getBytes());
    BannerDto.AdminBannerInfo info = createAdminBannerInfo(1L);

    when(bannerService.createBanner(any(BannerCommand.class))).thenReturn(info);

    // When & Then
    mockMvc
        .perform(
            multipart("/api/admin/banners")
                .file(image)
                .param("label", "Main Banner")
                .param("redirectUrl", "https://example.com")
                .param("active", "true")
                .requestAttr("claims", claims))
        .andExpect(status().isCreated())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.id").value(1L));

    ArgumentCaptor<BannerCommand> commandCaptor = ArgumentCaptor.forClass(BannerCommand.class);
    verify(bannerService).createBanner(commandCaptor.capture());
    BannerCommand command = commandCaptor.getValue();
    assertThat(command.label()).isEqualTo("Main Banner");
    assertThat(command.redirectUrl()).isEqualTo("https://example.com");
    assertThat(command.active()).isTrue();
    assertThat(command.image().originalFilename()).isEqualTo("banner.png");
    assertThat(command.image().contentType()).isEqualTo("image/png");
    assertThat(command.image().content()).isEqualTo("banner".getBytes());
  }

  @Test
  void 빈_이미지는_서비스_명령에서_이미지없음으로_변환한다() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.ADMIN.name());

    MockMultipartFile emptyImage =
        new MockMultipartFile("image", "banner.png", "image/png", new byte[0]);
    when(bannerService.createBanner(any(BannerCommand.class))).thenReturn(createAdminBannerInfo(1L));

    // When
    mockMvc
        .perform(
            multipart("/api/admin/banners")
                .file(emptyImage)
                .param("label", "Main Banner")
                .requestAttr("claims", claims))
        .andExpect(status().isCreated());

    // Then
    ArgumentCaptor<BannerCommand> commandCaptor = ArgumentCaptor.forClass(BannerCommand.class);
    verify(bannerService).createBanner(commandCaptor.capture());
    assertThat(commandCaptor.getValue().image()).isNull();
  }

  @Test
  void 제한크기를_초과한_이미지는_내용을_읽기전에_거부한다() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.ADMIN.name());

    MockMultipartFile oversizedImage =
        new MockMultipartFile("image", "banner.png", "image/png", new byte[0]) {
          @Override
          public boolean isEmpty() {
            return false;
          }

          @Override
          public long getSize() {
            return 5L * 1024 * 1024 + 1;
          }

          @Override
          public byte[] getBytes() throws IOException {
            throw new AssertionError("oversized image content should not be read");
          }
        };

    // When Then
    mockMvc
        .perform(
            multipart("/api/admin/banners")
                .file(oversizedImage)
                .param("label", "Main Banner")
                .requestAttr("claims", claims))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(bannerService);
  }

  @Test
  void 관리자가_배너순서일괄변경시_성공() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.ADMIN.name());

    BannerReorderForm form = new BannerReorderForm(List.of(2L, 1L));

    // When & Then
    mockMvc
        .perform(
            patch("/api/admin/banners/reorder")
                .requestAttr("claims", claims)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk());

    verify(bannerService).reorderBanners(List.of(2L, 1L));
  }

  @Test
  void 비관리자가_배너생성시_실패() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.USER.name());

    MockMultipartFile image =
        new MockMultipartFile("image", "banner.png", "image/png", "banner".getBytes());

    // When & Then
    mockMvc
        .perform(
            multipart("/api/admin/banners")
                .file(image)
                .param("label", "Main Banner")
                .requestAttr("claims", claims))
        .andExpect(status().isForbidden());
  }

  @Test
  void 관리자가_배너수정시_성공() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.ADMIN.name());

    MockMultipartFile image =
        new MockMultipartFile("image", "banner2.png", "image/png", "banner-2".getBytes());
    BannerDto.AdminBannerInfo info = createAdminBannerInfo(2L);

    when(bannerService.updateBanner(eq(2L), any(BannerCommand.class))).thenReturn(info);

    MockHttpServletRequestBuilder requestBuilder =
        multipart("/api/admin/banners/{bannerId}", 2L)
            .file(image)
            .param("label", "Updated Banner")
            .with(
                request -> {
                  request.setMethod("PATCH");
                  return request;
                })
            .requestAttr("claims", claims);

    // When & Then
    mockMvc
        .perform(requestBuilder)
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.id").value(2L));

    ArgumentCaptor<BannerCommand> commandCaptor = ArgumentCaptor.forClass(BannerCommand.class);
    verify(bannerService).updateBanner(eq(2L), commandCaptor.capture());
    BannerCommand command = commandCaptor.getValue();
    assertThat(command.label()).isEqualTo("Updated Banner");
    assertThat(command.redirectUrl()).isNull();
    assertThat(command.active()).isNull();
    assertThat(command.image().originalFilename()).isEqualTo("banner2.png");
    assertThat(command.image().contentType()).isEqualTo("image/png");
    assertThat(command.image().content()).isEqualTo("banner-2".getBytes());
  }

  @Test
  void 관리자가_배너삭제시_성공() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.ADMIN.name());

    // When & Then
    mockMvc
        .perform(delete("/api/admin/banners/{bannerId}", 2L).requestAttr("claims", claims))
        .andExpect(status().isOk());

    verify(bannerService).deleteBanner(2L);
  }

  @Test
  void 비관리자가_배너삭제시_실패() throws Exception {
    // Given
    Claims claims = mock(Claims.class);
    when(claims.get("rol", String.class)).thenReturn(Role.USER.name());

    // When & Then
    mockMvc
        .perform(delete("/api/admin/banners/{bannerId}", 2L).requestAttr("claims", claims))
        .andExpect(status().isForbidden());

    verifyNoInteractions(bannerService);
  }

  private BannerDto.AdminBannerInfo createAdminBannerInfo(Long bannerId) {
    Banner banner =
        Banner.builder()
            .label("label")
            .imagePath("banner/test.png")
            .redirectUrl("https://example.com")
            .active(true)
            .displayOrder(1)
            .build();
    ReflectionTestUtils.setField(banner, "bannerId", bannerId);
    return new BannerDto.AdminBannerInfo(banner, "http://localhost:8080/images/banner/test.png");
  }
}
