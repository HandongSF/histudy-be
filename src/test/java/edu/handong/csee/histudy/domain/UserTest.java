package edu.handong.csee.histudy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void null인_문자열필드를_null로_수정하면_기존값을_유지한다() {
    // Given
    User user = User.builder().sid(null).name(null).build();

    // When
    user.edit(null, null);

    // Then
    assertThat(user.getSid()).isNull();
    assertThat(user.getName()).isNull();
  }
}
