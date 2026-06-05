package com.example.tracking.module.auth;

import com.example.tracking.common.exception.AppException;
import com.example.tracking.config.JwtConfig;
import com.example.tracking.module.auth.dto.*;
import com.example.tracking.module.auth.entity.PasswordResetToken;
import com.example.tracking.module.auth.entity.RefreshToken;
import com.example.tracking.module.auth.entity.User;
import com.example.tracking.module.auth.repository.PasswordResetTokenRepository;
import com.example.tracking.module.auth.repository.RefreshTokenRepository;
import com.example.tracking.module.auth.repository.UserRepository;
import com.example.tracking.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final JavaMailSender mailSender;

    // -------------------------------------------------------------------------
    // 1.1 Đăng ký
    // -------------------------------------------------------------------------
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailAndDeletedFalse(req.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }

        User.Role role = req.getRole() != null ? req.getRole() : User.Role.BUYER;

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .role(role)
                .status(User.Status.ACTIVE)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    // -------------------------------------------------------------------------
    // 1.2 Đăng nhập
    // -------------------------------------------------------------------------
    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailAndDeletedFalse(req.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (user.getPassword() == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá");
        }

        return buildAuthResponse(user);
    }

    // -------------------------------------------------------------------------
    // 1.3 Refresh token
    // -------------------------------------------------------------------------
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest req) {
        RefreshToken rt = refreshTokenRepository
                .findByTokenAndRevokedFalseAndDeletedFalse(req.getRefreshToken())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"));

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn");
        }

        User user = rt.getUser();
        String accessToken = generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(req.getRefreshToken())
                .user(UserDto.from(user))
                .build();
    }

    // -------------------------------------------------------------------------
    // 1.4 Đăng xuất
    // -------------------------------------------------------------------------
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalseAndDeletedFalse(refreshToken)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    // -------------------------------------------------------------------------
    // 1.5 Quên mật khẩu
    // -------------------------------------------------------------------------
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmailAndDeletedFalse(req.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(prt);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject("Đặt lại mật khẩu");
            mail.setText("Link đặt lại mật khẩu (hết hạn sau 1 giờ):\n"
                    + "http://localhost:8080/reset-password?token=" + token);
            mailSender.send(mail);
        });
    }

    // -------------------------------------------------------------------------
    // 1.6 Đặt lại mật khẩu
    // -------------------------------------------------------------------------
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken prt = passwordResetTokenRepository
                .findByTokenAndUsedFalseAndDeletedFalse(req.getToken())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc đã sử dụng"));

        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token đã hết hạn");
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);

        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    // -------------------------------------------------------------------------
    // 1.7 Đổi mật khẩu
    // -------------------------------------------------------------------------
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không đúng");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // 1.8 Lấy profile
    // -------------------------------------------------------------------------
    public UserDto getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        return UserDto.from(user);
    }

    // -------------------------------------------------------------------------
    // 1.9 Cập nhật profile
    // -------------------------------------------------------------------------
    @Transactional
    public UserDto updateProfile(String userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());

        return UserDto.from(userRepository.save(user));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.from(user))
                .build();
    }

    private String generateAccessToken(User user) {
        return jwtUtil.generateAccessToken(user.getId(), Map.of("role", user.getRole().name()));
    }

    private String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtConfig.getRefreshTokenExpiry()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(rt);
        return token;
    }
}
