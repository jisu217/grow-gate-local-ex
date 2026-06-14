package com.growgate.auth.controller;

// 로그인·회원가입 API 엔드포인트
import com.growgate.auth.dto.LoginRequest;
import com.growgate.auth.dto.LoginResponse;
import com.growgate.auth.dto.SignupCompanyRequest;
import com.growgate.auth.dto.SignupPersonalRequest;
import com.growgate.auth.dto.SignupResponse;
import com.growgate.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    @PostMapping("/signup/personal")
    public ResponseEntity<SignupResponse> signupPersonal(@Valid @RequestBody SignupPersonalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupPersonal(request));
    }

    @PostMapping("/signup/company")
    public ResponseEntity<SignupResponse> signupCompany(@Valid @RequestBody SignupCompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupCompany(request));
    }
}
