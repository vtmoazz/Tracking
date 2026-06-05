package com.example.tracking.module.auth.repository;

import com.example.tracking.module.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByTokenAndUsedFalseAndDeletedFalse(String token);
}
