#!/usr/bin/env python3
import argparse
import csv
import datetime as dt
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


REDACTED = "[REDACTED]"


def env(name, default=""):
    return os.environ.get(name, default)


def first_env(*names):
    for name in names:
        value = env(name)
        if value:
            return value
    return ""


def yyyymm_range():
    today = dt.date.today()
    first_this_month = today.replace(day=1)
    end_month = first_this_month - dt.timedelta(days=1)
    start_month = (end_month.replace(day=1) - dt.timedelta(days=330)).replace(day=1)
    return start_month.strftime("%Y%m"), end_month.strftime("%Y%m")


def providers(args):
    start_yymm, end_yymm = yyyymm_range()
    return [
        {
            "provider": "KOTRA",
            "runtimeProvider": "KOTRA_MARKET",
            "agent": "market",
            "endpoint": env("KOTRA_MARKET_API_URL", "https://www.data.go.kr/data/15122648/openapi.do"),
            "apiKey": first_env("KOTRA_MARKET_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KOTRA_MARKET_API_KEY|DATAGO_SERVICE_KEY",
            "params": {"country": args.country, "keyword": args.product_name},
            "defaultZeroReason": "CATALOG_PAGE_ENDPOINT",
        },
        {
            "provider": "한국수출입은행 환율 정보",
            "runtimeProvider": "EXIM_EXCHANGE_RATE",
            "agent": "market-fx-reference",
            "endpoint": env("EXIM_EXCHANGE_RATE_API_URL", "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"),
            "apiKey": first_env("EXIM_EXCHANGE_RATE_API_KEY"),
            "apiKeyEnv": "EXIM_EXCHANGE_RATE_API_KEY",
            "keyParam": "authkey",
            "params": {
                "searchdate": env("EXIM_EXCHANGE_RATE_SEARCH_DATE", dt.date.today().strftime("%Y%m%d")),
                "data": env("EXIM_EXCHANGE_RATE_DATA_CODE", "AP01"),
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "한국은행 ECOS 100대 통계지표",
            "runtimeProvider": "BOK_KEY_STATISTICS",
            "agent": "market-macro-reference",
            "endpoint": env("BOK_KEY_STATISTICS_API_URL", "https://ecos.bok.or.kr/api/KeyStatisticList"),
            "apiKey": first_env("BOK_ECOS_API_KEY"),
            "apiKeyEnv": "BOK_ECOS_API_KEY",
            "pathKey": True,
            "pathSuffix": "/{key}/json/kr/1/" + env("BOK_KEY_STATISTICS_MAX_ROWS", "100"),
            "params": {},
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "KOTRA 해외전시회 정보",
            "runtimeProvider": "KOTRA_OVERSEAS_EXHIBITIONS",
            "agent": "support-exhibition-reference",
            "endpoint": env("KOTRA_OVERSEAS_EXHIBITIONS_SOURCE_URL", "https://www.data.go.kr/data/15003367/fileData.do"),
            "apiKey": first_env("KOTRA_OVERSEAS_EXHIBITIONS_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KOTRA_OVERSEAS_EXHIBITIONS_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KOTRA_OVERSEAS_EXHIBITIONS_FILE_PATH", "data/public/kotra_overseas_exhibitions_20250630.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KOTRA_OVERSEAS_EXHIBITIONS_FILE_AS_OF", "2025-06-30"),
            "officialUpdatedAt": env("KOTRA_OVERSEAS_EXHIBITIONS_OFFICIAL_UPDATED_AT", "2025-06-30"),
            "refreshDueAt": env("KOTRA_OVERSEAS_EXHIBITIONS_NEXT_UPDATE_DUE_AT", "2026-06-30"),
            "apiEndpoint": env("KOTRA_OVERSEAS_EXHIBITIONS_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "200",
                "returnType": "JSON",
            },
        },
        {
            "provider": "MOTIE 반도체디스플레이 산업 동향",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "industry-trend-reference",
            "endpoint": env("MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_SOURCE_URL", "https://www.data.go.kr/data/15051125/fileData.do"),
            "apiKey": first_env("MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_FILE_PATH", "data/public/motie_semiconductor_display_trends_20251231.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_FILE_AS_OF", "2025-12-31"),
            "officialUpdatedAt": env("MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_OFFICIAL_UPDATED_AT", "2026-06-10"),
            "refreshDueAt": env("MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_NEXT_UPDATE_DUE_AT", "2027-06-11"),
            "apiEndpoint": env("MOTIE_SEMICONDUCTOR_DISPLAY_TRENDS_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "30",
                "returnType": "JSON",
            },
        },
        {
            "provider": "MOTIE 제품 리콜처분현황",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "recall-disposition-statistics",
            "endpoint": env("MOTIE_RECALL_DISPOSITIONS_SOURCE_URL", "https://www.data.go.kr/data/15054301/fileData.do"),
            "apiKey": first_env("MOTIE_RECALL_DISPOSITIONS_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "MOTIE_RECALL_DISPOSITIONS_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("MOTIE_RECALL_DISPOSITIONS_FILE_PATH", "data/public/motie_product_recall_dispositions_20251231.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("MOTIE_RECALL_DISPOSITIONS_FILE_AS_OF", "2025-12-31"),
            "officialUpdatedAt": env("MOTIE_RECALL_DISPOSITIONS_OFFICIAL_UPDATED_AT", "2025-03-19"),
            "refreshDueAt": env("MOTIE_RECALL_DISPOSITIONS_NEXT_UPDATE_DUE_AT", "2026-03-19"),
            "apiEndpoint": env("MOTIE_RECALL_DISPOSITIONS_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "20",
                "returnType": "JSON",
            },
        },
        {
            "provider": "MOTIE 제품안전인증 신규등록현황",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "safety-cert-new-registration-statistics",
            "endpoint": env("MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_SOURCE_URL", "https://www.data.go.kr/data/15054302/fileData.do"),
            "apiKey": first_env("MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_FILE_PATH", "data/public/motie_product_safety_cert_new_registrations_20251231.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_FILE_AS_OF", "2025-12-31"),
            "officialUpdatedAt": env("MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_OFFICIAL_UPDATED_AT", "2026-03-11"),
            "refreshDueAt": env("MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_NEXT_UPDATE_DUE_AT", "2027-03-19"),
            "apiEndpoint": env("MOTIE_SAFETY_CERT_NEW_REGISTRATIONS_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "20",
                "returnType": "JSON",
            },
        },
        {
            "provider": "MOTIE 금속표준용어",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "knowledge-terminology-reference",
            "endpoint": env("MOTIE_METAL_STANDARD_TERMS_SOURCE_URL", "https://www.data.go.kr/data/3074836/fileData.do"),
            "apiKey": "",
            "apiKeyEnv": "none",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileKind": "docx",
            "filePath": env("MOTIE_METAL_STANDARD_TERMS_FILE_PATH", "data/public/motie_metal_standard_terms_20140514.docx"),
            "fileAsOf": env("MOTIE_METAL_STANDARD_TERMS_FILE_AS_OF", "2014-05-14"),
            "officialUpdatedAt": env("MOTIE_METAL_STANDARD_TERMS_OFFICIAL_UPDATED_AT", "2025-08-18"),
        },
        {
            "provider": "MOTIE FTA추진현황정보",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "knowledge-fta-status-reference",
            "endpoint": env("MOTIE_FTA_STATUS_SOURCE_URL", "https://www.data.go.kr/data/3074066/fileData.do"),
            "apiKey": "",
            "apiKeyEnv": "none",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileKind": "pdf",
            "filePath": env("MOTIE_FTA_STATUS_FILE_PATH", "data/public/motie_fta_status_20260531.pdf"),
            "fileAsOf": env("MOTIE_FTA_STATUS_FILE_AS_OF", "2026-05-31"),
            "officialUpdatedAt": env("MOTIE_FTA_STATUS_OFFICIAL_UPDATED_AT", "2026-05-31"),
        },
        {
            "provider": "KEA 산업부문 에너지사용 및 온실가스배출량 통계",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "knowledge-industrial-energy-ghg-reference",
            "endpoint": env("KEA_INDUSTRIAL_ENERGY_GHG_SOURCE_URL", "https://www.data.go.kr/data/15017225/fileData.do"),
            "apiKey": "",
            "apiKeyEnv": "none",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileKind": "hwpx",
            "filePath": env("KEA_INDUSTRIAL_ENERGY_GHG_FILE_PATH", "data/public/kea_industrial_energy_ghg_statistics_20241231.hwpx"),
            "fileAsOf": env("KEA_INDUSTRIAL_ENERGY_GHG_FILE_AS_OF", "2024-12-31"),
            "officialUpdatedAt": env("KEA_INDUSTRIAL_ENERGY_GHG_OFFICIAL_UPDATED_AT", "2026-06-12"),
            "refreshDueAt": env("KEA_INDUSTRIAL_ENERGY_GHG_NEXT_UPDATE_DUE_AT", "2027-05-31"),
        },
        {
            "provider": "무역/수출입 통계",
            "runtimeProvider": "KCS_TRADE",
            "agent": "market",
            "endpoint": env("KCS_TRADE_API_URL", "http://apis.data.go.kr/1220000/Newtrade/getNewtradeList"),
            "apiKey": first_env("KCS_TRADE_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KCS_TRADE_API_KEY|DATAGO_SERVICE_KEY",
            "params": {
                "hsCode": args.hs_code,
                "cntyCd": args.country,
                "strtYymm": start_yymm,
                "endYymm": end_yymm,
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "관세청 HS/관세율/운송비 reference",
            "runtimeProvider": "KCS_TARIFF_LOGISTICS",
            "agent": "market-reference",
            "endpoint": env("KCS_TARIFF_LOGISTICS_SOURCE_URL", "https://www.customs.go.kr/"),
            "apiKey": "",
            "apiKeyEnv": "none",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "filePath": env("KCS_TARIFF_LOGISTICS_FILE_PATH", "data/public/kcs_tariff_logistics_reference_20260531.csv"),
            "fileEncodings": ["utf-8-sig", "utf-8"],
            "fileAsOf": env("KCS_TARIFF_LOGISTICS_FILE_AS_OF", "2026-05-31"),
            "officialUpdatedAt": env("KCS_TARIFF_LOGISTICS_OFFICIAL_UPDATED_AT", "2026-05-31"),
            "refreshDueAt": env("KCS_TARIFF_LOGISTICS_NEXT_UPDATE_DUE_AT", "2026-07-31"),
        },
        {
            "provider": "KTL Certinfo",
            "runtimeProvider": "NOT_IMPLEMENTED",
            "agent": "regulation",
            "endpoint": first_env("KTL_CERTINFO_API_URL", "CERTINFO_API_URL"),
            "apiKey": first_env("KTL_CERTINFO_API_KEY", "CERTINFO_API_KEY"),
            "apiKeyEnv": "KTL_CERTINFO_API_KEY|CERTINFO_API_KEY",
            "params": {"country": args.country, "keyword": args.product_name},
            "defaultZeroReason": "CONNECTOR_NOT_IMPLEMENTED",
        },
        {
            "provider": "SafetyKorea",
            "runtimeProvider": "SAFETY_RECALL",
            "agent": "recall",
            "endpoint": env("SAFETY_RECALL_API_URL", "https://www.safetykorea.kr/release/openapi"),
            "apiKey": first_env("SAFETY_RECALL_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "SAFETY_RECALL_API_KEY|DATAGO_SERVICE_KEY",
            "params": {"hsCode": args.hs_code, "productName": args.product_name},
            "defaultZeroReason": "CATALOG_PAGE_ENDPOINT",
        },
        {
            "provider": "KATS 국내리콜정보",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "domestic-recall-reference",
            "endpoint": env("KATS_DOMESTIC_RECALL_SOURCE_URL", "https://www.data.go.kr/data/15040696/fileData.do"),
            "apiKey": "",
            "apiKeyEnv": "none",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KATS_DOMESTIC_RECALL_FILE_PATH", "data/public/kats_domestic_recall_20230809.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KATS_DOMESTIC_RECALL_FILE_AS_OF", "2023-08-09"),
            "officialUpdatedAt": env("KATS_DOMESTIC_RECALL_OFFICIAL_UPDATED_AT", "2025-09-05"),
            "apiEndpoint": env(
                "KATS_DOMESTIC_RECALL_API_URL",
                "https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f",
            ),
            "apiKey": first_env("KATS_DOMESTIC_RECALL_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KATS_DOMESTIC_RECALL_API_KEY|DATAGO_SERVICE_KEY",
            "apiParams": {
                "page": "1",
                "perPage": "10",
                "returnType": "JSON",
            },
        },
        {
            "provider": "KATS 인증제품 정보",
            "runtimeProvider": "KATS_CERTIFIED_PRODUCT_INFO",
            "agent": "regulation-certified-product-reference",
            "endpoint": env("KATS_CERTIFIED_PRODUCT_INFO_SOURCE_URL", "https://www.data.go.kr/data/15040703/fileData.do"),
            "apiKey": first_env("KATS_CERTIFIED_PRODUCT_INFO_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KATS_CERTIFIED_PRODUCT_INFO_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KATS_CERTIFIED_PRODUCT_INFO_FILE_PATH", "data/public/kats_certified_product_info_20220801.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KATS_CERTIFIED_PRODUCT_INFO_FILE_AS_OF", "2022-08-01"),
            "officialUpdatedAt": env("KATS_CERTIFIED_PRODUCT_INFO_OFFICIAL_UPDATED_AT", "2022-08-01"),
            "refreshDueAt": env("KATS_CERTIFIED_PRODUCT_INFO_NEXT_UPDATE_DUE_AT", "2026-08-01"),
            "apiEndpoint": env("KATS_CERTIFIED_PRODUCT_INFO_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "5000",
                "returnType": "JSON",
            },
        },
        {
            "provider": "KATS 인증제품 첨부파일",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "certified-product-attachment-reference",
            "endpoint": env("KATS_CERTIFIED_PRODUCT_ATTACHMENTS_SOURCE_URL", "https://www.data.go.kr/data/15040702/fileData.do"),
            "apiKey": first_env("KATS_CERTIFIED_PRODUCT_ATTACHMENTS_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KATS_CERTIFIED_PRODUCT_ATTACHMENTS_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KATS_CERTIFIED_PRODUCT_ATTACHMENTS_FILE_PATH", "data/public/kats_certified_product_attachments_20230809.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KATS_CERTIFIED_PRODUCT_ATTACHMENTS_FILE_AS_OF", "2023-08-09"),
            "officialUpdatedAt": env("KATS_CERTIFIED_PRODUCT_ATTACHMENTS_OFFICIAL_UPDATED_AT", "2025-09-05"),
            "apiEndpoint": env("KATS_CERTIFIED_PRODUCT_ATTACHMENTS_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "100",
                "returnType": "JSON",
            },
        },
        {
            "provider": "KATS 인증제품 제조공장",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "certified-product-factory-reference",
            "endpoint": env("KATS_CERTIFIED_PRODUCT_FACTORIES_SOURCE_URL", "https://www.data.go.kr/data/15040701/fileData.do"),
            "apiKey": first_env("KATS_CERTIFIED_PRODUCT_FACTORIES_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KATS_CERTIFIED_PRODUCT_FACTORIES_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KATS_CERTIFIED_PRODUCT_FACTORIES_FILE_PATH", "data/public/kats_certified_product_factories_20210823.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KATS_CERTIFIED_PRODUCT_FACTORIES_FILE_AS_OF", "2021-08-23"),
            "officialUpdatedAt": env("KATS_CERTIFIED_PRODUCT_FACTORIES_OFFICIAL_UPDATED_AT", "2025-09-05"),
            "apiEndpoint": env("KATS_CERTIFIED_PRODUCT_FACTORIES_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "100",
                "returnType": "JSON",
            },
        },
        {
            "provider": "KATS 제품안전 민원접수",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "product-safety-petition-reference",
            "endpoint": env("KATS_PRODUCT_SAFETY_PETITIONS_SOURCE_URL", "https://www.data.go.kr/data/15064256/fileData.do"),
            "apiKey": first_env("KATS_PRODUCT_SAFETY_PETITIONS_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KATS_PRODUCT_SAFETY_PETITIONS_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KATS_PRODUCT_SAFETY_PETITIONS_FILE_PATH", "data/public/kats_product_safety_petitions_20250711.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KATS_PRODUCT_SAFETY_PETITIONS_FILE_AS_OF", "2025-07-11"),
            "officialUpdatedAt": env("KATS_PRODUCT_SAFETY_PETITIONS_OFFICIAL_UPDATED_AT", "2025-09-03"),
            "refreshDueAt": env("KATS_PRODUCT_SAFETY_PETITIONS_NEXT_UPDATE_DUE_AT", "2026-08-04"),
            "apiEndpoint": env("KATS_PRODUCT_SAFETY_PETITIONS_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "100",
                "returnType": "JSON",
            },
        },
        {
            "provider": "KATS 국외리콜공표기관",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "recall-agency-reference",
            "endpoint": env("KATS_FOREIGN_RECALL_AGENCY_SOURCE_URL", "https://www.data.go.kr/data/15118082/fileData.do"),
            "apiKey": first_env("KATS_FOREIGN_RECALL_AGENCY_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KATS_FOREIGN_RECALL_AGENCY_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KATS_FOREIGN_RECALL_AGENCY_FILE_PATH", "data/public/kats_foreign_recall_agencies_20230808.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KATS_FOREIGN_RECALL_AGENCY_FILE_AS_OF", "2023-08-08"),
            "officialUpdatedAt": env("KATS_FOREIGN_RECALL_AGENCY_OFFICIAL_UPDATED_AT", "2023-08-08"),
            "apiEndpoint": env("KATS_FOREIGN_RECALL_AGENCY_API_URL", ""),
            "apiParams": {
                "page": "1",
                "perPage": "10",
                "returnType": "JSON",
            },
        },
        {
            "provider": "국가법령정보 현행법령 목록",
            "runtimeProvider": "KOREA_LAW_STATUTES",
            "agent": "regulation-domestic-law-reference",
            "endpoint": env("KOREA_LAW_STATUTES_API_URL", "https://www.law.go.kr/DRF/lawSearch.do"),
            "apiKey": first_env("LAW_OPENAPI_OC"),
            "apiKeyEnv": "LAW_OPENAPI_OC",
            "keyParam": "OC",
            "params": {
                "target": "law",
                "type": "JSON",
                "display": env("KOREA_LAW_STATUTES_DISPLAY", "20"),
                "page": "1",
                "query": env("KOREA_LAW_STATUTES_QUERY", "제품안전 인증"),
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "소비자24 리콜정보_위생용품",
            "runtimeProvider": "CONSUMER_RECALL_SANITATION",
            "agent": "recall-consumer24-sanitation",
            "endpoint": env("CONSUMER_RECALL_API_URL", "https://www.consumer.go.kr/openapi/recall/contents/index.do"),
            "apiKey": first_env("CONSUMER_RECALL_SANITATION_API_KEY"),
            "apiKeyEnv": "CONSUMER_RECALL_SANITATION_API_KEY",
            "params": {
                "pageNo": "1",
                "cntPerPage": env("CONSUMER_RECALL_COUNT_PER_PAGE", "20"),
                "cntntsId": "0208",
                "productNm": args.product_name,
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "소비자24 리콜정보_생활화학제품",
            "runtimeProvider": "CONSUMER_RECALL_CHEMICAL",
            "agent": "recall-consumer24-chemical",
            "endpoint": env("CONSUMER_RECALL_API_URL", "https://www.consumer.go.kr/openapi/recall/contents/index.do"),
            "apiKey": first_env("CONSUMER_RECALL_CHEMICAL_API_KEY"),
            "apiKeyEnv": "CONSUMER_RECALL_CHEMICAL_API_KEY",
            "params": {
                "pageNo": "1",
                "cntPerPage": env("CONSUMER_RECALL_COUNT_PER_PAGE", "20"),
                "cntntsId": "0403",
                "productNm": args.product_name,
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "소비자24 리콜정보_해외리콜",
            "runtimeProvider": "CONSUMER_RECALL_OVERSEAS",
            "agent": "recall-consumer24-overseas",
            "endpoint": env("CONSUMER_RECALL_API_URL", "https://www.consumer.go.kr/openapi/recall/contents/index.do"),
            "apiKey": first_env("CONSUMER_RECALL_OVERSEAS_API_KEY"),
            "apiKeyEnv": "CONSUMER_RECALL_OVERSEAS_API_KEY",
            "params": {
                "pageNo": "1",
                "cntPerPage": env("CONSUMER_RECALL_COUNT_PER_PAGE", "20"),
                "cntntsId": "0501",
                "productNm": args.product_name,
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "한국에너지공단 신재생에너지설비 KS 인증제품",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "renewable-ks-cert-reference",
            "endpoint": env("KEA_RENEWABLE_KS_CERT_API_URL", "https://www.data.go.kr/data/15100512/openapi.do"),
            "apiKey": first_env("KEA_RENEWABLE_KS_CERT_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KEA_RENEWABLE_KS_CERT_API_KEY|DATAGO_SERVICE_KEY",
            "params": {
                "pageNo": "1",
                "numOfRows": "10",
                "type": "json",
            },
            "defaultZeroReason": "CATALOG_PAGE_ENDPOINT",
        },
        {
            "provider": "한국무역보험공사 바이어 검색",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "buyer-search-reference",
            "endpoint": env("KSURE_BUYER_SEARCH_API_URL", "https://apis.data.go.kr/B552696/buyer"),
            "apiKey": first_env("KSURE_BUYER_SEARCH_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KSURE_BUYER_SEARCH_API_KEY|DATAGO_SERVICE_KEY",
            "params": {
                "pageNo": "1",
                "numOfRows": "10",
                "ctryCd": env("KSURE_BUYER_SEARCH_COUNTRY_CODE", "450"),
                "industryCd": env("KSURE_BUYER_SEARCH_INDUSTRY_CODE", "75999"),
                "prodNm": args.product_name,
                "buyerNm": "",
            },
            "filePath": env("KSURE_BUYER_SEARCH_CODEBOOK_PATH", "data/public/ksure_buyer_search_codebook.xlsx"),
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "KIPRISPlus",
            "runtimeProvider": "KIPRIS_FTO",
            "agent": "fto",
            "endpoint": env(
                "KIPRIS_PLUS_OPERATION_URL",
                env("KIPRIS_API_URL", "http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/getAdvancedSearch"),
            ),
            "apiKey": first_env("KIPRIS_PLUS_ACCESSKEY", "KIPRIS_PLUS_KEY", "KIPRIS_API_KEY"),
            "apiKeyEnv": "KIPRIS_PLUS_ACCESSKEY|KIPRIS_PLUS_KEY|KIPRIS_API_KEY",
            "params": {
                "inventionTitle": args.product_name,
                "astrtCont": args.product_name,
                "pageNo": "1",
                "numOfRows": "10",
                "patent": "true",
                "utility": "false",
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "기업마당/Bizinfo",
            "runtimeProvider": "BIZINFO_SUPPORT",
            "agent": "support",
            "endpoint": env("BIZINFO_API_URL", "https://www.exportvoucher.com/"),
            "apiKey": first_env("BIZINFO_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "BIZINFO_API_KEY|DATAGO_SERVICE_KEY",
            "params": {"keyword": args.product_name, "country": args.country},
            "defaultZeroReason": "CATALOG_PAGE_ENDPOINT",
        },
        {
            "provider": "MOTIR 외국인투자기업정보",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "foreign-investment-company-reference",
            "endpoint": env("MOTIR_FOREIGN_INVESTED_COMPANIES_URL", "https://www.data.go.kr/data/15054396/fileData.do"),
            "apiKey": "",
            "apiKeyEnv": "none",
            "params": {},
            "defaultZeroReason": "FILE_DATA_SOURCE_ONLY",
            "fileData": True,
        },
        {
            "provider": "산업통상부 무역구제제도 발생현황",
            "runtimeProvider": "MOTIE_TRADE_REMEDY_OCCURRENCE",
            "agent": "regulation-trade-remedy-occurrence-reference",
            "endpoint": env("MOTIE_TRADE_REMEDY_OCCURRENCE_SOURCE_URL", "https://www.data.go.kr/data/3039967/fileData.do"),
            "apiKey": first_env("MOTIE_TRADE_REMEDY_OCCURRENCE_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "MOTIE_TRADE_REMEDY_OCCURRENCE_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("MOTIE_TRADE_REMEDY_OCCURRENCE_FILE_PATH", "backend/BE-02/src/main/resources/publicdata/motie-trade-remedy-occurrences-20251231.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("MOTIE_TRADE_REMEDY_OCCURRENCE_FILE_AS_OF", "2025-12-31"),
            "officialUpdatedAt": env("MOTIE_TRADE_REMEDY_OCCURRENCE_OFFICIAL_UPDATED_AT", "2025-12-31"),
            "refreshDueAt": env("MOTIE_TRADE_REMEDY_OCCURRENCE_NEXT_UPDATE_DUE_AT", "2026-12-31"),
            "apiEndpoint": env("MOTIE_TRADE_REMEDY_OCCURRENCE_API_URL", "https://api.odcloud.kr/api/3039967/v1/uddi:aaf4da5b-3469-4e43-9e45-ef834a9a2de0_202002211020"),
            "apiParams": {
                "page": "1",
                "perPage": "100",
                "returnType": "JSON",
            },
        },
        {
            "provider": "산업통상부 무역구제 자유무역협정 목록자료",
            "runtimeProvider": "MOTIE_TRADE_REMEDY_FTA_LIST",
            "agent": "market-fta-list-reference",
            "endpoint": env("MOTIE_TRADE_REMEDY_FTA_LIST_SOURCE_URL", "https://www.data.go.kr/data/15117950/fileData.do"),
            "apiKey": first_env("MOTIE_TRADE_REMEDY_FTA_LIST_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "MOTIE_TRADE_REMEDY_FTA_LIST_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("MOTIE_TRADE_REMEDY_FTA_LIST_FILE_PATH", "backend/BE-02/src/main/resources/publicdata/motie-trade-remedy-fta-list-20250825.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("MOTIE_TRADE_REMEDY_FTA_LIST_FILE_AS_OF", "2025-08-25"),
            "officialUpdatedAt": env("MOTIE_TRADE_REMEDY_FTA_LIST_OFFICIAL_UPDATED_AT", "2025-08-25"),
            "refreshDueAt": env("MOTIE_TRADE_REMEDY_FTA_LIST_NEXT_UPDATE_DUE_AT", "2026-08-25"),
            "apiEndpoint": env("MOTIE_TRADE_REMEDY_FTA_LIST_API_URL", "https://api.odcloud.kr/api/15117950/v1/uddi:dd7abd3f-f7fc-4064-9377-ffd563de8d12"),
            "apiParams": {
                "page": "1",
                "perPage": "100",
                "returnType": "JSON",
            },
        },
        {
            "provider": "한국무역보험공사 77개국 업종별 연체율",
            "runtimeProvider": "KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE",
            "agent": "market-country-industry-overdue-reference",
            "endpoint": env("KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_SOURCE_URL", "https://www.data.go.kr/data/15104444/fileData.do"),
            "apiKey": first_env("KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_FILE_PATH", "backend/BE-02/src/main/resources/publicdata/ksure-country-industry-overdue-rates-20211231.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_FILE_AS_OF", "2021-12-31"),
            "officialUpdatedAt": env("KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_OFFICIAL_UPDATED_AT", "2021-12-31"),
            "refreshDueAt": env("KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_NEXT_UPDATE_DUE_AT", "2022-12-31"),
            "apiEndpoint": env("KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_API_URL", "https://api.odcloud.kr/api/15104444/v1/uddi:8deb1668-3932-4323-93bf-fad271cfdb0c"),
            "apiParams": {
                "page": "1",
                "perPage": "1500",
                "returnType": "JSON",
            },
        },
        {
            "provider": "한국산업단지공단 국가산업단지 산업동향정보_가동률",
            "runtimeProvider": "KICOX_OPERATION_RATE",
            "agent": "market-industrial-complex-operation-reference",
            "endpoint": env("KICOX_OPERATION_RATE_SOURCE_URL", "https://www.data.go.kr/data/15085889/fileData.do"),
            "apiKey": first_env("KICOX_OPERATION_RATE_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KICOX_OPERATION_RATE_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KICOX_OPERATION_RATE_FILE_PATH", "backend/BE-02/src/main/resources/publicdata/kicox-national-industrial-complex-operation-rate-20260331.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KICOX_OPERATION_RATE_FILE_AS_OF", "2026-03-31"),
            "officialUpdatedAt": env("KICOX_OPERATION_RATE_OFFICIAL_UPDATED_AT", "2026-03-31"),
            "refreshDueAt": env("KICOX_OPERATION_RATE_NEXT_UPDATE_DUE_AT", "2026-06-30"),
            "apiEndpoint": env("KICOX_OPERATION_RATE_API_URL", "https://api.odcloud.kr/api/15085889/v1/uddi:e32e7d2e-82a3-4cde-9d24-f3e90e2b1eb6"),
            "apiParams": {
                "page": "1",
                "perPage": "100",
                "returnType": "JSON",
            },
        },
        {
            "provider": "한국산업단지공단 공장등록현황통계_시도별 업종별 현황",
            "runtimeProvider": "KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY",
            "agent": "market-factory-registration-reference",
            "endpoint": env("KICOX_FACTORY_REGISTRATION_SOURCE_URL", "https://www.data.go.kr/data/15085508/fileData.do"),
            "apiKey": first_env("KICOX_FACTORY_REGISTRATION_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "KICOX_FACTORY_REGISTRATION_API_KEY|DATAGO_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "FILE_DATA_NOT_CONFIGURED",
            "fileData": True,
            "fileStrategy": "prefer_file_then_api_when_stale",
            "filePath": env("KICOX_FACTORY_REGISTRATION_FILE_PATH", "backend/BE-02/src/main/resources/publicdata/kicox-factory-registration-by-region-industry-20260331.csv"),
            "fileEncodings": ["utf-8-sig", "cp949", "euc-kr"],
            "fileAsOf": env("KICOX_FACTORY_REGISTRATION_FILE_AS_OF", "2026-03-31"),
            "officialUpdatedAt": env("KICOX_FACTORY_REGISTRATION_OFFICIAL_UPDATED_AT", "2026-03-31"),
            "refreshDueAt": env("KICOX_FACTORY_REGISTRATION_NEXT_UPDATE_DUE_AT", "2026-04-30"),
            "apiEndpoint": env("KICOX_FACTORY_REGISTRATION_API_URL", "https://api.odcloud.kr/api/15085508/v1/uddi:b0d2581a-2a88-47a6-af2d-87b575db32d2"),
            "apiParams": {
                "page": "1",
                "perPage": "100",
                "returnType": "JSON",
            },
        },
        {
            "provider": "외부 뉴스/RSS",
            "runtimeProvider": "EXPORT_EVIDENCE_FEED",
            "agent": "news-risk",
            "endpoint": env("PUBLIC_DATA_EXPORT_EVIDENCE_URLS", ""),
            "apiKey": "",
            "apiKeyEnv": "none",
            "params": {"country": args.country, "keyword": args.product_name},
            "defaultZeroReason": "OPERATION_ENDPOINT_NOT_CONFIGURED",
            "feed": True,
        },
        {
            "provider": "KOMSA MTIS",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "maritime-auth-status",
            "endpoint": env("MTIS_AUTH_STATUS_API_URL", "https://mtisopenapi.komsa.or.kr/uat/api/authStatus.do"),
            "apiKey": first_env("MTIS_SERVICE_KEY", "KOMSA_MTIS_SERVICE_KEY"),
            "apiKeyEnv": "MTIS_SERVICE_KEY|KOMSA_MTIS_SERVICE_KEY",
            "params": {},
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
        {
            "provider": "국토교통부 물류창고업등록정보",
            "runtimeProvider": "NOT_RUNTIME_CONNECTED",
            "agent": "logistics-warehouse",
            "endpoint": env("MOLIT_WAREHOUSE_API_URL", "http://apis.data.go.kr/1611000/whsinfoview2/WhsInfoList"),
            "apiKey": first_env("MOLIT_WAREHOUSE_API_KEY", "DATAGO_SERVICE_KEY"),
            "apiKeyEnv": "MOLIT_WAREHOUSE_API_KEY|DATAGO_SERVICE_KEY",
            "params": {
                "pageNo": "1",
                "numOfRows": "10",
                "type": "json",
            },
            "defaultZeroReason": "API_KEY_NOT_CONFIGURED",
        },
    ]


def is_catalog_or_application(endpoint):
    value = (endpoint or "").lower()
    return (
        ("data.go.kr" in value and value.endswith("/openapi.do"))
        or ("data.go.kr" in value and value.endswith("/filedata.do"))
        or ("infuser.odcloud.kr/oas/docs" in value)
        or value.rstrip("/") == "https://api.odcloud.kr/api"
        or ("plus.kipris.or.kr" in value and "/contents.do" in value)
        or ("plus.kipris.or.kr" in value and "/data/main/list.do" in value)
        or value == "https://www.exportvoucher.com/"
        or value == "https://www.safetykorea.kr/release/openapi"
    )


def mask_params(params, has_key, key_param="serviceKey"):
    masked = dict(params)
    if has_key:
        masked[key_param or "serviceKey"] = REDACTED
    return masked


def call_provider(provider, timeout):
    endpoint = provider["endpoint"]
    api_key = provider["apiKey"]
    if provider.get("runtimeProvider") == "NOT_IMPLEMENTED":
        return result(provider, "empty", "CONNECTOR_NOT_IMPLEMENTED")
    if provider.get("feed"):
        return call_feed(provider, timeout)
    if provider.get("fileData"):
        return call_file_data(provider, timeout)
    if not endpoint:
        return result(provider, "empty", "OPERATION_ENDPOINT_NOT_CONFIGURED")
    if is_catalog_or_application(endpoint):
        return result(provider, "empty", provider["defaultZeroReason"])
    if not api_key:
        return result(provider, "empty", "API_KEY_NOT_CONFIGURED")

    params = dict(provider["params"])
    if provider.get("pathKey"):
        suffix = provider.get("pathSuffix", "/{key}").replace("{key}", urllib.parse.quote(api_key, safe=""))
        url = endpoint.rstrip("/") + suffix
    else:
        key_param = provider.get("keyParam", "serviceKey")
        params[key_param] = api_key
        url = endpoint + ("&" if "?" in endpoint else "?") + urllib.parse.urlencode(params)
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            body = response.read().decode("utf-8", errors="replace")
            raw_count = count_records(body)
            return result(
                provider,
                "live" if raw_count > 0 else "empty",
                "" if raw_count > 0 else "EMPTY_LIVE_RESPONSE",
                http_status=response.status,
                raw_count=raw_count,
                normalized_count=raw_count,
                sample_raw_keys=sample_keys(body),
            )
    except urllib.error.HTTPError as exc:
        return result(provider, "fallback", "HTTP_ERROR", http_status=exc.code, error=str(exc))
    except Exception as exc:
        return result(provider, "fallback", "TIMEOUT" if "timed out" in str(exc).lower() else "HTTP_ERROR", error=str(exc))


def call_feed(provider, timeout):
    urls = [url.strip() for url in provider["endpoint"].split(",") if url.strip()]
    if not urls:
        return result(provider, "empty", "OPERATION_ENDPOINT_NOT_CONFIGURED")
    raw_count = 0
    errors = []
    for url in urls[:8]:
        try:
            request = urllib.request.Request(
                url,
                headers={
                    "User-Agent": "GrowGateLocalEX/1.0 export-evidence-feed",
                    "Accept": "application/rss+xml,application/atom+xml,application/json,text/xml;q=0.9,*/*;q=0.1",
                },
            )
            with urllib.request.urlopen(request, timeout=timeout) as response:
                body = response.read().decode("utf-8", errors="replace")
                raw_count += count_records(body)
        except Exception as exc:
            errors.append(f"{url}: {exc}")
    if raw_count == 0:
        return result(provider, "fallback", "FEED_RAW_COUNT_ZERO_OR_FETCH_FAILED", error="; ".join(errors[:3]))
    return result(provider, "external_feed", "", raw_count=raw_count, normalized_count=raw_count)


def call_file_data(provider, timeout):
    file_path = provider.get("filePath") or ""
    if not file_path:
        return result(provider, "empty", provider["defaultZeroReason"])
    if not os.path.exists(file_path):
        return result(provider, "empty", "FILE_DATA_NOT_FOUND", error=file_path)
    if provider.get("fileKind") == "docx":
        return call_docx_file_data(provider, file_path)
    if provider.get("fileKind") == "pdf":
        return call_pdf_file_data(provider, file_path)
    if provider.get("fileKind") == "hwpx":
        return call_hwpx_file_data(provider, file_path)
    errors = []
    for encoding in provider.get("fileEncodings", ["utf-8-sig"]):
        try:
            with open(file_path, newline="", encoding=encoding) as handle:
                reader = csv.DictReader(handle)
                rows = list(reader)
            count = len(rows)
            keys = list(rows[0].keys())[:12] if rows else list(reader.fieldnames or [])[:12]
            refresh_required = is_refresh_required(provider)
            if (
                provider.get("fileStrategy") == "prefer_file_then_api_when_stale"
                and refresh_required
                and provider.get("apiEndpoint")
                and provider.get("apiKey")
            ):
                api_result = call_operation_endpoint(provider, provider["apiEndpoint"], provider["apiKey"], provider.get("apiParams", {}), timeout)
                if api_result["sourceType"] == "cache" and api_result["rawCount"] == 0:
                    api_result["rawCount"] = count
                    api_result["normalizedCount"] = count
                    api_result["filteredCount"] = count
                    api_result["sampleRawKeys"] = keys
                api_result["refreshRequired"] = True
                api_result["fileAsOf"] = provider.get("fileAsOf", "")
                api_result["officialUpdatedAt"] = provider.get("officialUpdatedAt", "")
                api_result["filePath"] = file_path
                api_result["cacheRawCount"] = count
                return api_result
            zero_reason = ""
            if provider.get("fileStrategy") == "prefer_file_then_api_when_stale" and refresh_required:
                zero_reason = "CSV_CACHE_STALE_API_KEY_NOT_CONFIGURED" if not provider.get("apiKey") else "CSV_CACHE_STALE_API_REFRESH_SKIPPED"
            return result(
                provider,
                "cache" if count > 0 else "empty",
                zero_reason if count > 0 else "FILE_DATA_EMPTY",
                raw_count=count,
                normalized_count=count,
                sample_raw_keys=keys,
                metadata={
                    "refreshRequired": refresh_required,
                    "refreshPolicy": provider.get("fileStrategy", ""),
                    "fileAsOf": provider.get("fileAsOf", ""),
                    "officialUpdatedAt": provider.get("officialUpdatedAt", ""),
                    "refreshDueAt": provider.get("refreshDueAt", ""),
                    "cacheRawCount": count,
                },
            )
        except UnicodeDecodeError as exc:
            errors.append(f"{encoding}: {exc}")
        except Exception as exc:
            return result(provider, "fallback", "FILE_DATA_READ_ERROR", error=str(exc))
    return result(provider, "fallback", "FILE_DATA_ENCODING_ERROR", error="; ".join(errors))


def call_docx_file_data(provider, file_path):
    try:
        import zipfile
        with zipfile.ZipFile(file_path) as archive:
            names = archive.namelist()
        valid = "word/document.xml" in names
        return result(
            provider,
            "cache" if valid else "fallback",
            "" if valid else "DOCX_DOCUMENT_XML_MISSING",
            raw_count=1 if valid else 0,
            normalized_count=1 if valid else 0,
            sample_raw_keys=["docx", "word/document.xml"],
            metadata={
                "fileAsOf": provider.get("fileAsOf", ""),
                "officialUpdatedAt": provider.get("officialUpdatedAt", ""),
                "knowledgeUse": "terminology_dictionary",
                "documentPartCount": len(names),
            },
        )
    except Exception as exc:
        return result(provider, "fallback", "DOCX_READ_ERROR", error=str(exc))


def call_pdf_file_data(provider, file_path):
    try:
        with open(file_path, "rb") as handle:
            header = handle.read(5)
            handle.seek(0)
            data = handle.read()
        valid = header == b"%PDF-"
        page_count = len(re.findall(rb"/Type\s*/Page\b", data))
        return result(
            provider,
            "cache" if valid else "fallback",
            "" if valid else "PDF_HEADER_MISSING",
            raw_count=page_count if valid and page_count > 0 else 1 if valid else 0,
            normalized_count=page_count if valid and page_count > 0 else 1 if valid else 0,
            sample_raw_keys=["pdf", "pages", "sha256"],
            metadata={
                "fileAsOf": provider.get("fileAsOf", ""),
                "officialUpdatedAt": provider.get("officialUpdatedAt", ""),
                "knowledgeUse": "fta_status_context",
                "sha256": sha256_bytes(data),
                "pageCount": page_count if page_count > 0 else 1 if valid else 0,
            },
        )
    except Exception as exc:
        return result(provider, "fallback", "PDF_READ_ERROR", error=str(exc))


def call_hwpx_file_data(provider, file_path):
    try:
        import zipfile
        with open(file_path, "rb") as handle:
            data = handle.read()
        with zipfile.ZipFile(file_path) as archive:
            names = archive.namelist()
            sections = [name for name in names if name.startswith("Contents/section") and name.endswith(".xml")]
            images = [name for name in names if name.startswith("BinData/")]
            sample_texts = []
            for name in sections[:3]:
                try:
                    root = ET.fromstring(archive.read(name))
                except ET.ParseError:
                    continue
                for element in root.iter():
                    if element.text and element.text.strip():
                        sample_texts.append(element.text.strip())
                    if len(sample_texts) >= 12:
                        break
                if len(sample_texts) >= 12:
                    break
        valid = "mimetype" in names and bool(sections)
        return result(
            provider,
            "cache" if valid else "fallback",
            "" if valid else "HWPX_SECTION_XML_MISSING",
            raw_count=len(sections) if valid else 0,
            normalized_count=len(sections) if valid else 0,
            sample_raw_keys=["hwpx", "Contents/section*.xml", "sha256"],
            metadata={
                "fileAsOf": provider.get("fileAsOf", ""),
                "officialUpdatedAt": provider.get("officialUpdatedAt", ""),
                "refreshDueAt": provider.get("refreshDueAt", ""),
                "knowledgeUse": "industrial_energy_ghg_context",
                "sha256": sha256_bytes(data),
                "sectionCount": len(sections),
                "imageCount": len(images),
                "sampleText": sample_texts[:12],
            },
        )
    except Exception as exc:
        return result(provider, "fallback", "HWPX_READ_ERROR", error=str(exc))


def sha256_bytes(data):
    import hashlib
    return hashlib.sha256(data).hexdigest()


def is_refresh_required(provider):
    refresh_due_at = parse_date(provider.get("refreshDueAt", ""))
    if refresh_due_at:
        return dt.date.today() >= refresh_due_at
    file_as_of = parse_date(provider.get("fileAsOf", ""))
    official_updated_at = parse_date(provider.get("officialUpdatedAt", ""))
    return bool(file_as_of and official_updated_at and file_as_of < official_updated_at)


def parse_date(value):
    try:
        return dt.date.fromisoformat(value)
    except Exception:
        return None


def call_operation_endpoint(provider, endpoint, api_key, params, timeout):
    request_params = dict(params)
    key_param = provider.get("keyParam", "serviceKey")
    request_params[key_param] = api_key
    url = endpoint + ("&" if "?" in endpoint else "?") + urllib.parse.urlencode(request_params)
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            body = response.read().decode("utf-8", errors="replace")
            raw_count = count_records(body)
            return result(
                provider,
                "live" if raw_count > 0 else "empty",
                "" if raw_count > 0 else "EMPTY_LIVE_RESPONSE",
                http_status=response.status,
                raw_count=raw_count,
                normalized_count=raw_count,
                sample_raw_keys=sample_keys(body),
                endpoint=endpoint,
                operation_endpoint=True,
                request_params=request_params,
            )
    except urllib.error.HTTPError as exc:
        return result(
            provider,
            "cache",
            "CSV_CACHE_STALE_API_HTTP_ERROR",
            http_status=exc.code,
            error=str(exc),
            endpoint=endpoint,
            operation_endpoint=True,
            request_params=request_params,
        )
    except Exception as exc:
        reason = "CSV_CACHE_STALE_API_TIMEOUT" if "timed out" in str(exc).lower() else "CSV_CACHE_STALE_API_ERROR"
        return result(
            provider,
            "cache",
            reason,
            error=str(exc),
            endpoint=endpoint,
            operation_endpoint=True,
            request_params=request_params,
        )


def count_records(body):
    text = body or ""
    if not text.strip():
        return 0
    try:
        parsed = json.loads(text)
        return max_array_len(parsed)
    except Exception:
        pass
    try:
        root = ET.fromstring(text)
        return len(root.findall(".//item")) + len(root.findall(".//row")) + len(root.findall(".//record"))
    except Exception:
        return len(re.findall(r"<(?:item|entry|row|record)\b", text, flags=re.I))


def max_array_len(value):
    if isinstance(value, list):
        return max([len(value)] + [max_array_len(item) for item in value])
    if isinstance(value, dict):
        return max([0] + [max_array_len(item) for item in value.values()])
    return 0


def sample_keys(body):
    try:
        parsed = json.loads(body)
        if isinstance(parsed, dict):
            return list(parsed.keys())[:12]
    except Exception:
        pass
    return list(dict.fromkeys(re.findall(r"<([A-Za-z][A-Za-z0-9_:-]*)\b", body or "")[:12]))


def result(provider, source_type, zero_reason, http_status=None, raw_count=0, normalized_count=0,
           sample_raw_keys=None, error="", metadata=None, endpoint=None, operation_endpoint=None,
           request_params=None):
    effective_endpoint = endpoint if endpoint is not None else provider["endpoint"]
    effective_params = request_params if request_params is not None else provider["params"]
    return {
        "provider": provider["provider"],
        "runtimeProvider": provider["runtimeProvider"],
        "agent": provider["agent"],
        "endpoint": effective_endpoint or "not-configured",
        "operationEndpoint": bool(effective_endpoint) and not provider.get("fileData") and not is_catalog_or_application(effective_endpoint)
        if operation_endpoint is None else operation_endpoint,
        "apiKeyEnv": provider["apiKeyEnv"],
        "apiKeyConfigured": bool(provider["apiKey"]),
        "requestParamsMasked": mask_params(effective_params, bool(provider["apiKey"]), provider.get("keyParam", "serviceKey")),
        "filePath": provider.get("filePath", ""),
        "httpStatus": http_status,
        "rawCount": raw_count,
        "normalizedCount": normalized_count,
        "filteredCount": normalized_count,
        "sourceType": source_type,
        "zeroReason": zero_reason,
        "sampleRawKeys": sample_raw_keys or [],
        "fallbackUsed": source_type == "fallback",
        "checkedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
        "error": sanitize(error),
        **(metadata or {}),
    }


def sanitize(value):
    return re.sub(
        r"(?i)(serviceKey|servicekey|apiKey|apikey|key|token|access_token|client_secret)=([^&\s]+)",
        r"\1=[REDACTED]",
        value or "",
    )


def print_table(rows):
    headers = ["Provider", "Runtime", "Agent", "sourceType", "raw", "normalized", "zeroReason", "operationEndpoint", "apiKey"]
    print("| " + " | ".join(headers) + " |")
    print("| " + " | ".join(["---"] * len(headers)) + " |")
    for row in rows:
        print(
            "| {provider} | {runtimeProvider} | {agent} | {sourceType} | {rawCount} | {normalizedCount} | "
            "{zeroReason} | {operationEndpoint} | {apiKeyConfigured} |".format(**row)
        )


def main():
    parser = argparse.ArgumentParser(description="Check public/external data providers and explain zero-result causes.")
    parser.add_argument("--hs-code", default="850760")
    parser.add_argument("--country", default="DE")
    parser.add_argument("--product-name", default="리튬이온 배터리팩")
    parser.add_argument("--timeout", type=int, default=10)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    rows = [call_provider(provider, args.timeout) for provider in providers(args)]
    if args.json:
        print(json.dumps(rows, ensure_ascii=False, indent=2))
    else:
        print_table(rows)
        print("\nUse --json for masked request params, sample raw keys, and error details.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
