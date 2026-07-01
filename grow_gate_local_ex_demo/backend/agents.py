"""AI Agent orchestration for GROW-GATE Local EX.

The demo uses deterministic agents so it can be evaluated offline. Each agent has
one clear responsibility and produces auditable signals. In production, the
ReportAgent and RegulationAgent can be upgraded with RAG + LLM while keeping the
same API response schema.
"""
from __future__ import annotations

from typing import Any, Dict, List

from .data_connectors import PublicDataConnector, load_public_data_sources
from .external_llm import ExternalLLMClient
from .ml_models import (
    build_ml_feature_vector,
    readiness_level,
    run_xgboost_predictions,
    score_market,
    score_patent_risks,
    score_readiness,
    score_recall_risks,
    score_support_programs,
    select_regulations,
)
from .product_knowledge import ProductKnowledgeStore
from .schemas import (
    AnalysisResult,
    MarketSignal,
    PatentRisk,
    ProductInput,
    RecallRisk,
    RegulationItem,
    RoadmapItem,
    SupportProgram,
)
from .target_company import TargetCompanyEnhancement


class PlannerAgent:
    """Decides which analyses are required for the submitted product."""

    def plan(self, request: ProductInput) -> List[str]:
        tasks = ["market", "regulation", "recall", "patent", "support", "roadmap", "report"]
        if "battery" in {f.lower() for f in request.features} or request.hs_code.startswith("8507"):
            tasks.insert(2, "battery_passport")
        if any(f.lower() in {"digital", "iot", "software"} for f in request.features):
            tasks.insert(2, "cyber_readiness")
        if "carbon_material" in {f.lower() for f in request.features}:
            tasks.insert(2, "carbon_readiness")
        return tasks


class DataConnectorAgent:
    """Calls the data connector facade and creates the input evidence pack."""

    def __init__(self, connector: PublicDataConnector | None = None) -> None:
        self.connector = connector or PublicDataConnector()

    def collect(self, request: ProductInput) -> Dict[str, Any]:
        target = self.connector.fetch_target_market(request.target_market)
        region_profile = self.connector.fetch_region_profile(request.region)
        return {
            "target_market": target,
            "target_region": target.get("region", "EU"),
            "region_profile": region_profile,
            "markets": self.connector.fetch_markets(),
            "regulations": self.connector.fetch_regulations(),
            "recalls": self.connector.fetch_recalls(),
            "patents": self.connector.fetch_patents(),
            "support_programs": self.connector.fetch_support_programs(),
        }


class MarketScoringAgent:
    def run(self, request: ProductInput, evidence: Dict[str, Any]) -> List[MarketSignal]:
        region_weight = float(evidence.get("region_profile", {}).get("weight", 1.0))
        preferred_region = evidence.get("target_market", {}).get("name", request.target_market)
        signals: List[MarketSignal] = []
        for market in evidence["markets"]:
            score, reason = score_market(market, request.features, region_weight, preferred_region)
            signals.append(
                MarketSignal(
                    code=market["code"],
                    name=market["name"],
                    region=market["region"],
                    main_port=market["main_port"],
                    import_growth_yoy=market["import_growth_yoy"],
                    tariff_mfn=market["tariff_mfn"],
                    logistics_days_air=market["logistics_days_air"],
                    logistics_days_sea=market["logistics_days_sea"],
                    route_label=market["route_label"],
                    opportunity_score=score,
                    reason=reason,
                )
            )
        signals.sort(key=lambda item: item.opportunity_score, reverse=True)
        return signals[:5]


class RegulationAgent:
    def run(self, request: ProductInput, evidence: Dict[str, Any]) -> List[RegulationItem]:
        selected = select_regulations(
            evidence["regulations"],
            evidence["target_region"],
            request.hs_code,
            request.features,
        )
        return [RegulationItem(**item) for item in selected[:8]]


class RecallAgent:
    def run(self, request: ProductInput, evidence: Dict[str, Any]) -> List[RecallRisk]:
        selected = score_recall_risks(evidence["recalls"], request.product_name, request.features, top_n=3)
        return [
            RecallRisk(
                id=item["id"],
                product=item["product"],
                hazard=item["hazard"],
                severity=item["severity"],
                similarity=item["similarity"],
                risk_level=item["risk_level"],
                recommended_tests=item["recommended_tests"],
                design_actions=item["design_actions"],
            )
            for item in selected
        ]


class FTORiskAgent:
    def run(self, request: ProductInput, evidence: Dict[str, Any]) -> List[PatentRisk]:
        selected = score_patent_risks(evidence["patents"], request.product_name, request.features, top_n=3)
        return [
            PatentRisk(
                keyword=item["keyword"],
                korean_keyword=item["korean_keyword"],
                ipc=item["ipc"],
                filings=item["filings"],
                recent_growth=item["recent_growth"],
                risk=item["risk_score"],
                risk_level=item["risk_level"],
                avoidance_hints=item["avoidance_hints"],
            )
            for item in selected
        ]


