package edu.handong.csee.histudy.observability;

import edu.handong.csee.histudy.domain.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String REQUEST_ID_MDC_KEY = "request_id";
  private static final String CLAIMS_ATTRIBUTE = "claims";
  private static final String ANONYMOUS_ROLE = "anonymous";
  private static final String UNKNOWN_ROLE = "unknown";
  private static final String UNKNOWN_ROUTE = "UNKNOWN";
  private static final String REQUEST_ID_ATTRIBUTE =
      RequestLoggingFilter.class.getName() + ".requestId";
  private static final Pattern REQUEST_ID_PATTERN =
      Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

  @Override
  protected boolean shouldNotFilterErrorDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = resolveRequestId(request);
    long startedAt = System.nanoTime();
    boolean errorDispatch = request.getDispatcherType() == DispatcherType.ERROR;
    request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
    MDC.put(REQUEST_ID_MDC_KEY, requestId);

    int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    try {
      response.setHeader(REQUEST_ID_HEADER, requestId);
      filterChain.doFilter(request, response);
      status = response.getStatus();
    } catch (IOException | ServletException | RuntimeException exception) {
      status = response.isCommitted() ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      throw exception;
    } finally {
      try {
        if (!errorDispatch) {
          log.info(
              "http_request request_id=\"{}\" method=\"{}\" route=\"{}\" status={} duration_ms={} role=\"{}\"",
              requestId,
              request.getMethod(),
              resolveRoute(request),
              status,
              elapsedMilliseconds(startedAt),
              resolveRole(request));
        }
      } finally {
        MDC.remove(REQUEST_ID_MDC_KEY);
      }
    }
  }

  private String resolveRequestId(HttpServletRequest request) {
    Object storedRequestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
    if (storedRequestId instanceof String && isValidRequestId((String) storedRequestId)) {
      return (String) storedRequestId;
    }

    String requestId = request.getHeader(REQUEST_ID_HEADER);
    return isValidRequestId(requestId) ? requestId : UUID.randomUUID().toString();
  }

  private boolean isValidRequestId(String requestId) {
    return requestId != null && REQUEST_ID_PATTERN.matcher(requestId).matches();
  }

  private String resolveRoute(HttpServletRequest request) {
    Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return route instanceof String && !((String) route).isBlank() ? (String) route : UNKNOWN_ROUTE;
  }

  private String resolveRole(HttpServletRequest request) {
    Object claimsAttribute = request.getAttribute(CLAIMS_ATTRIBUTE);
    if (!(claimsAttribute instanceof Claims)) {
      return ANONYMOUS_ROLE;
    }

    try {
      String role = ((Claims) claimsAttribute).get("rol", String.class);
      if (role == null) {
        return UNKNOWN_ROLE;
      }
      return Role.valueOf(role).name();
    } catch (RuntimeException exception) {
      return UNKNOWN_ROLE;
    }
  }

  private long elapsedMilliseconds(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000;
  }
}
