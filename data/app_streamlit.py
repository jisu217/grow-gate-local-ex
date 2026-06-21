import os
import sys
import pandas as pd
import numpy as np

# Streamlit 패키지 확인 및 예외 안내
try:
    import streamlit as st
except ImportError:
    print("\n❌ [패키지 오류] 'streamlit' 라이브러리가 설치되어 있지 않습니다.")
    print("터미널에 다음 명령어를 입력하여 설치한 후 앱을 다시 실행해 주세요:")
    print("  pip install streamlit\n")
    sys.exit(1)

# 모듈 로딩 경로 확보
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# 로컬 ML 모델 코드 임포트 검사
try:
    import train_predict_model as pm
except ImportError as e:
    st.error(
        "❌ **모듈 임포트 에러**: `train_predict_model.py` 파일을 찾을 수 없습니다.\n"
        "이 앱 파일(`app_streamlit.py`)이 `train_predict_model.py`와 같은 `data` 폴더 안에 위치해 있는지 확인해 주세요."
    )
    st.info("💡 **해결 방법**: `streamlit run` 명령을 실행할 때 정확한 파일 경로를 지정했는지 확인해 주세요. (예: `streamlit run data/app_streamlit.py`)")
    sys.exit(1)


# 페이지 기본 설정
st.set_page_config(
    page_title="수출 역마진 위험 예측 시뮬레이터",
    page_icon="📊",
    layout="centered"
)

st.title("📊 수출 역마진 위험 및 마진 분석 시뮬레이터")
st.markdown("중소기업의 중간재 수출 데이터를 바탕으로, 다음 달 **실질 마진율 축소 및 역마진 위험성(Profitability Risk)**을 머신러닝 모형으로 예측합니다.")

# -------------------------------------------------------------
# 사이드바: 실행 안내 가이드 (초보자용)
# -------------------------------------------------------------
st.sidebar.header("ℹ️ 초보자를 위한 실행 가이드")
st.sidebar.markdown(
    """
    **1. 로컬 가동 명령어**
    터미널을 열고 프로젝트의 최상위 경로에서 아래 명령어를 실행하세요.
    ```bash
    streamlit run data/app_streamlit.py
    ```
    
    **2. 필수 설치 라이브러리**
    오류가 발생하면 아래 명령어로 필요한 패키지를 모두 설치하세요.
    ```bash
    pip install streamlit pandas numpy scikit-learn
    ```
    
    **3. 원리 요약**
    입력한 HS Code와 대상국을 바탕으로 로컬 데이터셋(`sample_trade.csv` 등)의 수치들을 자동 로드하고, `RandomForestClassifier` 모형을 학습시켜 다음 달 역마진 위험을 정량 예측합니다.
    """
)

# -------------------------------------------------------------
# 메인 폼: 사용자 입력 및 쿼리 조건 설정
# -------------------------------------------------------------
st.subheader("🔍 분석 조건 입력")
with st.form("analysis_form"):
    col1, col2, col3 = st.columns(3)
    with col1:
        product_name = st.text_input("📦 제품명", value="리튬이온 배터리팩")
    with col2:
        hs_code = st.text_input("🔢 HS Code (6자리)", value="850760")
    with col3:
        target_country = st.text_input("🌐 대상국 코드", value="DE")
        
    submit_btn = st.form_submit_button("역마진 위험 예측 실행 🚀")

