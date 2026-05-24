from backend.agents import GrowGateAgent
from backend.schemas import ProductInput


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
