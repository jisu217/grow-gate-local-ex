#!/usr/bin/env python3
import argparse
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
            "provider": "KIPRISPlus",
            "runtimeProvider": "KIPRIS_FTO",
            "agent": "fto",
            "endpoint": env("KIPRIS_API_URL", "https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300024"),
            "apiKey": first_env("KIPRIS_API_KEY", "KIPRIS_PLUS_KEY", "KIPRIS_PLUS_ACCESSKEY"),
            "apiKeyEnv": "KIPRIS_API_KEY|KIPRIS_PLUS_KEY|KIPRIS_PLUS_ACCESSKEY",
            "params": {"keyword": args.product_name, "country": args.country},
            "defaultZeroReason": "CATALOG_PAGE_ENDPOINT",
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
        or ("plus.kipris.or.kr" in value and "/contents.do" in value)
        or value == "https://www.exportvoucher.com/"
        or value == "https://www.safetykorea.kr/release/openapi"
    )


def mask_params(params, has_key):
    masked = dict(params)
    if has_key:
        masked["serviceKey"] = REDACTED
    return masked


def call_provider(provider, timeout):
    endpoint = provider["endpoint"]
    api_key = provider["apiKey"]
    if provider.get("runtimeProvider") == "NOT_IMPLEMENTED":
        return result(provider, "empty", "CONNECTOR_NOT_IMPLEMENTED")
    if provider.get("feed"):
        return call_feed(provider, timeout)
    if not endpoint:
        return result(provider, "empty", "OPERATION_ENDPOINT_NOT_CONFIGURED")
    if is_catalog_or_application(endpoint):
        return result(provider, "empty", provider["defaultZeroReason"])
    if not api_key:
        return result(provider, "empty", "API_KEY_NOT_CONFIGURED")

    params = dict(provider["params"])
    params["serviceKey"] = api_key
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
           sample_raw_keys=None, error=""):
    return {
        "provider": provider["provider"],
        "runtimeProvider": provider["runtimeProvider"],
        "agent": provider["agent"],
        "endpoint": provider["endpoint"] or "not-configured",
        "operationEndpoint": bool(provider["endpoint"]) and not is_catalog_or_application(provider["endpoint"]),
        "apiKeyEnv": provider["apiKeyEnv"],
        "apiKeyConfigured": bool(provider["apiKey"]),
        "requestParamsMasked": mask_params(provider["params"], bool(provider["apiKey"])),
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
