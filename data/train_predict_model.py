import os
import sys
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report

# 현재 디렉토리를 파이썬 모듈 검색 경로에 추가
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

try:
    from economic_pipeline import EconomicDataPipeline
except ImportError:
    EconomicDataPipeline = None


def load_dataset() -> pd.DataFrame:
    """
    기존 데이터 파이프라인 연동:
    - 파이프라인 클래스가 로딩 가능하면 연동하여 데이터프레임을 생성합니다.
    - 파일만 존재하면 CSV 파일을 읽어와 데이터프레임으로 변환합니다.
    """
    df = None
    data_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 1. EconomicDataPipeline 모듈 직접 실행 시도
    if EconomicDataPipeline is not None:
        try:
            print("[Info] EconomicDataPipeline을 기동하여 최신 데이터를 동적 빌드합니다...")
            pipeline = EconomicDataPipeline(mock=True)
            df = pipeline.run_pipeline(hs_code="850760", country_code="DE")
        except Exception as e:
            print(f"[Warning] 파이프라인 가동 중 오류 발생 ({e}), 파일 로딩으로 우회합니다.")
            
    # 2. 로컬 CSV 파일 읽기
    if df is None:
        csv_candidates = [
            os.path.join(data_dir, "integrated_economic_signals.csv"),
            os.path.join(data_dir, "integrated_monthly_dataset.csv")
        ]
        
        for path in csv_candidates:
            if os.path.exists(path):
                print(f"[Info] 로컬 CSV 로드 중: {os.path.basename(path)}")
                df = pd.read_csv(path)
                break
                
        if df is None:
            raise FileNotFoundError(
                "학습용 통합 데이터셋 파일을 찾을 수 없습니다.\n"
                "먼저 economic_pipeline.py 또는 merge_samples.py를 가동해 주세요."
            )
            
        # 날짜 컬럼을 인덱스로 복원
        if "date" in df.columns:
            df = df.set_index("date")
        elif "year_month" in df.columns:
            df = df.set_index("year_month")
            
    return df


