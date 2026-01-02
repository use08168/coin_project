from __future__ import annotations

import numpy as np
import pandas as pd

from utils import log


# feature_minute 기본 30개 피처 목록 (요청 명세 기준)
BASE_FEATURES = [
    "open_1m", "high_1m", "low_1m", "close_1m", "volume_1m", "trade_count_1m",
    "ret1m_log", "ret5m_log", "ret15m_log", "range_bps_1m",
    "rv15m", "rv60m",
    "vol_z_60m", "rvol_tod", "avg_trade_size_1m", "vwap_gap_bps",
    "taker_buy_qty_1m", "taker_sell_qty_1m", "buy_ratio_1m", "cvd_1m", "cvd_15m",
    "mid_price_1s", "spread_bps_1s", "depth_bid_sum_top20", "depth_ask_sum_top20", "imbalance_top20", "microprice_gap_bps",
    "mark_spot_bps", "oi_chg_1m", "liq_count_1m",
]

EXTRA_FEATURES = [
    "ret60m_log",
    "hour_sin", "hour_cos",
    "dow_sin", "dow_cos",
]


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
    return BASE_FEATURES + (EXTRA_FEATURES if use_optional else [])


def prepare_xy_for_training(
    df: pd.DataFrame,
    horizon_minutes: int = 60,
    use_optional: bool = False,
) -> tuple[pd.DataFrame, pd.Series, pd.DataFrame]:
    """
    학습용 데이터 생성:
      - target = 1 if close[t+60] > close[t] else 0
      - target 생성 위해 close_1m shift(-60)
      - 마지막 60분은 라벨 없음 → 제거
      - 결측 처리: 간단히 dropna(필요 피처 + target)
    반환:
      X, y, meta(ts_utc, close_now)
    """
    df2 = add_optional_features(df, enable=use_optional)

    # target
    if "close_1m" not in df2.columns:
        raise ValueError("close_1m column not found")

    future_close = df2["close_1m"].shift(-horizon_minutes)
    df2["target"] = (future_close > df2["close_1m"]).astype(int)

    feats = get_feature_columns(use_optional)
    missing = [c for c in feats if c not in df2.columns]
    if missing:
        raise ValueError(f"missing feature columns: {missing}")

    # 학습에 필요한 컬럼만
    needed = ["ts_utc", "close_1m"] + feats + ["target"]
    df3 = df2[needed].copy()

    # NaN/inf 처리
    df3 = df3.replace([np.inf, -np.inf], np.nan)

    # 마지막 60분 라벨 NaN 제거 + 피처 결측 제거
    df3 = df3.dropna(subset=feats + ["target"]).reset_index(drop=True)

    X = df3[feats]
    y = df3["target"].astype(int)
    meta = df3[["ts_utc", "close_1m"]].rename(columns={"close_1m": "close_now"})
    return X, y, meta
