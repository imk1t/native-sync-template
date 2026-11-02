#!/bin/bash
# セッション開始時に apm install を実行し、スキルの配置先（.claude/skills/ と .agents/skills/）を作る。
# apm が入っていない環境では何もせず終了する（apm のインストールは利用者に任せる）。
set -euo pipefail
cd "${CLAUDE_PROJECT_DIR:-.}"

if ! command -v apm >/dev/null 2>&1; then
  echo "apm が見つからないため、スキルの配置をスキップしました（README の「セットアップ」を参照）。" >&2
  exit 0
fi

bash tools/setup.sh
