from backend.agents import GrowGateAgent
from backend.app import app
from backend.external_llm import ExternalLLMClient, ExternalLLMConfig
from backend.schemas import ProductInput
from fastapi.testclient import TestClient
import httpx
import json
from pathlib import Path


def test_battery_pack_eu_demo():
    agent = GrowGateAgent()
    result = agent.analyze(ProductInput())
    assert result.readiness_score > 0
    assert result.markets
    assert result.regulations
    assert any("UN38.3" in item.title for item in result.regulations)
    assert result.roadmap[0].day_bucket == "30일"
    assert len(result.ml_predictions) == 4
    assert any("리드타임" in item.target for item in result.ml_predictions)


def test_ai_search_uses_product_context_and_returns_evidence_actions():
    client = TestClient(app)
    response = client.post(
        "/ai-search",
        json={
            "query": "미국 전자제품 FCC 인증 근거",
            "scope": "diagnosis",
            "product_input": {
                **ProductInput().model_dump(),
                "target_market": "United States",
                "features": ["electronics", "digital"],
            },
        },
    )
    assert response.status_code == 200
    payload = response.json()
    assert payload["scope"] == "diagnosis"
    assert payload["results"]
    assert payload["evidence_documents"]
    assert payload["suggested_actions"]
    assert any("FCC" in item["title"] or "UL" in item["title"] for item in payload["results"])


def test_ai_search_applies_recommendation_ranking_and_quality_signals():
    client = TestClient(app)
    response = client.post(
        "/ai-search",
        json={
            "query": "EU 배터리 여권 리콜 위험과 다음 추천 액션",
            "scope": "global",
            "product_input": ProductInput().model_dump(),
        },
    )
    assert response.status_code == 200
    payload = response.json()
    quality = payload["quality_signals"]
    assert quality["ranking_strategy"] == "intent_context_hybrid_mmr"
    assert quality["diversified"] is True
    assert {"regulation", "recall"} <= set(quality["detected_intents"])
    assert "battery passport" in quality["expanded_terms"]
    assert payload["results"][0]["metadata"]["rank_reason"]
    assert len({item["result_type"] for item in payload["results"][:4]}) >= 2
    assert any(action["label"] == "제품 지식 보강" for action in payload["suggested_actions"])


def test_agent_builds_sme_target_rag_prompt_and_harness_pack():
    agent = GrowGateAgent()
    result = agent.analyze(
        ProductInput(
            company_name="Pmgrow Corporation",
            region="경기",
            industry="배터리 서비스 플랫폼",
            product_name="전기차 배터리팩 진단·ESS 솔루션",
            hs_code="850760",
            target_market="Germany / EU",
            features=["battery", "electronics", "industrial"],
            annual_export_usd=120000,
            certifications=["KC", "UN38.3"],
        )
    )

    profile = result.raw_signals["target_company_profile"]
    rag = result.raw_signals["target_company_rag"]
    prompt_pack = result.raw_signals["advanced_prompt_pack"]
    harness = result.raw_signals["harness_report"]

    assert profile["company_name"] == "Pmgrow Corporation"
    assert profile["company_scale"] == "SME"
    assert rag["query"]["company_name"] == "Pmgrow Corporation"
    assert len(rag["documents"]) >= 5
    assert any(doc["source_type"] == "target_company_seed" for doc in rag["documents"])
    assert any("SME" in message["content"] or "중소기업" in message["content"] for message in prompt_pack["messages"])
    assert prompt_pack["output_contract"]["must_include"] == [
        "evidence_citations",
        "uncertainty_flags",
        "30_60_90_day_actions",
        "sme_support_matches",
    ]
    assert harness["overall_status"] in {"pass", "review"}
    assert harness["overall_score"] >= 80
    assert {check["id"] for check in harness["checks"]} >= {
        "evidence_grounding",
        "prompt_injection_guardrail",
        "sme_alignment",
        "source_traceability",
    }


