package edu.handong.csee.histudy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.handong.csee.histudy.dto.ExceptionResponse;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.service.DiscordService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ExceptionControllerTest {

  private static final String REQUEST_ID = "request-123";
  private static final String REQUEST_ID_MDC_KEY = "request_id";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private DiscordService discordService;

  private ExceptionController exceptionController;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    discordService = mock(DiscordService.class);
    exceptionController = new ExceptionController(discordService);

    Logger logger = (Logger) LoggerFactory.getLogger(ExceptionController.class);
    logAppender = new PreparingListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(ExceptionController.class);
    logger.detachAppender(logAppender);
    MDC.clear();
  }

  @Test
  void 처리되지_않은_예외가_발생하면_requestId와_예외_유형을_로그에_남기고_원문을_노출하지_않는다() {
    // given
    MDC.put(REQUEST_ID_MDC_KEY, REQUEST_ID);
    RuntimeException exception = new IllegalStateException("secret-token-value");
    ServletWebRequest request = webRequest();

    // when
    ResponseEntity<?> response = exceptionController.runtimeException(exception, request);

    // then
    assertThat(response.getBody()).isInstanceOf(ExceptionResponse.class);
    ExceptionResponse body = (ExceptionResponse) response.getBody();
    assertThat(body.getRequestId()).isEqualTo(REQUEST_ID);
    assertThat(body.getErrorId()).isNotBlank().matches("[0-9a-f-]{36}");
    assertThat(objectMapper.valueToTree(body).has("trace")).isFalse();
    assertThat(objectMapper.valueToTree(body).has("errorId")).isTrue();
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getFormattedMessage())
                  .contains("unhandled_exception")
                  .contains("request_id=" + REQUEST_ID)
                  .contains("error_id=" + body.getErrorId())
                  .contains("exception_type=IllegalStateException")
                  .doesNotContain("secret-token-value");
              assertThat(event.getThrowableProxy().getClassName())
                  .isEqualTo(IllegalStateException.class.getName());
            });
    verify(discordService)
        .notifyException(exception, request, REQUEST_ID, body.getErrorId());
  }

  @Test
  void 인증_예외가_발생하면_파서_메시지_대신_안전한_메시지를_응답하고_실패를_기록한다() {
    // given
    MDC.put(REQUEST_ID_MDC_KEY, REQUEST_ID);
    JwtException exception = new JwtException("token-parser-details");

    // when
    ResponseEntity<?> response = exceptionController.handleUnauthorized(exception, webRequest());

    // then
    ExceptionResponse body = (ExceptionResponse) response.getBody();
    assertThat(body.getMessage()).isEqualTo("인증 정보가 유효하지 않습니다.");
    assertThat(body.getRequestId()).isEqualTo(REQUEST_ID);
    assertThat(body.getErrorId()).isNull();
    assertThat(objectMapper.valueToTree(body).has("trace")).isFalse();
    assertThat(objectMapper.valueToTree(body).has("errorId")).isFalse();
    assertThat(logAppender.list)
        .anySatisfy(
            event ->
                assertThat(event.getFormattedMessage())
                    .contains("authentication_failed")
                    .contains("request_id=" + REQUEST_ID)
                    .contains("exception_type=JwtException")
                    .doesNotContain("token-parser-details"));
  }

  @Test
  void 권한_예외가_발생하면_requestId와_거부_유형을_기록한다() {
    // given
    MDC.put(REQUEST_ID_MDC_KEY, REQUEST_ID);

    // when
    ResponseEntity<?> response =
        exceptionController.handleForbidden(new ForbiddenException(), webRequest());

    // then
    ExceptionResponse body = (ExceptionResponse) response.getBody();
    assertThat(body.getRequestId()).isEqualTo(REQUEST_ID);
    assertThat(body.getErrorId()).isNull();
    assertThat(logAppender.list)
        .anySatisfy(
            event ->
                assertThat(event.getFormattedMessage())
                    .contains("authorization_denied")
                    .contains("request_id=" + REQUEST_ID)
                    .contains("exception_type=ForbiddenException"));
  }

  @Test
  void WebRequest가_없어도_예외_응답에_새_requestId를_발급한다() {
    // given
    RuntimeException exception = new IllegalStateException("failure");

    // when
    ResponseEntity<?> response = exceptionController.runtimeException(exception, null);

    // then
    ExceptionResponse body = (ExceptionResponse) response.getBody();
    assertThat(body.getRequestId()).isNotBlank().matches("[0-9a-f-]{36}");
    assertThat(body.getErrorId()).isNotBlank().matches("[0-9a-f-]{36}");
    verify(discordService)
        .notifyException(exception, null, body.getRequestId(), body.getErrorId());
  }

  private ServletWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest("GET", "/api/test"));
  }

  private static class PreparingListAppender<E> extends ListAppender<E> {

    @Override
    protected void append(E eventObject) {
      if (eventObject instanceof ILoggingEvent) {
        ((ILoggingEvent) eventObject).prepareForDeferredProcessing();
      }
      super.append(eventObject);
    }
  }
}
