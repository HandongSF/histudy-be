package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.BannerForm;
import edu.handong.csee.histudy.controller.form.BannerReorderForm;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.BannerDto;
import edu.handong.csee.histudy.exception.FileTransferException;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.exception.MissingParameterException;
import edu.handong.csee.histudy.service.BannerService;
import edu.handong.csee.histudy.service.command.BannerCommand;
import edu.handong.csee.histudy.service.command.BannerImage;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banners")
public class BannerAdminController {

  private static final String MESSAGE_MAX_FILE_SIZE = "이미지 파일 크기는 5MB 이하여야 합니다.";

  private final BannerService bannerService;

  @GetMapping
  public ResponseEntity<List<BannerDto.AdminBannerInfo>> getBanners(@RequestAttribute Claims claims) {
    requireAdmin(claims);
    return ResponseEntity.ok(bannerService.getAdminBanners());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BannerDto.AdminBannerInfo> createBanner(
      @ModelAttribute BannerForm form, @RequestAttribute Claims claims) {
    requireAdmin(claims);
    BannerDto.AdminBannerInfo created = bannerService.createBanner(toBannerCommand(form));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping(value = "/{bannerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BannerDto.AdminBannerInfo> updateBanner(
      @PathVariable Long bannerId,
      @ModelAttribute BannerForm form,
      @RequestAttribute Claims claims) {
    requireAdmin(claims);
    BannerDto.AdminBannerInfo updated = bannerService.updateBanner(bannerId, toBannerCommand(form));
    return ResponseEntity.ok(updated);
  }

  @PatchMapping("/reorder")
  public ResponseEntity<Void> reorderBanners(
      @RequestBody BannerReorderForm form, @RequestAttribute Claims claims) {
    requireAdmin(claims);
    bannerService.reorderBanners(form.getOrderedIds());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{bannerId}")
  public ResponseEntity<Void> deleteBanner(
      @PathVariable Long bannerId, @RequestAttribute Claims claims) {
    requireAdmin(claims);
    bannerService.deleteBanner(bannerId);
    return ResponseEntity.ok().build();
  }

  private void requireAdmin(Claims claims) {
    if (!Role.isAuthorized(claims, Role.ADMIN)) {
      throw new ForbiddenException();
    }
  }

  private BannerCommand toBannerCommand(BannerForm form) {
    return new BannerCommand(
        form.getLabel(),
        form.getRedirectUrl(),
        form.getActive(),
        toBannerImage(form.getImage()));
  }

  private BannerImage toBannerImage(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      return null;
    }
    if (image.getSize() > BannerImage.MAX_SIZE_BYTES) {
      throw new MissingParameterException(MESSAGE_MAX_FILE_SIZE);
    }

    try {
      return new BannerImage(
          image.getOriginalFilename(), image.getContentType(), image.getBytes());
    } catch (IOException e) {
      throw new FileTransferException();
    }
  }
}
