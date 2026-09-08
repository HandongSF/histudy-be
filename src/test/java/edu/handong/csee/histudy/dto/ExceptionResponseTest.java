package edu.handong.csee.histudy.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ExceptionResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void 선택적_errorId가_null이어도_필수_필드는_JSON에_포함한다() {
    // given
    ExceptionResponse response =
        new ExceptionResponse(HttpStatus.BAD_REQUEST, "message", null, null);

    // when
    JsonNode json = objectMapper.valueToTree(response);

    // then
    assertThat(json.has("code")).isTrue();
    assertThat(json.has("error")).isTrue();
    assertThat(json.has("message")).isTrue();
    assertThat(json.has("requestId")).isTrue();
    assertThat(json.get("requestId").isNull()).isTrue();
    assertThat(json.has("errorId")).isFalse();
  }
}
