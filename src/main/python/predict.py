"""
predict.py - 실시간 예측 (LAG 적용된 데이터 사용)

핵심: data_loader.load_latest_for_prediction() 사용 (LAG 적용)
"""
from __future__ import annotations

import argparse
import os
from datetime import datetime, timezone

import joblib
import numpy as np
import pandas as pd

from data_loader import load_latest_for_prediction, load_latest_close
from feature_engineer import prepare_x_for_prediction, get_feature_columns
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

        log(f"[PREDICT] Loaded model: {model_version}")
        log(f"[PREDICT] Features: {len(feats)}")

        # ★ LAG 적용된 데이터 로드
        log(f"[PREDICT] Loading latest data with LAG applied...")
        df = load_latest_for_prediction(args.symbol, minutes=180)  # 3시간 (warmup + 여유)
        
        if df.empty or len(df) < 10:
            error_json(
                "DATA_INSUFFICIENT",
                f"not enough recent rows for prediction. rows={len(df)}",
                extra={"symbol": args.symbol, "rows": int(len(df))}
            )
            return

        log(f"[PREDICT] Loaded {len(df)} rows")

        # 예측용 X 준비
        X, meta = prepare_x_for_prediction(df, use_optional=use_optional)
        
        # 결측 컬럼 확인
        missing = [c for c in feats if c not in X.columns]
        if missing:
            log(f"[PREDICT] Warning: Missing columns will be filled with 0: {missing}")
            for col in missing:
                X[col] = 0.0
        
        # Feature 순서 맞추기
        X = X[feats]

        # 예측
        proba = float(model.predict_proba(X)[:, 1][0])
        pred = int(proba >= 0.5)
        label = "UP" if pred == 1 else "DOWN"

        log(f"[PREDICT] Prediction: {label} ({proba:.4f})")

        # 현재 가격
        latest_close = load_latest_close(args.symbol)
        if latest_close:
            close_ts, current_close = latest_close
        else:
            close_ts = meta.get("ts_utc")
            current_close = meta.get("close_now", 0.0)

        # timestamp
        ts = meta.get("ts_utc")
        if hasattr(ts, "to_pydatetime"):
            ts = ts.to_pydatetime()
        if ts is None:
            ts = datetime.now(timezone.utc)
        if getattr(ts, "tzinfo", None) is None:
            ts = ts.replace(tzinfo=timezone.utc)

        json_stdout({
            "ok": True,
            "symbol": args.symbol,
            "timestamp": ts.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
            "current_close": float(current_close),
            "prediction": pred,
            "prediction_label": label,
            "probability": proba,
            "model_version": model_version,
            "features_used": int(len(feats)),
        })

    except Exception as e:
        detail = exception_to_detail(e)
        log("[PREDICT][ERROR]\n" + detail)
        error_json("PREDICT_FAILED", "prediction failed", detail=detail, extra={"symbol": args.symbol, "model": args.model})


if __name__ == "__main__":
    main()
