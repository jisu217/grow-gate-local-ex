package com.growgate.knowledge.controller;

// 제품 지식 API 엔드포인트
import com.growgate.knowledge.dto.KnowledgeDetailResponse;
import com.growgate.knowledge.dto.KnowledgeLogsResponse;
import com.growgate.knowledge.dto.UpdateKnowledgeRequest;
import com.growgate.knowledge.dto.UpdateKnowledgeResponse;
import com.growgate.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/{productId}/knowledge")
    public ResponseEntity<KnowledgeDetailResponse> getDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        return ResponseEntity.ok(knowledgeService.getDetail(userDetails.getUsername(), productId));
    }

    @PutMapping("/{productId}/knowledge")
    public ResponseEntity<UpdateKnowledgeResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @RequestBody UpdateKnowledgeRequest request) {
        return ResponseEntity.ok(knowledgeService.update(userDetails.getUsername(), productId, request));
    }

    @GetMapping("/{productId}/audit-logs")
    public ResponseEntity<KnowledgeLogsResponse> getLogs(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        return ResponseEntity.ok(knowledgeService.getLogs(userDetails.getUsername(), productId));
    }
}
