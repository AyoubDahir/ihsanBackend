package com.alihsan.backend.repository;

import com.alihsan.backend.domain.AuthOtpSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthOtpSessionRepository extends JpaRepository<AuthOtpSession, Long> {
    Optional<AuthOtpSession> findTopByMobileAndVerifiedAtIsNullOrderByCreatedAtDesc(String mobile);
}
