"""
train.py - LightGBM 모델 학습 (Class Weight 적용)

핵심 변경:
1. data_loader.load_features_from_raw() 사용 (LAG 적용)
2. class_weight='balanced' 적용 (Target 불균형 해결)
"""
from __future__ import annotations

import argparse
import os
from datetime import datetime

import joblib
import lightgbm as lgb
import numpy as np
import pandas as pd
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, roc_auc_score
from sklearn.utils.class_weight import compute_class_weight

from data_loader import load_features_from_raw
from feature_engineer import prepare_xy_for_training, get_feature_columns
from utils import log, json_stdout, error_json, exception_to_detail


def mda(y_true: np.ndarray, y_pred: np.ndarray) -> float:
    """MDA (Mean Directional Accuracy)"""
    return float((y_true == y_pred).mean())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--days", type=int, default=7)
    parser.add_argument("--symbol", type=str, default="BTCUSDT")
    parser.add_argument("--output", type=str, default="model/lgbm_btcusdt.pkl")
    parser.add_argument("--use-optional-features", action="store_true")
    parser.add_argument("--horizon", type=int, default=60, help="minutes ahead label (default 60)")
    parser.add_argument("--use-class-weight", action="store_true", default=True, help="apply balanced class weight")
    parser.add_argument("--min-rows", type=int, default=100, help="minimum rows after cleaning (default 100)")
    args = parser.parse_args()

    try:
        # ★ Raw 테이블에서 LAG 적용된 데이터 로드
        log(f"[TRAIN] Loading data from raw tables (LAG applied)...")
        df = load_features_from_raw(symbol=args.symbol, days=args.days)
        
        # 최소 데이터: horizon + 50 (테스트용 완화)
        min_raw_rows = args.horizon + 50
        if df.empty or len(df) < min_raw_rows:
            error_json(
                "DATA_INSUFFICIENT",
                f"not enough rows for training (need >= {min_raw_rows}). rows={len(df)}",
                extra={"symbol": args.symbol, "days": args.days, "rows": int(len(df))}
            )
            return

        X, y, meta = prepare_xy_for_training(df, horizon_minutes=args.horizon, use_optional=args.use_optional_features)

        if len(X) < args.min_rows:
            error_json(
                "DATA_INSUFFICIENT",
                f"after cleaning, not enough rows (need >= {args.min_rows}). rows={len(X)}",
                extra={"symbol": args.symbol, "rows_after_clean": int(len(X))}
            )
            return

        # 시계열 split (80/20)
        n = len(X)
        split = int(n * 0.8)
        X_train, X_test = X.iloc[:split], X.iloc[split:]
        y_train, y_test = y.iloc[:split], y.iloc[split:]

        log(f"[TRAIN] Train: {len(X_train)}, Test: {len(X_test)}")
        log(f"[TRAIN] Target distribution - Train: {y_train.value_counts().to_dict()}, Test: {y_test.value_counts().to_dict()}")

        # ★ Class Weight 계산 (Target 불균형 해결)
        class_weight_dict = None
        if args.use_class_weight:
            classes = np.array([0, 1])
            # y_train에 두 클래스 모두 있어야 함
            if len(y_train.unique()) < 2:
                log("[TRAIN] Warning: Only one class in training data, skipping class weight")
            else:
                weights = compute_class_weight('balanced', classes=classes, y=y_train)
                class_weight_dict = {0: weights[0], 1: weights[1]}
                log(f"[TRAIN] Class weights: DOWN(0)={weights[0]:.2f}x, UP(1)={weights[1]:.2f}x")

        # LightGBM params
        params = {
            "objective": "binary",
            "metric": ["binary_logloss", "auc"],
            "boosting_type": "gbdt",
            "num_leaves": 31,
            "learning_rate": 0.05,
            "feature_fraction": 0.8,
            "bagging_fraction": 0.8,
            "bagging_freq": 5,
            "verbose": -1,
            "n_estimators": 500,
            "random_state": 42,
        }
        
        # ★ Class Weight 적용
        if class_weight_dict:
            params["class_weight"] = class_weight_dict

        model = lgb.LGBMClassifier(**params)

        log(f"[TRAIN] Starting training with {X_train.shape[1]} features...")
        model.fit(
            X_train,
            y_train,
            eval_set=[(X_test, y_test)],
            eval_metric=["binary_logloss", "auc"],
            callbacks=[lgb.early_stopping(stopping_rounds=30, verbose=False)],
        )

        # 평가
        proba = model.predict_proba(X_test)[:, 1]
        pred = (proba >= 0.5).astype(int)

        acc = float(accuracy_score(y_test, pred))
        prec = float(precision_score(y_test, pred, zero_division=0))
        rec = float(recall_score(y_test, pred, zero_division=0))
        f1 = float(f1_score(y_test, pred, zero_division=0))
        mda_v = mda(y_test.to_numpy(), pred)
        
        # ROC-AUC (Test에 두 클래스 모두 있을 때만)
        try:
            auc = float(roc_auc_score(y_test, proba))
        except:
            auc = 0.0

        cm = confusion_matrix(y_test, pred).tolist()
        
        # 예측 분포 확인
        pred_up = int((pred == 1).sum())
        pred_down = int((pred == 0).sum())
        log(f"[TRAIN] Predictions: UP={pred_up}, DOWN={pred_down}")

        # 저장 폴더 생성
        out_path = args.output
        os.makedirs(os.path.dirname(out_path), exist_ok=True)

        # 모델 + 메타 저장
        model_version = f"lgbm_{args.symbol.lower()}_{datetime.utcnow().strftime('%Y%m%d_%H%M')}"
        pack = {
            "model": model,
            "model_version": model_version,
            "symbol": args.symbol,
            "days": args.days,
            "horizon": args.horizon,
            "features": get_feature_columns(args.use_optional_features),
            "use_optional_features": bool(args.use_optional_features),
            "use_class_weight": bool(args.use_class_weight),
            "class_weight": class_weight_dict,
            "trained_at_utc": datetime.utcnow().replace(microsecond=0).isoformat() + "Z",
            "best_iteration": int(getattr(model, "best_iteration_", 0) or 0),
            "data_info": {
                "total_rows": int(len(X)),
                "train_rows": int(len(X_train)),
                "test_rows": int(len(X_test)),
                "target_distribution": y.value_counts().to_dict(),
            }
        }
        joblib.dump(pack, out_path)
        
        # 메타 정보도 JSON으로 저장
        meta_path = out_path.replace(".pkl", "_meta.json")
        import json
        with open(meta_path, "w") as f:
            meta_info = {k: v for k, v in pack.items() if k != "model"}
            # numpy 타입 변환
            def convert(obj):
                if isinstance(obj, np.integer):
                    return int(obj)
                elif isinstance(obj, np.floating):
                    return float(obj)
                elif isinstance(obj, dict):
                    return {k: convert(v) for k, v in obj.items()}
                return obj
            json.dump(convert(meta_info), f, indent=2)

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
            "use_class_weight": bool(args.use_class_weight),
            "horizon_minutes": args.horizon,
            "model_path": out_path,
            "model_version": model_version,
            "metrics": {
                "accuracy": acc,
                "precision": prec,
                "recall": rec,
                "f1": f1,
                "mda": mda_v,
                "roc_auc": auc,
                "confusion_matrix": cm,
                "predictions": {"up": pred_up, "down": pred_down}
            }
        })

    except Exception as e:
        detail = exception_to_detail(e)
        log("[TRAIN][ERROR]\n" + detail)
        error_json("TRAIN_FAILED", "training failed", detail=detail, extra={"symbol": args.symbol})


if __name__ == "__main__":
    main()
