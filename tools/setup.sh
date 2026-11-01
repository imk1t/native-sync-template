#!/usr/bin/env bash
# clone 後のセットアップ。エージェント用スキルを apm で配置する（何度実行しても安全）。
# apm 自体はインストールしない。先に各自で入れておくこと（README の「セットアップ」）。
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v apm >/dev/null 2>&1; then
  echo "apm が見つかりません。README の「セットアップ」に沿って apm をインストールしてから再実行してください。" >&2
  exit 1
fi

apm install
