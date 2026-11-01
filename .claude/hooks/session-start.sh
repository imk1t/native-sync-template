#!/bin/bash
# セッション開始時に apm install を実行し、スキルの配置先（.claude/skills/ と .agents/skills/）を作る。
set -euo pipefail
cd "${CLAUDE_PROJECT_DIR:-.}"
bash tools/setup.sh
