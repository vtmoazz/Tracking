package com.example.tracking.module.auth;

import com.example.tracking.common.ApiResponse;
import com.example.tracking.module.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1.1 Đăng ký
    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(req)));
    }

    // 1.2 Đăng nhập
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    // 1.3 Refresh token
    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(req)));
    }

    // 1.4 Đăng xuất
    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công", null));
    }

    // 1.5 Quên mật khẩu
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Email đặt lại mật khẩu đã được gửi", null));
    }

    // 1.6 Đặt lại mật khẩu
    @PostMapping("/auth/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công", null));
    }

    // 1.7 Đổi mật khẩu
    @PostMapping("/auth/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(userDetails.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }

    // 1.8 Lấy profile
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(userDetails.getUsername())));
    }

    // 1.9 Cập nhật profile
    @PutMapping("/users/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.updateProfile(userDetails.getUsername(), req)));
    }
}
