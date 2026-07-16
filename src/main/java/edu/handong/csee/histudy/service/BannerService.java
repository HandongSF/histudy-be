package edu.handong.csee.histudy.service;

import static edu.handong.csee.histudy.util.ImageDirectories.BANNER;
import static org.springframework.util.ResourceUtils.isUrl;

import edu.handong.csee.histudy.domain.Banner;
import edu.handong.csee.histudy.dto.BannerDto;
import edu.handong.csee.histudy.exception.BannerNotFoundException;
import edu.handong.csee.histudy.exception.FileTransferException;
import edu.handong.csee.histudy.exception.MissingParameterException;
import edu.handong.csee.histudy.repository.BannerRepository;
import edu.handong.csee.histudy.service.command.BannerCommand;
import edu.handong.csee.histudy.service.command.BannerImage;
import edu.handong.csee.histudy.util.ImagePathMapper;
import edu.handong.csee.histudy.util.Utils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional
@RequiredArgsConstructor
public class BannerService {

  private static final String IMAGE_NAME_FALLBACK_LABEL = "image";
  private static final String MESSAGE_MISSING_IMAGE = "이미지 파일은 필수입니다.";
  private static final String MESSAGE_MISSING_LABEL = "label은(는) 필수입니다.";
  private static final String MESSAGE_MISSING_UPDATE_PAYLOAD = "수정할 값이 없습니다.";
  private static final String MESSAGE_MAX_FILE_SIZE = "이미지 파일 크기는 5MB 이하여야 합니다.";
  private static final String MESSAGE_IMAGE_ONLY = "이미지 파일만 업로드할 수 있습니다.";
  private static final String MESSAGE_REORDER_REQUIRES_ALL =
      "모든 배너 ID를 포함한 순서 목록이 필요합니다.";
  private static final String MESSAGE_REORDER_DUPLICATED = "중복된 배너 ID가 포함되어 있습니다.";
  private static final String MESSAGE_REORDER_INVALID_IDS = "유효하지 않은 배너 ID 목록입니다.";
  private static final String MESSAGE_REORDER_EMPTY_BANNERS = "현재 배너가 존재하지 않습니다.";
  private static final String MESSAGE_INVALID_REDIRECT_URL =
      "redirectUrl은 http/https URL이어야 합니다.";
  private static final String MESSAGE_EMPTY_REQUEST_BODY = "요청 본문이 비어 있습니다.";

  @Value("${custom.resource.location}")
  private String imageBaseLocation;

  private final BannerRepository bannerRepository;
  private final ImagePathMapper imagePathMapper;

