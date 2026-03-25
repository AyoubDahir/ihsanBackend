package com.alihsan.backend.service;

import com.alihsan.backend.domain.AuthOtpSession;
import com.alihsan.backend.dto.AuthCheckMobileResponse;
import com.alihsan.backend.dto.AuthSelfRegisterResponse;
import com.alihsan.backend.dto.AuthSendOtpResponse;
import com.alihsan.backend.dto.AuthVerifyOtpResponse;
import com.alihsan.backend.repository.AuthOtpSessionRepository;
import com.alihsan.backend.util.MobileNumberUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class MobileAuthService {
    private static final Logger log = LoggerFactory.getLogger(MobileAuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PrimeWorkflowService primeWorkflowService;
    private final AuthOtpSessionRepository authOtpSessionRepository;
    private final SmsService smsService;
    private final int otpTtlSeconds;
    private final int otpMaxAttempts;
    private final boolean returnOtpInResponse;

    public MobileAuthService(
        PrimeWorkflowService primeWorkflowService,
        AuthOtpSessionRepository authOtpSessionRepository,
        SmsService smsService,
        @Value("${auth.otp.ttl-seconds:300}") int otpTtlSeconds,
        @Value("${auth.otp.max-attempts:5}") int otpMaxAttempts,
        @Value("${auth.otp.return-in-response:false}") boolean returnOtpInResponse
    ) {
        this.primeWorkflowService = primeWorkflowService;
        this.authOtpSessionRepository = authOtpSessionRepository;
        this.smsService = smsService;
        this.otpTtlSeconds = otpTtlSeconds;
        this.otpMaxAttempts = otpMaxAttempts;
        this.returnOtpInResponse = returnOtpInResponse;
    }

    public AuthCheckMobileResponse checkMobile(String mobile) {
        String normalizedMobile = MobileNumberUtil.normalize(mobile);
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new IllegalArgumentException("mobile is required");
        }
        Map<String, String> patient = primeWorkflowService.findPatientByMobile(normalizedMobile);
        if (patient == null) {
            return new AuthCheckMobileResponse(
                false,
                normalizedMobile,
                null,
                null,
                "SIGNUP"
            );
        }
        return new AuthCheckMobileResponse(
            true,
            normalizedMobile,
            patient.get("id"),
            patient.get("name"),
            "SEND_OTP"
        );
    }

    @Transactional
    public AuthSendOtpResponse sendOtp(String mobile) {
        String normalizedMobile = MobileNumberUtil.normalize(mobile);
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new IllegalArgumentException("mobile is required");
        }
        String otpCode = generateOtp();
        smsService.sendOtp(normalizedMobile, otpCode);

        AuthOtpSession session = new AuthOtpSession();
        session.setMobile(normalizedMobile);
        session.setOtpCode(otpCode);
        session.setCreatedAt(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plusSeconds(otpTtlSeconds));
        session.setFailedAttempts(0);
        session.setMaxAttempts(otpMaxAttempts);
        authOtpSessionRepository.save(session);

        log.info("Generated OTP for mobile {} with session {}", normalizedMobile, session.getId());

        return new AuthSendOtpResponse(
            true,
            normalizedMobile,
            session.getExpiresAt().toString(),
            "VERIFY_OTP",
            returnOtpInResponse ? otpCode : null
        );
    }

    @Transactional
    public AuthSelfRegisterResponse selfRegister(
        String firstName,
        String lastName,
        String fullName,
        String mobile,
        String sex,
        Integer age,
        String ageType
    ) {
        String normalizedMobile = MobileNumberUtil.normalize(mobile);
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new IllegalArgumentException("mobile is required");
        }

        Map<String, String> existing = primeWorkflowService.findPatientByMobile(normalizedMobile);
        if (existing != null) {
            return new AuthSelfRegisterResponse(
                false,
                existing.get("id"),
                existing.get("name"),
                normalizedMobile,
                "SEND_OTP"
            );
        }

        String patientId = primeWorkflowService.registerPatientFromMobile(
            firstName,
            lastName,
            fullName,
            normalizedMobile,
            sex,
            age,
            ageType
        );

        Map<String, String> patient = primeWorkflowService.findPatientByMobile(normalizedMobile);
        return new AuthSelfRegisterResponse(
            true,
            patientId,
            patient == null ? null : patient.get("name"),
            normalizedMobile,
            "SEND_OTP"
        );
    }

    @Transactional
    public AuthVerifyOtpResponse verifyOtp(String mobile, String otp) {
        String normalizedMobile = MobileNumberUtil.normalize(mobile);
        String otpValue = otp == null ? "" : otp.trim();
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new IllegalArgumentException("mobile is required");
        }
        if (otpValue.isBlank()) {
            throw new IllegalArgumentException("otp is required");
        }

        Optional<AuthOtpSession> latestSession = authOtpSessionRepository
            .findTopByMobileAndVerifiedAtIsNullOrderByCreatedAtDesc(normalizedMobile);
        if (latestSession.isEmpty()) {
            throw new IllegalArgumentException("No active OTP session for this mobile");
        }

        AuthOtpSession session = latestSession.get();
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OTP expired. Please request a new OTP.");
        }
        if (session.getFailedAttempts() >= session.getMaxAttempts()) {
            throw new IllegalArgumentException("OTP attempts exceeded. Please request a new OTP.");
        }
        if (!session.getOtpCode().equals(otpValue)) {
            session.setFailedAttempts(session.getFailedAttempts() + 1);
            authOtpSessionRepository.save(session);
            throw new IllegalArgumentException("Invalid OTP");
        }

        session.setVerifiedAt(OffsetDateTime.now());
        authOtpSessionRepository.save(session);

        Map<String, String> patient = primeWorkflowService.findPatientByMobile(normalizedMobile);
        if (patient == null) {
            return new AuthVerifyOtpResponse(
                true,
                normalizedMobile,
                false,
                null,
                null,
                "SIGNUP"
            );
        }

        return new AuthVerifyOtpResponse(
            true,
            normalizedMobile,
            true,
            patient.get("id"),
            patient.get("name"),
            "LOGIN_SUCCESS"
        );
    }

    private String generateOtp() {
        int value = SECURE_RANDOM.nextInt(900_000) + 100_000;
        return String.valueOf(value);
    }
}
