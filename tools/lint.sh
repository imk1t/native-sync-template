#!/usr/bin/env bash
# Swift と Kotlin の静的解析をまとめて実行する。CI の lint ジョブと同じ設定・バージョンを使う。
#   bash tools/lint.sh
# SwiftLint (brew install swiftlint) と ktlint (brew install ktlint) を事前に入れておくこと。
# 生成コード（Generated/・generated/）は対象外。CI で使うバージョンは .github/workflows/ci.yml を参照。
set -euo pipefail
cd "$(dirname "$0")/.."

status=0

if command -v swiftlint >/dev/null 2>&1; then
  swiftlint lint --strict || status=1
else
  echo "swiftlint が見つかりません（brew install swiftlint）" >&2
  status=1
fi

if command -v ktlint >/dev/null 2>&1; then
  ktlint "**/*.kt" "**/*.kts" "!**/generated/**" "!**/build/**" || status=1
else
  echo "ktlint が見つかりません（brew install ktlint）" >&2
  status=1
fi

exit $status