  @Transactional(readOnly = true)
  public List<BannerDto.AdminBannerInfo> getAdminBanners() {
    return bannerRepository.findAllByOrderByDisplayOrderAsc().stream()
        .map(this::toAdminBannerInfo)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BannerDto.PublicBannerInfo> getPublicBanners() {
    return bannerRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
        .map(this::toPublicBannerInfo)
        .toList();
  }

  public BannerDto.AdminBannerInfo createBanner(BannerCommand command) {
    validateRequestIsNotNull(command);
    validateCreateCommand(command);
    BannerImage image = command.image();
    validateImage(image);

    String imagePath = saveImage(image, command.label());
    scheduleImageDeletionAfterRollback(imagePath);
    int nextOrder =
        bannerRepository.findTopByOrderByDisplayOrderDesc().map(Banner::getDisplayOrder).orElse(0)
            + 1;

    Banner banner =
        Banner.builder()
            .label(command.label().trim())
            .imagePath(imagePath)
            .redirectUrl(normalizeRedirectUrl(command.redirectUrl()))
            .active(command.active() == null || command.active())
            .displayOrder(nextOrder)
            .build();

    Banner saved = bannerRepository.save(banner);
    return toAdminBannerInfo(saved);
  }

  public BannerDto.AdminBannerInfo updateBanner(Long bannerId, BannerCommand command) {
    validateRequestIsNotNull(command);
    Banner banner = findBanner(bannerId);

    boolean hasAnyUpdate = false;
    hasAnyUpdate |= updateLabelIfPresent(banner, command.label());
    hasAnyUpdate |= updateRedirectUrlIfPresent(banner, command.redirectUrl());
    hasAnyUpdate |= updateActiveIfPresent(banner, command.active());
    hasAnyUpdate |= updateImageIfPresent(banner, command.image());

    if (!hasAnyUpdate) {
      throw new MissingParameterException(MESSAGE_MISSING_UPDATE_PAYLOAD);
    }

    return toAdminBannerInfo(banner);
  }

  public void deleteBanner(Long bannerId) {
    Banner banner = findBanner(bannerId);
    bannerRepository.delete(banner);
    scheduleImageDeletionAfterCommit(banner.getImagePath());
    normalizeDisplayOrder();
  }

  public void reorderBanners(List<Long> requestedOrder) {
    List<Long> orderedIds = requestedOrder == null ? List.of() : requestedOrder;
    List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();

    if (banners.isEmpty()) {
      if (orderedIds.isEmpty()) {
        return;
      }
      throw new MissingParameterException(MESSAGE_REORDER_EMPTY_BANNERS);
    }

    validateReorderPayload(orderedIds, banners);

    Map<Long, Integer> orderMap = new HashMap<>();
    for (int i = 0; i < orderedIds.size(); i++) {
      orderMap.put(orderedIds.get(i), i + 1);
    }

    for (Banner banner : banners) {
      banner.updateDisplayOrder(orderMap.get(banner.getBannerId()));
    }

    bannerRepository.saveAll(banners);
  }

  private void normalizeDisplayOrder() {
    List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
    if (banners.isEmpty()) {
      return;
    }

    for (int i = 0; i < banners.size(); i++) {
      banners.get(i).updateDisplayOrder(i + 1);
    }
    bannerRepository.saveAll(banners);
  }

  private void validateCreateCommand(BannerCommand command) {
    if (command.image() == null || command.image().isEmpty()) {
      throw new MissingParameterException(MESSAGE_MISSING_IMAGE);
    }
    validateBlankField(command.label(), MESSAGE_MISSING_LABEL);
  }

  private void validateBlankField(String field, String message) {
    if (field == null || field.isBlank()) {
      throw new MissingParameterException(message);
    }
  }

  private void validateImage(BannerImage image) {
    if (image.size() > BannerImage.MAX_SIZE_BYTES) {
      throw new MissingParameterException(MESSAGE_MAX_FILE_SIZE);
    }

    String contentType = image.contentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new MissingParameterException(MESSAGE_IMAGE_ONLY);
    }

    try (var inputStream = image.inputStream()) {
      BufferedImage decodedImage = ImageIO.read(inputStream);
      if (decodedImage == null) {
        throw new MissingParameterException(MESSAGE_IMAGE_ONLY);
      }
    } catch (IOException e) {
      throw new MissingParameterException(MESSAGE_IMAGE_ONLY);
    }
  }

  private void validateReorderPayload(List<Long> orderedIds, List<Banner> banners) {
    if (orderedIds.size() != banners.size()) {
      throw new MissingParameterException(MESSAGE_REORDER_REQUIRES_ALL);
    }

    if (orderedIds.stream().anyMatch(id -> id == null)) {
      throw new MissingParameterException(MESSAGE_REORDER_INVALID_IDS);
    }

    Set<Long> deduplicated = new HashSet<>(orderedIds);
    if (deduplicated.size() != orderedIds.size()) {
      throw new MissingParameterException(MESSAGE_REORDER_DUPLICATED);
    }

    Set<Long> existingIds = banners.stream().map(Banner::getBannerId).collect(Collectors.toSet());
    if (!existingIds.equals(deduplicated)) {
      throw new MissingParameterException(MESSAGE_REORDER_INVALID_IDS);
    }
  }

  private String normalizeRedirectUrl(String redirectUrl) {
    if (redirectUrl == null) {
      return null;
    }

    String normalized = redirectUrl.trim();
    if (normalized.isEmpty()) {
      return null;
    }

    try {
      URI uri = URI.create(normalized);
      String scheme = uri.getScheme();
      if (!uri.isAbsolute() || scheme == null) {
        throw new IllegalArgumentException();
      }
      if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
        throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException e) {
      throw new MissingParameterException(MESSAGE_INVALID_REDIRECT_URL);
    }

    return normalized;
  }

  private String saveImage(BannerImage image, String label) {
    String extension = getExtension(image.originalFilename());
    String normalizedLabel = normalizeLabelForFilename(label);
    String dateTime = Utils.getCurrentFormattedDateTime("yyyyMMdd_HHmmss");
    String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String relativePath =
        BANNER + normalizedLabel + "_" + dateTime + "_" + random + extension;

    File file = resolveStorageFile(relativePath);
    File parent = file.getParentFile();

    if (!parent.exists() && !parent.mkdirs()) {
      throw new FileTransferException();
    }

    try {
      Files.write(file.toPath(), image.content());
      return relativePath;
    } catch (IOException e) {
      throw new FileTransferException();
    }
  }

