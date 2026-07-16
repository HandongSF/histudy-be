package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.BannerForm;
import edu.handong.csee.histudy.controller.form.BannerReorderForm;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.BannerDto;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.service.BannerService;
import io.jsonwebtoken.Claims;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banners")
public class BannerAdminController {

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
    BannerDto.AdminBannerInfo created = bannerService.createBanner(form);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping(value = "/{bannerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BannerDto.AdminBannerInfo> updateBanner(
      @PathVariable Long bannerId,
      @ModelAttribute BannerForm form,
      @RequestAttribute Claims claims) {
    requireAdmin(claims);
    BannerDto.AdminBannerInfo updated = bannerService.updateBanner(bannerId, form);
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
}
