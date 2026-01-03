"""
feature_engineer.py - Feature 컬럼 정의 및 Target 생성

핵심: data_loader.py에서 LAG 적용된 데이터를 받아서 학습용 X, y 생성
"""
from __future__ import annotations

import numpy as np
import pandas as pd

from utils import log


# ========================================
# Feature 컬럼 목록 (LAG 적용된 raw 테이블 기반)
# ========================================
BASE_FEATURES = [
    # Kline 기반 (과거 데이터만 사용)
    "open_1m", "high_1m", "low_1m", "close_1m", "volume_1m", "trade_count_1m",
    "ret1m_log", "ret5m_log", "ret15m_log", "range_bps_1m",
    "rv15m", "rv60m", "vol_z_60m",
    "buy_ratio_1m", "cvd_1m", "cvd_15m",
    
    # Depth (LAG 적용됨 - t-1 시점)
    "spread_bps", "imbalance_top20", "microprice_gap_bps",
    
    # Mark (LAG 적용됨 - t-1 시점)
    "mark_spot_bps", "funding_rate",
    
    # AggTrade (LAG 적용됨 - t-1 시점)
    "vwap_gap_bps", "avg_trade_size_1m",
    
    # ForceOrder (LAG 적용됨 - t-1 시점)
    "liq_count_1m",
]

EXTRA_FEATURES = [
    "ret60m_log",
    "hour_sin", "hour_cos",
    "dow_sin", "dow_cos",
]


def _remove_duplicate_columns(df: pd.DataFrame) -> pd.DataFrame:
    """중복 컬럼 제거 (merge 시 _x, _y suffix 처리)"""
    # 중복 컬럼명 확인
    cols = df.columns.tolist()
    seen = set()
    duplicates = []
    for col in cols:
        if col in seen:
            duplicates.append(col)
        seen.add(col)
    
    if duplicates:
        log(f"[FE] Warning: Removing duplicate columns: {duplicates}")
        df = df.loc[:, ~df.columns.duplicated()]
    
    # _x, _y suffix 처리
    rename_map = {}
    drop_cols = []
    for col in df.columns:
        if col.endswith('_x'):
            base_name = col[:-2]
            if base_name not in df.columns:
                rename_map[col] = base_name
            else:
                drop_cols.append(col)
        elif col.endswith('_y'):
            drop_cols.append(col)
    
    if rename_map:
        log(f"[FE] Renaming columns: {rename_map}")
        df = df.rename(columns=rename_map)
    
    if drop_cols:
        log(f"[FE] Dropping suffix columns: {drop_cols}")
        df = df.drop(columns=drop_cols, errors='ignore')
    
    return df


def add_optional_features(df: pd.DataFrame, enable: bool = True) -> pd.DataFrame:
    """
    선택 피처 생성:
      - ret60m_log: ln(close / close.shift(60))
      - hour_sin/cos, dow_sin/cos: UTC 기준
    """
    if not enable:
        return df

    if "ts_utc" not in df.columns:
        return df

    out = df.copy()

    # ret60m_log
    if "close_1m" in out.columns:
        out["ret60m_log"] = np.log(out["close_1m"] / out["close_1m"].shift(60)).replace([np.inf, -np.inf], np.nan)

    # 시간 주기성 (UTC)
    ts = pd.to_datetime(out["ts_utc"], utc=True, errors="coerce")
    hours = ts.dt.hour.astype(float)
    dows = ts.dt.dayofweek.astype(float)  # Mon=0

    out["hour_sin"] = np.sin(2 * np.pi * hours / 24.0)
    out["hour_cos"] = np.cos(2 * np.pi * hours / 24.0)
    out["dow_sin"] = np.sin(2 * np.pi * dows / 7.0)
    out["dow_cos"] = np.cos(2 * np.pi * dows / 7.0)

    log("[FE] optional features added: ret60m_log, hour/dow sin/cos")
    return out


def get_feature_columns(use_optional: bool = False) -> list[str]:
    """사용할 Feature 컬럼 목록 반환"""
    return BASE_FEATURES + (EXTRA_FEATURES if use_optional else [])


