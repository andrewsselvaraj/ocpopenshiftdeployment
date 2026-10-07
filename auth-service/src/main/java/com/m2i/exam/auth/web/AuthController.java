package com.m2i.exam.auth.web;

import com.m2i.exam.auth.service.AuthService;
import com.m2i.exam.auth.service.UserService;
import com.m2i.exam.auth.web.AuthDtos.ChangePasswordRequest;
import com.m2i.exam.auth.web.AuthDtos.LoginRequest;
import com.m2i.exam.auth.web.AuthDtos.LoginResponse;
import com.m2i.exam.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Operation(summary = "Log in with e-mail and password; returns a bearer JWT")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Claims of the current token")
    @GetMapping("/me")
    public Map<String, Object> me() {
        Jwt jwt = CurrentUser.jwt();
        return jwt.getClaims();
    }

    @Operation(summary = "Change the caller's own password")
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(request);
    }
}
