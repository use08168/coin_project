import os

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("DB_PORT", 3306)),
    "user": os.getenv("DB_USER", "coin_killer"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "coin_killer"),
    "charset": "utf8mb4",
}

# 기본 테이블명
TABLE_FEATURE_MINUTE = os.getenv("TABLE_FEATURE_MINUTE", "feature_minute")