class SupportMatchingAgent:
    def run(
        self,
        request: ProductInput,
        evidence: Dict[str, Any],
        regulations: List[RegulationItem],
        recalls: List[RecallRisk],
        patents: List[PatentRisk],
    ) -> List[SupportProgram]:
        selected = score_support_programs(
            evidence["support_programs"],
            request.features,
            [item.model_dump() for item in regulations],
            [item.model_dump() for item in recalls],
            [item.model_dump() for item in patents],
            top_n=5,
        )
        return [SupportProgram(**item) for item in selected]


class ReadinessEstimationAgent:
    """Runs defensible readiness estimation models on top of normalized signals."""

    def run(
        self,
        request: ProductInput,
        target_region: str,
        markets: List[MarketSignal],
        regulations: List[RegulationItem],
        recalls: List[RecallRisk],
        patents: List[PatentRisk],
        support_programs: List[SupportProgram],
    ) -> List[Dict[str, Any]]:
        feature_row = build_ml_feature_vector(
            [market.opportunity_score for market in markets],
            [item.model_dump() for item in regulations],
            [item.model_dump() for item in recalls],
            [item.model_dump() for item in patents],
            [item.model_dump() for item in support_programs],
            request.features,
            target_region,
            request.has_bom,
            request.has_test_report,
            request.has_label_draft,
        )
        return run_xgboost_predictions(feature_row)


class RoadmapAgent:
    def run(
        self,
        request: ProductInput,
        regulations: List[RegulationItem],
        recalls: List[RecallRisk],
        patents: List[PatentRisk],
        support_programs: List[SupportProgram],
    ) -> List[RoadmapItem]:
        critical_regs = [r for r in regulations if r.priority in {"critical", "high"}]
        first_tests = []
        for recall in recalls:
            for test in recall.recommended_tests:
                if test not in first_tests:
                    first_tests.append(test)
        first_supports = [p.name for p in support_programs if p.roadmap_stage == "30days"][:2]
        patent_hints = []
        for patent in patents:
            for hint in patent.avoidance_hints:
                if hint not in patent_hints:
                    patent_hints.append(hint)

        reg_titles = [r.title for r in critical_regs[:3]] or [r.title for r in regulations[:3]]
        return [
            RoadmapItem(
                day_bucket="30일",
                title="제품·규제 갭 진단과 지원사업 선신청",
                tasks=[
                    f"{request.product_name} 사양서, BOM, HS Code 근거자료를 한 폴더로 정리",
                    "우선 인증 후보: " + ", ".join(reg_titles),
                    "시험 전 체크리스트: " + ", ".join(first_tests[:4]),
                    "추천 지원사업 신청 검토: " + ", ".join(first_supports or [support_programs[0].name if support_programs else "지역 수출지원사업"]),
                ],
                output="수출 준비도 진단서 v1, 인증·시험 견적요청서, 지원사업 신청 초안",
                owner="품질·인증 담당자 / 해외영업 담당자",
            ),
            RoadmapItem(
                day_bucket="60일",
                title="시험·인증 실행과 바이어 검증",
                tasks=[
                    "시험기관 상담 후 필수 시험·선택 시험·샘플 수량을 확정",
                    "대상국 라벨, 사용설명서, DoC/기술문서 초안을 작성",
                    "KOTRA·현지 네트워크로 바이어 요구 인증과 포장 조건을 확인",
                    "특허/FTO 1차 검토: " + ", ".join(patent_hints[:3] or ["선행기술 키워드 확장"]),
                ],
                output="시험 신청서, 기술문서 패키지 초안, 바이어 검증 메모, FTO 키워드 맵",
                owner="인증 담당자 / 기술개발 담당자",
            ),
            RoadmapItem(
                day_bucket="90일",
                title="시장 테스트와 스케일 수출 준비",
                tasks=[
                    "시험 결과 보완사항을 설계·라벨·포장에 반영",
                    "리콜 위험 기반 FMEA와 출하 전 품질 체크리스트를 운영",
                    "수출단가, 물류 리드타임, 통관 서류, A/S 정책을 견적서에 반영",
                    "성공 가능성이 높은 1개 국가부터 파일럿 주문·샘플 발송을 진행",
                ],
                output="수출 실행 패키지, FMEA, 파일럿 바이어 제안서, 2차 지원사업 계획",
                owner="대표이사 / 해외영업 / 품질책임자",
            ),
        ]


class ReportAgent:
    def summarize(
        self,
        request: ProductInput,
        readiness_score: int,
        markets: List[MarketSignal],
        regulations: List[RegulationItem],
        recalls: List[RecallRisk],
        patents: List[PatentRisk],
        support_programs: List[SupportProgram],
    ) -> str:
        top_market = markets[0].name if markets else request.target_market
        top_reg = regulations[0].title if regulations else "대상국 규제 확인 필요"
        top_recall = recalls[0].hazard if recalls else "유사 리콜 위험 낮음"
        top_support = support_programs[0].name if support_programs else "지원사업 확인 필요"
        patent_msg = patents[0].korean_keyword if patents else "선행기술"
        return (
            f"{request.region} 소재 {request.industry} 기업의 '{request.product_name}'은 {top_market} 진출 기준 "
            f"수출 준비도 {readiness_score}/100으로 산정되었습니다. 우선순위는 {top_reg} 검토, "
            f"리콜 위험({top_recall})에 대한 시험 전 점검, {patent_msg} 관련 FTO 키워드 검토, "
            f"그리고 {top_support} 신청입니다. 30일 내 제품자료·시험견적·지원사업 신청을 끝내고, "
            "60일 내 인증 실행과 바이어 요구사항 검증, 90일 내 파일럿 수출 패키지 완성을 권장합니다."
        )


