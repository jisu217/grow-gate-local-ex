import os
import pandas as pd
import numpy as np

def run_integration():
    print("=== [데이터 분석 조교: 월별 데이터셋 통합 실습 시작] ===")
    
    # 디렉토리 경로 지정
    data_dir = os.path.dirname(os.path.abspath(__file__))
    trade_path = os.path.join(data_dir, "sample_trade.csv")
    exchange_path = os.path.join(data_dir, "sample_exchange.csv")
    news_path = os.path.join(data_dir, "sample_kotra_news.csv")
    
    # -------------------------------------------------------------
    # 1. 파일 읽기 및 로딩 검사
    # -------------------------------------------------------------
    print("\n[Step 1] CSV 파일 로딩 중...")
    try:
        df_trade = pd.read_csv(trade_path)
        df_exchange = pd.read_csv(exchange_path)
        df_news = pd.read_csv(news_path)
        print("✔ 모든 파일 로드 완료!")
    except FileNotFoundError as e:
        print(f"❌ 에러: 파일을 찾을 수 없습니다. 경로를 확인해 주세요. {e}")
        return

    # -------------------------------------------------------------
    # 2. 날짜 변환 및 표준 월 인덱스 생성
    # -------------------------------------------------------------
    print("\n[Step 2] 날짜 변환 및 표준 연월(Period) 생성...")
    
    # 각 파일의 날짜 컬럼을 datetime으로 변환 후, 월 단위(YYYY-MM) Period 컬럼 생성
    df_trade['datetime'] = pd.to_datetime(df_trade['date'])
    df_trade['year_month'] = df_trade['datetime'].dt.to_period('M')
    
    df_exchange['datetime'] = pd.to_datetime(df_exchange['date'])
    df_exchange['year_month'] = df_exchange['datetime'].dt.to_period('M')
    
    df_news['datetime'] = pd.to_datetime(df_news['publish_date'])
    df_news['year_month'] = df_news['datetime'].dt.to_period('M')
    
    print("✔ 날짜 타입 변환 완료 및 월단위 인덱스 생성 완료.")

    # -------------------------------------------------------------
    # 3. 국가 코드 표준화 및 조인 오류 검사 (Join Error Checking)
    # -------------------------------------------------------------
    print("\n[Step 3] 국가 코드 표준화 및 조인 정합성 검사...")
    
    # 기준 국가 코드 목록 정의 (Trade 데이터 기반)
    df_trade['country_code'] = df_trade['country_code'].str.strip().str.upper()
    valid_country_codes = set(df_trade['country_code'].unique())
    print(f"기준 국가 코드 (수출 실적 기준): {valid_country_codes}")
    
    # KOTRA 뉴스 데이터 국가 코드 표준화 전처리
    df_news['country_code'] = df_news['country_code'].str.strip()
    
    # 수동 매핑 사전 정의 (소문자 변환, 비표준 약어 치환)
    country_mapping = {
        'GER': 'DE',
        'GERMANY': 'DE',
        'USA': 'US',
        'de': 'DE',
        'us': 'US'
    }
    
    # 1차 치환 매핑 적용
    df_news['country_code_cleaned'] = df_news['country_code'].map(country_mapping).fillna(df_news['country_code']).str.upper()
    
    # 매핑 후 불일치하는 국가 코드 찾아내기 (조인 오류 검사)
    news_country_codes = set(df_news['country_code_cleaned'].unique())
    mismatched_codes = news_country_codes - valid_country_codes
    
    if mismatched_codes:
        print(f"⚠ [경고] 기준 수출 데이터셋에 없는 국가 코드가 뉴스 데이터셋에서 발견되었습니다: {mismatched_codes}")
        print("--> 조인에서 누락되거나 왜곡될 위험이 있습니다. 유효하지 않은 데이터는 필터링하거나 추가 매핑 처리가 필요합니다.")
        # 유효하지 않은 XX 코드 등의 행 확인용 출력
        invalid_rows = df_news[df_news['country_code_cleaned'].isin(mismatched_codes)]
        print("불일치 데이터 예시:")
        print(invalid_rows[['publish_date', 'country_code', 'title']])
        
        # 정합성을 위해 일치하지 않는 국가 코드는 제외하거나 처리 (여기서는 조인 제외 처리)
        df_news = df_news[~df_news['country_code_cleaned'].isin(mismatched_codes)].copy()
        df_news['country_code'] = df_news['country_code_cleaned']
    else:
        df_news['country_code'] = df_news['country_code_cleaned']
        print("✔ 모든 국가 코드가 정상적으로 매핑되었습니다.")

    # -------------------------------------------------------------
    # 4. 일별/주별 데이터 월별 전처리 및 집계 (Resampling & Aggregation)
    # -------------------------------------------------------------
    print("\n[Step 4] 환율 및 뉴스 데이터 월별 집계...")
    
    # (A) 환율 데이터: 통화별(USD, EUR) 일별 환율 -> 월별 평균값 집계
    # 피벗을 통해 통화별 컬럼으로 펼친 후 월별 평균 계산
    df_exchange_pivot = df_exchange.pivot_table(
        index='datetime', 
        columns='currency', 
        values='rate', 
        aggfunc='first'
    )
    # 월별 평균 계산
    df_exchange_monthly = df_exchange_pivot.resample('ME').mean()
    df_exchange_monthly.index = df_exchange_monthly.index.to_period('M')
    df_exchange_monthly = df_exchange_monthly.rename(columns={
        'USD': 'rate_usd',
        'EUR': 'rate_eur'
    })
    print("✔ 환율 데이터 월별 평균화 완료.")
    
    # (B) 뉴스 데이터: 월별, 국가별 뉴스 건수 및 평균 감성점수 집계
    df_news_monthly = df_news.groupby(['year_month', 'country_code']).agg(
        news_count=('title', 'count'),
        avg_sentiment=('sentiment_score', 'mean')
    ).reset_index()
    print("✔ 뉴스 데이터 월별/국가별 집계 완료.")

    # -------------------------------------------------------------
    # 5. 데이터 병합 (Merge)
    # -------------------------------------------------------------
    print("\n[Step 5] 데이터셋 병합 (Trade + Exchange + News)...")
    
    # 1단계: 무역(Trade) 데이터와 환율(Exchange) 데이터 병합 (월 단위 기준)
    df_merged = pd.merge(
        df_trade, 
        df_exchange_monthly, 
        left_on='year_month', 
        right_index=True, 
        how='left'
    )
    
    # 2단계: 1단계 결과와 뉴스(News) 집계 데이터 병합 (월 단위 & 국가 코드 기준)
    df_final = pd.merge(
        df_merged, 
        df_news_monthly, 
        on=['year_month', 'country_code'], 
        how='left'
    )
    
    # -------------------------------------------------------------
    # 6. 결측치 처리 (Handling Missing Values)
    # -------------------------------------------------------------
    print("\n[Step 6] 결측치 탐색 및 보간 처리...")
    print("결측치 개수 확인 (보간 전):")
    print(df_final.isnull().sum())
    
    # 결측치 처리 규칙 정의:
    # 1. 수출금액(export_value) 누락 시 해당 국가의 평균값 또는 0으로 대체 (여기서는 0 대체)
    df_final['export_value'] = df_final['export_value'].fillna(0)
    
    # 2. 환율(rate_usd, rate_eur) 누락 시 시계열 순방향 채우기(ffill) 후 역방향 채우기(bfill) 적용
    df_final['rate_usd'] = df_final['rate_usd'].ffill().bfill()
    df_final['rate_eur'] = df_final['rate_eur'].ffill().bfill()
    
    # 3. 뉴스 건수(news_count) 및 감성점수(avg_sentiment) 누락 시 뉴스가 없었던 것이므로 각각 0과 중립(0.0)으로 채움
    df_final['news_count'] = df_final['news_count'].fillna(0).astype(int)
    df_final['avg_sentiment'] = df_final['avg_sentiment'].fillna(0.0)
    
    print("\n결측치 처리 후 데이터프레임 상태:")
    print(df_final.isnull().sum())

    # -------------------------------------------------------------
    # 7. 정렬 및 최종 파일 저장
    # -------------------------------------------------------------
    # 컬럼 정리 및 정렬
    df_final = df_final.sort_values(by=['year_month', 'country_code']).reset_index(drop=True)
    cols_to_keep = [
        'year_month', 'country_code', 'hs_code', 
        'export_value', 'export_weight', 
        'rate_usd', 'rate_eur', 
        'news_count', 'avg_sentiment'
    ]
    df_final = df_final[cols_to_keep]
    
    output_path = os.path.join(data_dir, "integrated_monthly_dataset.csv")
    df_final.to_csv(output_path, index=False)
    
    print(f"\n✔ [완료] 최종 월별 통합 데이터셋이 성공적으로 저장되었습니다.")
    print(f"파일 경로: {os.path.normpath(output_path)}")
    print("\n--- 최종 데이터셋 프리뷰 (마지막 5행) ---")
    print(df_final.to_string())

if __name__ == "__main__":
    run_integration()
