#!/usr/bin/env bash
# clone 後のセットアップ。エージェント用スキルを apm で配置する（何度実行しても安全）。
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v apm >/dev/null 2>&1; then
  echo "apm が見つからないため pip で入れます（brew install apm でも可）"
  python3 -m pip install --quiet apm-cli
fi

apm install
