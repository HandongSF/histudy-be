package edu.handong.csee.histudy.observability.audit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

class AuditContextResolverTest {
  private final UserRepository repository = mock(UserRepository.class);
  private final AuditContextResolver resolver = new AuditContextResolver(repository);

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void 사용자가_있으면_내부_ID와_요청문맥만_반환한다() {
    // given
    User user = User.builder().email("private@example.com").name("private-name").build();
    ReflectionTestUtils.setField(user, "userId", 42L);
    when(repository.findUserByEmail("private@example.com")).thenReturn(Optional.of(user));
    MDC.put("request_id", "request-first");
    // when
    AuditContext first = resolver.resolve("private@example.com", Role.ADMIN);
    MDC.put("request_id", "request-second");
    AuditContext second = resolver.resolve("private@example.com", Role.ADMIN);
    // then
    assertThat(first).isEqualTo(new AuditContext("request-first", 42L, Role.ADMIN));
    assertThat(second.requestId()).isEqualTo("request-second");
    assertThat(first.toString()).doesNotContain("private@example.com", "private-name");
  }

  @Test
  void 사용자가_없으면_행위자만_미확인으로_반환한다() {
    // given
    when(repository.findUserByEmail("missing@example.com")).thenReturn(Optional.empty());
    // when
    AuditContext context = resolver.resolve("missing@example.com", Role.ADMIN);
    // then
    assertThat(context).isEqualTo(new AuditContext("unknown", null, Role.ADMIN));
  }

  @Test
  void subject가_없으면_DB를_조회하지_않는다() {
    // given
    // when
    AuditContext missing = resolver.resolve(null, Role.ADMIN);
    AuditContext blank = resolver.resolve("  ", Role.ADMIN);
    // then
    assertThat(missing.actorId()).isNull();
    assertThat(blank.actorId()).isNull();
    verifyNoInteractions(repository);
  }

  @Test
  void DB조회가_실패하면_행위자_부재로_숨기지_않는다() {
    // given
    RuntimeException failure = new IllegalStateException("database failure");
    when(repository.findUserByEmail("admin@example.com")).thenThrow(failure);
    // when then
    assertThatThrownBy(() -> resolver.resolve("admin@example.com", Role.ADMIN)).isSameAs(failure);
  }
  @Test
  void 필수_문맥이_없으면_생성시점에_거절한다() {
    // given when then
    assertThatThrownBy(() -> new AuditContext("request", 1L, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new MatchingExecutedEvent(null, 1L, 0, 0, 0, 0))
        .isInstanceOf(NullPointerException.class);
    assertThat(new AuditContext(null, null, Role.ADMIN).requestId()).isEqualTo("unknown");
  }

}
