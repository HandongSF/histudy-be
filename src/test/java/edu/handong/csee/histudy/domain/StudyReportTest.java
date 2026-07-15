package edu.handong.csee.histudy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StudyReportTest {

  @Test
  void null인_문자열필드를_null로_수정하면_기존값을_유지한다() {
    // Given
    StudyReport report =
        StudyReport.builder()
            .title(null)
            .content(null)
            .totalMinutes(0)
            .studyGroup(null)
            .participants(List.of())
            .images(List.of())
            .courses(List.of())
            .build();

    // When
    boolean updated = report.update(null, null, null, List.of(), List.of(), List.of());

    // Then
    assertThat(updated).isTrue();
    assertThat(report.getTitle()).isNull();
    assertThat(report.getContent()).isNull();
    assertThat(report.getTotalMinutes()).isZero();
  }
}
