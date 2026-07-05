#!/usr/bin/env python3
"""Collect KIPRIS Plus catalog pages and optionally smoke-test one operation.

The script never writes API keys. It stores redacted request metadata, page
checksums, and parsed catalog hints so KIPRIS evidence can be distinguished
from live operation results.
"""

import argparse
import datetime as dt
import hashlib
import html
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request


REDACTED = "[REDACTED]"
CATALOG_URL = os.environ.get("KIPRIS_PLUS_CATALOG_BASE_URL", "https://plus.kipris.or.kr/portal/data/main/List.do")
CATALOG_MENU_NO = os.environ.get("KIPRIS_PLUS_CATALOG_MENU_NO", "210103")
DEFAULT_OPERATION_URL = "http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/getAdvancedSearch"
KEY_ENV_NAMES = ("KIPRIS_PLUS_ACCESSKEY", "KIPRIS_PLUS_KEY", "KIPRIS_API_KEY")


def env_first(*names):
    for name in names:
        value = os.environ.get(name, "")
        if value:
            return value
    return ""


def catalog_url(page):
    query = {
        "menuNo": CATALOG_MENU_NO,
        "searchCondition": "",
        "clasKeyword": "",
        "orderTp": "",
        "frUseYn": "",
        "bulkfreeyn": "",
        "subTab": "",
        "pageIndex": str(page),
    }
    return CATALOG_URL + "?" + urllib.parse.urlencode(query)


