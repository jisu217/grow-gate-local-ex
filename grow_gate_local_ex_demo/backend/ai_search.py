"""Deterministic AI search for the standalone search page."""
from __future__ import annotations

from typing import Any, Dict, List, Sequence

from .agents import GrowGateAgent
from .data_connectors import PipelineJob, PublicDataConnector, load_public_data_sources
from .ml_models import token_similarity
from .schemas import AISearchRequest, AISearchResponse, AISearchResultItem, AISuggestedAction


INTENT_TERMS = {
    "regulation": {"규제", "인증", "ce", "iec", "un38.3", "여권", "passport", "battery passport", "rohs", "fcc", "ul"},
    "recall": {"리콜", "위험", "안전", "화재", "발열", "단락", "hazard", "recall", "safety"},
    "support": {"지원", "사업", "보조금", "바우처", "신청", "funding", "voucher"},
    "roadmap": {"다음", "액션", "로드맵", "일정", "30일", "60일", "90일", "todo", "action"},
    "data": {"데이터", "출처", "근거", "api", "source", "evidence", "catalog"},
    "market": {"시장", "수출", "관세", "대상국", "growth", "tariff", "market"},
    "patent": {"특허", "fto", "회피", "선행기술", "ipc", "patent"},
}

QUERY_EXPANSIONS = {
    "배터리 여권": ["battery passport", "EU battery regulation", "탄소발자국", "QR 식별체계"],
    "여권": ["battery passport", "EU 배터리 규정"],
    "리콜": ["recall", "제품안전", "hazard", "FMEA"],
    "인증": ["certification", "시험성적서", "DoC", "규격"],
    "지원": ["지원사업", "수출바우처", "해외규격인증"],
    "관세": ["tariff", "HS Code", "통관"],
    "특허": ["FTO", "선행기술", "IPC"],
}


