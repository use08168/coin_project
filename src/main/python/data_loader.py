"""
data_loader.py - Raw 테이블에서 Feature 생성 (LAG 적용)

핵심: Depth/Mark/AggTrade/ForceOrder는 t-1 시점 데이터 사용하여 미래 누수 방지
"""
from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Optional

import numpy as np
import pandas as pd
import pymysql

from config import DB_CONFIG
from utils import log


def _connect():
    """pd.read_sql용 연결 (DictCursor 사용 안함)"""
    return pymysql.connect(
        host=DB_CONFIG["host"],
        port=DB_CONFIG["port"],
        user=DB_CONFIG["user"],
        password=DB_CONFIG["password"],
        database=DB_CONFIG["database"],
        charset=DB_CONFIG["charset"],
        cursorclass=pymysql.cursors.Cursor,  # ★ DictCursor 아님!
        autocommit=True,
    )


def _connect_dict():
    """단일 row 조회용 (DictCursor)"""
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


def _ensure_numeric(df: pd.DataFrame, cols: list) -> pd.DataFrame:
    """DECIMAL 타입을 float로 변환"""
    for col in cols:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")
    return df


def load_features_from_raw(
    symbol: str = "BTCUSDT",
    days: int = 7,
    end_ts_utc: Optional[datetime] = None,
) -> pd.DataFrame:
    """
    Raw 테이블들에서 Feature를 직접 계산 (LAG 적용)
    
    ★ 핵심: Depth/Mark/AggTrade/ForceOrder는 t-1 시점 데이터 사용
    
    Returns:
        DataFrame with columns: ts_utc, close_1m, + all features
    """
    conn = _connect()
    
    if end_ts_utc is None:
        end_ts_utc = datetime.now(timezone.utc)
    if end_ts_utc.tzinfo is None:
        end_ts_utc = end_ts_utc.replace(tzinfo=timezone.utc)
    
    start_ts_utc = end_ts_utc - timedelta(days=days)
    
    log(f"[DataLoader] Loading {days} days of data for {symbol}")
    
    # ========================================
    # 1. Kline 데이터 (기준)
    # ========================================
    sql_kline = """
        SELECT symbol, ts_utc,
               open_ AS open_1m, high_ AS high_1m, low_ AS low_1m, close_ AS close_1m,
               volume_ AS volume_1m, trade_count AS trade_count_1m,
               taker_buy_vol, (volume_ - taker_buy_vol) AS taker_sell_vol
        FROM f_kline_1m
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
        ORDER BY ts_utc ASC
    """
    
    df = pd.read_sql(sql_kline, conn, params=(
        symbol, 
        start_ts_utc.replace(tzinfo=None), 
        end_ts_utc.replace(tzinfo=None)
    ))
    
    if df.empty:
        log("[DataLoader] No kline data found!")
        conn.close()
        return df
    
    log(f"[DataLoader] Loaded {len(df)} klines")
    
    # 타입 변환
    df["ts_utc"] = pd.to_datetime(df["ts_utc"], utc=True, errors="coerce")
    numeric_cols = ["open_1m", "high_1m", "low_1m", "close_1m", "volume_1m", 
                    "trade_count_1m", "taker_buy_vol", "taker_sell_vol"]
    df = _ensure_numeric(df, numeric_cols)
    
    # ========================================
    # 2. 기본 Feature 계산 (Kline 기반 - 과거 데이터만 사용)
    # ========================================
    df["ret1m_log"] = np.log(df["close_1m"] / df["close_1m"].shift(1))
    df["ret5m_log"] = np.log(df["close_1m"] / df["close_1m"].shift(5))
    df["ret15m_log"] = np.log(df["close_1m"] / df["close_1m"].shift(15))
    df["range_bps_1m"] = (df["high_1m"] - df["low_1m"]) / df["close_1m"] * 10000
    
    df["rv15m"] = df["ret1m_log"].rolling(15).std() * np.sqrt(15)
    df["rv60m"] = df["ret1m_log"].rolling(60).std() * np.sqrt(60)
    
    vol_mean = df["volume_1m"].rolling(60).mean()
    vol_std = df["volume_1m"].rolling(60).std()
    df["vol_z_60m"] = (df["volume_1m"] - vol_mean) / vol_std.replace(0, np.nan)
    
    # CVD from kline
    df["cvd_1m"] = df["taker_buy_vol"] - df["taker_sell_vol"]
    df["cvd_15m"] = df["cvd_1m"].rolling(15).sum()
    
    # Buy ratio from kline
    total_vol = df["taker_buy_vol"] + df["taker_sell_vol"]
    df["buy_ratio_1m"] = df["taker_buy_vol"] / total_vol.replace(0, np.nan)
    
    log("[DataLoader] Basic features calculated")
    
    # ========================================
    # 3. Depth 데이터 (★ LAG: t-1 시점 사용)
    # ========================================
    sql_depth = """
        SELECT 
            DATE_FORMAT(ts_utc, '%%Y-%%m-%%d %%H:%%i:00') AS ts_minute,
            AVG(spread_bps) AS spread_bps,
            AVG(imbalance_top20) AS imbalance_top20,
            AVG(microprice_gap_bps) AS microprice_gap_bps,
            AVG(mid_price) AS mid_price
        FROM f_depth_snapshot_1s
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
        GROUP BY ts_minute
        ORDER BY ts_minute
    """
    df_depth = pd.read_sql(sql_depth, conn, params=(
        symbol, 
        start_ts_utc.replace(tzinfo=None), 
        end_ts_utc.replace(tzinfo=None)
    ))
    
    if not df_depth.empty:
        df_depth["ts_minute"] = pd.to_datetime(df_depth["ts_minute"], utc=True, errors="coerce")
        df_depth = _ensure_numeric(df_depth, ["spread_bps", "imbalance_top20", "microprice_gap_bps", "mid_price"])
        
        # ★ LAG 적용: 1분 뒤로 shift하여 "이전 분" 데이터로 만듦
        df_depth["ts_minute_next"] = df_depth["ts_minute"] + pd.Timedelta(minutes=1)
        df_depth = df_depth.drop(columns=["ts_minute"]).rename(columns={"ts_minute_next": "ts_minute"})
        
        df["ts_minute"] = df["ts_utc"].dt.floor("min")
        df = df.merge(df_depth, on="ts_minute", how="left")
        log(f"[DataLoader] Depth merged with LAG (t-1)")
    else:
        df["spread_bps"] = 0.0
        df["imbalance_top20"] = 0.0
        df["microprice_gap_bps"] = 0.0
        df["mid_price"] = 0.0
        df["ts_minute"] = df["ts_utc"].dt.floor("min")
        log("[DataLoader] No depth data, using zeros")
    
    # ========================================
    # 4. Mark 데이터 (★ LAG: t-1 시점 사용)
    # ========================================
    sql_mark = """
        SELECT 
            DATE_FORMAT(ts_utc, '%%Y-%%m-%%d %%H:%%i:00') AS ts_minute,
            AVG(mark_price) AS mark_price_avg,
            AVG(funding_rate) AS funding_rate
        FROM f_mark_1s
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
        GROUP BY ts_minute
        ORDER BY ts_minute
    """
    df_mark = pd.read_sql(sql_mark, conn, params=(
        symbol, 
        start_ts_utc.replace(tzinfo=None), 
        end_ts_utc.replace(tzinfo=None)
    ))
    
    if not df_mark.empty:
        df_mark["ts_minute"] = pd.to_datetime(df_mark["ts_minute"], utc=True, errors="coerce")
        df_mark = _ensure_numeric(df_mark, ["mark_price_avg", "funding_rate"])
        
        # ★ LAG 적용
        df_mark["ts_minute_next"] = df_mark["ts_minute"] + pd.Timedelta(minutes=1)
        df_mark = df_mark.drop(columns=["ts_minute"]).rename(columns={"ts_minute_next": "ts_minute"})
        
        df = df.merge(df_mark, on="ts_minute", how="left")
        df["mark_spot_bps"] = (df["mark_price_avg"] - df["close_1m"]) / df["close_1m"] * 10000
        log(f"[DataLoader] Mark merged with LAG (t-1)")
    else:
        df["mark_price_avg"] = 0.0
        df["funding_rate"] = 0.0
        df["mark_spot_bps"] = 0.0
        log("[DataLoader] No mark data, using zeros")
    
    # ========================================
    # 5. AggTrade 데이터 (★ LAG: t-1 시점 사용)
    # ========================================
    sql_agg = """
        SELECT 
            ts_utc AS ts_minute,
            taker_buy_qty, taker_sell_qty, trade_count AS agg_trade_count, vwap_price
        FROM f_aggtrade_1m
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
        ORDER BY ts_utc
    """
    df_agg = pd.read_sql(sql_agg, conn, params=(
        symbol, 
        start_ts_utc.replace(tzinfo=None), 
        end_ts_utc.replace(tzinfo=None)
    ))
    
    if not df_agg.empty:
        df_agg["ts_minute"] = pd.to_datetime(df_agg["ts_minute"], utc=True, errors="coerce")
        df_agg = _ensure_numeric(df_agg, ["taker_buy_qty", "taker_sell_qty", "agg_trade_count", "vwap_price"])
        
        # ★ LAG 적용
        df_agg["ts_minute_next"] = df_agg["ts_minute"] + pd.Timedelta(minutes=1)
        df_agg = df_agg.drop(columns=["ts_minute"]).rename(columns={"ts_minute_next": "ts_minute"})
        
        df = df.merge(df_agg, on="ts_minute", how="left")
        df["vwap_gap_bps"] = (df["close_1m"] - df["vwap_price"]) / df["vwap_price"].replace(0, np.nan) * 10000
        df["avg_trade_size_1m"] = (df["taker_buy_qty"] + df["taker_sell_qty"]) / df["agg_trade_count"].replace(0, np.nan)
        log(f"[DataLoader] AggTrade merged with LAG (t-1)")
    else:
        df["vwap_gap_bps"] = 0.0
        df["avg_trade_size_1m"] = 0.0
        log("[DataLoader] No aggtrade data, using zeros")
    
    # ========================================
    # 6. ForceOrder (청산) 데이터 (★ LAG: t-1 시점 사용)
    # ========================================
    sql_force = """
        SELECT 
            DATE_FORMAT(event_utc, '%%Y-%%m-%%d %%H:%%i:00') AS ts_minute,
            COUNT(*) AS liq_count_1m
        FROM f_forceorder
        WHERE symbol = %s AND event_utc >= %s AND event_utc < %s
        GROUP BY ts_minute
    """
    df_force = pd.read_sql(sql_force, conn, params=(
        symbol, 
        start_ts_utc.replace(tzinfo=None), 
        end_ts_utc.replace(tzinfo=None)
    ))
    
    if not df_force.empty:
        df_force["ts_minute"] = pd.to_datetime(df_force["ts_minute"], utc=True, errors="coerce")
        
        # ★ LAG 적용
        df_force["ts_minute_next"] = df_force["ts_minute"] + pd.Timedelta(minutes=1)
        df_force = df_force.drop(columns=["ts_minute"]).rename(columns={"ts_minute_next": "ts_minute"})
        
        df = df.merge(df_force, on="ts_minute", how="left")
        df["liq_count_1m"] = df["liq_count_1m"].fillna(0)
        log(f"[DataLoader] ForceOrder merged with LAG (t-1)")
    else:
        df["liq_count_1m"] = 0.0
        log("[DataLoader] No forceorder data, using zeros")
    
    conn.close()
    
    # ========================================
    # 7. 결측값 처리
    # ========================================
    fill_cols = ["spread_bps", "imbalance_top20", "microprice_gap_bps", "mid_price",
                 "mark_price_avg", "funding_rate", "mark_spot_bps", "vwap_gap_bps", "avg_trade_size_1m"]
    for col in fill_cols:
        if col in df.columns:
            df[col] = df[col].ffill()
    
    df = df.fillna(0)
    
    # Warmup 기간 제거 (rv60m은 60분 필요 + LAG 1분)
    if len(df) > 61:
        df = df.iloc[61:].reset_index(drop=True)
    
    log(f"[DataLoader] Final: {len(df)} rows with LAG applied")
    
    return df


