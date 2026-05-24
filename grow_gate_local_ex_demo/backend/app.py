"""FastAPI backend for GROW-GATE Local EX demo.

Run:
    uvicorn backend.app:app --reload --host 127.0.0.1 --port 8000
"""
from __future__ import annotations

import os
import subprocess
from pathlib import Path
from typing import Any, Dict

from fastapi import FastAPI, HTTPException, Query, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .agents import GrowGateAgent
from .data_connectors import PipelineJob, load_dataset, load_public_data_sources
from .public_data_pipeline import PublicDataPipeline, ROOT, run_and_write
from .schemas import AnalysisResult, GitCommitRequest, GitInitRequest, ProductInput


def _cors_origins() -> list[str]:
    configured = os.getenv("GROW_GATE_CORS_ORIGINS")
    if configured:
        return [origin.strip() for origin in configured.split(",") if origin.strip()]
    return [
        "http://127.0.0.1:8000",
        "http://localhost:8000",
        "http://127.0.0.1:8765",
        "http://localhost:8765",
        "null",
    ]


def _safe_output_path(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(ROOT.resolve()))
    except ValueError:
        return path.name


PROJECT_ROOT = ROOT.parent
LOCAL_CLIENTS = {"127.0.0.1", "::1", "localhost"}


def _require_local_request(request: Request) -> None:
    client_host = request.client.host if request.client else ""
    if client_host not in LOCAL_CLIENTS:
        raise HTTPException(status_code=403, detail="Git operations are only available from localhost.")


def _run_git(args: list[str], timeout: int = 30) -> Dict[str, Any]:
    command = ["git", *args]
    try:
        completed = subprocess.run(
            command,
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
    except subprocess.TimeoutExpired as exc:
        return {
            "ok": False,
            "action": args[0] if args else "git",
            "command": command,
            "cwd": str(PROJECT_ROOT),
            "stdout": exc.stdout or "",
            "stderr": exc.stderr or "Command timed out.",
            "returncode": 124,
            "hint": "Git command timed out. Check network/authentication or run the command manually.",
        }
    return {
        "ok": completed.returncode == 0,
        "action": args[0] if args else "git",
        "command": command,
        "cwd": str(PROJECT_ROOT),
        "stdout": completed.stdout.strip(),
        "stderr": completed.stderr.strip(),
        "returncode": completed.returncode,
    }


def _is_git_repo() -> bool:
    return bool(_run_git(["rev-parse", "--is-inside-work-tree"])["ok"])


def _safe_project_paths(paths: list[str]) -> list[str]:
    safe_paths: list[str] = []
    for item in paths:
        candidate = (PROJECT_ROOT / item).resolve()
        try:
            candidate.relative_to(PROJECT_ROOT.resolve())
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=f"Unsafe git path: {item}") from exc
        safe_paths.append(item)
    return safe_paths

app = FastAPI(
    title="GROW-GATE Local EX API",
    description="지역 제조기업 수출규제·인증 실행 AI 에이전트 시연 API",
    version="0.1.0-demo",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_origins(),
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Content-Type", "Accept"],
)

agent = GrowGateAgent()


@app.middleware("http")
async def add_security_headers(request, call_next) -> Response:
    response: Response = await call_next(request)
    response.headers.setdefault("X-Content-Type-Options", "nosniff")
    response.headers.setdefault("Referrer-Policy", "no-referrer")
    response.headers.setdefault("X-Frame-Options", "DENY")
    response.headers.setdefault("Cache-Control", "no-store")
    return response


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok", "service": "GROW-GATE Local EX"}


@app.get("/datasets")
def datasets() -> Dict[str, Any]:
    """Return available demo dataset metadata and counts."""
    data = load_dataset()
    return {
        "metadata": data.get("metadata", {}),
        "counts": {
            "markets": len(data.get("markets", [])),
            "regulations": len(data.get("regulations", [])),
            "recalls": len(data.get("recalls", [])),
            "patents": len(data.get("patents", [])),
            "support_programs": len(data.get("support_programs", [])),
            "regions": len(data.get("regions", [])),
        },
    }


@app.get("/pipeline/preview")
def pipeline_preview() -> Dict[str, Any]:
    """Show ETL/RAG-ready normalized documents."""
    payload = PipelineJob().run()
    return {
        "regulation_documents": payload["regulation_documents"][:3],
        "recall_documents": payload["recall_documents"][:3],
        "screen_data_map": payload["screen_data_map"],
        "public_data_sources": payload["public_data_sources"],
        "total_regulation_documents": len(payload["regulation_documents"]),
        "total_recall_documents": len(payload["recall_documents"]),
    }


@app.get("/public-data/sources")
def public_data_sources() -> Dict[str, Any]:
    """Return the screen-by-screen public-data source catalog."""
    return load_public_data_sources()


@app.post("/public-data/collect")
def collect_public_data(use_live: bool = Query(default=False)) -> Dict[str, Any]:
    """Run public-data collection and normalization.

    `use_live=false` keeps the demo deterministic and writes seed-backed public
    data. `use_live=true` attempts official sources when keys are configured.
    """
    if use_live and os.getenv("GROW_GATE_ENABLE_LIVE_COLLECTION") != "1":
        raise HTTPException(
            status_code=403,
            detail="Live public-data collection is disabled. Set GROW_GATE_ENABLE_LIVE_COLLECTION=1 to enable it.",
        )
    output_path, payload = run_and_write(use_live=use_live)
    return {
        "output_path": _safe_output_path(output_path),
        "metadata": payload["metadata"],
        "counts": {key: len(value) for key, value in payload.items() if isinstance(value, list)},
        "api_call_log": payload["api_call_log"],
        "evidence_source": payload["evidence_source"],
    }


