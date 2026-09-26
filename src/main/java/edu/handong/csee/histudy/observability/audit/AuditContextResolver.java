package edu.handong.csee.histudy.observability.audit;

import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AuditContextResolver {

  private final UserRepository userRepository;

  public AuditContext resolve(String subject, Role role) {
    Long actorId = StringUtils.hasText(subject)
        ? userRepository.findUserByEmail(subject).map(User::getUserId).orElse(null)
        : null;
    String requestId = MDC.get("request_id");
    return new AuditContext(
        StringUtils.hasText(requestId) ? requestId : "unknown", actorId, role);
  }
}
