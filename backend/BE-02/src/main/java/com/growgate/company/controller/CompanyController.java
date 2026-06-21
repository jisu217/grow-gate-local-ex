package com.growgate.company.controller;

// 내 회사 정보 API 엔드포인트
import com.growgate.company.dto.CompanyMeResponse;
import com.growgate.company.dto.UpdateCompanyRequest;
import com.growgate.company.dto.UpdateCompanyResponse;
import com.growgate.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/me")
    public ResponseEntity<CompanyMeResponse> getMyCompany(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(companyService.getMyCompany(userDetails.getUsername()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UpdateCompanyResponse> updateMyCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateCompanyRequest request) {
        return ResponseEntity.ok(companyService.updateMyCompany(userDetails.getUsername(), request));
    }
}
