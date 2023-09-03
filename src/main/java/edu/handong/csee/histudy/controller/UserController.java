package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.UserForm;
import edu.handong.csee.histudy.domain.Role;
import edu.handong.csee.histudy.dto.ApplyFormDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.jwt.JwtPair;
import edu.handong.csee.histudy.service.JwtService;
import edu.handong.csee.histudy.service.UserService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Tag(name = "일반 사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @Operation(summary = "회원가입")
    @PostMapping
    public ResponseEntity<UserDto.UserLogin> createUser(@RequestBody UserForm userForm) {
        userService.signUp(userForm);
        JwtPair tokens = jwtService.issueToken(userForm.getEmail(), userForm.getName(), Role.USER);

        return ResponseEntity.ok(UserDto.UserLogin.builder()
                .isRegistered(true)
                .tokenType("Bearer ")
                .tokens(tokens)
                .role(Role.USER.name())
                .build());
    }

    @Operation(summary = "유저 검색")
    @SecurityRequirement(name = "USER")
    @GetMapping
    public ResponseEntity<UserDto> searchUser(
            @Parameter(allowEmptyValue = true) @RequestParam(name = "search") Optional<String> keyword,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) Optional<String> header) {
        String token = jwtService.extractToken(header);
        Claims claims = jwtService.validate(token);
        String email = claims.getSubject();

        List<UserDto.UserMatching> users = userService.search(keyword)
                .stream()
                .filter(u -> u.getRole().equals(Role.USER))
                .filter(u -> !u.getEmail().equals(email))
                .map(UserDto.UserMatching::new)
                .toList();

        return ResponseEntity.ok(new UserDto(users));
    }

    @Operation(summary = "내 정보 조회")
    @SecurityRequirements({
            @SecurityRequirement(name = "USER"),
            @SecurityRequirement(name = "MEMBER"),
            @SecurityRequirement(name = "ADMIN")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto.UserMe> getMyInfo(
            @RequestAttribute Claims claims) {
        if (Role.isAuthorized(claims, Role.values())) {
            UserDto.UserMe info = userService.getUserMe(Optional.ofNullable(claims.getSubject()));
            return ResponseEntity.ok(info);
        }
        throw new ForbiddenException();
    }

    @Operation(summary = "스터디 그룹 신청 정보 조회")
    @SecurityRequirement(name = "USER")
    @GetMapping("/me/forms")
    public ResponseEntity<ApplyFormDto> getMyApplicationForm(
            @RequestAttribute Claims claims) {
        if (Role.isAuthorized(claims, Role.USER)) {
            Optional<ApplyFormDto> userInfo = userService.getUserInfo(claims.getSubject());
            return userInfo
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        throw new ForbiddenException();
    }

    @Operation(summary = "전체 유저 스터디 신청 정보 조회")
    @SecurityRequirement(name = "ADMIN")
    @Deprecated
    @GetMapping("/manageUsers")
    public List<UserDto.UserInfo> userList(
            @RequestAttribute Claims claims) {
        if (Role.isAuthorized(claims, Role.ADMIN)) {
            return userService.getUsers(claims.getSubject());
        }
        throw new ForbiddenException();
    }
}
