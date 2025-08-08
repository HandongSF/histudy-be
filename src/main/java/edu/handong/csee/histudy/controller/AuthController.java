package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.TokenForm;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.MissingTokenException;
import edu.handong.csee.histudy.jwt.GrantType;
import edu.handong.csee.histudy.jwt.JwtPair;
import edu.handong.csee.histudy.jwt.TokenInfo;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.UserService;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(
        origins = "${custom.origin.allowed}",
        methods = {
                RequestMethod.POST,
                RequestMethod.GET,
                RequestMethod.DELETE,
                RequestMethod.PATCH,
                RequestMethod.OPTIONS
        },
        allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/login")
    public ResponseEntity<UserDto.UserLogin> login(@RequestParam("sub") Optional<String> subOr) {
        User user = userService.getUser(subOr);
        JwtPair tokens = jwtService.issueToken(user.getEmail(), user.getName(), user.getRole());

        return ResponseEntity.ok(
                UserDto.UserLogin.builder()
                        .isRegistered(true)
                        .tokenType("Bearer ")
                        .tokens(tokens)
                        .role(user.getRole().name())
                        .build());
    }

    @PostMapping("/token")
    public ResponseEntity<TokenInfo> issueAccessToken(@RequestBody TokenForm tokenForm) {
        String refreshToken = Optional.ofNullable(tokenForm.getRefreshToken())
                .orElseThrow(MissingTokenException::new);
        Claims claims = jwtService.validate(refreshToken);
        String accessToken = jwtService.issueToken(claims, GrantType.ACCESS_TOKEN);

        return ResponseEntity.ok(new TokenInfo(GrantType.ACCESS_TOKEN, accessToken));
    }
}
