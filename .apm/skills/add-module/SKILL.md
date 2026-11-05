---
name: add-module
description: modules/ 配下に新しいモジュール（features/<名前>、core/<名前>、shared/<名前>）を iOS と Android の両方に新規作成するときに使う。Package.swift と settings.gradle.kts への登録まで含む。既存モジュールの変更は sync-feature を使う。
---

# モジュールの追加

## 置き場所と名前
- 機能: `modules/features/<名前>/`、基盤: `modules/core/<名前>/`、feature 共通の UI・モデル・処理: `modules/shared/<名前>/`。
- 共通 UI とトークンは `shared/designsystem`（依存先は `core/utils`・`core/extensions` のみ）。その他の shared の依存先は core のみ。shared 同士は依存しない。
- 汎用の標準型拡張は `core/extensions`（内部依存なし）、専用の型で提供する汎用処理は `core/utils`（依存先は `core/extensions` のみ）。両者に外部ライブラリを追加しない。機能固有の Extension はその機能に置く。
- 中身は `spec.md`、`ios/`、`android/`。名前は両 OS で対応させる（iOS の `FeatureLogin` ⇔ Android の `:features:login`）。

## 手順
1. AGENTS.md の依存ルールに沿って、置き場所と依存先を決める。
2. `spec.md` を書く。既存の `modules/features/search/spec.md` と同じ見出し（概要、画面遷移、画面と状態、ユーザー操作と結果、使う API とトークン、OS 固有の差分、シナリオ）にする。画面を持つ機能は `core/navigation` に依存し、画面を `specs/routes.yaml` に足す。
3. iOS: `modules/Package.swift` に target を**1 行 1 target**で足し、products に `.library` も足す。`path:` は `<module>/ios/Sources/<Group>/<Name>`（例: `features/login/ios/Sources/Features/Login`）。テストは `<module>/ios/Tests/<Group>/<Name>` に置き、`.testTarget(name: "<Target>Tests", dependencies: ["<Target>"], path: ...)` を足す。target 名は `FeatureLogin` など一意な名前を使う。個別の `Package.swift` は作らない（`ios/` が SwiftPM で衝突するため。docs/adr/0001）。
4. Android: `modules/<module>/android/build.gradle.kts` を作り（実装は `src/main/kotlin/`、テストは `src/test/kotlin/`）、`apps/android/settings.gradle.kts` に `module(":<名前>", "<module のパス>")` を足す。JVM ターゲットは 17（`kotlin { jvmToolchain(17) }`）。
5. 他モジュールへの依存は、iOS は `Package.swift` の `dependencies`、Android は `project(":...")` で書き、**両 OS で同じ依存**にする。
6. 画面から使うなら `apps/ios` と `apps/android` に組み立てだけを足す。
7. `bun tools/check-spec-sync.ts --update both <モジュールのパス>` を実行し、`bun tools/check-deps.ts` と `bun tools/check-spec-sync.ts` を通す。
8. 機能の中身は `sync-feature` の手順で実装する。
