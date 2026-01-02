from __future__ import annotations

import argparse
import os
from datetime import datetime, timezone

import joblib
import numpy as np
import pandas as pd

from data_loader import load_latest_feature_window, load_latest_close
from feature_engineer import add_optional_features, get_feature_columns
from utils import log, json_stdout, error_json, exception_to_detail


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--symbol", type=str, default="BTCUSDT")
    parser.add_argument("--model", type=str, default="model/lgbm_btcusdt.pkl")
    args = parser.parse_args()

    try:
        if not os.path.exists(args.model):
            error_json("MODEL_NOT_FOUND", f"model file not found: {args.model}", extra={"model": args.model})
            return

        pack = joblib.load(args.model)
        model = pack.get("model")
        model_version = pack.get("model_version", "unknown")
        use_optional = bool(pack.get("use_optional_features", False))
        feats = pack.get("features") or get_feature_columns(use_optional)

        # 예측용: 최신 60분 데이터 로드 (옵션 피처(ret60m) 계산하려면 60 이상이 필요)
        df = load_latest_feature_window(args.symbol, minutes=60 if not use_optional else 120)
        if df.empty or len(df) < 60:
            error_json(
                "DATA_INSUFFICIENT",
                f"not enough recent rows for prediction. rows={len(df)}",
                extra={"symbol": args.symbol, "rows": int(len(df))}
            )
            return

        # 최신 시점(마지막 row)으로 예측
        df2 = add_optional_features(df, enable=use_optional)
        df2 = df2.replace([np.inf, -np.inf], np.nan)

        # 마지막 행을 feature로 사용
        last = df2.iloc[-1].copy()

        missing = [c for c in feats if c not in df2.columns]
        if missing:
            error_json("FEATURE_MISSING", f"missing feature columns: {missing}", extra={"missing": missing})
            return

        x = pd.DataFrame([last[feats].to_dict()])

        # 결측은 간단히 0으로 채우고 시작 (훈련에서도 dropna 했으니, 실전에서는 더 엄격히 해도 됨)
        x = x.fillna(0.0)

        proba = float(model.predict_proba(x)[:, 1][0])
        pred = int(proba >= 0.5)
        label = "UP" if pred == 1 else "DOWN"

        # current close
        latest_close = load_latest_close(args.symbol)
        if latest_close:
            close_ts, current_close = latest_close
        else:
            close_ts = last.get("ts_utc")
            if hasattr(close_ts, "to_pydatetime"):
                close_ts = close_ts.to_pydatetime()
            current_close = float(last.get("close_1m", 0.0))

        # timestamp는 "예측 기준 시각" (마지막 feature ts)
        ts = last.get("ts_utc")
        if hasattr(ts, "to_pydatetime"):
            ts = ts.to_pydatetime()
        if ts is None:
            ts = datetime.now(timezone.utc)
        if getattr(ts, "tzinfo", None) is None:
            ts = ts.replace(tzinfo=timezone.utc)

        json_stdout({
            "symbol": args.symbol,
            "timestamp": ts.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
            "current_close": float(current_close),
            "prediction": pred,
            "prediction_label": label,
            "probability": proba,
            "model_version": model_version,
            "features_used": int(len(feats)),
            "ok": True
        })

    except Exception as e:
        detail = exception_to_detail(e)
        log("[PREDICT][ERROR]\n" + detail)
        error_json("PREDICT_FAILED", "prediction failed", detail=detail, extra={"symbol": args.symbol, "model": args.model})


if __name__ == "__main__":
    main()
