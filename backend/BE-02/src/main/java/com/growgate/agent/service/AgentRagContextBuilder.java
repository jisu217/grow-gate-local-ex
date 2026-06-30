package com.growgate.agent.service;

import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.KnowledgeLoopItem;
import com.growgate.knowledge.entity.KnowledgeSource;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.publicdata.entity.PublicDataRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AgentRagContextBuilder {

    public Map<String, Object> build(ProductKnowledge knowledge,
                                     List<Certification> certifications,
                                     List<Fto> ftoItems,
                                     List<BuyerRequirement> buyerRequirements,
                                     List<KnowledgeSource> knowledgeSources,
                                     List<PublicDataRecord> publicDataRecords,
                                     List<KnowledgeLoopItem> acceptedLoopItems) {
        return Map.of(
                "productKnowledge", knowledge == null ? Map.of() : Map.of(
                        "trustGrade", knowledge.getTrustGrade() != null ? knowledge.getTrustGrade().name() : "",
                        "sourceType", safe(knowledge.getSourceType()),
                        "notes", safe(knowledge.getNotes())
                ),
                "certifications", certifications.stream().limit(10).map(cert -> Map.of(
                        "name", safe(cert.getName()),
                        "expiresAt", cert.getExpiresAt() != null ? cert.getExpiresAt().toString() : "",
                        "renewalCycle", cert.getRenewalCycle() == null ? "" : cert.getRenewalCycle().toString()
                )).toList(),
                "fto", ftoItems.stream().limit(10).map(fto -> Map.of(
                        "targetCountry", safe(fto.getTargetCountry()),
                        "riskLevel", fto.getRiskLevel() != null ? fto.getRiskLevel().name() : "",
                        "summary", safe(fto.getSummary())
                )).toList(),
                "buyerRequirements", buyerRequirements.stream().limit(10).map(req -> Map.of(
                        "buyerName", safe(req.getBuyerName()),
                        "country", safe(req.getCountry()),
                        "requirements", safe(req.getRequirements())
                )).toList(),
                "knowledgeSources", knowledgeSources.stream().limit(10).map(source -> Map.of(
                        "documentName", safe(source.getDocumentName()),
                        "url", safe(source.getUrl()),
                        "retrievedAt", source.getRetrievedAt() != null ? source.getRetrievedAt().toString() : ""
                )).toList(),
                "publicDataSignals", publicDataRecords.stream().limit(10).map(record -> Map.of(
                        "provider", record.getProvider() != null ? record.getProvider().name() : "",
                        "usedFor", safe(record.getUsedFor()),
                        "matchedKey", safe(record.getMatchedKey()),
                        "fallbackUsed", record.isFallbackUsed(),
                        "scoreContribution", record.getScoreContribution() == null ? 0 : record.getScoreContribution()
                )).toList(),
                "acceptedKnowledgeLoop", acceptedLoopItems.stream().limit(10).map(item -> Map.of(
                        "type", safe(item.getType()),
                        "title", safe(item.getTitle()),
                        "evidence", safe(item.getEvidence()),
                        "rationale", safe(item.getRationale()),
                        "impact", safe(item.getImpact())
                )).toList()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
