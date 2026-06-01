"""Pydantic schemas for GROW-GATE Local EX demo API."""
from __future__ import annotations

from typing import Any, ClassVar, Dict, List, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ProductInput(BaseModel):
    """기업·제품 입력값.

    실제 서비스에서는 사업자등록 기반 기업 프로필, HS Code 검증 결과,
    인증 보유현황, 원산지·공급망 데이터 등을 추가로 받습니다.
    """

    model_config = ConfigDict(str_strip_whitespace=True)

    ALLOWED_FEATURES: ClassVar[set[str]] = {
        "battery",
        "electronics",
        "industrial",
        "digital",
        "iot",
        "consumer",
        "carbon_material",
    }

    company_name: str = Field(default="경남 배터리테크", min_length=1, max_length=80, description="기업명")
    region: str = Field(default="경남", min_length=1, max_length=40, description="지역/테크노파크 관할")
    industry: str = Field(default="전기전자", min_length=1, max_length=60, description="업종")
    product_name: str = Field(default="리튬이온 배터리팩", min_length=1, max_length=100, description="제품명")
    hs_code: str = Field(default="850760", min_length=4, max_length=10, pattern=r"^[0-9.\-\s]+$", description="HS Code")
    target_market: str = Field(default="Germany / EU", min_length=1, max_length=80, description="대상국 또는 권역")
    features: List[str] = Field(
        default_factory=lambda: ["battery", "electronics", "industrial"],
        min_length=1,
        max_length=7,
        description="제품 특성 태그: battery, electronics, digital, iot, consumer, carbon_material 등",
    )
    annual_export_usd: float = Field(default=0.0, ge=0, description="최근 연간 수출액")
    certifications: List[str] = Field(default_factory=lambda: ["KC", "UN38.3"], max_length=20, description="보유 인증")
    has_bom: bool = Field(default=True, description="BOM/소재 정보 보유 여부")
    has_test_report: bool = Field(default=True, description="시험성적서 보유 여부")
    has_label_draft: bool = Field(default=True, description="라벨/사용설명서 초안 보유 여부")
    notes: Optional[str] = Field(default=None, max_length=1000, description="추가 메모")

    @field_validator("hs_code")
    @classmethod
    def normalize_and_validate_hs_code(cls, value: str) -> str:
        normalized = "".join(ch for ch in value if ch.isdigit())
        if not 4 <= len(normalized) <= 10:
            raise ValueError("HS Code must contain 4 to 10 digits")
        return normalized

    @field_validator("features")
    @classmethod
    def validate_features(cls, values: List[str]) -> List[str]:
        cleaned: list[str] = []
        for value in values:
            feature = str(value).strip().lower()
            if feature not in cls.ALLOWED_FEATURES:
                raise ValueError(f"unsupported product feature: {feature}")
            if feature not in cleaned:
                cleaned.append(feature)
        return cleaned

    @field_validator("certifications")
    @classmethod
    def validate_certifications(cls, values: List[str]) -> List[str]:
        cleaned: list[str] = []
        for value in values:
            certification = str(value).strip()
            if not certification:
                continue
            if len(certification) > 40:
                raise ValueError("certification names must be 40 characters or fewer")
            if certification not in cleaned:
                cleaned.append(certification)
        return cleaned


class GitInitRequest(BaseModel):
    """Local Git repository initialization request.

    The backend never stores GitHub credentials. Push only works when the
    machine already has valid GitHub authentication configured.
    """

    remote_url: str = Field(
        default="https://github.com/jiyongKim615/grow_gate_local_ex.git",
        max_length=240,
        pattern=r"^https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:\.git)?$",
        description="GitHub HTTPS remote URL",
    )
    branch: str = Field(default="main", max_length=80, pattern=r"^[A-Za-z0-9._/-]+$", description="Default branch")


class GitCommitRequest(BaseModel):
    """Local Git commit request."""

    message: str = Field(default="feat: add team operations dashboards", min_length=1, max_length=120)
    include_paths: List[str] = Field(
        default_factory=lambda: [
            ".gitignore",
            "docs/superpowers",
            "grow_gate_local_ex_demo/backend",
            "grow_gate_local_ex_demo/frontend",
            "grow_gate_local_ex_demo/tests",
            "grow_gate_local_ex_demo/README.md",
        ],
        min_length=1,
        max_length=20,
    )

    @field_validator("include_paths")
    @classmethod
    def validate_include_paths(cls, values: List[str]) -> List[str]:
        cleaned: list[str] = []
        for value in values:
            path = str(value).strip().replace("\\", "/")
            if not path or path.startswith("/") or ".." in path.split("/"):
                raise ValueError("include paths must be relative project paths")
            if path not in cleaned:
                cleaned.append(path)
        return cleaned


class GitActionResult(BaseModel):
    ok: bool
    action: str
    command: List[str]
    cwd: str
    stdout: str = ""
    stderr: str = ""
    returncode: int = 0
    hint: Optional[str] = None


class MarketSignal(BaseModel):
    code: str
    name: str
    region: str
    main_port: str
    import_growth_yoy: float
    tariff_mfn: float
    logistics_days_air: str
    logistics_days_sea: str
    route_label: str
    opportunity_score: int
    reason: str


class RegulationItem(BaseModel):
    id: str
    title: str
    scope: str
    priority: Literal["critical", "high", "medium", "low"]
    typical_evidence: List[str]
    action: str
    matched_reason: str


class RecallRisk(BaseModel):
    id: str
    product: str
    hazard: str
    severity: float
    similarity: float
    risk_level: Literal["LOW", "MEDIUM", "HIGH"]
    recommended_tests: List[str]
    design_actions: List[str]


class PatentRisk(BaseModel):
    keyword: str
    korean_keyword: str
    ipc: List[str]
    filings: int
    recent_growth: float
    risk: float
    risk_level: Literal["LOW", "MEDIUM", "HIGH"]
    avoidance_hints: List[str]


class SupportProgram(BaseModel):
    id: str
    name: str
    benefit: str
    priority_rule: str
    roadmap_stage: str
    fit_score: int
    reason: str


class RoadmapItem(BaseModel):
    day_bucket: Literal["30일", "60일", "90일"]
    title: str
    tasks: List[str]
    output: str
    owner: str


class MLFeatureImportance(BaseModel):
    feature: str
    importance: float
    direction: Literal["positive", "negative", "mixed"]


class MLPrediction(BaseModel):
    model_name: str
    model_type: str
    target: str
    value: float
    unit: str
    confidence: float
    interpretation: str
    feature_importance: List[MLFeatureImportance]


class AnalysisResult(BaseModel):
    request: ProductInput
    readiness_score: int
    readiness_level: Literal["LOW", "MEDIUM", "HIGH"]
    score_breakdown: Dict[str, int]
    markets: List[MarketSignal]
    regulations: List[RegulationItem]
    recalls: List[RecallRisk]
    patents: List[PatentRisk]
    support_programs: List[SupportProgram]
    roadmap: List[RoadmapItem]
    ml_predictions: List[MLPrediction] = Field(default_factory=list)
    ai_summary: str
    guardrails: List[str]
    raw_signals: Dict[str, Any] = Field(default_factory=dict)