class GuardrailAgent:
    def run(self) -> List[str]:
        return [
            "본 결과는 공공데이터와 데모 규칙 기반의 1차 진단이며, 최종 인증 가능 여부는 시험기관·관세사·변리사 확인이 필요합니다.",
            "HS Code와 제품 사양이 바뀌면 대상 규제와 지원사업 우선순위가 달라질 수 있습니다.",
            "실서비스에서는 데이터 수집·연계 시점, 출처 URL, 문서 버전, 불확실성 점수를 함께 표시해야 합니다.",
        ]


class GrowGateAgent:
    """End-to-end orchestration agent."""

    def __init__(self, connector: PublicDataConnector | None = None) -> None:
        self.planner = PlannerAgent()
        self.data = DataConnectorAgent(connector)
        self.market = MarketScoringAgent()
        self.regulation = RegulationAgent()
        self.recall = RecallAgent()
        self.fto = FTORiskAgent()
        self.support = SupportMatchingAgent()
        self.predict = ReadinessEstimationAgent()
        self.roadmap = RoadmapAgent()
        self.report = ReportAgent()
        self.guardrail = GuardrailAgent()
        self.target_company = TargetCompanyEnhancement()
        self.llm = ExternalLLMClient()
        self.product_knowledge = ProductKnowledgeStore()

    def analyze(self, request: ProductInput) -> AnalysisResult:
        plan = self.planner.plan(request)
        evidence = self.data.collect(request)
        markets = self.market.run(request, evidence)
        regulations = self.regulation.run(request, evidence)
        recalls = self.recall.run(request, evidence)
        patents = self.fto.run(request, evidence)
        support_programs = self.support.run(request, evidence, regulations, recalls, patents)
        ml_predictions = self.predict.run(
            request,
            evidence.get("target_region", "EU"),
            markets,
            regulations,
            recalls,
            patents,
            support_programs,
        )
        roadmap = self.roadmap.run(request, regulations, recalls, patents, support_programs)
        score, breakdown = score_readiness(
            [market.opportunity_score for market in markets],
            [item.model_dump() for item in regulations],
            [item.model_dump() for item in recalls],
            [item.model_dump() for item in patents],
            [item.model_dump() for item in support_programs],
            request.certifications,
            request.has_bom,
            request.has_test_report,
            request.has_label_draft,
        )
        summary = self.report.summarize(request, score, markets, regulations, recalls, patents, support_programs)
        target_company_signals = self.target_company.build(request, regulations, recalls, support_programs)
        product_knowledge_record = self.product_knowledge.find_match(request)
        if product_knowledge_record:
            product_knowledge_signal = {"matched": True, "record": product_knowledge_record}
            product_rag_pack = product_knowledge_record["rag_pack"]
            ml_feature_enrichment = product_knowledge_record["feature_preview"]
        else:
            product_knowledge_signal = {
                "matched": False,
                "record": None,
                "message": "등록된 제품 사전 지식이 없어 입력값과 공공데이터 기반으로 분석했습니다.",
            }
            product_rag_pack = {"documents": [], "knowledge_terms": []}
            ml_feature_enrichment = {
                "feature_row": {"registered_knowledge_flag": 0},
                "signals": {},
                "explanation": "등록된 제품 지식이 없어서 기본 ML feature만 사용했습니다.",
            }
        llm_inference = self.llm.generate(target_company_signals["advanced_prompt_pack"], summary)
        final_summary = llm_inference["summary"]
        return AnalysisResult(
            request=request,
            readiness_score=score,
            readiness_level=readiness_level(score),
            score_breakdown=breakdown,
            markets=markets,
            regulations=regulations,
            recalls=recalls,
            patents=patents,
            support_programs=support_programs,
            roadmap=roadmap,
            ml_predictions=ml_predictions,
            ai_summary=final_summary,
            guardrails=self.guardrail.run(),
            raw_signals={
                "agent_plan": plan,
                "target_region": evidence.get("target_region"),
                "region_profile": evidence.get("region_profile"),
                "public_data_sources": load_public_data_sources()["sources"],
                "screen_data_map": load_public_data_sources()["screen_data_map"],
                **target_company_signals,
                "product_knowledge": product_knowledge_signal,
                "product_rag_pack": product_rag_pack,
                "ml_feature_enrichment": ml_feature_enrichment,
                "llm_inference": llm_inference,
            },
        )
