"""Optional external LLM inference gateway.

The default path is disabled so the demo remains deterministic. When a provider
is configured through environment variables, the gateway calls an external
inference API and falls back to the rule-based summary on errors.
"""
from __future__ import annotations

import os
import time
from dataclasses import dataclass
from typing import Any, Dict, List

import httpx


@dataclass(frozen=True)
class ExternalLLMConfig:
    provider: str = "disabled"
    hf_space_url: str = ""
    hf_token: str = ""
    groq_api_key: str = ""
    groq_model: str = "llama-3.1-8b-instant"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.0-flash"
    timeout_seconds: float = 12.0

    @classmethod
    def from_env(cls) -> "ExternalLLMConfig":
        return cls(
            provider=os.getenv("GROW_GATE_LLM_PROVIDER", "disabled").strip().lower() or "disabled",
            hf_space_url=os.getenv("GROW_GATE_HF_SPACE_URL", "").strip(),
            hf_token=os.getenv("GROW_GATE_HF_TOKEN", "").strip(),
            groq_api_key=os.getenv("GROQ_API_KEY", "").strip(),
            groq_model=os.getenv("GROW_GATE_GROQ_MODEL", "llama-3.1-8b-instant").strip(),
            gemini_api_key=os.getenv("GEMINI_API_KEY", "").strip(),
            gemini_model=os.getenv("GROW_GATE_GEMINI_MODEL", "gemini-2.0-flash").strip(),
            timeout_seconds=float(os.getenv("GROW_GATE_LLM_TIMEOUT_SECONDS", "12")),
        )


class ExternalLLMClient:
    """Calls free-tier capable external model providers with deterministic fallback."""

    def __init__(self, config: ExternalLLMConfig | None = None, transport: httpx.BaseTransport | None = None) -> None:
        self.config = config or ExternalLLMConfig.from_env()
        self.transport = transport

    def generate(self, prompt_pack: Dict[str, Any], fallback_summary: str) -> Dict[str, Any]:
        provider = self.config.provider
        if provider in {"", "disabled", "none", "off"}:
            return self._fallback("disabled", fallback_summary, provider="disabled")

        started = time.perf_counter()
        try:
            if provider == "hf_space":
                summary = self._call_hf_space(prompt_pack)
            elif provider == "groq":
                summary = self._call_groq(prompt_pack)
            elif provider == "gemini":
                summary = self._call_gemini(prompt_pack)
            else:
                return self._fallback("unsupported_provider", fallback_summary, provider=provider)
        except Exception as exc:  # pragma: no cover - exact network exceptions vary by provider.
            result = self._fallback("fallback", fallback_summary, provider=provider, error=str(exc))
            result["latency_ms"] = round((time.perf_counter() - started) * 1000)
            return result

        return {
            "provider": provider,
            "status": "ok",
            "used_fallback": False,
            "summary": summary,
            "model": self._model_label(provider),
            "latency_ms": round((time.perf_counter() - started) * 1000),
        }

    def _call_hf_space(self, prompt_pack: Dict[str, Any]) -> str:
        if not self.config.hf_space_url:
            raise ValueError("GROW_GATE_HF_SPACE_URL is required for hf_space provider")
        headers = {"Content-Type": "application/json"}
        if self.config.hf_token:
            headers["Authorization"] = f"Bearer {self.config.hf_token}"
        payload = {
            "prompt": self._prompt_text(prompt_pack),
            "prompt_pack": prompt_pack,
            "task": "grow_gate_export_readiness_summary",
        }
        data = self._post_json(self.config.hf_space_url, payload, headers)
        return self._extract_text(data)

    def _call_groq(self, prompt_pack: Dict[str, Any]) -> str:
        if not self.config.groq_api_key:
            raise ValueError("GROQ_API_KEY is required for groq provider")
        messages = self._chat_messages(prompt_pack)
        data = self._post_json(
            "https://api.groq.com/openai/v1/chat/completions",
            {"model": self.config.groq_model, "messages": messages, "temperature": 0.2, "max_tokens": 700},
            {
                "Authorization": f"Bearer {self.config.groq_api_key}",
                "Content-Type": "application/json",
            },
        )
        return data["choices"][0]["message"]["content"].strip()

    def _call_gemini(self, prompt_pack: Dict[str, Any]) -> str:
        if not self.config.gemini_api_key:
            raise ValueError("GEMINI_API_KEY is required for gemini provider")
        url = (
            "https://generativelanguage.googleapis.com/v1beta/models/"
            f"{self.config.gemini_model}:generateContent?key={self.config.gemini_api_key}"
        )
        data = self._post_json(
            url,
            {
                "contents": [
                    {
                        "role": "user",
                        "parts": [{"text": self._prompt_text(prompt_pack)}],
                    }
                ],
                "generationConfig": {"temperature": 0.2, "maxOutputTokens": 700},
            },
            {"Content-Type": "application/json"},
        )
        return data["candidates"][0]["content"]["parts"][0]["text"].strip()

    def _post_json(self, url: str, payload: Dict[str, Any], headers: Dict[str, str]) -> Dict[str, Any]:
        with httpx.Client(timeout=self.config.timeout_seconds, transport=self.transport) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            return response.json()

    def _prompt_text(self, prompt_pack: Dict[str, Any]) -> str:
        messages = self._chat_messages(prompt_pack)
        contract = prompt_pack.get("output_contract", {})
        lines = [f"{message['role'].upper()}: {message['content']}" for message in messages]
        if contract:
            lines.append("OUTPUT CONTRACT: " + str(contract))
        return "\n\n".join(lines)

    def _chat_messages(self, prompt_pack: Dict[str, Any]) -> List[Dict[str, str]]:
        messages: list[Dict[str, str]] = []
        for item in prompt_pack.get("messages", []):
            role = item.get("role", "user")
            if role not in {"system", "user", "assistant"}:
                role = "user"
            content = str(item.get("content", "")).strip()
            if content:
                messages.append({"role": role, "content": content})
        if not messages:
            messages.append({"role": "user", "content": "GROW-GATE Local EX export readiness summary."})
        return messages

    def _extract_text(self, data: Any) -> str:
        if isinstance(data, str):
            return data.strip()
        if isinstance(data, list):
            for item in data:
                text = self._extract_text(item)
                if text:
                    return text
        if isinstance(data, dict):
            for key in ("summary", "generated_text", "response", "text", "output"):
                value = data.get(key)
                if isinstance(value, str) and value.strip():
                    return value.strip()
            if "data" in data:
                return self._extract_text(data["data"])
        raise ValueError("External LLM response did not include summary text")

    def _fallback(self, status: str, fallback_summary: str, provider: str, error: str | None = None) -> Dict[str, Any]:
        result = {
            "provider": provider,
            "status": status,
            "used_fallback": True,
            "summary": fallback_summary,
            "model": self._model_label(provider),
        }
        if error:
            result["error"] = error
        return result

    def _model_label(self, provider: str) -> str:
        if provider == "groq":
            return self.config.groq_model
        if provider == "gemini":
            return self.config.gemini_model
        if provider == "hf_space":
            return self.config.hf_space_url
        return "deterministic-fallback"