def test_target_company_context_endpoint_exposes_rag_prompt_harness():
    client = TestClient(app)
    response = client.get("/target-company/context")
    assert response.status_code == 200
    payload = response.json()
    assert payload["profile"]["company_scale"] == "SME"
    assert payload["rag"]["documents"]
    assert payload["prompt_pack"]["prompt_version"] == "sme-rag-harness-v1"
    assert payload["harness_report"]["overall_score"] >= 80


def test_ai_diagnosis_tab_uses_workspace3_step_ui():
    html = (Path(__file__).resolve().parents[1] / "frontend" / "workspace.html").read_text(encoding="utf-8")
    assert "GROW-GATE Local EX · AI 진단" in html
    assert 'id="stageWrap"' in html
    assert "const STEPS=[" in html
    assert "S04 READINESS SCORING LAB" in html
    assert "S11 PUBLIC DATA PIPELINE" in html
    assert 'href="index.html"' in html
    assert 'href="roadmap.html"' in html


def test_ai_diagnosis_login_and_agent_first_flow_copy():
    html = (Path(__file__).resolve().parents[1] / "frontend" / "workspace.html").read_text(encoding="utf-8")
    assert 'id="loginScreen"' in html
    assert 'id="loginForm"' in html
    assert "function handleLogin" in html
    assert "이 단계의 데이터 정리와 진단 초안은 AI Agent 기반으로 생성됩니다" in html
    assert "데이터 호출" not in html
    assert "{no:'01',nm:'기업·제품 입력',aux:'Input'}" in html
    assert "{no:'02',nm:'AI 예비진단',aux:'Agent Brief'}" in html
    assert "{no:'03',nm:'근거 데이터 연계',aux:'Evidence'}" in html


def test_external_llm_disabled_returns_deterministic_fallback():
    client = ExternalLLMClient(config=ExternalLLMConfig(provider="disabled"))
    result = client.generate({"messages": []}, "규칙 기반 요약")
    assert result["status"] == "disabled"
    assert result["used_fallback"] is True
    assert result["summary"] == "규칙 기반 요약"


def test_external_llm_hf_space_parses_summary_without_network():
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/agent"
        return httpx.Response(200, json={"summary": "외부 오픈소스 모델 기반 추론 요약"})

    transport = httpx.MockTransport(handler)
    client = ExternalLLMClient(
        config=ExternalLLMConfig(provider="hf_space", hf_space_url="https://example.test/agent"),
        transport=transport,
    )
    result = client.generate({"messages": [{"role": "user", "content": "진단해줘"}]}, "fallback")
    assert result["status"] == "ok"
    assert result["provider"] == "hf_space"
    assert result["used_fallback"] is False
    assert result["summary"] == "외부 오픈소스 모델 기반 추론 요약"


def test_external_llm_hf_space_parses_nested_gradio_style_data():
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["content-type"] == "application/json"
        body = json.loads(request.content)
        assert body["task"] == "grow_gate_export_readiness_summary"
        assert "OUTPUT CONTRACT" in body["prompt"]
        return httpx.Response(200, json={"data": [{"generated_text": "ZeroGPU Space 기반 컨설팅 요약"}]})

    transport = httpx.MockTransport(handler)
    client = ExternalLLMClient(
        config=ExternalLLMConfig(provider="hf_space", hf_space_url="https://example.test/run/predict"),
        transport=transport,
    )
    result = client.generate(
        {
            "messages": [{"role": "user", "content": "EU 배터리 인증 위험을 요약해줘"}],
            "output_contract": {"format": "consulting_brief"},
        },
        "fallback",
    )
    assert result["status"] == "ok"
    assert result["summary"] == "ZeroGPU Space 기반 컨설팅 요약"