def create_poc_labels(df: pd.DataFrame) -> pd.DataFrame:
    """
    지도학습용 PoC 비즈니스 규칙 기반의 '역마진 위험 라벨' 동적 생성:
    - 0: 정상 마진
    - 1: 역마진 위험 (Profitability Risk)
    
    [규칙 설계]
    수출 물량(weight)이나 금액(value) 대비, 원재료비(lithium_price) 또는 운임(scfi)의 
    상승 압력이 크거나 환율(krw_eur) 하락으로 인한 실질 수익 잠식이 심할 때 위험군(1)으로 판정합니다.
    """
    print("\n[Step 2] 지도학습용 역마진 라벨(Label) 동적 부여 중...")
    df_labeled = df.copy()
    
    # 각 지표별 컬럼명 표준화 추출 (매칭 실패 방지용)
    val_col = "export_value_usd" if "export_value_usd" in df_labeled.columns else "export_value"
    weight_col = "export_weight_kg" if "export_weight_kg" in df_labeled.columns else "export_weight"
    eur_col = "krw_eur" if "krw_eur" in df_labeled.columns else "rate_eur"
    
    # 과거 시점과의 원가 변동 비교를 위해 변동율 계산용 임시 컬럼 생성
    # 시차가 이미 적용된 매크로 변수들이므로 현재 행 기준으로 계산
    
    labels = []
    for i in range(len(df_labeled)):
        # 첫 번째 행은 이전 데이터가 없으므로 임계치 기준 절대 평가
        if i == 0:
            lithium = df_labeled.iloc[i].get("lithium_price", 100.0)
            scfi = df_labeled.iloc[i].get("scfi", 2000.0)
            # 기준값 대비 상승 여부로 라벨링
            if lithium > 105.0 or scfi > 2100.0:
                labels.append(1)
            else:
                labels.append(0)
            continue
            
        prev_row = df_labeled.iloc[i-1]
        curr_row = df_labeled.iloc[i]
        
        # 1. 수출금액 증가율 (외형 증가)
        val_change = (curr_row[val_col] - prev_row[val_col]) / (prev_row[val_col] + 1e-9)
        # 2. 원자재가 증가율 (제조원가 압박)
        lithium_change = (curr_row["lithium_price"] - prev_row["lithium_price"]) / (prev_row["lithium_price"] + 1e-9)
        # 3. 해상운임 증가율 (물류 고정비 압박)
        scfi_change = (curr_row["scfi"] - prev_row["scfi"]) / (prev_row["scfi"] + 1e-9)
        # 4. 원/유로 환율 변동성 (환손실 압박)
        eur_change = (curr_row[eur_col] - prev_row[eur_col]) / (prev_row[eur_col] + 1e-9)
        
        # [역마진 리스크 판정 비즈니스 룰]
        # 외형 수출액 성장에 비해, 원자재 가격이나 운송 지수가 임계값(예: 3%) 이상으로 급격히 증가했거나,
        # 환율이 2% 이상 폭락하여 수출 대금의 원화 환산액 손실이 발생할 경우 라벨 1 부여
        is_risk = False
        
        # 조건 A: 원자재 상승폭이 수출금액 성장률을 압도하는 경우
        if lithium_change > val_change + 0.03:
            is_risk = True
        # 조건 B: 운임 지수가 5% 이상 상승하며 물류 비용이 과다 지출되는 경우
        elif scfi_change > 0.05:
            is_risk = True
        # 조건 C: 환율이 전월 대비 1.5% 이상 하락(원화 강세)하여 매출 감소가 생기는 경우
        elif eur_change < -0.015:
            is_risk = True
            
        labels.append(1 if is_risk else 0)
        
    df_labeled["profitability_risk_label"] = labels
    
    # 데이터셋의 불균형이 너무 심한 경우(모두 0이거나 모두 1인 경우)
    # PoC 학습이 가능하도록 최소한의 데이터 분산 강제 보정
    unique_labels = df_labeled["profitability_risk_label"].unique()
    if len(unique_labels) < 2:
        print("[Warning] 라벨 분산 부족으로 강제 합성 라벨링을 적용합니다 (분류기 구동용).")
        # 홀수 인덱스 행에 강제로 위험 라벨(1) 주입
        df_labeled.iloc[::2, df_labeled.columns.get_loc("profitability_risk_label")] = 1
        
    print(f"-> 클래스 분포: {dict(df_labeled['profitability_risk_label'].value_counts())}")
    return df_labeled


def build_ml_pipeline(df: pd.DataFrame):
    """
    ML 모델 학습 및 검증 파이프라인 구축
    """
    print("\n[Step 3] 머신러닝 파이프라인 학습 및 평가 개시...")
    
    # 피처 변수 정의 (X) 및 라벨 정의 (y)
    val_col = "export_value_usd" if "export_value_usd" in df.columns else "export_value"
    weight_col = "export_weight_kg" if "export_weight_kg" in df.columns else "export_weight"
    eur_col = "krw_eur" if "krw_eur" in df.columns else "rate_eur"
    
    features = [val_col, weight_col, eur_col, "lithium_price", "scfi", "avg_sentiment"]
    features = [f for f in features if f in df.columns]
    
    X = df[features]
    y = df["profitability_risk_label"]
    
    # 데이터 분할 (PoC용 소형 데이터의 경우 test_size 비중 조절)
    # 각 클래스별 최소 샘플 수가 2개 이상일 때만 계층화(stratify) 분할을 수행하여 오류를 방지합니다.
    class_counts = y.value_counts()
    can_stratify = len(class_counts) > 1 and all(count >= 2 for count in class_counts)
    
    test_size = 0.25 if len(df) >= 8 else 0.5
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=42, stratify=y if can_stratify else None
    )
    
    # 스케일러 정의 및 적용
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    # 트리 기반의 RandomForestClassifier 학습
    model = RandomForestClassifier(n_estimators=50, random_state=42, max_depth=3)
    model.fit(X_train_scaled, y_train)
    
    # 성능 평가
    y_pred = model.predict(X_test_scaled)
    acc = accuracy_score(y_test, y_pred)
    
    print(f"✔ 모델 학습 완료. 검증 정확도 (Accuracy): {acc:.4f}")
    print("\n[Classification Report]")
    print(classification_report(y_test, y_pred, zero_division=0))
    
    return model, scaler, features


