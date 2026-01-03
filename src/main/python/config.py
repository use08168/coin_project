"""
config.py - DB 설정 및 테이블 정보

.env 파일에서 환경변수 로드
"""
import os
from pathlib import Path

# ★ .env 파일 로드
from dotenv import load_dotenv

# 프로젝트 루트의 .env 찾기
current_dir = Path(__file__).resolve().parent
project_root = current_dir.parent.parent.parent  # src/main/python -> project root
env_path = project_root / ".env"

if env_path.exists():
    load_dotenv(env_path)
    print(f"[CONFIG] Loaded .env from: {env_path}", file=__import__('sys').stderr)
else:
    # 현재 디렉토리에서도 찾기
    load_dotenv()

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("DB_PORT", 3306)),
    "user": os.getenv("DB_USER", "coin_killer"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "coin_killer"),
    "charset": "utf8mb4",
}

# Raw 데이터 테이블 (WebSocket 수집)
TABLE_KLINE = "f_kline_1m"
TABLE_DEPTH = "f_depth_snapshot_1s"
TABLE_MARK = "f_mark_1s"
TABLE_AGGTRADE = "f_aggtrade_1m"
TABLE_FORCEORDER = "f_forceorder"

# 예측 결과 저장 테이블
TABLE_PRED = "model_pred_60m"

# 기존 호환성 (사용 안함)
TABLE_FEATURE_MINUTE = os.getenv("TABLE_FEATURE_MINUTE", "feature_minute")
