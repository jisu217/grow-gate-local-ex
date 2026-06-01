"""Contextual AI search over diagnosis, RAG, and data-catalog artifacts."""
from __future__ import annotations

from typing import Any, Dict, List, Sequence

from .data_connectors import PublicDataConnector, load_public_data_sources, load_xgboost_data_catalog
from .ml_models import token_similarity, tokenize
from .product_pipeline import ProductAnalysisStore
from .rag import DemoRAGIndex
from .schemas import (
    AISearchRequest,
    AISearchResponse,
    AISearchResultItem,
    AISuggestedAction,
    AnalysisRunRecord,
    EvidenceDocument,
    ProductInput,
    ProductRecord,
)


class AISearchService:
    """Deterministic search contract that can be upgraded to vector search."""

    def __init__(
        self,
        store: ProductAnalysisStore,
        rag_index: DemoRAGIndex | None = None,
        connector: PublicDataConnector | None = None,
    ) -> None:
        self.store = store
        self.connector = connector or PublicDataConnector()
        self.rag_index = rag_index or DemoRAGIndex(self.connector)

    def search(self, request: AISearchRequest) -> AISearchResponse:
        product = self._resolve_product(request)
        run = self._resolve_run(request)
        product_input = product.input if product else run.result.request if run else request.product_input or ProductInput()
        target_region = self.connector.fetch_target_market(product_input.target_market).get("region", "EU")
        evidence = self.rag_index.search(
            product_input,
            source_types=["regulation", "recall", "patent", "support"],
            query=request.query,
            target_region=target_region,
            top_k=6,
        )
        diagnosis_results = self._diagnosis_results(request.query, product, run, evidence)
        catalog_results, data_sources = self._catalog_results(request.query)

        if request.scope == "diagnosis":
            results = diagnosis_results + catalog_results[:2]
        elif request.scope == "data_catalog":
            results = catalog_results + diagnosis_results[:1]
        else:
            results = diagnosis_results + catalog_results

        results.sort(key=lambda item: item.score, reverse=True)
        results = results[:8]
        data_sources = data_sources[:5] if request.scope in {"data_catalog", "global"} else data_sources[:3]
        return AISearchResponse(
            query=request.query,
            scope=request.scope,
            answer=self._answer(request, product_input, results, evidence, data_sources),
            results=results,
            evidence_documents=evidence[:5],
            data_sources=data_sources,
            suggested_actions=self._actions(request, product, run, results, data_sources),
            guardrails=[
                "검색 답변은 로컬 seed/RAG 근거와 등록된 분석 run을 바탕으로 한 시연용 판단입니다.",
                "인증·통상 법적 최종 판단은 공식 원문과 전문가 검증으로 확정해야 합니다.",
            ],
        )

    def _resolve_product(self, request: AISearchRequest) -> ProductRecord | None:
        if not request.product_id:
            return None
        try:
            return self.store.get_product(request.product_id)
        except KeyError:
            return None

    def _resolve_run(self, request: AISearchRequest) -> AnalysisRunRecord | None:
        if request.analysis_run_id:
            try:
                return self.store.get_run(request.analysis_run_id)
            except KeyError:
                return None
        if request.product_id:
            run_ids = self.store.product_runs.get(request.product_id, [])
            if run_ids:
                return self.store.get_run(run_ids[-1])
        return None

    def _diagnosis_results(
        self,
        query: str,
        product: ProductRecord | None,
        run: AnalysisRunRecord | None,
        evidence: Sequence[EvidenceDocument],
    ) -> List[AISearchResultItem]:
        results: List[AISearchResultItem] = []
        if run:
            result = run.result
            summary = (
                f"준비도 {result.readiness_score}/100, 규제 {len(result.regulations)}건, "
                f"리콜 {len(result.recalls)}건, 지원사업 {len(result.support_programs)}건이 연결되어 있습니다."
            )
            results.append(
                AISearchResultItem(
                    title=f"{result.request.product_name} AI 진단 run {run.run_id}",
                    result_type="analysis_run",
                    source_type="analysis",
                    summary=summary,
                    score=max(0.78, token_similarity(query, f"{result.ai_summary} {summary}")),
                    source_id=run.run_id,
                    metadata={"product_id": run.product_id, "readiness_score": result.readiness_score},
                )
            )
            for item in result.roadmap[:3]:
                text = f"{item.day_bucket} {item.title} {' '.join(item.tasks)} {item.output}"
                results.append(
                    AISearchResultItem(
                        title=f"{item.day_bucket} 실행: {item.title}",
                        result_type="roadmap",
                        source_type="analysis_roadmap",
                        summary=item.output,
                        score=0.45 + token_similarity(query, text) * 0.5,
                        source_id=run.run_id,
                        metadata={"day_bucket": item.day_bucket, "owner": item.owner},
                    )
                )
            for item in result.support_programs[:3]:
                text = f"{item.name} {item.benefit} {item.priority_rule}"
                results.append(
                    AISearchResultItem(
                        title=item.name,
                        result_type="support_program",
                        source_type="support",
                        summary=item.benefit,
                        score=0.42 + token_similarity(query, text) * 0.5,
                        source_id=item.id,
                        metadata={"fit_score": item.fit_score, "roadmap_stage": item.roadmap_stage},
                    )
                )
        elif product:
            text = f"{product.input.product_name} {product.input.hs_code} {product.input.target_market}"
            results.append(
                AISearchResultItem(
                    title=f"{product.input.product_name} 제품 마스터",
                    result_type="product",
                    source_type="product_master",
                    summary=f"HS {product.input.hs_code}, 대상국 {product.input.target_market}, 문서 준비도 {product.document_readiness_score}점",
                    score=0.58 + token_similarity(query, text) * 0.35,
                    source_id=product.product_id,
                    metadata={"document_readiness_score": product.document_readiness_score},
                )
            )

        for doc in evidence:
            results.append(
                AISearchResultItem(
                    title=doc.title,
                    result_type="rag_evidence",
                    source_type=doc.source_type,
                    summary=doc.chunk_text,
                    score=max(0.1, doc.confidence),
                    source_id=doc.doc_id,
                    source_url=doc.source_url,
                    metadata={"matched_terms": doc.matched_terms},
                )
            )
        return results

    def _catalog_results(self, query: str) -> tuple[List[AISearchResultItem], List[Dict[str, Any]]]:
        catalog = load_xgboost_data_catalog()
        public_sources = load_public_data_sources()
        candidates: List[tuple[float, Dict[str, Any]]] = []
        for source in catalog.get("sources", []):
            text = self._source_text(source)
            score = token_similarity(query, text)
            if score <= 0 and not self._has_catalog_keyword_overlap(query, source):
                continue
            candidates.append((min(1.0, 0.32 + score * 0.68), source))

        if not candidates:
            for source in catalog.get("sources", [])[:8]:
                candidates.append((0.22, source))

        candidates.sort(key=lambda row: row[0], reverse=True)
        results: List[AISearchResultItem] = []
        data_sources: List[Dict[str, Any]] = []
        for score, source in candidates[:8]:
            source_id = source.get("catalog_id") or source.get("source_id")
            summary = source.get("use_in_product") or source.get("description") or source.get("feature_candidates", "")
            results.append(
                AISearchResultItem(
                    title=source.get("name", source_id or "data source"),
                    result_type="data_source",
                    source_type=source.get("connector", "catalog"),
                    summary=summary,
                    score=round(score, 3),
                    source_id=source_id,
                    source_url=source.get("official_url"),
                    metadata={
                        "provider": source.get("provider"),
                        "target_table": source.get("target_table"),
                        "update_cycle": source.get("update_cycle"),
                        "priority": source.get("priority"),
                    },
                )
            )
            data_sources.append(source)

        public_catalog = public_sources.get("sources", public_sources if isinstance(public_sources, list) else [])
        for source in public_catalog[:3]:
            text = self._source_text(source)
            if token_similarity(query, text) > 0:
                data_sources.append(source)
        return results, data_sources

    def _source_text(self, source: Dict[str, Any]) -> str:
        values: List[str] = []
        for key in (
            "catalog_id",
            "source_id",
            "name",
            "provider",
            "connector",
            "target_table",
            "use_in_product",
            "description",
            "feature_candidates",
            "model_target",
            "collection_method",
        ):
            value = source.get(key)
            if isinstance(value, list):
                values.extend(str(item) for item in value)
            elif value is not None:
                values.append(str(value))
        return " ".join(values)

    def _has_catalog_keyword_overlap(self, query: str, source: Dict[str, Any]) -> bool:
        query_tokens = set(tokenize(query))
        text_tokens = set(tokenize(self._source_text(source)))
        domain_tokens = {"리콜", "recall", "xgboost", "feature", "공공", "외부", "인증", "support", "trade"}
        return bool((query_tokens & text_tokens) or (query_tokens & domain_tokens))

    def _answer(
        self,
        request: AISearchRequest,
        product: ProductInput,
        results: Sequence[AISearchResultItem],
        evidence: Sequence[EvidenceDocument],
        data_sources: Sequence[Dict[str, Any]],
    ) -> str:
        top_titles = ", ".join(item.title for item in results[:3]) or "검색 결과 없음"
        evidence_count = len(evidence)
        source_count = len(data_sources)
        if request.scope == "data_catalog":
            return (
                f"{product.product_name} 기준 데이터 카탈로그에서 {source_count}개 출처 후보를 찾았습니다. "
                f"상위 후보는 {top_titles}입니다. XGBoost feature 연결성, 수집 방식, 갱신 주기를 함께 검토하세요."
            )
        if request.scope == "global":
            return (
                f"{product.product_name} 관련 진단 run, RAG 근거, 데이터 출처를 통합 검색했습니다. "
                f"근거 {evidence_count}건과 데이터 출처 {source_count}건이 연결되며, 상위 결과는 {top_titles}입니다."
            )
        return (
            f"{product.product_name} AI 진단 문맥에서 근거 {evidence_count}건을 찾았습니다. "
            f"상위 결과는 {top_titles}이며, 규제·리콜·지원사업 근거를 함께 확인하는 흐름이 적합합니다."
        )

    def _actions(
        self,
        request: AISearchRequest,
        product: ProductRecord | None,
        run: AnalysisRunRecord | None,
        results: Sequence[AISearchResultItem],
        data_sources: Sequence[Dict[str, Any]],
    ) -> List[AISuggestedAction]:
        actions: List[AISuggestedAction] = []
        if run or any(item.result_type == "rag_evidence" for item in results):
            actions.append(
                AISuggestedAction(
                    label="근거 데이터 섹션 열기",
                    action_type="open_evidence",
                    payload={"target": "workspace-evidence", "run_id": run.run_id if run else None},
                )
            )
        if product:
            actions.append(
                AISuggestedAction(
                    label="현재 제품 재분석",
                    action_type="run_analysis",
                    payload={"product_id": product.product_id},
                )
            )
        if request.scope in {"data_catalog", "global"} or data_sources:
            actions.append(
                AISuggestedAction(
                    label="데이터 명세 화면에서 출처 검토",
                    action_type="open_data_catalog",
                    payload={"href": "data-catalog.html", "source_ids": [item.get("catalog_id") or item.get("source_id") for item in data_sources[:3]]},
                )
            )
        if any(item.result_type == "roadmap" for item in results):
            actions.append(
                AISuggestedAction(
                    label="실행 로드맵 확인",
                    action_type="open_roadmap",
                    payload={"target": "workspace-report"},
                )
            )
        return actions[:4]
