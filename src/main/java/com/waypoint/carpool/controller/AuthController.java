package com.waypoint.carpool.controller;

import com.waypoint.carpool.dto.auth.*;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.service.AuthService;
import com.waypoint.carpool.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleAuthRequest req) {
        return authService.loginWithGoogle(req.idToken());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }

    @PatchMapping("/me/upi")
    public UserResponse updateUpi(@AuthenticationPrincipal User user, @Valid @RequestBody UpiUpdateRequest req) {
        return authService.updateUpiId(user, req.upiId());
    }

    // ---- forgot password (email OTP) ----

    // Always responds 204 whether or not the email is registered — see
    // PasswordResetService.requestOtp for why (prevents account enumeration).
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.requestOtp(req.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.email(), req.code(), req.newPassword());
    }
}
