package edu.handong.csee.histudy.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReportCommandTest {

  @Test
  void 리포트목록이_null이면_빈목록으로_변환한다() {
    // Given

    // When
    ReportCommand command = new ReportCommand("제목", "내용", 60L, null, null, null);

    // Then
    assertThat(command.participantIds()).isEmpty();
    assertThat(command.imageUrls()).isEmpty();
    assertThat(command.courseIds()).isEmpty();
  }
}
