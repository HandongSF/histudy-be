package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.exception.InvalidTokenTypeException;
import edu.handong.csee.histudy.jwt.GrantType;
import edu.handong.csee.histudy.jwt.JwtPair;
import edu.handong.csee.histudy.jwt.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JwtServiceTest {

  private JwtService jwtService;
  private JwtProperties jwtProperties;

  @BeforeEach
  void init() {
    String secret =
        Base64.getEncoder().encodeToString("test-secret-key-for-jwt-service-test".getBytes());
    jwtProperties = new JwtProperties(secret, "HIStudy", "3600", "86400");
    jwtService = new JwtService(jwtProperties);
  }

  @Test
  void 유저정보제공시_토큰쌍발급() {
    // Given
    String email = "test@example.com";
    String name = "Test User";
    Role role = Role.USER;

    // When
    JwtPair jwtPair = jwtService.issueToken(email, name, role);

    // Then
    assertThat(jwtPair.getAccessToken()).isNotNull();
    assertThat(jwtPair.getRefreshToken()).isNotNull();
    assertThat(jwtPair.getAccessToken()).isNotEqualTo(jwtPair.getRefreshToken());
  }

  @Test
  void 유효토큰시_클레임반환() {
    // Given
    String email = "test@example.com";
    String name = "Test User";
    Role role = Role.USER;
    JwtPair jwtPair = jwtService.issueToken(email, name, role);

    // When
    Claims claims = jwtService.validate(jwtPair.getAccessToken());

    // Then
    assertThat(claims.getSubject()).isEqualTo(email);
    assertThat(claims.get("name", String.class)).isEqualTo(name);
    assertThat(claims.get("rol", String.class)).isEqualTo(role.name());
    assertThat(claims.getIssuer()).isEqualTo("HIStudy");
  }

  @Test
  void 잘못된토큰시_예외발생() {
    // Given
    String invalidToken = "invalid.jwt.token";

    // When & Then
    assertThatThrownBy(() -> jwtService.validate(invalidToken)).isInstanceOf(JwtException.class);
  }

  @Test
  void Bearer헤더시_토큰추출() {
    // Given
    String token = "sample-jwt-token";
    String bearerToken = "Bearer " + token;
    Optional<String> headerOr = Optional.of(bearerToken);

    // When
    String extractedToken = jwtService.extractToken(headerOr);

    // Then
    assertThat(extractedToken).isEqualTo(token);
  }

  @Test
  void 잘못된헤더형식시_예외발생() {
    // Given
    Optional<String> headerOr = Optional.of("InvalidFormat token");

    // When & Then
    assertThatThrownBy(() -> jwtService.extractToken(headerOr))
        .isInstanceOf(InvalidTokenTypeException.class);
  }

  @Test
  void 헤더없을시_예외발생() {
    // Given
    Optional<String> headerOr = Optional.empty();

    // When & Then
    assertThatThrownBy(() -> jwtService.extractToken(headerOr))
        .isInstanceOf(InvalidTokenTypeException.class);
  }

  @Test
  void 클레임으로_액세스토큰발급() {
    // Given
    String email = "test@example.com";
    String name = "Test User";
    Role role = Role.USER;
    JwtPair originalPair = jwtService.issueToken(email, name, role);
    Claims claims = jwtService.validate(originalPair.getAccessToken());

    // When
    String newToken = jwtService.issueToken(claims, GrantType.ACCESS_TOKEN);

    // Then
    assertThat(newToken).isNotNull();
    Claims newClaims = jwtService.validate(newToken);
    assertThat(newClaims.getSubject()).isEqualTo(email);
    assertThat(newClaims.get("name", String.class)).isEqualTo(name);
    assertThat(newClaims.get("rol", String.class)).isEqualTo(role.name());
  }

  @Test
  void 클레임으로_리프레시토큰발급() {
    // Given
    String email = "test@example.com";
    String name = "Test User";
    Role role = Role.USER;
    JwtPair originalPair = jwtService.issueToken(email, name, role);
    Claims claims = jwtService.validate(originalPair.getAccessToken());

    // When
    String newToken = jwtService.issueToken(claims, GrantType.REFRESH_TOKEN);

    // Then
    assertThat(newToken).isNotNull();
    Claims newClaims = jwtService.validate(newToken);
    assertThat(newClaims.getSubject()).isEqualTo(email);
    assertThat(newClaims.get("name", String.class)).isEqualTo(name);
    assertThat(newClaims.get("rol", String.class)).isEqualTo(role.name());
  }

  @Test
  void 관리자역할시_토큰발급() {
    // Given
    String email = "admin@example.com";
    String name = "Admin User";
    Role role = Role.ADMIN;

    // When
    JwtPair jwtPair = jwtService.issueToken(email, name, role);
    Claims claims = jwtService.validate(jwtPair.getAccessToken());

    // Then
    assertThat(claims.get("rol", String.class)).isEqualTo("ADMIN");
  }

  @Test
  void 멤버역할시_토큰발급() {
    // Given
    String email = "member@example.com";
    String name = "Member User";
    Role role = Role.MEMBER;

    // When
    JwtPair jwtPair = jwtService.issueToken(email, name, role);
    Claims claims = jwtService.validate(jwtPair.getAccessToken());

    // Then
    assertThat(claims.get("rol", String.class)).isEqualTo("MEMBER");
  }
}
