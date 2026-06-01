from backend.agents import GrowGateAgent
from backend.app import app
from backend.public_data_pipeline import PublicDataPipeline
from backend.schemas import ProductInput
from fastapi.testclient import TestClient


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
    assert result.regulations[0].evidence_documents


def test_product_registration_analysis_run_and_rag_evidence():
    client = TestClient(app)
    payload = {
        "company_name": "경남 배터리테크",
        "region": "경남",
        "industry": "전기전자",
        "product_name": "리튬이온 배터리팩",
        "hs_code": "850760",
        "target_market": "Germany / EU",
        "features": ["battery", "electronics", "industrial"],
        "certifications": ["KC", "UN38.3"],
        "has_bom": True,
        "has_test_report": True,
        "has_label_draft": True,
        "documents": {
            "has_bom": True,
            "has_test_report": True,
            "has_label_manual": True,
            "has_msds": False,
        },
    }

    product_response = client.post("/products", json=payload)
    assert product_response.status_code == 200
    product = product_response.json()
    assert product["product_id"].startswith("P")
    assert product["document_readiness_score"] >= 70

    run_response = client.post(
        "/analysis/run",
        json={"product_id": product["product_id"], "analysis_mode": "full"},
    )
    assert run_response.status_code == 200
    run = run_response.json()
    assert run["run_id"].startswith("R")
    assert run["product_id"] == product["product_id"]
    assert run["result"]["regulations"][0]["evidence_documents"]
    assert run["result"]["raw_signals"]["rag_context"]["regulation_documents"]

    lookup_response = client.get(f"/analysis/{run['run_id']}")
    assert lookup_response.status_code == 200
    assert lookup_response.json()["run_id"] == run["run_id"]


def test_update_impact_pipeline_marks_products_for_reanalysis():
    client = TestClient(app)
    product_response = client.post("/products", json=ProductInput().model_dump())
    product_id = product_response.json()["product_id"]

    impact_response = client.post(
        "/updates/impact",
        json={
            "changed_source_types": ["regulation", "recall"],
            "country": "EU",
            "hs_code_prefix": "8507",
            "features": ["battery"],
        },
    )
    assert impact_response.status_code == 200
    impact = impact_response.json()
    assert any(item["product_id"] == product_id for item in impact["affected_products"])
    assert impact["recommended_action"] == "reanalyze"


def test_xgboost_catalog_is_exposed_to_api_and_pipeline():
    client = TestClient(app)

    catalog_response = client.get("/xgboost-data/catalog")
    assert catalog_response.status_code == 200
    catalog = catalog_response.json()
    assert catalog["metadata"]["total_sources"] == 41
    assert len(catalog["sources"]) == 41
    assert len(catalog["feature_mappings"]) == 5
    assert len(catalog["collection_plan"]) == 8
    assert len(catalog["internal_labels"]) == 7
    assert len(catalog["session_playbooks"]) == 3
    assert catalog["session_playbooks"][0]["session_id"] == "excel-catalog-update"
    assert any(source["catalog_id"] == "E-09" and source["connector"] == "recall" for source in catalog["sources"])

    payload = PublicDataPipeline().run(use_live=False)
    assert len(payload["xgboost_catalog_sources"]) == 41
    assert payload["xgboost_feature_mappings"][0]["model_target"] == "90일 파일럿 수출 실행 가능성"
    assert any(row["source_id"] == "E-04" for row in payload["external_fetch_candidates"])
    assert payload["session_playbooks"][1]["session_id"] == "external-data-collection"


def test_ai_search_returns_contextual_evidence_and_actions():
    client = TestClient(app)
    product_response = client.post("/products", json=ProductInput().model_dump())
    assert product_response.status_code == 200
    product = product_response.json()
    run_response = client.post(
        "/analysis/run",
        json={"product_id": product["product_id"], "analysis_mode": "full"},
    )
    assert run_response.status_code == 200
    run = run_response.json()

    diagnosis_response = client.post(
        "/ai-search",
        json={
            "query": "EU 배터리 여권과 리콜 위험 근거를 보여줘",
            "scope": "diagnosis",
            "product_id": product["product_id"],
            "analysis_run_id": run["run_id"],
        },
    )
    assert diagnosis_response.status_code == 200
    diagnosis = diagnosis_response.json()
    assert diagnosis["scope"] == "diagnosis"
    assert "근거" in diagnosis["answer"]
    assert diagnosis["results"]
    assert diagnosis["evidence_documents"]
    assert diagnosis["suggested_actions"][0]["action_type"] in {"open_evidence", "run_analysis"}
    assert any(item["result_type"] == "analysis_run" for item in diagnosis["results"])

    catalog_response = client.post(
        "/ai-search",
        json={
            "query": "리콜 위험도 XGBoost feature로 쓸 외부 데이터",
            "scope": "data_catalog",
        },
    )
    assert catalog_response.status_code == 200
    catalog = catalog_response.json()
    assert catalog["scope"] == "data_catalog"
    assert catalog["data_sources"]
    assert any(source["source_id"].startswith("E-") for source in catalog["data_sources"])
    assert any(action["action_type"] == "open_data_catalog" for action in catalog["suggested_actions"])