@app.get("/public-data/normalized")
def public_data_normalized() -> Dict[str, Any]:
    """Preview normalized public data without writing a file."""
    return PublicDataPipeline().run(use_live=False)


@app.get("/git/status")
def git_status(request: Request) -> Dict[str, Any]:
    """Return local Git status for the project root."""
    _require_local_request(request)
    if not _is_git_repo():
        return {
            "ok": False,
            "is_repo": False,
            "project_root": str(PROJECT_ROOT),
            "hint": "This folder is not a Git repository. Initialize it before committing.",
        }
    status = _run_git(["status", "--short"])
    branch = _run_git(["branch", "--show-current"])
    remote = _run_git(["remote", "-v"])
    return {
        "ok": status["ok"],
        "is_repo": True,
        "project_root": str(PROJECT_ROOT),
        "branch": branch["stdout"],
        "status": status,
        "remote": remote,
    }


@app.post("/git/init")
def git_init(payload: GitInitRequest, request: Request) -> Dict[str, Any]:
    """Initialize local Git and connect the GitHub remote.

    This does not store credentials. Push succeeds only when GitHub auth is
    already configured on the user's machine.
    """
    _require_local_request(request)
    steps: list[Dict[str, Any]] = []
    if not _is_git_repo():
        steps.append(_run_git(["init"]))
    steps.append(_run_git(["branch", "-M", payload.branch]))
    remote_check = _run_git(["remote", "get-url", "origin"])
    if remote_check["ok"]:
        steps.append(_run_git(["remote", "set-url", "origin", payload.remote_url]))
    else:
        steps.append(_run_git(["remote", "add", "origin", payload.remote_url]))
    return {
        "ok": all(step["ok"] for step in steps),
        "project_root": str(PROJECT_ROOT),
        "remote_url": payload.remote_url,
        "branch": payload.branch,
        "steps": steps,
        "hint": "If push returns 403, authenticate with GitHub locally and retry push.",
    }


@app.post("/git/commit")
def git_commit(payload: GitCommitRequest, request: Request) -> Dict[str, Any]:
    """Stage safe project paths and create a local commit."""
    _require_local_request(request)
    if not _is_git_repo():
        raise HTTPException(status_code=409, detail="This folder is not a Git repository. Run /git/init first.")
    include_paths = _safe_project_paths(payload.include_paths)
    steps: list[Dict[str, Any]] = []
    steps.append(_run_git(["add", "--", *include_paths]))
    if not steps[-1]["ok"]:
        return {"ok": False, "steps": steps, "hint": "Failed while staging files."}
    status = _run_git(["status", "--short"])
    steps.append(status)
    if status["ok"] and not status["stdout"]:
        return {"ok": True, "committed": False, "steps": steps, "hint": "No changes to commit."}
    steps.append(_run_git(["commit", "-m", payload.message]))
    return {
        "ok": steps[-1]["ok"],
        "committed": steps[-1]["ok"],
        "message": payload.message,
        "steps": steps,
        "hint": None if steps[-1]["ok"] else "Commit failed. Check Git user.name/user.email or resolve the error shown.",
    }


@app.post("/git/push")
def git_push(request: Request, branch: str = Query(default="main", pattern=r"^[A-Za-z0-9._/-]+$")) -> Dict[str, Any]:
    """Push the current branch to GitHub using local credentials."""
    _require_local_request(request)
    if not _is_git_repo():
        raise HTTPException(status_code=409, detail="This folder is not a Git repository. Run /git/init first.")
    result = _run_git(["push", "-u", "origin", branch], timeout=60)
    result["hint"] = None if result["ok"] else "Push failed. GitHub authentication or repository write permission is required."
    return result


@app.post("/analyze", response_model=AnalysisResult)
def analyze(request: ProductInput) -> AnalysisResult:
    """Run the end-to-end AI Agent analysis."""
    return agent.analyze(request)


@app.post("/report")
def report(request: ProductInput) -> JSONResponse:
    """Return a printable report payload. The frontend can render this to PDF."""
    result = agent.analyze(request)
    markdown = build_markdown_report(result)
    return JSONResponse({"markdown": markdown, "result": result.model_dump()})


def build_markdown_report(result: AnalysisResult) -> str:
    lines = [
        f"# GROW-GATE Local EX 수출 준비도 리포트",
        "",
        f"- 기업: {result.request.company_name}",
        f"- 지역/업종: {result.request.region} / {result.request.industry}",
        f"- 제품/HS Code: {result.request.product_name} / {result.request.hs_code}",
        f"- 대상 시장: {result.request.target_market}",
        f"- 준비도 점수: {result.readiness_score}/100 ({result.readiness_level})",
        "",
        "## AI 요약",
        result.ai_summary,
        "",
        "## 규제·인증 우선순위",
    ]
    for item in result.regulations[:5]:
        lines.append(f"- [{item.priority.upper()}] {item.title}: {item.action}")
    lines.extend(["", "## 30·60·90일 실행 로드맵"])
    for item in result.roadmap:
        lines.append(f"### {item.day_bucket} - {item.title}")
        for task in item.tasks:
            lines.append(f"- {task}")
        lines.append(f"- 산출물: {item.output}")
    lines.extend(["", "## 유의사항"])
    for guardrail in result.guardrails:
        lines.append(f"- {guardrail}")
    return "\n".join(lines)