def test_external_llm_http_error_falls_back_without_breaking_demo():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(503, json={"error": "cold start queue"})

    transport = httpx.MockTransport(handler)
    client = ExternalLLMClient(
        config=ExternalLLMConfig(provider="hf_space", hf_space_url="https://example.test/agent"),
        transport=transport,
    )
    result = client.generate({"messages": [{"role": "user", "content": "진단"}]}, "기본 규칙 기반 요약")
    assert result["provider"] == "hf_space"
    assert result["status"] == "fallback"
    assert result["used_fallback"] is True
    assert result["summary"] == "기본 규칙 기반 요약"
    assert "error" in result
    assert "latency_ms" in result


def test_external_llm_unsupported_provider_uses_fallback_summary():
    client = ExternalLLMClient(config=ExternalLLMConfig(provider="unknown_free_gpu"))
    result = client.generate({"messages": [{"role": "user", "content": "진단"}]}, "fallback summary")
    assert result["provider"] == "unknown_free_gpu"
    assert result["status"] == "unsupported_provider"
    assert result["used_fallback"] is True
    assert result["summary"] == "fallback summary"


def test_external_llm_groq_uses_openai_compatible_chat_contract():
    def handler(request: httpx.Request) -> httpx.Response:
        assert str(request.url) == "https://api.groq.com/openai/v1/chat/completions"
        assert request.headers["authorization"] == "Bearer test-groq-key"
        body = json.loads(request.content)
        assert body["model"] == "llama-test"
        assert body["temperature"] == 0.2
        assert body["messages"] == [
            {"role": "system", "content": "중소기업 수출 컨설턴트"},
            {"role": "user", "content": "개발자 정책"},
            {"role": "user", "content": "사용자 질문"},
        ]
        return httpx.Response(200, json={"choices": [{"message": {"content": "Groq 오픈모델 추론 요약"}}]})

    transport = httpx.MockTransport(handler)
    client = ExternalLLMClient(
        config=ExternalLLMConfig(provider="groq", groq_api_key="test-groq-key", groq_model="llama-test"),
        transport=transport,
    )
    result = client.generate(
        {
            "messages": [
                {"role": "system", "content": "중소기업 수출 컨설턴트"},
                {"role": "developer", "content": "개발자 정책"},
                {"role": "user", "content": "사용자 질문"},
            ]
        },
        "fallback",
    )
    assert result["status"] == "ok"
    assert result["provider"] == "groq"
    assert result["model"] == "llama-test"
    assert result["summary"] == "Groq 오픈모델 추론 요약"


def test_external_llm_gemini_parses_candidate_text():
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.host == "generativelanguage.googleapis.com"
        assert request.url.params["key"] == "test-gemini-key"
        body = json.loads(request.content)
        assert body["generationConfig"]["temperature"] == 0.2
        assert "GROW-GATE" in body["contents"][0]["parts"][0]["text"]
        return httpx.Response(
            200,
            json={"candidates": [{"content": {"parts": [{"text": "Gemini 무료 티어 추론 요약"}]}}]},
        )

    transport = httpx.MockTransport(handler)
    client = ExternalLLMClient(
        config=ExternalLLMConfig(provider="gemini", gemini_api_key="test-gemini-key"),
        transport=transport,
    )
    result = client.generate({"messages": []}, "fallback")
    assert result["status"] == "ok"
    assert result["provider"] == "gemini"
    assert result["summary"] == "Gemini 무료 티어 추론 요약"


def test_agent_uses_external_llm_summary_when_provider_succeeds():
    class StubLLM:
        def generate(self, prompt_pack, fallback_summary):
            assert prompt_pack["output_contract"]["format"] == "consulting_brief"
            assert fallback_summary
            return {
                "provider": "hf_space",
                "status": "ok",
                "used_fallback": False,
                "summary": "외부 오픈소스 Agent가 생성한 수출 실행 요약",
                "model": "https://example.test/agent",
                "latency_ms": 12,
            }

    agent = GrowGateAgent()
    agent.llm = StubLLM()
    result = agent.analyze(ProductInput())
    inference = result.raw_signals["llm_inference"]
    assert result.ai_summary == "외부 오픈소스 Agent가 생성한 수출 실행 요약"
    assert inference["provider"] == "hf_space"
    assert inference["used_fallback"] is False


