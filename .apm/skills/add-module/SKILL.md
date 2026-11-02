---
name: add-module
description: modules/ 配下に新しいモジュール（features/<名前>、core/<名前>、designsystem など）を iOS と Android の両方に追加するときの手順。
---

# モジュールの追加

## 置き場所と名前
- 機能: `modules/features/<名前>/`、基盤: `modules/core/<名前>/`。
- 中身は `spec.md`、`ios/`、`android/`。名前は両 OS で対応させる（iOS の `FeatureLogin` ⇔ Android の `:features:login`）。

## 手順
1. 依存ルールを確認する（AGENTS.md）。`core/utils` は何にも依存しない、`core/*` は他の `core/*` のみ、`designsystem` は `core/utils` のみ、`features/*` は `core/*` と `designsystem` のみ。
2. `spec.md` を書く。既存の `modules/features/items/spec.md` と同じ見出し（概要、画面と状態、ユーザー操作と結果、使う API とトークン、OS 固有の差分、シナリオ）にする。
3. iOS: `modules/Package.swift` に target を**1 行 1 target**で足す。`path:` は `<module>/ios/Sources/<Target>`。products にも `.library` を足す。テストは `<module>/ios/Tests/<Target>Tests` に置き、`.testTarget(name: "<Target>Tests", dependencies: ["<Target>"], path: ...)` を足す。個別の `Package.swift` は作らない（同名の `ios/` が SwiftPM で衝突するため。docs/adr/0001）。
4. Android: `modules/<module>/android/build.gradle.kts` を作り（実装は `src/main/kotlin/`、テストは `src/test/kotlin/`）、`apps/android/settings.gradle.kts` に `module(":<名前>", "<module のパス>")` を足す。JVM ターゲットは 17（`kotlin { jvmToolchain(17) }`）。
5. 他のモジュールへの依存は、iOS は `Package.swift` の `dependencies: [...]`、Android は `project(":...")` で書き、**両 OS で同じ依存**にする。
6. 画面から使うなら `apps/ios` と `apps/android` に組み立てを足す（ロジックは置かない）。
7. `python3 tools/check_spec_sync.py --update both <モジュールのパス>` を実行し、`check_deps.py`・`check_spec_sync.py` を通す。
8. 機能の中身は `sync-feature` の手順で実装する。