def load_latest_for_prediction(symbol: str = "BTCUSDT", minutes: int = 180) -> pd.DataFrame:
    """
    실시간 예측용: 최근 N분 데이터 로드 (LAG 적용)
    
    NOTE: rv60m 계산을 위해 최소 120분 필요
    """
    conn = _connect()
    
    end_ts = datetime.now(timezone.utc)
    start_ts = end_ts - timedelta(minutes=minutes + 61)  # warmup 포함
    
    # Kline 로드
    sql_kline = """
        SELECT symbol, ts_utc,
               open_ AS open_1m, high_ AS high_1m, low_ AS low_1m, close_ AS close_1m,
               volume_ AS volume_1m, trade_count AS trade_count_1m,
               taker_buy_vol, (volume_ - taker_buy_vol) AS taker_sell_vol
        FROM f_kline_1m
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
        ORDER BY ts_utc ASC
    """
    
    df = pd.read_sql(sql_kline, conn, params=(
        symbol, 
        start_ts.replace(tzinfo=None), 
        end_ts.replace(tzinfo=None)
    ))
    
    if df.empty:
        conn.close()
        return df
    
    # 타입 변환
    df["ts_utc"] = pd.to_datetime(df["ts_utc"], utc=True, errors="coerce")
    numeric_cols = ["open_1m", "high_1m", "low_1m", "close_1m", "volume_1m", 
                    "trade_count_1m", "taker_buy_vol", "taker_sell_vol"]
    df = _ensure_numeric(df, numeric_cols)
    
    # Feature 계산
    df["ret1m_log"] = np.log(df["close_1m"] / df["close_1m"].shift(1))
    df["ret5m_log"] = np.log(df["close_1m"] / df["close_1m"].shift(5))
    df["ret15m_log"] = np.log(df["close_1m"] / df["close_1m"].shift(15))
    df["range_bps_1m"] = (df["high_1m"] - df["low_1m"]) / df["close_1m"] * 10000
    df["rv15m"] = df["ret1m_log"].rolling(15).std() * np.sqrt(15)
    df["rv60m"] = df["ret1m_log"].rolling(60).std() * np.sqrt(60)
    
    vol_mean = df["volume_1m"].rolling(60).mean()
    vol_std = df["volume_1m"].rolling(60).std()
    df["vol_z_60m"] = (df["volume_1m"] - vol_mean) / vol_std.replace(0, np.nan)
    
    df["cvd_1m"] = df["taker_buy_vol"] - df["taker_sell_vol"]
    df["cvd_15m"] = df["cvd_1m"].rolling(15).sum()
    
    total_vol = df["taker_buy_vol"] + df["taker_sell_vol"]
    df["buy_ratio_1m"] = df["taker_buy_vol"] / total_vol.replace(0, np.nan)
    
    df["ts_minute"] = df["ts_utc"].dt.floor("min")
    
    # Depth (LAG 적용)
    sql_depth = """
        SELECT DATE_FORMAT(ts_utc, '%%Y-%%m-%%d %%H:%%i:00') AS ts_minute,
               AVG(spread_bps) AS spread_bps, AVG(imbalance_top20) AS imbalance_top20,
               AVG(microprice_gap_bps) AS microprice_gap_bps
        FROM f_depth_snapshot_1s
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
        GROUP BY ts_minute
    """
    df_depth = pd.read_sql(sql_depth, conn, params=(symbol, start_ts.replace(tzinfo=None), end_ts.replace(tzinfo=None)))
    if not df_depth.empty:
        df_depth["ts_minute"] = pd.to_datetime(df_depth["ts_minute"], utc=True, errors="coerce")
        df_depth = _ensure_numeric(df_depth, ["spread_bps", "imbalance_top20", "microprice_gap_bps"])
        df_depth["ts_minute"] = df_depth["ts_minute"] + pd.Timedelta(minutes=1)  # LAG
        df = df.merge(df_depth, on="ts_minute", how="left")
    else:
        df["spread_bps"] = 0.0
        df["imbalance_top20"] = 0.0
        df["microprice_gap_bps"] = 0.0
    
    # Mark (LAG 적용)
    sql_mark = """
        SELECT DATE_FORMAT(ts_utc, '%%Y-%%m-%%d %%H:%%i:00') AS ts_minute,
               AVG(funding_rate) AS funding_rate
        FROM f_mark_1s
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
        GROUP BY ts_minute
    """
    df_mark = pd.read_sql(sql_mark, conn, params=(symbol, start_ts.replace(tzinfo=None), end_ts.replace(tzinfo=None)))
    if not df_mark.empty:
        df_mark["ts_minute"] = pd.to_datetime(df_mark["ts_minute"], utc=True, errors="coerce")
        df_mark = _ensure_numeric(df_mark, ["funding_rate"])
        df_mark["ts_minute"] = df_mark["ts_minute"] + pd.Timedelta(minutes=1)  # LAG
        df = df.merge(df_mark, on="ts_minute", how="left")
    else:
        df["funding_rate"] = 0.0
    
    df["mark_spot_bps"] = 0.0  # 간소화
    
    # AggTrade (LAG 적용)
    sql_agg = """
        SELECT ts_utc AS ts_minute, vwap_price,
               (taker_buy_qty + taker_sell_qty) / NULLIF(trade_count, 0) AS avg_trade_size_1m
        FROM f_aggtrade_1m
        WHERE symbol = %s AND ts_utc >= %s AND ts_utc < %s
    """
    df_agg = pd.read_sql(sql_agg, conn, params=(symbol, start_ts.replace(tzinfo=None), end_ts.replace(tzinfo=None)))
    if not df_agg.empty:
        df_agg["ts_minute"] = pd.to_datetime(df_agg["ts_minute"], utc=True, errors="coerce")
        df_agg = _ensure_numeric(df_agg, ["vwap_price", "avg_trade_size_1m"])
        df_agg["ts_minute"] = df_agg["ts_minute"] + pd.Timedelta(minutes=1)  # LAG
        df = df.merge(df_agg, on="ts_minute", how="left")
        df["vwap_gap_bps"] = (df["close_1m"] - df["vwap_price"]) / df["vwap_price"].replace(0, np.nan) * 10000
    else:
        df["vwap_gap_bps"] = 0.0
        df["avg_trade_size_1m"] = 0.0
    
    # ForceOrder (LAG 적용)
    sql_force = """
        SELECT DATE_FORMAT(event_utc, '%%Y-%%m-%%d %%H:%%i:00') AS ts_minute,
               COUNT(*) AS liq_count_1m
        FROM f_forceorder
        WHERE symbol = %s AND event_utc >= %s AND event_utc < %s
        GROUP BY ts_minute
    """
    df_force = pd.read_sql(sql_force, conn, params=(symbol, start_ts.replace(tzinfo=None), end_ts.replace(tzinfo=None)))
    if not df_force.empty:
        df_force["ts_minute"] = pd.to_datetime(df_force["ts_minute"], utc=True, errors="coerce")
        df_force["ts_minute"] = df_force["ts_minute"] + pd.Timedelta(minutes=1)  # LAG
        df = df.merge(df_force, on="ts_minute", how="left")
    else:
        df["liq_count_1m"] = 0.0
    
    conn.close()
    
    # 결측값 처리
    df = df.ffill().fillna(0)
    
    # Warmup 제거
    if len(df) > 61:
        df = df.iloc[61:].reset_index(drop=True)
    
    return df


def load_latest_close(symbol: str) -> Optional[tuple[datetime, float]]:
    """최신 close 가격 조회"""
    conn = _connect_dict()
    sql = """
        SELECT ts_utc, close_
        FROM f_kline_1m
        WHERE symbol = %s
        ORDER BY ts_utc DESC
        LIMIT 1
    """
    with conn.cursor() as cur:
        cur.execute(sql, (symbol,))
        row = cur.fetchone()
    conn.close()
    
    if not row:
        return None
    
    ts = pd.to_datetime(row["ts_utc"], utc=True)
    return ts.to_pydatetime(), float(row["close_"])
