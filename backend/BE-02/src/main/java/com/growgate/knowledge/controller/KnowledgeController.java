package com.growgate.knowledge.controller;

// 제품 지식 API 엔드포인트
import com.growgate.knowledge.dto.BuyerRequirementResponse;
import com.growgate.knowledge.dto.CertificationResponse;
import com.growgate.knowledge.dto.CreateBuyerRequirementRequest;
import com.growgate.knowledge.dto.CreateCertificationRequest;
import com.growgate.knowledge.dto.CreateFtoRequest;
import com.growgate.knowledge.dto.CreateKnowledgeSourceRequest;
import com.growgate.knowledge.dto.DeleteEntryResponse;
import com.growgate.knowledge.dto.FtoResponse;
import com.growgate.knowledge.dto.KnowledgeDetailResponse;
import com.growgate.knowledge.dto.KnowledgeLoopActionResponse;
import com.growgate.knowledge.dto.KnowledgeLoopItemsResponse;
import com.growgate.knowledge.dto.KnowledgeLogsResponse;
import com.growgate.knowledge.dto.KnowledgeSourceResponse;
import com.growgate.knowledge.dto.UpdateBuyerRequirementRequest;
import com.growgate.knowledge.dto.UpdateCertificationRequest;
import com.growgate.knowledge.dto.UpdateFtoRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(knowledgeService.getLogs(userDetails.getUsername(), productId, page, size));
    }

    @PostMapping("/{productId}/certifications")
    public ResponseEntity<CertificationResponse> createCertification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @Valid @RequestBody CreateCertificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.createCertification(userDetails.getUsername(), productId, request));
    }

    @PutMapping("/{productId}/certifications/{certificationId}")
    public ResponseEntity<CertificationResponse> updateCertification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String certificationId,
            @RequestBody UpdateCertificationRequest request) {
        return ResponseEntity.ok(
                knowledgeService.updateCertification(userDetails.getUsername(), productId, certificationId, request));
    }

    @DeleteMapping("/{productId}/certifications/{certificationId}")
    public ResponseEntity<DeleteEntryResponse> deleteCertification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String certificationId) {
        return ResponseEntity.ok(
                knowledgeService.deleteCertification(userDetails.getUsername(), productId, certificationId));
    }

    @PostMapping("/{productId}/fto")
    public ResponseEntity<FtoResponse> createFto(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @Valid @RequestBody CreateFtoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.createFto(userDetails.getUsername(), productId, request));
    }

    @PutMapping("/{productId}/fto/{ftoId}")
    public ResponseEntity<FtoResponse> updateFto(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String ftoId,
            @RequestBody UpdateFtoRequest request) {
        return ResponseEntity.ok(knowledgeService.updateFto(userDetails.getUsername(), productId, ftoId, request));
    }

    @DeleteMapping("/{productId}/fto/{ftoId}")
    public ResponseEntity<DeleteEntryResponse> deleteFto(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String ftoId) {
        return ResponseEntity.ok(knowledgeService.deleteFto(userDetails.getUsername(), productId, ftoId));
    }

    @PostMapping("/{productId}/buyer-requirements")
    public ResponseEntity<BuyerRequirementResponse> createBuyerRequirement(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @Valid @RequestBody CreateBuyerRequirementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.createBuyerRequirement(userDetails.getUsername(), productId, request));
    }

    @PutMapping("/{productId}/buyer-requirements/{buyerRequirementId}")
    public ResponseEntity<BuyerRequirementResponse> updateBuyerRequirement(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String buyerRequirementId,
            @RequestBody UpdateBuyerRequirementRequest request) {
        return ResponseEntity.ok(knowledgeService.updateBuyerRequirement(
                userDetails.getUsername(), productId, buyerRequirementId, request));
    }

    @DeleteMapping("/{productId}/buyer-requirements/{buyerRequirementId}")
    public ResponseEntity<DeleteEntryResponse> deleteBuyerRequirement(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String buyerRequirementId) {
        return ResponseEntity.ok(knowledgeService.deleteBuyerRequirement(
                userDetails.getUsername(), productId, buyerRequirementId));
    }

    @PostMapping("/{productId}/knowledge/sources")
    public ResponseEntity<KnowledgeSourceResponse> createKnowledgeSource(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @RequestBody CreateKnowledgeSourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.createKnowledgeSource(userDetails.getUsername(), productId, request));
    }

    @DeleteMapping("/{productId}/knowledge/sources/{sourceId}")
    public ResponseEntity<DeleteEntryResponse> deleteKnowledgeSource(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String sourceId) {
        return ResponseEntity.ok(knowledgeService.deleteKnowledgeSource(userDetails.getUsername(), productId, sourceId));
    }

    @GetMapping("/{productId}/knowledge/loop-items")
    public ResponseEntity<KnowledgeLoopItemsResponse> getKnowledgeLoopItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        return ResponseEntity.ok(knowledgeService.getKnowledgeLoopItems(userDetails.getUsername(), productId));
    }

    @PostMapping("/{productId}/knowledge/loop-items/{itemId}/accept")
    public ResponseEntity<KnowledgeLoopActionResponse> acceptKnowledgeLoopItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String itemId) {
        return ResponseEntity.ok(knowledgeService.acceptKnowledgeLoopItem(userDetails.getUsername(), productId, itemId));
    }

    @PostMapping("/{productId}/knowledge/loop-items/{itemId}/dismiss")
    public ResponseEntity<KnowledgeLoopActionResponse> dismissKnowledgeLoopItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @PathVariable String itemId) {
        return ResponseEntity.ok(knowledgeService.dismissKnowledgeLoopItem(userDetails.getUsername(), productId, itemId));
    }
}
