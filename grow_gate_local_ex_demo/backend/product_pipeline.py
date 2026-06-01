"""Product registration, analysis-run storage, and update impact pipeline."""
from __future__ import annotations

from datetime import datetime, timezone
from typing import Dict, List

from .agents import GrowGateAgent
from .data_connectors import PublicDataConnector, normalize_hs_code
from .schemas import (
    AffectedProduct,
    AnalysisRunRecord,
    ProductDocumentFlags,
    ProductInput,
    ProductRecord,
    ProductRegistrationRequest,
    UpdateImpactReport,
    UpdateImpactRequest,
)


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


class ProductAnalysisStore:
    """In-process serving store for the local demo backend.

    The object models the production pipeline contract: product master rows,
    document readiness rows, analysis run rows, and update-impact rows. Swapping
    the dictionaries for SQL tables does not change the API shape.
    """

    def __init__(self, agent: GrowGateAgent | None = None) -> None:
        self.agent = agent or GrowGateAgent()
        self.products: Dict[str, ProductRecord] = {}
        self.runs: Dict[str, AnalysisRunRecord] = {}
        self.product_runs: Dict[str, List[str]] = {}
        self.update_events: Dict[str, UpdateImpactReport] = {}
        self.connector = PublicDataConnector()

    def register_product(self, payload: ProductRegistrationRequest) -> ProductRecord:
        product_id = self._next_id("P", len(self.products) + 1)
        documents = payload.documents or self._documents_from_product(payload)
        if payload.product_name and payload.hs_code and not documents.has_product_spec:
            documents = documents.model_copy(update={"has_product_spec": True})
        now = utc_now()
        record = ProductRecord(
            product_id=product_id,
            input=ProductInput(**payload.model_dump(exclude={"documents"})),
            documents=documents,
            document_readiness_score=self._document_score(documents),
            created_at=now,
            updated_at=now,
        )
        self.products[product_id] = record
        self.product_runs.setdefault(product_id, [])
        return record

    def run_analysis(self, product_id: str, analysis_mode: str = "full") -> AnalysisRunRecord:
        product = self.products[product_id]
        result = self.agent.analyze(product.input)
        run_id = self._next_id("R", len(self.runs) + 1)
        record = AnalysisRunRecord(
            run_id=run_id,
            product_id=product_id,
            analysis_mode=analysis_mode,
            status="completed",
            result=result,
            created_at=utc_now(),
        )
        self.runs[run_id] = record
        self.product_runs.setdefault(product_id, []).append(run_id)
        return record

    def get_run(self, run_id: str) -> AnalysisRunRecord:
        return self.runs[run_id]

    def get_product(self, product_id: str) -> ProductRecord:
        return self.products[product_id]

    def impact_report(self, request: UpdateImpactRequest) -> UpdateImpactReport:
        affected = [self._affected_row(product, request) for product in self.products.values()]
        affected = [item for item in affected if item is not None]
        event_id = self._next_id("U", len(self.update_events) + 1)
        report = UpdateImpactReport(
            event_id=event_id,
            changed_source_types=request.changed_source_types,
            affected_products=affected,
            recommended_action="reanalyze" if affected else "monitor",
            created_at=utc_now(),
        )
        self.update_events[event_id] = report
        return report

    def _affected_row(self, product: ProductRecord, request: UpdateImpactRequest) -> AffectedProduct | None:
        reasons: List[str] = []
        input_row = product.input
        if request.country:
            target_region = self.connector.fetch_target_market(input_row.target_market).get("region", input_row.target_market)
            country = request.country.lower()
            if country in input_row.target_market.lower() or country == str(target_region).lower():
                reasons.append(f"target market matches {request.country}")
        if request.hs_code_prefix and normalize_hs_code(input_row.hs_code).startswith(request.hs_code_prefix):
            reasons.append(f"HS Code prefix {request.hs_code_prefix}")
        feature_overlap = sorted({f.lower() for f in input_row.features} & {f.lower() for f in request.features})
        if feature_overlap:
            reasons.append("feature overlap " + ", ".join(feature_overlap))
        if not reasons:
            return None
        runs = self.product_runs.get(product.product_id, [])
        return AffectedProduct(
            product_id=product.product_id,
            product_name=input_row.product_name,
            target_market=input_row.target_market,
            matched_reasons=reasons,
            last_run_id=runs[-1] if runs else None,
        )

    def _documents_from_product(self, payload: ProductRegistrationRequest) -> ProductDocumentFlags:
        return ProductDocumentFlags(
            has_bom=payload.has_bom,
            has_test_report=payload.has_test_report,
            has_label_manual=payload.has_label_draft,
            has_msds=False,
            has_product_spec=bool(payload.product_name and payload.hs_code),
        )

    def _document_score(self, documents: ProductDocumentFlags) -> int:
        values = [
            documents.has_bom,
            documents.has_test_report,
            documents.has_label_manual,
            documents.has_msds,
            documents.has_product_spec,
        ]
        return round(sum(1 for item in values if item) / len(values) * 100)

    def _next_id(self, prefix: str, sequence: int) -> str:
        date_code = datetime.now(timezone.utc).strftime("%Y%m%d")
        return f"{prefix}{date_code}{sequence:04d}"
