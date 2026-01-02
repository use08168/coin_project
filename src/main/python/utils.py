import json
import sys
import traceback
from datetime import datetime, timezone
from typing import Any, Dict, Optional


def log(msg: str) -> None:
    """stderr 로깅 (Java에서 stdout만 파싱하기 위함)"""
    print(msg, file=sys.stderr, flush=True)


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def to_iso(dt) -> Optional[str]:
    if dt is None:
        return None
    # pandas Timestamp / datetime 모두 처리
    try:
        if hasattr(dt, "to_pydatetime"):
            dt = dt.to_pydatetime()
        if dt.tzinfo is None:
            # DB에서 UTC로 들어온다고 가정: naive면 UTC로 간주
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    except Exception:
        return str(dt)


def json_stdout(obj: Dict[str, Any]) -> None:
    """stdout에 JSON만 출력"""
    print(json.dumps(obj, ensure_ascii=False), flush=True)


def error_json(code: str, message: str, detail: Optional[str] = None, extra: Optional[Dict[str, Any]] = None) -> None:
    payload = {
        "ok": False,
        "error": {
            "code": code,
            "message": message,
            "detail": detail,
        },
        "timestamp": utc_now_iso(),
    }
    if extra:
        payload.update(extra)
    json_stdout(payload)


def exception_to_detail(e: Exception) -> str:
    return "".join(traceback.format_exception(type(e), e, e.__traceback__))[-2000:]


def safe_div(a: float, b: float, default: float = 0.0) -> float:
    try:
        if b == 0:
            return default
        return a / b
    except Exception:
        return default