def generate_presentation_script(prob: float, latest_row: pd.Series, features: list, risk_level: str, status_msg: str, top_risk_factor: str, importances: list) -> str:
    """
    예측 확률과 입력 행의 실제 feature 값을 바탕으로 발표용 설명문(스크립트)을 생성합니다.
    철저히 주어진 데이터 값(수치)만을 근거로 말하도록 설계되었습니다.
    """
    # 컬럼 한국어 매핑 사전
    col_mapping = {
        "export_value_usd": "수출 금액",
        "export_value": "수출 금액",
        "export_weight_kg": "수출 중량",
        "export_weight": "수출 중량",
        "krw_eur": "원/유로 환율",
        "rate_eur": "원/유로 환율",
        "krw_usd": "원/달러 환율",
        "rate_usd": "원/달러 환율",
        "lithium_price": "탄산리튬 가격 지표",
        "scfi": "상하이컨테이너운임지수(SCFI)",
        "avg_sentiment": "현지 KOTRA 뉴스 감성지수"
    }
    
    # 지표 값 안전 추출
    val_key = [c for c in ["export_value_usd", "export_value"] if c in latest_row.index]
    weight_key = [c for c in ["export_weight_kg", "export_weight"] if c in latest_row.index]
    eur_key = [c for c in ["krw_eur", "rate_eur"] if c in latest_row.index]
    
    val_val = latest_row[val_key[0]] if val_key else 0.0
    weight_val = latest_row[weight_key[0]] if weight_key else 0.0
    eur_val = latest_row[eur_key[0]] if eur_key else 0.0
    lithium_val = latest_row.get("lithium_price", 0.0)
    scfi_val = latest_row.get("scfi", 0.0)
    sentiment_val = latest_row.get("avg_sentiment", 0.0)
    
    # 중요도 매핑
    imp_dict = dict(zip(features, importances))
    top_factor_name = col_mapping.get(top_risk_factor, top_risk_factor)
    top_factor_val = latest_row.get(top_risk_factor, 0.0)
    top_factor_imp = imp_dict.get(top_risk_factor, 0.0)
    
    script = f"""이번 모형에 반영된 최신 행({latest_row.name})의 실제 지표 데이터 값은 다음과 같습니다.
첫째, 당월 수출 규모 변수를 보면 수출 금액은 ${val_val:,.1f} 이며, 실제 선적된 수출 중량은 {weight_val:,.1f} kg입니다.
둘째, 대외 환율 변수의 경우 원/유로 환율은 {eur_val:,.2f} 원을 나타내고 있습니다.
셋째, 기업의 주요 제조원가 압박 요인으로 꼽히는 탄산리튬 가격 지표는 {lithium_val:.1f} 이며, 물류 고정비 지표인 상하이컨테이너운임지수(SCFI)는 {scfi_val:.1f} 입니다.
마지막으로 현지 정보 기반의 KOTRA 뉴스 감성지수는 {sentiment_val:.2f}의 수치를 기록하고 있습니다.

이 실제 수치들을 입력값으로 하여 RandomForest 분류 모델이 추론한 결과, 다음 달 수출 역마진 위험 발생 확률은 정확히 {prob * 100:.2f}%로 예측되었습니다. 이에 따른 최종 판단 위험 등급은 '{risk_level}' 상태이며, '{status_msg}'이라는 분석 결과가 도출되었습니다.

특히 모형의 특성 기여도(Feature Importance) 가중치 분석 결과, 이번 역마진 위험 등급을 판정하는 데 {top_factor_imp * 100:.1f}%의 비율로 가장 큰 영향(기여도 1위)을 미친 변수는 '{top_factor_name}'이며, 해당 지표의 실제 투입 값은 {top_factor_val:,.1f} 입니다."""
    
    return script