def test_agent_records_external_llm_status_when_disabled():
    result = GrowGateAgent().analyze(ProductInput())
    inference = result.raw_signals["llm_inference"]
    assert inference["provider"] == "disabled"
    assert inference["used_fallback"] is True
    assert inference["summary"] == result.ai_summary


def test_analyze_endpoint_exposes_llm_inference_audit_signal():
    client = TestClient(app)
    response = client.post("/analyze", json=ProductInput().model_dump())
    assert response.status_code == 200
    payload = response.json()
    inference = payload["raw_signals"]["llm_inference"]
    assert inference["provider"] == "disabled"
    assert inference["used_fallback"] is True
    assert inference["summary"] == payload["ai_summary"]


def test_product_knowledge_api_upserts_and_returns_rag_feature_preview(tmp_path, monkeypatch):
    from backend.app import product_knowledge_store
    from backend.product_knowledge import ProductKnowledgeStore
    import backend.app as app_module

    store = ProductKnowledgeStore(tmp_path / "product_knowledge.json")
    monkeypatch.setattr(app_module, "product_knowledge_store", store)

    client = TestClient(app)
    payload = {
        "company_name": "경남 배터리테크",
        "product_name": "리튬이온 배터리팩",
        "hs_code": "8507.60",
        "target_market": "Germany / EU",
        "product_family": "industrial battery pack",
        "features": ["battery", "electronics"],
        "certifications": ["UN38.3", "KC"],
        "materials": ["NCM cell", "BMS", "aluminum case"],
        "buyer_requirements": ["EU battery passport", "carbon footprint"],
        "recall_keywords": ["thermal runaway", "short circuit"],
        "fto_keywords": ["battery pack cooling", "BMS balancing"],
        "documents": [
            {
                "title": "UN38.3 시험성적서",
                "source_url": "https://example.test/un38",
                "document_type": "test_report",
                "summary": "운송 안전 시험 근거",
                "reliability": "high",
            }
        ],
        "owner": "품질팀",
        "status": "active",
    }

    create_response = client.post("/product-knowledge", json=payload)
    assert create_response.status_code == 200
    created = create_response.json()
    assert created["product_id"]
    assert created["version"] == 1
    assert created["feature_preview"]["feature_row"]["battery_flag"] == 1
    assert created["rag_pack"]["documents"][0]["title"] == "UN38.3 시험성적서"

    update_response = client.post(
        "/product-knowledge",
        json={**payload, "certifications": ["UN38.3", "KC", "IEC 62133"], "owner": "인증팀"},
    )
    assert update_response.status_code == 200
    updated = update_response.json()
    assert updated["product_id"] == created["product_id"]
    assert updated["version"] == 2
    assert updated["certifications"] == ["UN38.3", "KC", "IEC 62133"]
    assert updated["feature_preview"]["signals"]["certification_count"] == 3

    list_response = client.get("/product-knowledge")
    assert list_response.status_code == 200
    items = list_response.json()["items"]
    assert len(items) == 1
    assert items[0]["owner"] == "인증팀"

    monkeypatch.setattr(app_module, "product_knowledge_store", product_knowledge_store)