class AISearchService:
    """Search diagnosis outputs, RAG-ready documents, and public data sources."""

    def __init__(self, agent: GrowGateAgent, connector: PublicDataConnector | None = None) -> None:
        self.agent = agent
        self.connector = connector or PublicDataConnector()

    def search(self, request: AISearchRequest) -> AISearchResponse:
        analysis = self.agent.analyze(request.product_input)
        profile = self._query_profile(request, analysis.model_dump())
        evidence = self._evidence_documents(profile)
        data_sources = self._data_sources(profile)
        results = self._analysis_results(profile, analysis.model_dump()) + self._evidence_results(evidence) + self._data_source_results(data_sources)
        if request.scope == "diagnosis":
            results = [item for item in results if item.result_type != "data_source"] + self._data_source_results(data_sources[:2])
        elif request.scope == "data_catalog":
            results = self._data_source_results(data_sources) + self._analysis_results(profile, analysis.model_dump())[:1]
        results = self._diversify_results(results, top_k=8)
        return AISearchResponse(
            query=request.query,
            scope=request.scope,
            answer=self._answer(request, analysis.model_dump(), results, evidence, data_sources, profile),
            results=results,
            evidence_documents=evidence[:5],
            data_sources=data_sources[:5],
            suggested_actions=self._actions(request.scope, bool(data_sources), profile, results),
            guardrails=[
                "AI 검색은 로컬 seed 데이터와 RAG-ready 문서를 바탕으로 한 시연용 판단입니다.",
                "인증·통상 최종 판단은 공식 원문, 시험기관, 관세사, 변리사 검토로 확정해야 합니다.",
            ],
            quality_signals={
                "ranking_strategy": "intent_context_hybrid_mmr",
                "detected_intents": profile["intents"],
                "expanded_terms": profile["expanded_terms"],
                "context_terms": profile["context_terms"],
                "diversified": True,
                "recommendation_basis": [
                    "query intent",
                    "product features",
                    "target market",
                    "HS Code",
                    "evidence confidence",
                ],
            },
        )

    def _query_profile(self, request: AISearchRequest, analysis: Dict[str, Any]) -> Dict[str, Any]:
        query = request.query
        query_lower = query.lower()
        expanded_terms: list[str] = []
        for keyword, terms in QUERY_EXPANSIONS.items():
            if keyword.lower() in query_lower:
                expanded_terms.extend(terms)
        detected = []
        expanded_text = " ".join([query, *expanded_terms])
        expanded_lower = expanded_text.lower()
        for intent, terms in INTENT_TERMS.items():
            if any(term.lower() in expanded_lower for term in terms):
                detected.append(intent)
        if not detected:
            detected = ["data" if request.scope == "data_catalog" else "regulation"]
        product = analysis["request"]
        context_terms = [
            product["product_name"],
            product["hs_code"],
            product["target_market"],
            *product.get("features", []),
        ]
        return {
            "query": query,
            "expanded_query": " ".join([query, *expanded_terms, *context_terms]),
            "expanded_terms": sorted(set(expanded_terms)),
            "intents": detected,
            "context_terms": context_terms,
        }

    def _analysis_results(self, profile: Dict[str, Any], analysis: Dict[str, Any]) -> List[AISearchResultItem]:
        request = analysis["request"]
        summary = (
            f"{request['product_name']} 준비도 {analysis['readiness_score']}/100, "
            f"규제 {len(analysis['regulations'])}건, 리콜 {len(analysis['recalls'])}건, "
            f"지원사업 {len(analysis['support_programs'])}건이 연결되었습니다."
        )
        results = [
            AISearchResultItem(
                title=f"{request['product_name']} 준비도 진단 요약",
                result_type="analysis",
                source_type="agent_analysis",
                summary=summary,
                score=self._hybrid_score(profile, analysis["ai_summary"], "analysis", "agent_analysis", base_floor=0.7),
                metadata={
                    "readiness_score": analysis["readiness_score"],
                    "target_market": request["target_market"],
                    "rank_reason": "제품 입력값과 준비도 진단 요약을 기준으로 기본 문맥 결과를 우선 노출",
                },
            )
        ]
        for item in analysis["regulations"][:5]:
            text = f"{item['title']} {item['scope']} {item['action']} {' '.join(item.get('typical_evidence', []))}"
            score = self._hybrid_score(profile, text, "rag_evidence", "regulation", priority=item.get("priority"))
            results.append(
                AISearchResultItem(
                    title=item["title"],
                    result_type="rag_evidence",
                    source_type="regulation",
                    summary=f"{item['scope']} · {item['action']}",
                    score=score,
                    source_id=item["id"],
                    metadata={
                        "priority": item["priority"],
                        "matched_reason": item.get("matched_reason", ""),
                        "rank_reason": self._rank_reason(profile, "regulation", text),
                    },
                )
            )
        for item in analysis["roadmap"][:3]:
            text = f"{item['day_bucket']} {item['title']} {' '.join(item['tasks'])} {item['output']}"
            results.append(
                AISearchResultItem(
                    title=f"{item['day_bucket']} 실행: {item['title']}",
                    result_type="roadmap",
                    source_type="roadmap",
                    summary=item["output"],
                    score=self._hybrid_score(profile, text, "roadmap", "roadmap", base_floor=0.34),
                    metadata={"owner": item["owner"], "rank_reason": self._rank_reason(profile, "roadmap", text)},
                )
            )
        for item in analysis["support_programs"][:3]:
            text = f"{item['name']} {item['benefit']} {item['priority_rule']}"
            results.append(
                AISearchResultItem(
                    title=item["name"],
                    result_type="support_program",
                    source_type="support",
                    summary=item["benefit"],
                    score=self._hybrid_score(profile, text, "support_program", "support", base_floor=0.32),
                    source_id=item["id"],
                    metadata={
                        "fit_score": item["fit_score"],
                        "roadmap_stage": item["roadmap_stage"],
                        "rank_reason": self._rank_reason(profile, "support", text),
                    },
                )
            )
        return results

    def _evidence_documents(self, profile: Dict[str, Any]) -> List[Dict[str, Any]]:
        pipeline = PipelineJob(self.connector)
        documents: List[Dict[str, Any]] = []
        for doc in pipeline.build_regulation_documents():
            documents.append(self._ranked_document(profile, "regulation", doc))
        for doc in pipeline.build_recall_documents():
            documents.append(self._ranked_document(profile, "recall", doc))
        documents.sort(key=lambda item: item["confidence"], reverse=True)
        return documents

    def _ranked_document(self, profile: Dict[str, Any], source_type: str, doc: Dict[str, Any]) -> Dict[str, Any]:
        text = f"{doc['title']} {doc['text']}"
        confidence = self._hybrid_score(profile, text, "rag_evidence", source_type, base_floor=0.12)
        return {
            "doc_id": f"{source_type}-{doc['doc_id']}",
            "source_type": source_type,
            "title": doc["title"],
            "confidence": round(confidence, 3),
            "matched_terms": self._matched_terms(profile, text),
            "chunk_text": doc["text"][:420],
            "rank_reason": self._rank_reason(profile, source_type, text),
        }

    def _evidence_results(self, evidence: Sequence[Dict[str, Any]]) -> List[AISearchResultItem]:
        return [
            AISearchResultItem(
                title=item["title"],
                result_type="rag_evidence",
                source_type=item["source_type"],
                summary=item["chunk_text"],
                score=item["confidence"],
                source_id=item["doc_id"],
            )
            for item in evidence[:6]
        ]

    def _data_sources(self, profile: Dict[str, Any]) -> List[Dict[str, Any]]:
        catalog = load_public_data_sources()
        sources = []
        for source in catalog.get("sources", []):
            text = " ".join(str(source.get(key, "")) for key in ("source_id", "name", "provider", "connector", "target_table", "use_in_product"))
            source = dict(source)
            source["score"] = self._hybrid_score(profile, text, "data_source", source.get("connector", "data"), base_floor=0.12)
            source["rank_reason"] = self._rank_reason(profile, "data", text)
            sources.append(source)
        sources.sort(key=lambda item: item["score"], reverse=True)
        return sources

    def _data_source_results(self, sources: Sequence[Dict[str, Any]]) -> List[AISearchResultItem]:
        return [
            AISearchResultItem(
                title=source["name"],
                result_type="data_source",
                source_type=source.get("connector", "public_data"),
                summary=source.get("use_in_product", ""),
                score=source.get("score", 0.12),
                source_id=source.get("source_id"),
                source_url=source.get("official_url"),
                metadata={
                    "provider": source.get("provider"),
                    "target_table": source.get("target_table"),
                    "update_cycle": source.get("update_cycle"),
                    "rank_reason": source.get("rank_reason", "데이터 출처 메타데이터와 검색어의 의미 유사도 기준"),
                },
            )
            for source in sources[:6]
        ]

    def _answer(
        self,
        request: AISearchRequest,
        analysis: Dict[str, Any],
        results: Sequence[AISearchResultItem],
        evidence: Sequence[Dict[str, Any]],
        data_sources: Sequence[Dict[str, Any]],
        profile: Dict[str, Any],
    ) -> str:
        product = analysis["request"]["product_name"]
        top_titles = ", ".join(item.title for item in results[:3]) or "검색 결과 없음"
        intent_text = ", ".join(profile["intents"])
        if request.scope == "data_catalog":
            return f"{product} 기준 데이터 출처 {len(data_sources)}건을 찾았습니다. 감지 의도는 {intent_text}이며, 상위 후보는 {top_titles}입니다."
        if request.scope == "global":
            return f"{product} 관련 준비도 진단, 근거 문서, 공공데이터 출처를 통합 검색했습니다. 감지 의도는 {intent_text}이며, 상위 결과는 {top_titles}입니다."
        return f"{product} 준비도 진단 문맥에서 근거 {len(evidence)}건을 찾았습니다. 감지 의도는 {intent_text}이며, 상위 결과는 {top_titles}입니다."

    def _actions(self, scope: str, has_sources: bool, profile: Dict[str, Any], results: Sequence[AISearchResultItem]) -> List[AISuggestedAction]:
        actions = [
            AISuggestedAction(label="검색 근거 다시 확인", action_type="run_search", payload={"scope": scope}),
            AISuggestedAction(label="실행 로드맵 보기", action_type="open_roadmap", payload={"href": "roadmap.html"}),
        ]
        if results:
            actions.insert(
                0,
                AISuggestedAction(
                    label="상위 근거 검토",
                    action_type="open_evidence",
                    payload={"source_id": results[0].source_id, "result_type": results[0].result_type},
                ),
            )
        if {"regulation", "recall", "patent"} & set(profile["intents"]):
            actions.append(
                AISuggestedAction(
                    label="제품 지식 보강",
                    action_type="review_source",
                    payload={"href": "product-knowledge.html", "reason": "검색 의도와 제품 사전 지식을 연결하면 추천 품질이 개선됩니다."},
                )
            )
        if has_sources:
            actions.append(AISuggestedAction(label="데이터 명세 화면 열기", action_type="open_data_catalog", payload={"href": "data-catalog.html"}))
        return actions

    def _hybrid_score(
        self,
        profile: Dict[str, Any],
        text: str,
        result_type: str,
        source_type: str,
        base_floor: float = 0.0,
        priority: str | None = None,
    ) -> float:
        lexical = token_similarity(profile["expanded_query"], text)
        intent_boost = self._intent_boost(profile["intents"], result_type, source_type)
        context_boost = min(0.18, len(self._matched_terms(profile, text)) * 0.045)
        priority_boost = {"critical": 0.12, "high": 0.08, "medium": 0.035, "low": 0.0}.get(str(priority or "").lower(), 0.0)
        score = max(base_floor, 0.22 + lexical * 0.48 + intent_boost + context_boost + priority_boost)
        return round(min(1.0, score), 3)

    def _intent_boost(self, intents: Sequence[str], result_type: str, source_type: str) -> float:
        boost = 0.0
        if "regulation" in intents and source_type == "regulation":
            boost += 0.18
        if "recall" in intents and source_type == "recall":
            boost += 0.18
        if "support" in intents and source_type == "support":
            boost += 0.15
        if "roadmap" in intents and result_type == "roadmap":
            boost += 0.15
        if "data" in intents and result_type == "data_source":
            boost += 0.14
        if "market" in intents and result_type in {"analysis", "data_source"}:
            boost += 0.1
        if "patent" in intents and source_type in {"support", "patent"}:
            boost += 0.1
        return min(0.28, boost)

    def _matched_terms(self, profile: Dict[str, Any], text: str) -> List[str]:
        text_lower = text.lower()
        terms = [*profile["expanded_terms"], *profile["context_terms"]]
        return sorted({term for term in terms if str(term).lower() in text_lower})

    def _rank_reason(self, profile: Dict[str, Any], source_type: str, text: str) -> str:
        matched = self._matched_terms(profile, text)
        if matched:
            return f"{source_type} 결과가 검색 의도와 제품 문맥({', '.join(matched[:4])})에 일치"
        return f"{source_type} 결과가 검색 의도({', '.join(profile['intents'])})와 의미적으로 근접"

    def _diversify_results(self, results: Sequence[AISearchResultItem], top_k: int) -> List[AISearchResultItem]:
        pool = sorted(results, key=lambda item: item.score, reverse=True)
        selected: list[AISearchResultItem] = []
        seen_types: set[str] = set()
        seen_titles: set[str] = set()
        while pool and len(selected) < top_k:
            best_index = 0
            best_score = -1.0
            for index, item in enumerate(pool):
                title_key = f"{item.source_type}:{item.title}".lower()
                if title_key in seen_titles:
                    continue
                diversity_bonus = 0.08 if item.result_type not in seen_types else 0.0
                adjusted = item.score + diversity_bonus - len([x for x in selected if x.source_type == item.source_type]) * 0.025
                if adjusted > best_score:
                    best_score = adjusted
                    best_index = index
            if best_score < 0:
                break
            chosen = pool.pop(best_index)
            seen_types.add(chosen.result_type)
            seen_titles.add(f"{chosen.source_type}:{chosen.title}".lower())
            selected.append(chosen)
        return selected
