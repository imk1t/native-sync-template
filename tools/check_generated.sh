#!/usr/bin/env bash
# 再生成して差分が出ないことを確認する
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tools/generate.py
# diff だけでは、削除後に再生成された未追跡ファイルを検出できない。
changes="$(git status --porcelain --untracked-files=all -- \
  modules/designsystem/ios/Sources/DesignSystem/Generated \
  modules/designsystem/android/src/main/kotlin/com/example/designsystem/generated \
  modules/core/network/ios/Sources/CoreNetwork/Generated \
  modules/core/network/android/src/main/kotlin/com/example/corenetwork/generated \
  modules/core/navigation/ios/Sources/CoreNavigation/Generated \
  modules/core/navigation/android/src/main/kotlin/com/example/corenavigation/generated)"
if [[ -n "$changes" ]]; then
  printf '%s\n' "$changes"
  echo "生成コードが最新ではありません。tools/generate.py を実行してコミットしてください"
  exit 1
fi
