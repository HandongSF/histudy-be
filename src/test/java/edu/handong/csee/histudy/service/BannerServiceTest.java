package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.handong.csee.histudy.controller.form.BannerForm;
import edu.handong.csee.histudy.domain.Banner;
import edu.handong.csee.histudy.dto.BannerDto;
import edu.handong.csee.histudy.exception.BannerNotFoundException;
import edu.handong.csee.histudy.exception.MissingParameterException;
import edu.handong.csee.histudy.service.repository.fake.FakeBannerRepository;
import edu.handong.csee.histudy.util.ImagePathMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class BannerServiceTest {

  @TempDir Path tempDir;

  private final byte[] bannerPngBytes = createBannerPngBytes();
  private final Banner firstBanner =
      Banner.builder()
          .label("First")
          .imagePath("banner/first.png")
          .redirectUrl("https://example.com/first")
          .active(true)
          .displayOrder(1)
          .build();
  private final Banner secondBanner =
      Banner.builder()
          .label("Second")
          .imagePath("banner/second.png")
          .redirectUrl("https://example.com/second")
          .active(true)
          .displayOrder(2)
          .build();
  private final Banner visibleBanner =
      Banner.builder()
          .label("Visible")
          .imagePath("banner/visible.png")
          .redirectUrl("https://example.com/visible")
          .active(true)
          .displayOrder(1)
          .build();
  private final Banner hiddenBanner =
      Banner.builder()
          .label("Hidden")
          .imagePath("banner/hidden.png")
          .redirectUrl("https://example.com/hidden")
          .active(false)
          .displayOrder(2)
          .build();
  private final Banner existingBanner =
      Banner.builder()
          .label("Existing")
          .imagePath("banner/existing.png")
          .redirectUrl("https://example.com/existing")
          .active(true)
          .displayOrder(1)
          .build();

  private FakeBannerRepository bannerRepository;
  private BannerService bannerService;

  @BeforeEach
  void setUp() {
    bannerRepository = new FakeBannerRepository();
    ImagePathMapper imagePathMapper = new ImagePathMapper();
    ReflectionTestUtils.setField(imagePathMapper, "origin", "https://histudy.handong.edu");
    ReflectionTestUtils.setField(imagePathMapper, "imageBasePath", "/images");
    bannerService = new BannerService(bannerRepository, imagePathMapper);
    ReflectionTestUtils.setField(bannerService, "imageBaseLocation", tempDir.toString() + "/");
  }

  @Test
  void 등록된_전체_배너_목록을_조회하면_노출순으로_반환한다() {
    // Given
    bannerRepository.save(secondBanner);
    bannerRepository.save(firstBanner);

    // When
    List<BannerDto.AdminBannerInfo> result = bannerService.getAdminBanners();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(BannerDto.AdminBannerInfo::getLabel)
        .containsExactly("First", "Second");
  }

  @Test
  void 공개된_배너_목록을_조회하면_활성_배너만_반환한다() {
    // Given
    bannerRepository.save(visibleBanner);
    bannerRepository.save(hiddenBanner);

    // When
    List<BannerDto.PublicBannerInfo> result = bannerService.getPublicBanners();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getRedirectUrl()).isEqualTo("https://example.com/visible");
  }

  @Test
  void 새로운_배너를_등록하면_다음_노출순서로_저장된다() throws Exception {
    // Given
    bannerRepository.save(existingBanner);
    BannerForm form =
        new BannerForm(
            "  Spring Banner  ",
            "https://example.com/banner",
            true,
            new MockMultipartFile("image", "banner.png", "image/png", bannerPngBytes));

    // When
    BannerDto.AdminBannerInfo result = bannerService.createBanner(form);

    // Then
    assertThat(result.getLabel()).isEqualTo("Spring Banner");
    assertThat(result.getDisplayOrder()).isEqualTo(2);
    assertThat(bannerRepository.findAll()).hasSize(2);
    Path storedImage = tempDir.resolve(result.getImageUrl().replace("https://histudy.handong.edu/images/", ""));
    assertThat(Files.exists(storedImage)).isTrue();
  }

  @Test
  void 이미지_없이_배너를_등록하면_예외가_발생한다() {
    // Given
    BannerForm form = new BannerForm("Banner", "https://example.com/banner", true, null);

    // When Then
    assertThatThrownBy(() -> bannerService.createBanner(form))
        .isInstanceOf(MissingParameterException.class);
  }

  @Test
  void 배너_정보를_업데이트하면_텍스트정보를_변경한다() throws Exception {
    // Given
    BannerDto.AdminBannerInfo created =
        bannerService.createBanner(
            new BannerForm(
                "Original",
                "https://example.com/original",
                true,
                new MockMultipartFile("image", "banner.png", "image/png", bannerPngBytes)));
    String originalImageUrl = created.getImageUrl();

    // When
    BannerDto.AdminBannerInfo updated =
        bannerService.updateBanner(
            created.getId(), new BannerForm("Updated", "https://example.com/updated", false, null));

    // Then
    assertThat(updated.getLabel()).isEqualTo("Updated");
    assertThat(updated.isActive()).isFalse();
    assertThat(updated.getRedirectUrl()).isEqualTo("https://example.com/updated");
    assertThat(updated.getImageUrl()).isEqualTo(originalImageUrl);
    Banner persisted =
        bannerRepository
            .findById(created.getId())
            .orElseThrow(() -> new AssertionError("updated banner should remain in repository"));
    assertThat(persisted.getLabel()).isEqualTo("Updated");
    assertThat(persisted.isActive()).isFalse();
    assertThat(persisted.getRedirectUrl()).isEqualTo("https://example.com/updated");
    assertThat(persisted.getImagePath())
        .isEqualTo(originalImageUrl.replace("https://histudy.handong.edu/images/", ""));
  }

  @Test
  void 배너_순서를_재배치하면_노출순서가_재정렬된다() {
    // Given
    Banner first = bannerRepository.save(firstBanner);
    Banner second = bannerRepository.save(secondBanner);

    // When
    bannerService.reorderBanners(List.of(second.getBannerId(), first.getBannerId()));

    // Then
    assertThat(bannerRepository.findAllByOrderByDisplayOrderAsc())
        .extracting(Banner::getBannerId)
        .containsExactly(second.getBannerId(), first.getBannerId());
    assertThat(bannerRepository.findAllByOrderByDisplayOrderAsc())
        .extracting(Banner::getDisplayOrder)
        .containsExactly(1, 2);
  }

  @Test
  void 유효하지_않은_ID로_배너_순서를_재배치하면_예외가_발생한다() {
    // Given
    Banner first = bannerRepository.save(firstBanner);

    // When Then
    assertThatThrownBy(
            () ->
                bannerService.reorderBanners(List.of(first.getBannerId(), 999L)))
        .isInstanceOf(MissingParameterException.class);
  }

  @Test
  void 배너를_삭제하면_남은_배너의_노출순서가_정규화된다() {
    // Given
    Banner first = bannerRepository.save(firstBanner);
    Banner second = bannerRepository.save(secondBanner);

    // When
    bannerService.deleteBanner(first.getBannerId());

    // Then
    assertThat(bannerRepository.findAll()).hasSize(1);
    assertThat(bannerRepository.findAll().get(0).getBannerId()).isEqualTo(second.getBannerId());
    assertThat(bannerRepository.findAll().get(0).getDisplayOrder()).isEqualTo(1);
  }

  @Test
  void 존재하지_않는_배너를_삭제하면_예외가_발생한다() {
    // Given

    // When Then
    assertThatThrownBy(() -> bannerService.deleteBanner(999L))
        .isInstanceOf(BannerNotFoundException.class);
  }

  @Test
  void 존재하지_않는_배너를_업데이트하면_예외가_발생한다() {
    // Given
    BannerForm form = new BannerForm("Updated", "https://example.com/updated", true, null);

    // When Then
    assertThatThrownBy(() -> bannerService.updateBanner(999L, form))
        .isInstanceOf(BannerNotFoundException.class);
  }

  private byte[] createBannerPngBytes() {
    try {
      BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(image, "png", outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("failed to create banner image fixture", e);
    }
  }
}