def prepare_xy_for_training(
    df: pd.DataFrame,
    horizon_minutes: int = 60,
    use_optional: bool = False,
) -> tuple[pd.DataFrame, pd.Series, pd.DataFrame]:
    """
    학습용 데이터 생성:
      - target = 1 if close[t+60] > close[t] else 0
      - 마지막 60분은 라벨 없음 → 제거
      
    ★ 주의: df는 이미 data_loader.py에서 LAG가 적용된 상태여야 함
    
    반환:
      X, y, meta(ts_utc, close_now)
    """
    # ★ 중복 컬럼 제거
    df2 = _remove_duplicate_columns(df.copy())
    
    df2 = add_optional_features(df2, enable=use_optional)

    # target 생성
    if "close_1m" not in df2.columns:
        raise ValueError("close_1m column not found")

    future_close = df2["close_1m"].shift(-horizon_minutes)
    
    # ★ 버그 수정: NaN을 제대로 처리
    # NaN > value는 False가 되어 0으로 잘못 변환되는 문제 해결
    df2["target"] = np.where(
        future_close.isna(),
        np.nan,  # future_close가 NaN이면 target도 NaN
        (future_close > df2["close_1m"]).astype(float)
    )

    feats = get_feature_columns(use_optional)
    
    # 누락된 컬럼 확인 및 0으로 채우기
    missing = [c for c in feats if c not in df2.columns]
    if missing:
        log(f"[FE] Warning: Missing columns will be filled with 0: {missing}")
        for col in missing:
            df2[col] = 0.0

    # 학습에 필요한 컬럼만
    needed = ["ts_utc", "close_1m"] + feats + ["target"]
    available = [c for c in needed if c in df2.columns]
    df3 = df2[available].copy()

    # NaN/inf 처리
    df3 = df3.replace([np.inf, -np.inf], np.nan)

    # ★ 마지막 horizon분 라벨 NaN 제거 + 피처 결측 제거
    valid_feats = [c for c in feats if c in df3.columns]
    before_drop = len(df3)
    df3 = df3.dropna(subset=valid_feats + ["target"]).reset_index(drop=True)
    after_drop = len(df3)
    log(f"[FE] Dropped {before_drop - after_drop} rows (horizon + NaN features)")

    # ★ 중복 컬럼 다시 확인
    df3 = df3.loc[:, ~df3.columns.duplicated()]
    
    X = df3[valid_feats].copy()
    
    # ★ X에서도 중복 컬럼 제거
    X = X.loc[:, ~X.columns.duplicated()]
    
    y = df3["target"].astype(int)
    meta = df3[["ts_utc", "close_1m"]].rename(columns={"close_1m": "close_now"})
    
    log(f"[FE] Prepared X: {X.shape}, y distribution: {y.value_counts().to_dict()}")
    
    return X, y, meta


def prepare_x_for_prediction(
    df: pd.DataFrame,
    use_optional: bool = False,
) -> tuple[pd.DataFrame, dict]:
    """
    예측용 데이터 생성 (마지막 row 사용)
    
    ★ 주의: df는 이미 data_loader.py에서 LAG가 적용된 상태여야 함
    
    반환:
      X (1 row), meta (ts_utc, close_now)
    """
    # ★ 중복 컬럼 제거
    df2 = _remove_duplicate_columns(df.copy())
    df2 = add_optional_features(df2, enable=use_optional)
    
    feats = get_feature_columns(use_optional)
    
    # 누락된 컬럼 0으로 채우기
    for col in feats:
        if col not in df2.columns:
            df2[col] = 0.0
    
    # NaN/inf 처리
    df2 = df2.replace([np.inf, -np.inf], np.nan).fillna(0)
    
    # 마지막 행 사용
    last = df2.iloc[-1]
    
    X = pd.DataFrame([last[feats].to_dict()])
    
    # ★ 중복 컬럼 제거
    X = X.loc[:, ~X.columns.duplicated()]
    
    meta = {
        "ts_utc": last.get("ts_utc"),
        "close_now": float(last.get("close_1m", 0)),
    }
    
    return X, meta