# -------------------------------------------------------------
# 분석 수행 및 결과 출력
# -------------------------------------------------------------
if submit_btn:
    st.info("⚙️ 로컬 데이터 파이프라인 가동 및 RandomForest 모형 학습 중...")
    
    try:
        # 1. 데이터 로드 및 파이프라인 구동
        # pm.load_dataset() 내부에 economic_pipeline이 임포트되어 연동됩니다.
        raw_df = pm.load_dataset()
        
        # 2. 입력받은 대상국 코드에 맞추어 유효 검사
        country_code = target_country.strip().upper()
        
        # 3. 비즈니스 룰 기반 라벨링 생성
        labeled_df = pm.create_poc_labels(raw_df)
        
        # 4. 머신러닝 모델 학습 및 검증
        model, scaler, features = pm.build_ml_pipeline(labeled_df)
        
        # 5. 최신 월 데이터 추출
        latest_row = labeled_df.iloc[[-1]]
        latest_data = latest_row[features]
        latest_scaled = scaler.transform(latest_data)
        
        # 6. 다음 달 역마진 발생 확률 계산 (클래스 1의 확률)
        prob = model.predict_proba(latest_scaled)[0][1]
        
        # 7. 위험 등급 및 등급별 컬러/가이드 매핑
        if prob >= 0.75:
            risk_level = "CRITICAL"
            color = "red"
            status_msg = "팔수록 손해가 발생하는 구조적 역마진 확정 상태. 신속히 단가 재협상 및 판매 보류 필요."
        elif prob >= 0.45:
            risk_level = "HIGH"
            color = "orange"
            status_msg = "원가 및 물류비 상승폭이 수출 이익을 초과함. 마진이 한 자릿수대로 축소될 위기."
        elif prob >= 0.20:
            risk_level = "MEDIUM"
            color = "yellow"
            status_msg = "마진의 잠식이 일어나고 있으나, 아직 감내 가능한 수준. 주의 깊게 관찰 필요."
        else:
            risk_level = "LOW"
            color = "green"
            status_msg = "수출 원가 및 대외 조건이 안정적이며 양호한 마진 구조 유지 중."
            
        # 8. 가장 기여도가 높은 위험 인자 추출
        importances = model.feature_importances_
        top_idx = np.argmax(importances)
        top_risk_factor = features[top_idx]
        
        # 컬럼 이름의 가독성 매핑
        col_mapping = {
            "export_value_usd": "수출 금액(USD)",
            "export_value": "수출 금액(USD)",
            "export_weight_kg": "수출 중량(kg)",
            "export_weight": "수출 중량(kg)",
            "krw_eur": "원/유로 환율",
            "rate_eur": "원/유로 환율",
            "krw_usd": "원/달러 환율",
            "rate_usd": "원/달러 환율",
            "lithium_price": "탄산리튬 가격 지표",
            "scfi": "상하이컨테이너운임지수(SCFI)",
            "avg_sentiment": "현지 KOTRA 뉴스 감성지수"
        }
        top_factor_korean = col_mapping.get(top_risk_factor, top_risk_factor)
        
        st.success("✔ 모델 학습 및 다음 달 마진 추론이 성공적으로 완료되었습니다!")
        
        # -------------------------------------------------------------
        # 결과 대시보드 시각화
        # -------------------------------------------------------------
        st.subheader("📊 예측 분석 결과")
        
        # 주요 지표 메트릭 카드 레이아웃
        m1, m2, m3 = st.columns(3)
        with m1:
            st.metric(
                label="📅 데이터 기준 월", 
                value=str(latest_row.index[0])
            )
        with m2:
            st.metric(
                label="📈 다음 달 역마진 위험도", 
                value=f"{prob * 100:.1f}%"
            )
        with m3:
            st.metric(
                label="⚠️ 최대 기여 리스크 요인", 
                value=top_factor_korean
            )
            
        # 위험 등급 강조 영역
        st.markdown(
            f"""
            <div style="background-color: rgba({255 if color in ['red', 'orange'] else 100}, {100 if color == 'red' else 200}, 100, 0.15); 
                        padding: 15px; border-radius: 8px; border-left: 5px solid {color}; margin: 15px 0;">
                <h4 style="margin: 0; color: {color};">위험 등급: {risk_level}</h4>
                <p style="margin: 5px 0 0 0; color: #333; font-weight: 500;">{status_msg}</p>
            </div>
            """, 
            unsafe_allow_html=True
        )

        # -------------------------------------------------------------
        # 정량 근거 기반 예측 설명문 출력
        # -------------------------------------------------------------
        st.subheader("📝 예측 설명문")
        latest_row_series = labeled_df.iloc[-1]
        
        # 대본 생성 함수 호출
        presentation_script = pm.generate_presentation_script(
            prob=prob,
            latest_row=latest_row_series,
            features=features,
            risk_level=risk_level,
            status_msg=status_msg,
            top_risk_factor=top_risk_factor,
            importances=importances
        )
        
        # 텍스트 에어리어에 배치하여 복사하기 쉽게 처리
        st.text_area(
            label="정량 지표 및 모델 추론 기여도에 기초하여 작성된 설명문입니다 (텍스트 복사 가능).",
            value=presentation_script,
            height=280
        )
        
    except FileNotFoundError as fnf:
        st.error(f"❌ **데이터 파일 누락**: {fnf}")
        st.info("💡 **해결 방법**: `sample_trade.csv`, `sample_exchange.csv`, `sample_kotra_news.csv` 파일이 `data` 폴더 안에 온전히 존재하는지 확인한 후 파이프라인을 먼저 돌려주세요.")
        
    except Exception as exc:
        st.error("❌ **모델 연동 중 에러 발생**")
        st.code(str(exc), language="python")
        st.info("💡 **해결 방법**: 사용하시는 터미널 경로와 환경 변수 설정을 확인한 후 다시 시도해 주세요.")
