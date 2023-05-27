package edu.handong.csee.histudy.interceptor;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationInterceptorTests {

    private final static String BEARER = "Bearer ";
    private final static String JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwibmFtZSI6InVzZXIifQ." +
            "ZB9yuKUvuufMfbXkzB647PoSCw-3rc2FA-PoHzKLYgM";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @DisplayName("토큰이 유효한 경우")
    @Test
    void AuthenticationInterceptorTests_15() throws Exception {
        // given

        // when
        MvcResult mvcResult = mvc
                .perform(get("/api/tests")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + JWT))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        Map<String, String> res = mapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        // then
        assertEquals("test@example.com", res.get(Claims.SUBJECT));
        assertEquals("http://localhost:8080", res.get(Claims.ISSUER));
        assertEquals("user", res.get("name"));
    }

    @DisplayName("토큰이 유효하지 않은 경우: 만료")
    @Test
    void AuthenticationInterceptorTests_63() throws Exception {
        final String expiredJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwibmFtZSI6InVzZXIiLCJpYXQiOjEyMzQ1Njc4OTAsImV4cCI6MTM0NTY3ODkxMH0." +
                "cqKPD3nBW1h14G_ghc-1O57y5VCxLyXb765UPoFbckY";

        mvc
                .perform(get("/api/tests")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + expiredJwt))
                .andExpect(status().isUnauthorized())
                .andDo(print())
                .andReturn();
    }

    @DisplayName("토큰이 유효하지 않은 경우: 헤더 미포함")
    @Test
    void AuthenticationInterceptorTests_73() throws Exception {
        mvc
                .perform(get("/api/tests"))
                .andExpect(status().isUnauthorized())
                .andDo(print())
                .andReturn();
    }

    @DisplayName("토큰이 유효하지 않은 경우: Missing issuer")
    @Test
    void AuthenticationInterceptorTests_88() throws Exception {
        final String invalidJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwibmFtZSI6InVzZXIifQ." +
                "Z2ezi-zT1ldOhGhLqMVmPd6OqF8xD39-vFWoB6fScsA";

        mvc
                .perform(get("/api/tests")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + invalidJwt))
                .andExpect(status().isUnauthorized())
                .andDo(print())
                .andReturn();
    }
}
