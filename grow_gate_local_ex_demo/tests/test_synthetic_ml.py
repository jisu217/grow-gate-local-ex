from backend.synthetic_ml import predict_gate_v1
import unittest


class SyntheticMlContractTest(unittest.TestCase):
    def test_predict_gate_v1_returns_synthetic_limited_contract(self):
        result = predict_gate_v1(
            {
                "companyName": "창원에너지팩 주식회사",
                "productName": "산업용 리튬이온 배터리팩 48V 80Ah",
                "hsCode": "850760",
                "targetCountry": "Germany / EU",
                "ruleScore": 65,
            }
        )

        self.assertIs(result["syntheticTraining"], True)
        self.assertIn(result["predictedGate"], {"HOLD", "CONDITIONAL_GO", "STOP", "GO"})
        self.assertTrue(result["shapTopFeatures"])
        self.assertTrue(any("override" in item for item in result["limitations"]))


if __name__ == "__main__":
    unittest.main()
