#!/usr/bin/env bash
# 再生成して差分が出ないことを確認する
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tools/generate.py
# diff だけでは、削除後に再生成された未追跡ファイルを検出できない。
changes="$(git status --porcelain --untracked-files=all -- modules/designsystem)"
if [[ -n "$changes" ]]; then
  printf '%s\n' "$changes"
  echo "生成コードが最新ではありません。tools/generate.py を実行してコミットしてください"
  exit 1
fi