def fetch(url, timeout):
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "GrowGateLocalEX/1.0 kipris-catalog-collector",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,application/json;q=0.8,*/*;q=0.1",
        },
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read()
        text = body.decode(response.headers.get_content_charset() or "utf-8", errors="replace")
        return response.status, text


def parse_catalog_page(text):
    total_match = re.search(r"총\s*([0-9,]+)\s*건", text)
    total_count = int(total_match.group(1).replace(",", "")) if total_match else None
    if total_count and total_count > 1000:
        total_count = None
    anchors = []
    for match in re.finditer(r"<a\b([^>]*)>(.*?)</a>", text, flags=re.I | re.S):
        attrs, label = match.groups()
        href_match = re.search(r"href=[\"']([^\"']+)[\"']", attrs, flags=re.I)
        label_text = clean_text(label)
        if not href_match:
            continue
        href = html.unescape(href_match.group(1))
        absolute = urllib.parse.urljoin(CATALOG_URL, href)
        if is_catalog_candidate(absolute, label_text):
            anchors.append({"label": label_text, "url": absolute})
    product_like_text = []
    for line in clean_text(text).splitlines():
        if re.search(r"\b(API|BULK|Open API|특허|상표|디자인|심판|공보)\b", line, flags=re.I):
            product_like_text.append(line[:300])
    return {
        "totalCount": total_count,
        "candidateLinks": dedupe_links(anchors),
        "catalogHints": product_like_text[:60],
    }


def clean_text(value):
    value = re.sub(r"<script\b.*?</script>", " ", value, flags=re.I | re.S)
    value = re.sub(r"<style\b.*?</style>", " ", value, flags=re.I | re.S)
    value = re.sub(r"<[^>]+>", " ", value)
    value = html.unescape(value)
    lines = [re.sub(r"\s+", " ", line).strip() for line in value.splitlines()]
    return "\n".join(line for line in lines if line)


def is_catalog_candidate(url, label):
    lowered = url.lower()
    if "logout" in lowered or "login" in lowered:
        return False
    if any(token in lowered for token in ("/data/", "detail", "list.do", "view.do")):
        return True
    return bool(label and re.search(r"\b(API|BULK|특허|상표|디자인|심판|공보)\b", label, flags=re.I))


def dedupe_links(links):
    seen = set()
    result = []
    for link in links:
        key = (link["url"], link["label"])
        if key in seen:
            continue
        seen.add(key)
        result.append(link)
    return result


def sha256(text):
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def operation_smoke(args):
    operation_url = os.environ.get("KIPRIS_PLUS_OPERATION_URL", DEFAULT_OPERATION_URL).strip()
    api_key = env_first(*KEY_ENV_NAMES)
    if not operation_url:
        return {"attempted": False, "reason": "KIPRIS_PLUS_OPERATION_URL_NOT_CONFIGURED"}
    if not api_key:
        return {"attempted": False, "reason": "KIPRIS_PLUS_ACCESSKEY_NOT_CONFIGURED"}

    params = {}
    for pair in args.operation_param:
        if "=" not in pair:
            continue
        key, value = pair.split("=", 1)
        params[key] = value
    params[args.key_param] = api_key
    url = operation_url + ("&" if "?" in operation_url else "?") + urllib.parse.urlencode(params)
    try:
        status, body = fetch(url, args.timeout)
        return {
            "attempted": True,
            "endpoint": operation_url,
            "requestParamsMasked": {**{k: v for k, v in params.items() if k != args.key_param}, args.key_param: REDACTED},
            "httpStatus": status,
            "rawSha256": sha256(body),
            "rawCount": count_records(body),
            "sampleKeys": sample_keys(body),
        }
    except urllib.error.HTTPError as exc:
        return {
            "attempted": True,
            "endpoint": operation_url,
            "requestParamsMasked": {**{k: v for k, v in params.items() if k != args.key_param}, args.key_param: REDACTED},
            "httpStatus": exc.code,
            "error": sanitize(str(exc)),
        }
    except Exception as exc:
        return {
            "attempted": True,
            "endpoint": operation_url,
            "requestParamsMasked": {**{k: v for k, v in params.items() if k != args.key_param}, args.key_param: REDACTED},
            "error": sanitize(str(exc)),
        }


def count_records(text):
    try:
        parsed = json.loads(text)
        return max_array_len(parsed)
    except Exception:
        pass
    return len(re.findall(r"<(?:item|entry|row|record)\b", text or "", flags=re.I))


def max_array_len(value):
    if isinstance(value, list):
        return max([len(value)] + [max_array_len(item) for item in value])
    if isinstance(value, dict):
        return max([0] + [max_array_len(item) for item in value.values()])
    return 0


def sample_keys(text):
    try:
        parsed = json.loads(text)
        if isinstance(parsed, dict):
            return list(parsed.keys())[:12]
    except Exception:
        pass
    return list(dict.fromkeys(re.findall(r"<([A-Za-z][A-Za-z0-9_:-]*)\b", text or "")[:12]))


def sanitize(value):
    value = value or ""
    for name in KEY_ENV_NAMES:
        secret = os.environ.get(name, "")
        if secret:
            value = value.replace(secret, REDACTED)
    return re.sub(r"(?i)(accessKey|serviceKey|apiKey|key|token)=([^&\s]+)", r"\1=[REDACTED]", value)


def main():
    parser = argparse.ArgumentParser(description="Collect KIPRIS Plus catalog page metadata without storing API keys.")
    parser.add_argument("--start-page", type=int, default=int(os.environ.get("KIPRIS_PLUS_CATALOG_START_PAGE", "1")))
    parser.add_argument("--end-page", type=int, default=int(os.environ.get("KIPRIS_PLUS_CATALOG_END_PAGE", "13")))
    parser.add_argument("--timeout", type=int, default=15)
    parser.add_argument("--output", default=os.environ.get("KIPRIS_PLUS_CATALOG_OUTPUT", "data/public/kipris_plus_catalog_pages_1_13.json"))
    parser.add_argument("--expected-total-count", type=int, default=int(os.environ.get("KIPRIS_PLUS_CATALOG_EXPECTED_TOTAL_COUNT", "144")))
    parser.add_argument("--key-param", default=os.environ.get("KIPRIS_PLUS_KEY_PARAM", "ServiceKey"))
    parser.add_argument(
        "--operation-param",
        action="append",
        default=[
            "inventionTitle=센서",
            "astrtCont=발명",
            "pageNo=1",
            "numOfRows=10",
            "patent=true",
            "utility=false",
        ],
        help="Optional operation smoke param in key=value form.",
    )
    args = parser.parse_args()

    pages = []
    for page in range(args.start_page, args.end_page + 1):
        url = catalog_url(page)
        try:
            status, text = fetch(url, args.timeout)
            parsed = parse_catalog_page(text)
            pages.append(
                {
                    "pageIndex": page,
                    "url": url,
                    "httpStatus": status,
                    "htmlSha256": sha256(text),
                    "totalCount": parsed["totalCount"],
                    "candidateLinkCount": len(parsed["candidateLinks"]),
                    "candidateLinks": parsed["candidateLinks"],
                    "catalogHints": parsed["catalogHints"],
                }
            )
        except Exception as exc:
            pages.append(
                {
                    "pageIndex": page,
                    "url": url,
                    "httpStatus": None,
                    "error": sanitize(str(exc)),
                }
            )

    payload = {
        "provider": "KIPRIS Plus",
        "sourceType": "cache" if any(page.get("httpStatus") == 200 for page in pages) else "fallback",
        "endpointType": "kipris-plus-catalog-page",
        "checkedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
        "range": {"startPage": args.start_page, "endPage": args.end_page},
        "officialUrl": catalog_url(args.start_page),
        "termsUrl": "https://plus.kipris.or.kr/portal/main/contents.do?menuNo=200031",
        "expectedTotalCount": args.expected_total_count,
        "apiKeyEnv": "|".join(KEY_ENV_NAMES),
        "apiKeyConfigured": bool(env_first(*KEY_ENV_NAMES)),
        "apiKeyStored": False,
        "catalogTotalCount": next((page.get("totalCount") for page in pages if page.get("totalCount") is not None), None),
        "pages": pages,
        "operationSmoke": operation_smoke(args),
        "usageWarning": "KIPRIS Plus catalog/API results are preliminary public IP data signals only. They are not an infringement, validity, or final FTO legal opinion.",
    }
    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
    print(json.dumps({"output": args.output, "pages": len(pages), "apiKeyStored": False}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
