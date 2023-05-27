package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.controller.form.UserInfo;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.jwt.GrantType;
import edu.handong.csee.histudy.jwt.JwtProperties;
import edu.handong.csee.histudy.jwt.JwtSecret;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtSecret jwtSecret;
    private final JwtProperties jwtProperties;

    public String issueToken(String email, String name, GrantType typ) {
        SecretKey secretKey = jwtSecret.getKey();
        Instant now = Instant.now();

        Date iat = new Date(now.toEpochMilli());
        Date exp = calcExpiry(now, jwtProperties.getTokenExpiry(typ));

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(email)
                .claim("name", name)
                .claim("rol", Role.USER.name())
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(secretKey)
                .compact();
    }

    public String issueToken(UserInfo userInfo, GrantType typ) {
        SecretKey secretKey = jwtSecret.getKey();
        Instant now = Instant.now();

        Date iat = new Date(now.toEpochMilli());
        Date exp = calcExpiry(now, jwtProperties.getTokenExpiry(typ));

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(userInfo.getEmail())
                .claim("name", userInfo.getName())
                .claim("rol", Role.USER.name())
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(secretKey)
                .compact();
    }

    public Optional<Claims> validate(Optional<String> token) {
        JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(jwtSecret.getKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build();

        try {
            return Optional.of(token)
                    .filter(Optional::isPresent)
                    .map(jwt -> parser
                            .parseClaimsJws(jwt.get())
                            .getBody());
        } catch (JwtException e) {
            log.debug(e.getMessage());
        }
        return Optional.empty();
    }

    private Date calcExpiry(Instant now, String expiryAsString) {
        long expiry = Long.parseLong(expiryAsString);
        return new Date(now.plusSeconds(expiry).toEpochMilli());
    }
}
