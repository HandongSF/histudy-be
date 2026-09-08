package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.handong.csee.histudy.service.DiscordClient.Embed;
import edu.handong.csee.histudy.service.DiscordClient.Field;
import edu.handong.csee.histudy.service.DiscordClient.Payload;
import java.net.SocketTimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class DiscordServiceTest {

  private static final String REQUEST_ID = "request-123";
  private static final String ERROR_ID = "error-456";

  private DiscordClient client;
  private DiscordService discordService;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    client = mock(DiscordClient.class);
    discordService = new DiscordService(client);

    Logger logger = (Logger) LoggerFactory.getLogger(DiscordService.class);
    logAppender = new PreparingListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(DiscordService.class);
    logger.detachAppender(logAppender);
  }

  @Test
  void 예외_알림에는_식별자와_유형만_전송하고_예외_원문과_URI를_제외한다() {
    // given
    Exception exception = new IllegalStateException("secret-token-value");
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/42");

    // when
    discordService.notifyException(
        exception, new ServletWebRequest(request), REQUEST_ID, ERROR_ID);

    // then
    var payloadCaptor = org.mockito.ArgumentCaptor.forClass(Payload.class);
    verify(client).executeWebhook(payloadCaptor.capture());
    Embed embed = payloadCaptor.getValue().embeds().get(0);
    String fieldValues =
        embed.fields().stream().map(Field::value).collect(Collectors.joining("\n"));
    assertThat(embed.title()).isEqualTo("HIStudy server exception");
    assertThat(fieldValues)
        .contains(REQUEST_ID)
        .contains(ERROR_ID)
        .contains("IllegalStateException")
        .contains("POST")
        .doesNotContain("secret-token-value")
        .doesNotContain("/api/users/42");
  }

  @Test
  void 알림_전송에_실패하면_식별자와_예외_유형만_로그에_남긴다() {
    // given
    Exception exception = new IllegalStateException("secret-token-value");
    RuntimeException notificationFailure = new RuntimeException("webhook-secret");
    doThrow(notificationFailure).when(client).executeWebhook(any(Payload.class));

    // when
    discordService.notifyException(
        exception,
        new ServletWebRequest(new MockHttpServletRequest("POST", "/api/users/42")),
        REQUEST_ID,
        ERROR_ID);

    // then
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getFormattedMessage())
                  .contains("discord_notification_failed")
                  .contains("request_id=" + REQUEST_ID)
                  .contains("error_id=" + ERROR_ID)
                  .contains("exception_type=RuntimeException")
                  .doesNotContain("webhook-secret")
                  .doesNotContain("secret-token-value")
                  .doesNotContain("/api/users/42");
              assertThat(event.getThrowableProxy()).isNull();
            });
  }

  @Test
  void 알림_전송이_시간초과되면_시간초과_이벤트를_원문없이_기록한다() {
    // given
    Exception exception = new IllegalStateException("secret-token-value");
    RuntimeException notificationFailure =
        new RuntimeException("webhook-secret", new SocketTimeoutException("webhook-secret"));
    doThrow(notificationFailure).when(client).executeWebhook(any(Payload.class));

    // when
    discordService.notifyException(
        exception,
        new ServletWebRequest(new MockHttpServletRequest("POST", "/api/users/42")),
        REQUEST_ID,
        ERROR_ID);

    // then
    assertThat(logAppender.list)
        .anySatisfy(
            event ->
                assertThat(event.getFormattedMessage())
                    .contains("discord_notification_timeout")
                    .contains("request_id=" + REQUEST_ID)
                    .contains("error_id=" + ERROR_ID)
                    .doesNotContain("webhook-secret")
                    .doesNotContain("secret-token-value")
                    .doesNotContain("/api/users/42"));
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
