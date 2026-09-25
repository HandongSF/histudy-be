package edu.handong.csee.histudy.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Claims;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class RequestLoggingFilterTest {

  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String REQUEST_ID_MDC_KEY = "request_id";
  private static final String CLAIMS_ATTRIBUTE = "claims";

  private final RequestLoggingFilter filter = new RequestLoggingFilter();

  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    logAppender = new PreparingListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    logger.detachAppender(logAppender);
    MDC.clear();
  }

  @Test
  void 유효한_요청_ID가_전달되면_응답과_완료_로그에_같은_ID를_남긴다() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.addHeader(REQUEST_ID_HEADER, "request-123");
    request.setQueryString("token=secret-token-value");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    // when
    filter.doFilter(request, response, new MockFilterChain());

    // then
    assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("request-123");
    assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull();
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getFormattedMessage())
                  .contains("http_request")
                  .contains("request_id=\"request-123\"")
                  .contains("method=\"GET\"")
                  .contains("route=\"/api/test\"")
                  .contains("role=\"anonymous\"")
                  .doesNotContain("path=")
                  .doesNotContain("secret-token-value")
                  .containsPattern("duration_ms=\\d+")
                  .contains("status=200");
              assertThat(event.getMDCPropertyMap()).containsEntry(REQUEST_ID_MDC_KEY, "request-123");
            });
  }

  @Test
  void 인증된_사용자의_역할을_요청_완료_로그에_남긴다() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users/42");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/admin/users/{userId}");
    Claims claims = Mockito.mock(Claims.class);
    Mockito.when(claims.get("rol", String.class)).thenReturn("ADMIN");
    request.setAttribute(CLAIMS_ATTRIBUTE, claims);
    MockHttpServletResponse response = new MockHttpServletResponse();

    // when
    filter.doFilter(request, response, new MockFilterChain());

    // then
    assertThat(logAppender.list)
        .anySatisfy(
            event ->
                assertThat(event.getFormattedMessage())
                    .contains("route=\"/api/admin/users/{userId}\"")
                    .contains("role=\"ADMIN\"")
                    .doesNotContain("/api/admin/users/42"));
  }

  @Test
  void 역할_클레임을_읽을_수_없으면_요청을_중단하지_않고_unknown으로_기록한다() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users/42");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/admin/users/{userId}");
    Claims claims = Mockito.mock(Claims.class);
    Mockito.when(claims.get("rol", String.class))
        .thenThrow(new IllegalArgumentException("invalid role claim"));
    request.setAttribute(CLAIMS_ATTRIBUTE, claims);
    MockHttpServletResponse response = new MockHttpServletResponse();

    // when
    filter.doFilter(request, response, new MockFilterChain());

    // then
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(logAppender.list)
        .anySatisfy(
            event -> assertThat(event.getFormattedMessage()).contains("role=\"unknown\""));
  }

  @Test
  void 유효하지_않은_요청_ID가_전달되면_새로운_ID를_발급한다() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.addHeader(REQUEST_ID_HEADER, "invalid request id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    // when
    filter.doFilter(request, response, new MockFilterChain());

    // then
    String requestId = response.getHeader(REQUEST_ID_HEADER);
    assertThat(requestId).isNotEqualTo("invalid request id").matches("[0-9a-f-]{36}");
    assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull();
  }

  @Test
  void 처리_중_예외가_발생하면_500_상태로_완료_로그를_남기고_예외를_전파한다() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain failingChain = (servletRequest, servletResponse) -> {
      throw new IOException("downstream failure");
    };

    // when
    assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
        // then
        .isInstanceOf(IOException.class);

    assertThat(logAppender.list)
        .anySatisfy(
            event -> assertThat(event.getFormattedMessage()).contains("status=500"));
  }

  @Test
  void 응답이_커밋된_뒤_예외가_발생하면_커밋된_상태를_완료_로그에_남긴다() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain failingChain = (servletRequest, servletResponse) -> {
      MockHttpServletResponse httpServletResponse = (MockHttpServletResponse) servletResponse;
      httpServletResponse.setStatus(201);
      httpServletResponse.flushBuffer();
      throw new IOException("downstream failure");
    };

    // when
    assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
        // then
        .isInstanceOf(IOException.class);

    assertThat(logAppender.list)
        .anySatisfy(
            event -> assertThat(event.getFormattedMessage()).contains("status=201"));
  }

  @Test
  void 오류_디스패치가_재실행되면_기존_ID를_응답_헤더에_복원하고_로그를_중복하지_않는다()
      throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse initialResponse = new MockHttpServletResponse();
    filter.doFilter(request, initialResponse, new MockFilterChain());
    String requestId = initialResponse.getHeader(REQUEST_ID_HEADER);
    MockHttpServletResponse errorResponse = new MockHttpServletResponse();
    request.setDispatcherType(DispatcherType.ERROR);
    FilterChain errorDispatchChain =
        (servletRequest, servletResponse) ->
            assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isEqualTo(requestId);

    // when
    filter.doFilter(request, errorResponse, errorDispatchChain);

    // then
    assertThat(errorResponse.getHeader(REQUEST_ID_HEADER)).isEqualTo(requestId);
    assertThat(logAppender.list).hasSize(1);
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
