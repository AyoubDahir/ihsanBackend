package com.alihsan.backend.controller;

import com.alihsan.backend.dto.AuthCheckMobileRequest;
import com.alihsan.backend.dto.AuthCheckMobileResponse;
import com.alihsan.backend.dto.AuthSendOtpRequest;
import com.alihsan.backend.dto.AuthSendOtpResponse;
import com.alihsan.backend.dto.AuthVerifyOtpRequest;
import com.alihsan.backend.dto.AuthVerifyOtpResponse;
import com.alihsan.backend.service.MobileAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {
    private final MobileAuthService mobileAuthService;

    public MobileAuthController(MobileAuthService mobileAuthService) {
        this.mobileAuthService = mobileAuthService;
    }

    @PostMapping("/check-mobile")
    public AuthCheckMobileResponse checkMobile(@Valid @RequestBody AuthCheckMobileRequest request) {
        return mobileAuthService.checkMobile(request.mobile());
    }

    @PostMapping("/send-otp")
    public AuthSendOtpResponse sendOtp(@Valid @RequestBody AuthSendOtpRequest request) {
        return mobileAuthService.sendOtp(request.mobile());
    }

    @PostMapping("/verify-otp")
    public AuthVerifyOtpResponse verifyOtp(@Valid @RequestBody AuthVerifyOtpRequest request) {
        return mobileAuthService.verifyOtp(request.mobile(), request.otp());
    }
}