def test_agent_enriches_analysis_with_registered_product_knowledge(tmp_path, monkeypatch):
    from backend.product_knowledge import ProductKnowledgeStore
    import backend.agents as agents_module

    store = ProductKnowledgeStore(tmp_path / "product_knowledge.json")
    store.upsert(
        {
            "company_name": "경남 배터리테크",
            "product_name": "리튬이온 배터리팩",
            "hs_code": "850760",
            "target_market": "Germany / EU",
            "product_family": "industrial battery pack",
            "features": ["battery", "electronics", "industrial"],
            "certifications": ["UN38.3"],
            "materials": ["BMS", "cell holder"],
            "buyer_requirements": ["EU battery passport"],
            "recall_keywords": ["overheating"],
            "fto_keywords": ["pack cooling"],
            "documents": [
                {
                    "title": "BOM 및 소재정보",
                    "source_url": "https://example.test/bom",
                    "document_type": "bom",
                    "summary": "셀, BMS, 케이스 구성",
                    "reliability": "medium",
                }
            ],
            "owner": "제품팀",
            "status": "active",
        }
    )
    monkeypatch.setattr(agents_module, "ProductKnowledgeStore", lambda: store)

    result = agents_module.GrowGateAgent().analyze(ProductInput())
    knowledge = result.raw_signals["product_knowledge"]
    rag_pack = result.raw_signals["product_rag_pack"]
    feature_enrichment = result.raw_signals["ml_feature_enrichment"]

    assert knowledge["matched"] is True
    assert knowledge["record"]["owner"] == "제품팀"
    assert rag_pack["documents"][0]["document_type"] == "bom"
    assert feature_enrichment["feature_row"]["registered_knowledge_flag"] == 1
    assert "AI Agent/RAG/ML 사전 지식" in feature_enrichment["explanation"]


def test_product_knowledge_frontend_page_is_registered_in_navigation():
    root = Path(__file__).resolve().parents[1] / "frontend"
    html = (root / "product-knowledge.html").read_text(encoding="utf-8")
    index = (root / "index.html").read_text(encoding="utf-8")
    script = (root / "product-knowledge.js").read_text(encoding="utf-8")

    assert "제품 지식 관리" in html
    assert 'id="productKnowledgeForm"' in html
    assert 'id="knowledgeList"' in html
    assert "AI Agent와 ML 모델의 사전 지식" in html
    assert 'href="product-knowledge.html"' in index
    assert "fetch(`${API_BASE}/product-knowledge`" in script
    assert "renderFeaturePreview" in script


def test_rag_database_schema_exposes_supabase_pgvector_tables():
    client = TestClient(app)
    response = client.get("/rag/schema")
    assert response.status_code == 200
    payload = response.json()
    assert payload["storage_mode"] == "json_fallback_supabase_ready"
    assert payload["vector_extension"] == "pgvector"
    assert {table["name"] for table in payload["tables"]} >= {
        "rag_documents",
        "rag_chunks",
        "rag_embeddings",
        "agent_outputs",
        "eval_cases",
    }


def test_ml_train_runs_airflow_ready_pipeline_and_records_history(tmp_path, monkeypatch):
    from backend.operational_pipeline import PipelineRunStore
    import backend.app as app_module

    store = PipelineRunStore(tmp_path / "pipeline_runs.json")
    monkeypatch.setattr(app_module, "pipeline_run_store", store)

    client = TestClient(app)
    train_response = client.post("/ml/train")
    assert train_response.status_code == 200
    train_payload = train_response.json()
    assert train_payload["status"] == "success"
    assert train_payload["run_type"] == "ml_train"
    assert [step["task_id"] for step in train_payload["steps"]] == [
        "collect_public_sources",
        "validate_raw_data",
        "normalize_documents",
        "build_rag_chunks",
        "build_ml_features",
        "train_xgboost",
        "evaluate_model",
        "publish_model_artifacts",
    ]
    assert train_payload["artifacts"]["rag_database"]["tables"]["rag_chunks"] > 0
    assert train_payload["artifacts"]["model_version"]["model_version_id"].startswith("model-")
    assert train_payload["artifacts"]["evaluation"]["overall_status"] in {"pass", "review"}

    runs_response = client.get("/pipeline/runs")
    assert runs_response.status_code == 200
    assert runs_response.json()["total"] == 1

    status_response = client.get("/pipeline/latest-status")
    assert status_response.status_code == 200
    latest = status_response.json()
    assert latest["latest_run"]["run_id"] == train_payload["run_id"]
    assert latest["airflow_ready"] is True


