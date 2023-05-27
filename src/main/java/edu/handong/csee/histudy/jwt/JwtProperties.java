package edu.handong.csee.histudy.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "custom.jwt")
public class JwtProperties {

    private final String issuer;
    private final String accessTokenExpiry;
    private final String refreshTokenExpiry;

    @ConstructorBinding
    public JwtProperties(String issuer, String accessTokenExpiry, String refreshTokenExpiry) {
        this.issuer = issuer;
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getTokenExpiry(GrantType typ) {
        return (typ.equals(GrantType.ACCESS_TOKEN))
                ? accessTokenExpiry
                : refreshTokenExpiry;
    }
}
