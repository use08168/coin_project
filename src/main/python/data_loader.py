from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Optional

import pandas as pd
import pymysql

from config import DB_CONFIG, TABLE_FEATURE_MINUTE
from utils import log


def _connect():
    return pymysql.connect(
        host=DB_CONFIG["host"],
        port=DB_CONFIG["port"],
        user=DB_CONFIG["user"],
        password=DB_CONFIG["password"],
        database=DB_CONFIG["database"],
        charset=DB_CONFIG["charset"],
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
    )


def _ensure_utc_ts(df: pd.DataFrame) -> pd.DataFrame:
    if "ts_utc" in df.columns:
        df["ts_utc"] = pd.to_datetime(df["ts_utc"], utc=True, errors="coerce")
    return df


def load_feature_minutes(
    symbol: str,
    days: int = 7,
    end_ts_utc: Optional[datetime] = None,
    limit: Optional[int] = None,
) -> pd.DataFrame:
    """
    feature_minute에서 최근 N일 또는 limit 만큼 로드.
    - ts_utc는 UTC로 파싱
    - 정렬은 ASC로 반환
    """
    if end_ts_utc is None:
        end_ts_utc = datetime.now(timezone.utc)
    if end_ts_utc.tzinfo is None:
        end_ts_utc = end_ts_utc.replace(tzinfo=timezone.utc)

    start_ts_utc = end_ts_utc - timedelta(days=days)

    sql = f"""
        SELECT *
        FROM {TABLE_FEATURE_MINUTE}
        WHERE symbol = %s
          AND ts_utc >= %s
          AND ts_utc < %s
        ORDER BY ts_utc ASC
    """

    # limit이 있으면 맨 뒤에서 limit개만 가져오는게 효율적이지만,
    # 우선 단순하게 ORDER ASC 후 pandas tail로 제한
    with _connect() as conn:
        with conn.cursor() as cur:
            log(f"[DB] load_feature_minutes symbol={symbol}, days={days}, range=({start_ts_utc},{end_ts_utc})")
            cur.execute(sql, (symbol, start_ts_utc.replace(tzinfo=None), end_ts_utc.replace(tzinfo=None)))
            rows = cur.fetchall()

    df = pd.DataFrame(rows)
    if df.empty:
        return df

    df = _ensure_utc_ts(df)
    df = df.dropna(subset=["ts_utc"]).sort_values("ts_utc").reset_index(drop=True)

    if limit is not None and limit > 0:
        df = df.tail(limit).reset_index(drop=True)

    return df


def load_latest_feature_window(symbol: str, minutes: int = 60) -> pd.DataFrame:
    """
    예측용: 최신 minutes 행 로드(보통 60개).
    - ORDER BY ts_utc DESC LIMIT minutes 후 다시 ASC 정렬
    """
    sql = f"""
        SELECT *
        FROM {TABLE_FEATURE_MINUTE}
        WHERE symbol = %s
        ORDER BY ts_utc DESC
        LIMIT %s
    """
    with _connect() as conn:
        with conn.cursor() as cur:
            log(f"[DB] load_latest_feature_window symbol={symbol}, minutes={minutes}")
            cur.execute(sql, (symbol, minutes))
            rows = cur.fetchall()

    df = pd.DataFrame(rows)
    if df.empty:
        return df
    df = _ensure_utc_ts(df)
    df = df.dropna(subset=["ts_utc"]).sort_values("ts_utc").reset_index(drop=True)
    return df


def load_latest_close(symbol: str) -> Optional[tuple[datetime, float]]:
    """
    최신 close_1m 하나 조회 (current_close 반환용)
    """
    sql = f"""
        SELECT ts_utc, close_1m
        FROM {TABLE_FEATURE_MINUTE}
        WHERE symbol = %s
        ORDER BY ts_utc DESC
        LIMIT 1
    """
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (symbol,))
            row = cur.fetchone()

    if not row:
        return None
    ts = pd.to_datetime(row["ts_utc"], utc=True, errors="coerce")
    if pd.isna(ts):
        return None
    return ts.to_pydatetime(), float(row["close_1m"])