  private String getExtension(String originalFilename) {
    if (originalFilename == null || originalFilename.isBlank()) {
      return ".img";
    }

    String normalizedFilename = originalFilename.replace("\\", "/");
    String sanitizedFilename = normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1);
    int extensionStart = sanitizedFilename.lastIndexOf('.');
    if (extensionStart < 0 || extensionStart == sanitizedFilename.length() - 1) {
      return ".img";
    }

    String extension = sanitizedFilename.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    if (!extension.matches("[a-z0-9]{1,10}")) {
      return ".img";
    }

    return "." + extension;
  }

  private void deleteImage(String imagePath) {
    if (imagePath == null || isUrl(imagePath)) {
      return;
    }

    try {
      Files.deleteIfExists(resolveStorageFile(imagePath).toPath());
    } catch (IOException e) {
      throw new FileTransferException();
    }
  }

  private void validateRequestIsNotNull(Object request) {
    if (request == null) {
      throw new MissingParameterException(MESSAGE_EMPTY_REQUEST_BODY);
    }
  }

  private boolean updateLabelIfPresent(Banner banner, String label) {
    if (label == null) {
      return false;
    }
    validateBlankField(label, MESSAGE_MISSING_LABEL);
    banner.updateLabel(label.trim());
    return true;
  }

  private boolean updateRedirectUrlIfPresent(Banner banner, String redirectUrl) {
    if (redirectUrl == null) {
      return false;
    }
    banner.updateRedirectUrl(normalizeRedirectUrl(redirectUrl));
    return true;
  }

  private boolean updateActiveIfPresent(Banner banner, Boolean active) {
    if (active == null) {
      return false;
    }
    banner.updateActive(active);
    return true;
  }

  private boolean updateImageIfPresent(Banner banner, BannerImage image) {
    if (image == null || image.isEmpty()) {
      return false;
    }
    validateImage(image);
    String oldImagePath = banner.getImagePath();
    String newImagePath = saveImage(image, banner.getLabel());
    scheduleImageDeletionAfterRollback(newImagePath);
    banner.updateImagePath(newImagePath);
    scheduleImageDeletionAfterCommit(oldImagePath);
    return true;
  }

  private File resolveStorageFile(String relativePath) {
    Path basePath = Path.of(imageBaseLocation).toAbsolutePath().normalize();
    Path resolvedPath =
        basePath.resolve(stripLeadingPathSeparator(relativePath)).normalize();
    if (!resolvedPath.startsWith(basePath)) {
      throw new FileTransferException();
    }
    return resolvedPath.toFile();
  }

  private String stripLeadingPathSeparator(String path) {
    if (path == null) {
      return "";
    }
    if (path.startsWith("/") || path.startsWith("\\")) {
      return path.substring(1);
    }
    return path;
  }

  private void scheduleImageDeletionAfterCommit(String imagePath) {
    runAfterCommit(() -> deleteImageQuietly(imagePath));
  }

  private void scheduleImageDeletionAfterRollback(String imagePath) {
    runAfterRollback(() -> deleteImageQuietly(imagePath));
  }

  private void runAfterCommit(Runnable task) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      task.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            task.run();
          }
        });
  }

  private void runAfterRollback(Runnable task) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
              task.run();
            }
          }
        });
  }

  private void deleteImageQuietly(String imagePath) {
    try {
      deleteImage(imagePath);
    } catch (RuntimeException ignored) {
      // Intentionally ignore cleanup failures to avoid masking transaction completion.
    }
  }

  private String normalizeLabelForFilename(String label) {
    if (label == null) {
      return IMAGE_NAME_FALLBACK_LABEL;
    }

    String sanitized =
        label
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_-]+", "-")
            .replaceAll("^-+|-+$", "");
    if (sanitized.isBlank()) {
      return IMAGE_NAME_FALLBACK_LABEL;
    }
    return sanitized;
  }

  private Banner findBanner(Long bannerId) {
    return bannerRepository.findById(bannerId).orElseThrow(BannerNotFoundException::new);
  }

  private BannerDto.AdminBannerInfo toAdminBannerInfo(Banner banner) {
    return new BannerDto.AdminBannerInfo(
        banner, imagePathMapper.getFullPath(banner.getImagePath()));
  }

  private BannerDto.PublicBannerInfo toPublicBannerInfo(Banner banner) {
    return new BannerDto.PublicBannerInfo(
        banner, imagePathMapper.getFullPath(banner.getImagePath()));
  }
}
