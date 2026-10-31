#!/usr/bin/env bash
# 再生成して差分が出ないことを確認する
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tools/generate.py
git diff --exit-code -- modules/designsystem || { echo "生成コードが最新ではありません。tools/generate.py を実行してコミットしてください"; exit 1; }
