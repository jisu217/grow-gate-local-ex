package com.growgate.agent.service;

import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.KnowledgeLoopItem;
import com.growgate.knowledge.entity.KnowledgeSource;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.publicdata.entity.PublicDataRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EvidenceContextBuilder {

    public Map<String, Object> build(ProductKnowledge knowledge,
                                     List<Certification> certifications,
                                     List<Fto> ftoItems,
                                     List<BuyerRequirement> buyerRequirements,
                                     List<KnowledgeSource> knowledgeSources,
                                     List<PublicDataRecord> publicDataRecords,
                                     List<KnowledgeLoopItem> acceptedLoopItems) {
        List<RetrievalChunk> chunks = buildChunks(knowledge, certifications, ftoItems, buyerRequirements,
                knowledgeSources, publicDataRecords, acceptedLoopItems);
        String query = buildQuery(knowledge, certifications, ftoItems, buyerRequirements, publicDataRecords);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("productKnowledge", knowledge == null ? Map.of() : Map.of(
                        "trustGrade", knowledge.getTrustGrade() != null ? knowledge.getTrustGrade().name() : "",
                        "sourceType", safe(knowledge.getSourceType()),
                        "notes", safe(knowledge.getNotes())
                ));
        context.put("certifications", certifications.stream().limit(10).map(cert -> Map.of(
                        "name", safe(cert.getName()),
                        "expiresAt", cert.getExpiresAt() != null ? cert.getExpiresAt().toString() : "",
                        "renewalCycle", cert.getRenewalCycle() == null ? "" : cert.getRenewalCycle().toString()
                )).toList());
        context.put("fto", ftoItems.stream().limit(10).map(fto -> Map.of(
                        "targetCountry", safe(fto.getTargetCountry()),
                        "riskLevel", fto.getRiskLevel() != null ? fto.getRiskLevel().name() : "",
                        "summary", safe(fto.getSummary())
                )).toList());
        context.put("buyerRequirements", buyerRequirements.stream().limit(10).map(req -> Map.of(
                        "buyerName", safe(req.getBuyerName()),
                        "country", safe(req.getCountry()),
                        "requirements", safe(req.getRequirements())
                )).toList());
        context.put("knowledgeSources", knowledgeSources.stream().limit(10).map(source -> Map.of(
                        "documentName", safe(source.getDocumentName()),
                        "url", safe(source.getUrl()),
                        "retrievedAt", source.getRetrievedAt() != null ? source.getRetrievedAt().toString() : ""
                )).toList());
        context.put("publicDataSignals", publicDataRecords.stream().limit(10).map(record -> Map.of(
                        "provider", record.getProvider() != null ? record.getProvider().name() : "",
                        "usedFor", safe(record.getUsedFor()),
                        "matchedKey", safe(record.getMatchedKey()),
                        "fallbackUsed", record.isFallbackUsed(),
                        "scoreContribution", record.getScoreContribution() == null ? 0 : record.getScoreContribution()
                )).toList());
        context.put("acceptedKnowledgeLoop", acceptedLoopItems.stream().limit(10).map(item -> Map.of(
                        "type", safe(item.getType()),
                        "title", safe(item.getTitle()),
                        "evidence", safe(item.getEvidence()),
                        "rationale", safe(item.getRationale()),
                        "impact", safe(item.getImpact())
                )).toList());
        context.put("retrievalMode", "source-bound-evidence-search");
        context.put("retrievalLabel", "DB 지식과 공공데이터 record를 citation 가능한 chunk로 검색");
        context.put("claimBoundary", "vector DB RAG가 아니라 진단 DB와 source trace 안에서만 근거를 찾는 bounded evidence retrieval");
        context.put("retrievalQuery", query);
        context.put("retrievalTopK", retrieveTopK(query, chunks, 5));
        return context;
    }

    private List<RetrievalChunk> buildChunks(ProductKnowledge knowledge,
                                             List<Certification> certifications,
                                             List<Fto> ftoItems,
                                             List<BuyerRequirement> buyerRequirements,
                                             List<KnowledgeSource> knowledgeSources,
                                             List<PublicDataRecord> publicDataRecords,
                                             List<KnowledgeLoopItem> acceptedLoopItems) {
        List<RetrievalChunk> chunks = new ArrayList<>();
        if (knowledge != null && !safe(knowledge.getNotes()).isBlank()) {
            chunks.add(new RetrievalChunk("knowledge:product-notes", "ProductKnowledge", "product_knowledge",
                    "internal", safe(knowledge.getNotes())));
        }
        certifications.stream().limit(20).forEach(cert -> chunks.add(new RetrievalChunk(
                "certification:" + safe(cert.getName()),
                safe(cert.getName()),
                "certification",
                "internal",
                "certification " + safe(cert.getName()) + " expiresAt "
                        + (cert.getExpiresAt() == null ? "" : cert.getExpiresAt()) + " renewalCycle "
                        + (cert.getRenewalCycle() == null ? "" : cert.getRenewalCycle())
        )));
        ftoItems.stream().limit(20).forEach(fto -> chunks.add(new RetrievalChunk(
                "fto:" + safe(fto.getTargetCountry()) + ":" + chunks.size(),
                safe(fto.getTargetCountry()),
                "fto",
                "internal",
                "fto targetCountry " + safe(fto.getTargetCountry()) + " risk "
                        + (fto.getRiskLevel() == null ? "" : fto.getRiskLevel().name()) + " " + safe(fto.getSummary())
        )));
        buyerRequirements.stream().limit(20).forEach(req -> chunks.add(new RetrievalChunk(
                "buyer:" + safe(req.getBuyerName()) + ":" + chunks.size(),
                safe(req.getBuyerName()),
                "buyer_requirement",
                "internal",
                "buyer " + safe(req.getBuyerName()) + " country " + safe(req.getCountry())
                        + " requirements " + safe(req.getRequirements())
        )));
        knowledgeSources.stream().limit(20).forEach(source -> chunks.add(new RetrievalChunk(
                "source:" + safe(source.getDocumentName()) + ":" + chunks.size(),
                safe(source.getUrl()),
                "knowledge_source",
                "source_document",
                safe(source.getDocumentName()) + " " + safe(source.getUrl())
        )));
        publicDataRecords.stream().limit(30).forEach(record -> chunks.add(new RetrievalChunk(
                "public-data:" + safe(record.getId()),
                safe(record.getMatchedKey()),
                "public_data",
                safe(record.getSourceType()).isBlank() ? (record.isFallbackUsed() ? "fallback" : "live") : record.getSourceType(),
                "provider " + (record.getProvider() == null ? "" : record.getProvider().name())
                        + " usedFor " + safe(record.getUsedFor())
                        + " hsCode " + safe(record.getHsCode())
                        + " targetCountry " + safe(record.getTargetCountry())
                        + " matchedKey " + safe(record.getMatchedKey())
                        + " metrics " + safe(record.getNormalizedPayload())
        )));
        acceptedLoopItems.stream().limit(20).forEach(item -> chunks.add(new RetrievalChunk(
                "knowledge-loop:" + safe(item.getId()),
                safe(item.getTitle()),
                "knowledge_loop",
                "accepted_loop",
                safe(item.getTitle()) + " " + safe(item.getEvidence()) + " "
                        + safe(item.getRationale()) + " " + safe(item.getImpact())
        )));
        return chunks;
    }

    private String buildQuery(ProductKnowledge knowledge,
                              List<Certification> certifications,
                              List<Fto> ftoItems,
                              List<BuyerRequirement> buyerRequirements,
                              List<PublicDataRecord> publicDataRecords) {
        return String.join(" ",
                safe(knowledge == null ? "" : knowledge.getNotes()),
                certifications.stream().map(Certification::getName).filter(Objects::nonNull).collect(Collectors.joining(" ")),
                ftoItems.stream().map(Fto::getTargetCountry).filter(Objects::nonNull).collect(Collectors.joining(" ")),
                buyerRequirements.stream().map(BuyerRequirement::getRequirements).filter(Objects::nonNull).collect(Collectors.joining(" ")),
                publicDataRecords.stream().map(record -> safe(record.getHsCode()) + " " + safe(record.getTargetCountry())
                        + " " + safe(record.getUsedFor())).collect(Collectors.joining(" "))
        ).trim();
    }

    private List<Map<String, Object>> retrieveTopK(String query, List<RetrievalChunk> chunks, int k) {
        Set<String> queryTokens = tokenize(query);
        return chunks.stream()
                .map(chunk -> Map.entry(chunk, score(queryTokens, chunk.text())))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<RetrievalChunk, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(k)
                .map(entry -> {
                    RetrievalChunk chunk = entry.getKey();
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("chunkId", chunk.chunkId());
                    result.put("sourceId", chunk.sourceId());
                    result.put("sourceType", chunk.sourceType());
                    result.put("category", chunk.category());
                    result.put("score", Math.round(entry.getValue() * 100.0) / 100.0);
                    result.put("sourceBound", !chunk.sourceId().isBlank() && !"internal".equals(chunk.sourceId()));
                    result.put("displayRole", displayRole(chunk.category(), chunk.sourceType()));
                    result.put("usedSentence", firstSentence(chunk.text()));
                    return result;
                })
                .toList();
    }

    private double score(Set<String> queryTokens, String text) {
        if (queryTokens.isEmpty() || text == null || text.isBlank()) return 0;
        List<String> chunkTokens = tokenize(text).stream().toList();
        if (chunkTokens.isEmpty()) return 0;
        double score = 0;
        for (String token : queryTokens) {
            long tf = chunkTokens.stream().filter(token::equals).count();
            if (tf > 0) {
                score += (1.0 + Math.log(tf)) * Math.log(1.0 + (120.0 / chunkTokens.size()));
            }
        }
        return score;
    }

    private Set<String> tokenize(String value) {
        return List.of(safe(value).toLowerCase().split("[^a-z0-9가-힣]+")).stream()
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
    }

    private String firstSentence(String value) {
        String safeValue = safe(value).replaceAll("\\s+", " ").trim();
        if (safeValue.length() <= 220) return safeValue;
        return safeValue.substring(0, 220) + "...";
    }

    private String displayRole(String category, String sourceType) {
        if ("public_data".equals(category)) return "공공데이터 근거 chunk";
        if ("knowledge_loop".equals(category)) return "담당자 승인 지식화 chunk";
        if ("knowledge_source".equals(category)) return "등록 문서 출처 chunk";
        if ("fallback".equals(sourceType)) return "fallback 보강 필요 chunk";
        return "제품 지식 chunk";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record RetrievalChunk(String chunkId, String sourceId, String category, String sourceType, String text) {
    }
}
