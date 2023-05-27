package edu.handong.csee.histudy.jwt;

import lombok.Getter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Getter
public class JwtSecret {

    private final SecretKey key;

    public JwtSecret(String secret) {
        byte[] decoded = Base64.getDecoder().decode(secret);
        this.key = new SecretKeySpec(decoded, "HmacSHA256");
    }
}
