package com.growgate.knowledge.controller;

import com.growgate.knowledge.dto.CreateKnowledgeRequest;
import com.growgate.knowledge.dto.CreateKnowledgeResponse;
import com.growgate.knowledge.dto.DeleteKnowledgeResponse;
import com.growgate.knowledge.dto.KnowledgeDetailResponse;
import com.growgate.knowledge.dto.KnowledgeListResponse;
import com.growgate.knowledge.dto.KnowledgeLogsResponse;
import com.growgate.knowledge.dto.UpdateKnowledgeRequest;
import com.growgate.knowledge.dto.UpdateKnowledgeResponse;
import com.growgate.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public ResponseEntity<KnowledgeListResponse> getList(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(knowledgeService.getList(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CreateKnowledgeResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateKnowledgeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.create(userDetails.getUsername(), request));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<KnowledgeDetailResponse> getDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        return ResponseEntity.ok(knowledgeService.getDetail(userDetails.getUsername(), productId));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<UpdateKnowledgeResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @RequestBody UpdateKnowledgeRequest request) {
        return ResponseEntity.ok(knowledgeService.update(userDetails.getUsername(), productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<DeleteKnowledgeResponse> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        return ResponseEntity.ok(knowledgeService.delete(userDetails.getUsername(), productId));
    }

    @GetMapping("/{productId}/logs")
    public ResponseEntity<KnowledgeLogsResponse> getLogs(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        return ResponseEntity.ok(knowledgeService.getLogs(userDetails.getUsername(), productId));
    }
}
