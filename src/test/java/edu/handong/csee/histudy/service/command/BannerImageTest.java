package edu.handong.csee.histudy.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BannerImageTest {

  @Test
  void 이미지내용은_생성시점과_조회시점에_방어적으로복사한다() {
    // Given
    byte[] source = "banner".getBytes();
    BannerImage image = new BannerImage("banner.png", "image/png", source);

    // When
    source[0] = 'X';
    byte[] exposed = image.content();
    exposed[1] = 'Y';

    // Then
    assertThat(image.content()).isEqualTo("banner".getBytes());
  }
}