def test_frontend_copy_uses_korean_safe_wrapping_and_pipeline_controls():
    root = Path(__file__).resolve().parents[1] / "frontend"
    workspace = (root / "workspace.html").read_text(encoding="utf-8")
    styles = (root / "styles.css").read_text(encoding="utf-8")
    data_html = (root / "data.html").read_text(encoding="utf-8")
    app_js = (root / "app.js").read_text(encoding="utf-8")

    assert "판단 기준이 됩니다." in workspace
    assert "한글 줄바꿈 보호" in workspace
    assert "word-break: keep-all" in workspace
    assert "overflow-wrap: break-word" in workspace
    assert "korean-copy-safe" in styles
    assert "overflow-wrap: anywhere" not in styles[styles.index(".korean-copy-safe") : styles.index(".technical-wrap")]
    assert "ML 재학습 실행" in data_html
    assert "pipelineRunHistory" in data_html
    assert "데모 DB 시드 생성" in data_html
    assert "실사용 시뮬레이션 실행" in data_html
    assert "demoSimulationReport" in data_html
    assert "fetch(`${API_BASE}/ml/train`" in app_js
    assert "fetch(`${API_BASE}/db/seed`" in app_js
    assert "fetch(`${API_BASE}/simulation/run`" in app_js
    assert "renderPipelineRunHistory" in app_js


def test_local_demo_database_seeds_real_product_rag_and_pipeline_data(tmp_path, monkeypatch):
    from backend.local_demo_db import LocalDemoDatabase
    import backend.app as app_module

    db = LocalDemoDatabase(tmp_path / "grow_gate_demo.db")
    monkeypatch.setattr(app_module, "demo_database", db)

    client = TestClient(app)
    seed_response = client.post("/db/seed")
    assert seed_response.status_code == 200
    seeded = seed_response.json()
    assert seeded["status"] == "seeded"
    assert seeded["counts"]["product_knowledge"] >= 1
    assert seeded["counts"]["rag_documents"] >= 5
    assert seeded["counts"]["rag_chunks"] >= 5
    assert seeded["counts"]["pipeline_runs"] >= 1
    assert seeded["counts"]["model_versions"] >= 1

    status_response = client.get("/db/status")
    assert status_response.status_code == 200
    status = status_response.json()
    assert status["database_path"].endswith("grow_gate_demo.db")
    assert status["counts"] == seeded["counts"]


def test_simulation_run_generates_report_review_and_persists_result(tmp_path, monkeypatch):
    from backend.local_demo_db import LocalDemoDatabase
    import backend.app as app_module

    db = LocalDemoDatabase(tmp_path / "grow_gate_demo.db")
    monkeypatch.setattr(app_module, "demo_database", db)

    client = TestClient(app)
    response = client.post("/simulation/run")
    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "success"
    assert payload["flow"] == [
        "seed_database",
        "register_product_knowledge",
        "run_ai_analysis",
        "run_ai_search",
        "build_report",
        "review_report_quality",
        "persist_simulation",
    ]
    assert payload["analysis"]["readiness_score"] > 0
    assert payload["search"]["quality_signals"]["ranking_strategy"] == "intent_context_hybrid_mmr"
    assert payload["report_review"]["overall_status"] in {"pass", "review"}
    assert payload["report_review"]["checks"]["has_ai_summary"] is True
    assert payload["report_review"]["checks"]["has_rag_evidence"] is True
    assert payload["report_review"]["checks"]["has_roadmap"] is True
    assert payload["db_counts_after"]["analysis_reports"] >= 1
    assert "GROW-GATE Local EX 수출 준비도 리포트" in payload["report_markdown"]
