import os
import pandas as pd
import numpy as np
import httpx
from datetime import datetime, timedelta

class EconomicDataPipeline:
    """
    무역 및 경제 데이터 수집 자동화 파이프라인 (GROW-GATE Economic Pipeline)
    
    [수집 지표]
    1. 관세청: HS 8507.60 대독일 수출액 & 중량 (최근 12개월)
    2. 한국은행 ECOS: 원/달러, 원/유로 환율 (최근 3개월, 일별 -> 월별 평균)
    3. FRED: 독일/유로존 경기선행지수 (CLI) & 글로벌 공급망 압력지수 (GSCPI) (월별, Lagging 1개월)
    4. SCFI (상하이컨테이너운임지수): 물류비 지표 (주별 -> 월별 평균)
    5. KOMIS/LME: 리튬, 니켈 원자재 가격 (일별 -> 월별 평균)
    """
    
    def __init__(self, api_keys: dict = None, mock: bool = True):
        self.api_keys = api_keys or {}
        self.mock = mock
        # API Keys 예시:
        # self.api_keys = {
        #     "customs": "YOUR_KOREA_CUSTOMS_KEY",
        #     "ecos": "YOUR_BOK_ECOS_KEY",
        #     "fred": "YOUR_FRED_KEY",
        #     "komis": "YOUR_KOMIS_KEY"
        # }
        
    def fetch_customs_trade_data(self, hs_code: str, country_code: str) -> pd.DataFrame:
        """
        1. 관세청 품목별 국가별 수출입실적 API 호출 (HS Code 850760, Germany)
        """
        if self.mock:
            # 최근 12개월 모사 데이터 생성 (2025-06 ~ 2026-05)
            dates = pd.date_range(start="2025-06-01", end="2026-05-31", freq="ME")
            data = {
                "export_value_usd": [
                    12500000, 13100000, 12800000, 14200000, 13800000, 15100000,
                    14900000, 15500000, 16200000, 15900000, 17100000, 16800000
                ],
                "export_weight_kg": [
                    520000, 540000, 530000, 590000, 570000, 620000,
                    610000, 630000, 660000, 650000, 700000, 690000
                ]
            }
            df = pd.DataFrame(data, index=dates)
            df.index.name = "date"
            return df
            
        # [실제 API 호출 코드]
        api_key = self.api_keys.get("customs", "MOCK_KEY")
        url = "https://api.customs.go.kr/opendata/service/TradeStatDb/getItemTradeList"
        to_date = datetime.now()
        fr_date = to_date - timedelta(days=365)
        
        params = {
            "serviceKey": api_key,
            "priodFr": fr_date.strftime("%Y%m"),
            "priodTo": to_date.strftime("%Y%m"),
            "hsSgn": hs_code,
            "cntryCode": country_code,
            "imexVolCode": "2", # 2: 수출
            "type": "json"
        }
        
        try:
            response = httpx.get(url, params=params, timeout=10.0)
            response.raise_for_status()
            res_json = response.json()
            
            items = res_json.get("response", {}).get("body", {}).get("items", {}).get("item", [])
            records = []
            for item in items:
                period = str(item.get("priodTitle")).replace(".", "")
                date = pd.to_datetime(period + "01") + pd.offsets.MonthEnd(0)
                records.append({
                    "date": date,
                    "export_value_usd": float(item.get("expDlr", 0)),
                    "export_weight_kg": float(item.get("expWgt", 0))
                })
            
            df = pd.DataFrame(records).set_index("date")
            df = df.sort_index()
            return df
        except Exception as e:
            print(f"[관세청 API 에러 - Fallback Mock 작동]: {e}")
            self.mock = True
            return self.fetch_customs_trade_data(hs_code, country_code)

    def fetch_ecos_exchange_rates(self) -> pd.DataFrame:
        """
        2. 한국은행 ECOS API 호출 (최근 3개월 원/달러, 원/유로 일별 환율)
        """
        if self.mock:
            # 최근 90일 일별 환율 데이터 모사 생성
            dates = pd.date_range(start="2026-03-01", end="2026-05-31", freq="D")
            usd_base = np.random.normal(1370, 10, len(dates))
            eur_base = np.random.normal(1470, 12, len(dates))
            
            # 주말/공휴일 결측 처리
            for idx in range(len(dates)):
                if dates[idx].weekday() in [5, 6]:
                    usd_base[idx] = np.nan
                    eur_base[idx] = np.nan
            
            df = pd.DataFrame({
                "krw_usd": usd_base,
                "krw_eur": eur_base
            }, index=dates)
            df.index.name = "date"
            return df
            
        # [실제 API 호출 코드]
        api_key = self.api_keys.get("ecos", "MOCK_KEY")
        to_date_str = datetime.now().strftime("%Y%m%d")
        fr_date_str = (datetime.now() - timedelta(days=90)).strftime("%Y%m%d")
        
        records = []
        for code, col_name in [("0000001", "krw_usd"), ("0000053", "krw_eur")]:
            url = f"http://ecos.bok.or.kr/api/StatisticSearch/{api_key}/json/kr/1/500/036Y001/D/{fr_date_str}/{to_date_str}/{code}"
            try:
                response = httpx.get(url, timeout=10.0)
                response.raise_for_status()
                res_json = response.json()
                rows = res_json.get("StatisticSearch", {}).get("row", [])
                for row in rows:
                    date = pd.to_datetime(row.get("TIME"))
                    val = float(row.get("DATA_VALUE", 0))
                    records.append({"date": date, "key": col_name, "value": val})
            except Exception as e:
                print(f"[ECOS API 에러 - {col_name}]: {e}")
                
        if not records:
            print("[ECOS API 호출 실패 - Fallback Mock 작동]")
            self.mock = True
            return self.fetch_ecos_exchange_rates()
            
        df_raw = pd.DataFrame(records)
        df = df_raw.pivot(index="date", columns="key", values="value")
        return df

    def fetch_fred_indicators(self) -> pd.DataFrame:
        """
        3. FRED API 호출 (독일 경기선행지수 및 글로벌 공급망 압력지수)
        - LOLITOTODEM156S: 독일 경기선행지수 (OECD CLI)
        - GSCPI: 글로벌 공급망 압력지수
        """
        if self.mock:
            dates = pd.date_range(start="2025-06-01", end="2026-05-31", freq="ME")
            df = pd.DataFrame({
                "germany_cli": np.random.normal(99.5, 0.8, len(dates)),
                "gscpi": np.random.normal(0.1, 0.4, len(dates))
            }, index=dates)
            df.index.name = "date"
            return df
            
        # [실제 FRED API 호출 코드]
        api_key = self.api_keys.get("fred", "MOCK_KEY")
        to_date_str = datetime.now().strftime("%Y-%m-%d")
        fr_date_str = (datetime.now() - timedelta(days=365)).strftime("%Y-%m-%d")
        
        series_ids = {
            "LOLITOTODEM156S": "germany_cli",
            "GSCPI": "gscpi"
        }
        
        dfs = []
        for sid, col_name in series_ids.items():
            url = f"https://api.stlouisfed.org/fred/series/observations"
            params = {
                "series_id": sid,
                "api_key": api_key,
                "file_type": "json",
                "observation_start": fr_date_str,
                "observation_end": to_date_str
            }
            try:
                response = httpx.get(url, params=params, timeout=10.0)
                response.raise_for_status()
                res_json = response.json()
                obs = res_json.get("observations", [])
                
                records = []
                for ob in obs:
                    date = pd.to_datetime(ob["date"]) + pd.offsets.MonthEnd(0)
                    val = float(ob["value"]) if ob["value"] != "." else np.nan
                    records.append({"date": date, col_name: val})
                
                temp_df = pd.DataFrame(records).set_index("date")
                dfs.append(temp_df)
            except Exception as e:
                print(f"[FRED API 에러 - {col_name}]: {e}")
                
        if not dfs:
            print("[FRED API 호출 실패 - Fallback Mock 작동]")
            self.mock = True
            return self.fetch_fred_indicators()
            
        df = pd.concat(dfs, axis=1)
        return df

    def fetch_scfi_index(self) -> pd.DataFrame:
        """
        4. 상하이컨테이너운임지수 (SCFI) 최근 3개월 추이 수집
        """
        if self.mock:
            dates = pd.date_range(start="2026-03-01", end="2026-05-31", freq="W-FRI")
            scfi_values = np.random.normal(1800, 150, len(dates))
            
            df = pd.DataFrame({"scfi": scfi_values}, index=dates)
            df.index.name = "date"
            return df
            
        # [실제 구현 시: Investing.com 등 크롤링 인터페이스]
        # 여기서는 API 시그니처만 정의하고 mock을 반환하도록 보장합니다.
        self.mock = True
        return self.fetch_scfi_index()

    def fetch_raw_materials_prices(self) -> pd.DataFrame:
        """
        5. 핵심 원자재(리튬, 니켈) 가격 수집
        """
        if self.mock:
            dates = pd.date_range(start="2026-03-01", end="2026-05-31", freq="D")
            lithium = np.random.normal(100000, 3000, len(dates))
            nickel = np.random.normal(18000, 500, len(dates))
            
            # 주말 공백 처리
            for idx in range(len(dates)):
                if dates[idx].weekday() in [5, 6]:
                    lithium[idx] = np.nan
                    nickel[idx] = np.nan
                    
            df = pd.DataFrame({
                "lithium_price": lithium,
                "nickel_price": nickel
            }, index=dates)
            df.index.name = "date"
            return df
            
        # [실제 구현 시: KOMIS API 등 연동]
        self.mock = True
        return self.fetch_raw_materials_prices()

    def run_pipeline(self, hs_code: str = "850760", country_code: str = "DE") -> pd.DataFrame:
        """
        전체 수집 파이프라인 가동 및 전처리/병합 실행
        """
        print(f"=== [START PIPELINE] HS Code: {hs_code}, Target Country: {country_code} ===")
        
        # 1. 각 데이터 소스별 수집
        df_trade = self.fetch_customs_trade_data(hs_code, country_code)
        df_fx = self.fetch_ecos_exchange_rates()
        df_fred = self.fetch_fred_indicators()
        df_scfi = self.fetch_scfi_index()
        df_materials = self.fetch_raw_materials_prices()
        
        # 2. 일별/주별 데이터 -> 월별 평균값(Monthly Average) 전처리 (Resampling)
        # Pandas의 resample('ME') (또는 'M')를 사용하여 매월 말일 기준으로 평균치 집계
        df_fx_monthly = df_fx.resample("ME").mean()
        df_scfi_monthly = df_scfi.resample("ME").mean()
        df_materials_monthly = df_materials.resample("ME").mean()
        
        # 3. 데이터 프레임 병합 (Outer Join)
        dfs = [df_trade, df_fx_monthly, df_fred, df_scfi_monthly, df_materials_monthly]
        
        df_merged = dfs[0]
        for df_next in dfs[1:]:
            df_merged = pd.merge(df_merged, df_next, left_index=True, right_index=True, how="outer")
            
        # 4. 결측치(NaN) 처리 - 순방향(Forward Fill) 후 역방향(Backward Fill) 보간
        df_filled = df_merged.ffill().bfill()
        
        # 5. 경제 지표 시차(Lagging) 반영 (shift(1) 적용)
        # 예측용 외부 매크로 지표만 한 달 전 지표로 shift(1) 처리하여 다음 달 수출과 매핑
        lagged_columns = [
            "krw_usd", "krw_eur", "germany_cli", "gscpi", 
            "scfi", "lithium_price", "nickel_price"
        ]
        
        df_final = df_filled.copy()
        for col in lagged_columns:
            if col in df_final.columns:
                df_final[col] = df_final[col].shift(1)
                
        return df_final

if __name__ == "__main__":
    pipeline = EconomicDataPipeline(mock=True)
    final_df = pipeline.run_pipeline(hs_code="850760", country_code="DE")
    
    # 결과를 C:\Users\sykim\Downloads\grow_gate_local_ex-main\grow_gate_local_ex-main\grow_gate_local_ex_demo\data 디렉토리에 저장
    output_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(output_dir, "integrated_economic_signals.csv")
    final_df.to_csv(output_path)
    print(f"\n[성공] 통합 데이터 프레임이 다음 경로에 저장되었습니다: {os.path.normpath(output_path)}")
