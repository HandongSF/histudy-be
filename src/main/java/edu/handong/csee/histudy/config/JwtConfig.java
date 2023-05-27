package edu.handong.csee.histudy.config;

import edu.handong.csee.histudy.jwt.JwtProperties;
import edu.handong.csee.histudy.jwt.JwtSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Value("${custom.jwt.secret}")
    private String secret;

    @Bean
    public JwtSecret jwtSecret() {
        return new JwtSecret(secret);
    }
}
