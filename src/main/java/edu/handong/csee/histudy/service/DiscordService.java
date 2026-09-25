package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.service.DiscordClient.Embed;
import edu.handong.csee.histudy.service.DiscordClient.Field;
import edu.handong.csee.histudy.service.DiscordClient.Payload;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordService {

  private static final int ERROR_COLOR = 15548997;
  private static final String ERROR_TITLE = "HIStudy server exception";
  private static final String UNKNOWN_VALUE = "Unknown";
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final DiscordClient client;

  public void notifyException(
      Exception exception, WebRequest request, String requestId, String errorId) {
    try {
      if (request instanceof ServletWebRequest servletRequest) {
        Payload payload =
            createExceptionPayload(
                requestId,
                errorId,
                exception.getClass().getSimpleName(),
                getHttpMethod(servletRequest));
        client.executeWebhook(payload);
      }
    } catch (Exception notificationFailure) {
      if (isTimeout(notificationFailure)) {
        log.warn(
            "discord_notification_timeout request_id={} error_id={}", requestId, errorId);
      } else {
        log.error(
            "discord_notification_failed request_id={} error_id={} exception_type={}",
            requestId,
            errorId,
            notificationFailure.getClass().getSimpleName());
      }
    }
  }

  private String getHttpMethod(ServletWebRequest request) {
    try {
      return request.getHttpMethod() == null ? UNKNOWN_VALUE : request.getHttpMethod().name();
    } catch (Exception e) {
      return UNKNOWN_VALUE;
    }
  }

  private Payload createExceptionPayload(
      String requestId, String errorId, String exceptionType, String httpMethod) {

    List<Field> fields = new ArrayList<>();

    fields.add(createField("Timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)));
    fields.add(createField("Request ID", requestId));
    fields.add(createField("Error ID", errorId));
    fields.add(createField("Exception Type", exceptionType));
    fields.add(createField("HTTP Method", httpMethod));

    return new Payload(
        List.of(Embed.builder().title(ERROR_TITLE).color(ERROR_COLOR).fields(fields).build()));
  }

  private boolean isTimeout(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      if (current instanceof SocketTimeoutException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private Field createField(String name, String value) {
    return Field.builder()
        .name(name)
        .value(value != null && !value.trim().isEmpty() ? value : UNKNOWN_VALUE)
        .inline(false)
        .build();
  }
}
