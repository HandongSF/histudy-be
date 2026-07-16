package edu.handong.csee.histudy.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacyStudyApplicationCommandTest {

  @Test
  void 신청목록이_null이면_빈목록으로_변환한다() {
    // Given

    // When
    LegacyStudyApplicationCommand command = new LegacyStudyApplicationCommand(null, null);

    // Then
    assertThat(command.friendStudentIds()).isEmpty();
    assertThat(command.courseIds()).isEmpty();
  }
}