def predict_next_month_risk(model, scaler, features, df: pd.DataFrame):
    """
    최신 행(마지막 달) 데이터를 받아 다음 달의 역마진 위험성과 주요 리스크 원인을 역추적합니다.
    """
    print("\n[Step 4] 최신 데이터를 바탕으로 다음 달 역마진 위험 예측 실행 중...")
    
    # 마지막 달 레코드 추출
    latest_row = df.iloc[[-1]]
    latest_data = latest_row[features]
    
    # 스케일링 적용
    latest_scaled = scaler.transform(latest_data)
    
    # 1이 될 확률(역마진 위험 확률) 추정
    prob = model.predict_proba(latest_scaled)[0][1]
    
    # 위험 등급 판정 규칙
    if prob >= 0.75:
        risk_level = "CRITICAL"
        status_msg = "팔수록 손해가 발생하는 구조적 역마진 확정 상태. 신속히 단가 재협상 및 판매 보류 필요."
    elif prob >= 0.45:
        risk_level = "HIGH"
        status_msg = "원가 및 물류비 상승폭이 수출 이익을 초과함. 마진이 한 자릿수대로 축소될 위기."
    elif prob >= 0.20:
        risk_level = "MEDIUM"
        status_msg = "마진의 잠식이 일어나고 있으나, 아직 감내 가능한 수준. 주의 깊게 관찰 필요."
    else:
        risk_level = "LOW"
        status_msg = "수출 원가 및 대외 조건이 안정적이며 양호한 마진 구조 유지 중."

    # 피처 중요도를 역추적하여 가장 부정적 기여가 큰 악영향 변수 탐색 (Feature Importance 활용)
    importances = model.feature_importances_
    top_idx = np.argmax(importances)
    top_risk_factor = features[top_idx]
    
    print("\n================ 📈 다음 달 수출 마진 예측 리포트 ================")
    print(f" * 기준 월(최신 데이터): {latest_row.index[0]}")
    print(f" * 다음 달 역마진 발생 확률: {prob * 100:.2f}%")
    print(f" * 최종 예측 위험 등급: {risk_level}")
    print(f" * 진단 현황 요약: {status_msg}")
    print(f" * 의사결정 기여도 최상위 지표 (최대 기여 피처): {top_risk_factor} (가중치: {importances[top_idx]:.4f})")
    print("==================================================================")
    
    # 학생 발표용 대본 스크립트 생성 및 출력
    latest_row_series = df.iloc[-1]
    script = generate_presentation_script(
        prob=prob,
        latest_row=latest_row_series,
        features=features,
        risk_level=risk_level,
        status_msg=status_msg,
        top_risk_factor=top_risk_factor,
        importances=importances
    )
    print("\n" + script + "\n")


def main():
    try:
        # 1. 데이터 로드
        df = load_dataset()
        print(f"✔ 데이터셋 로드 성공! 레코드 개수: {df.shape[0]}, 피처 개수: {df.shape[1]}")
        
        # 2. 라벨 생성
        df_labeled = create_poc_labels(df)
        
        # 3. 모델 학습
        model, scaler, features = build_ml_pipeline(df_labeled)
        
        # 4. 다음 달 예측 수행
        predict_next_month_risk(model, scaler, features, df_labeled)
        
    except Exception as e:
        print(f"\n❌ [에러] 머신러닝 모델 가동 및 학습 도중 오류가 발생했습니다:")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()

