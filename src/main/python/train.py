from __future__ import annotations

import argparse
import os
from datetime import datetime

import joblib
import lightgbm as lgb
import numpy as np
import pandas as pd
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix

from data_loader import load_feature_minutes
from feature_engineer import prepare_xy_for_training, get_feature_columns
from utils import log, json_stdout, error_json, exception_to_detail


def mda(y_true: np.ndarray, y_pred: np.ndarray) -> float:
    """
    MDA (Mean Directional Accuracy)
    이진 방향 예측에서 사실상 accuracy와 유사하지만,
    논문 맥락을 위해 별도 표기.
    """
    return float((y_true == y_pred).mean())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--days", type=int, default=7)
    parser.add_argument("--symbol", type=str, default="BTCUSDT")
    parser.add_argument("--output", type=str, default="model/lgbm_btcusdt.pkl")
    parser.add_argument("--use-optional-features", action="store_true")
    parser.add_argument("--horizon", type=int, default=60, help="minutes ahead label (default 60)")
    args = parser.parse_args()

    try:
        df = load_feature_minutes(symbol=args.symbol, days=args.days)
        if df.empty or len(df) < (args.horizon + 200):
            error_json(
                "DATA_INSUFFICIENT",
                f"not enough rows for training (need > horizon+200). rows={len(df)}",
                extra={"symbol": args.symbol, "days": args.days, "rows": int(len(df))}
            )
            return

        X, y, meta = prepare_xy_for_training(df, horizon_minutes=args.horizon, use_optional=args.use_optional_features)

        if len(X) < 500:
            error_json(
                "DATA_INSUFFICIENT",
                f"after cleaning, not enough rows. rows={len(X)}",
                extra={"symbol": args.symbol, "rows_after_clean": int(len(X))}
            )
            return

        # 시계열 split (80/20)
        n = len(X)
        split = int(n * 0.8)
        X_train, X_test = X.iloc[:split], X.iloc[split:]
        y_train, y_test = y.iloc[:split], y.iloc[split:]

        # LightGBM params (요청 명세 기반)
        params = {
            "objective": "binary",
            "metric": "binary_logloss",
            "boosting_type": "gbdt",
            "num_leaves": 31,
            "learning_rate": 0.05,
            "feature_fraction": 0.8,
            "bagging_fraction": 0.8,
            "bagging_freq": 5,
            "verbose": -1,
            "n_estimators": 100,
        }
        early_stopping_rounds = 10

        model = lgb.LGBMClassifier(**params)

        log(f"[TRAIN] rows train={len(X_train)}, test={len(X_test)}, feats={X_train.shape[1]}")
        model.fit(
            X_train,
            y_train,
            eval_set=[(X_test, y_test)],
            eval_metric="binary_logloss",
            callbacks=[lgb.early_stopping(stopping_rounds=early_stopping_rounds, verbose=False)],
        )

        # 평가
        proba = model.predict_proba(X_test)[:, 1]
        pred = (proba >= 0.5).astype(int)

        acc = float(accuracy_score(y_test, pred))
        prec = float(precision_score(y_test, pred, zero_division=0))
        rec = float(recall_score(y_test, pred, zero_division=0))
        f1 = float(f1_score(y_test, pred, zero_division=0))
        mda_v = mda(y_test.to_numpy(), pred)

        cm = confusion_matrix(y_test, pred).tolist()

        # 저장 폴더 생성
        out_path = args.output
        os.makedirs(os.path.dirname(out_path), exist_ok=True)

        # 모델 + 메타 같이 저장 (ProcessBuilder에서 버전 확인용)
        model_version = f"lgbm_{args.symbol.lower()}_{datetime.utcnow().strftime('%Y%m%d')}"
        pack = {
            "model": model,
            "model_version": model_version,
            "symbol": args.symbol,
            "days": args.days,
            "horizon": args.horizon,
            "features": get_feature_columns(args.use_optional_features),
            "use_optional_features": bool(args.use_optional_features),
            "trained_at_utc": datetime.utcnow().replace(microsecond=0).isoformat() + "Z",
            "best_iteration": int(getattr(model, "best_iteration_", 0) or 0),
        }
        joblib.dump(pack, out_path)

        # stdout: JSON 결과
        json_stdout({
            "ok": True,
            "symbol": args.symbol,
            "days": args.days,
            "rows_used": int(len(X)),
            "train_rows": int(len(X_train)),
            "test_rows": int(len(X_test)),
            "features_used": int(X_train.shape[1]),
            "use_optional_features": bool(args.use_optional_features),
            "horizon_minutes": args.horizon,
            "model_path": out_path,
            "model_version": model_version,
            "metrics": {
                "accuracy": acc,
                "precision": prec,
                "recall": rec,
                "f1": f1,
                "mda": mda_v,
                "confusion_matrix": cm
            }
        })

    except Exception as e:
        detail = exception_to_detail(e)
        log("[TRAIN][ERROR]\n" + detail)
        error_json("TRAIN_FAILED", "training failed", detail=detail, extra={"symbol": args.symbol})


if __name__ == "__main__":
    main()
