# AGENTS.md

iOS(Swift)と Android(Kotlin)をネイティブで同時に開発するモノレポ。両 OS の整合は `modules/**/spec.md` を基準に保つ。

## 構成
- `apps/{ios,android}`: 組み立てだけ。ロジックは置かない。
- アプリ層は起動・AppRoot の枠・AppDependencies の共有依存・AppRouter の対応表を持つ。個別画面と ViewModel の生成・保持は各 feature の公開入口 `*Screen` に置く。
- `specs/`: 単一の正。`openapi`(API)、`tokens`(デザイントークン)、`routes.yaml`(画面の一覧)、`scenarios`(共通シナリオ)、`third-party.yaml`(外部ライブラリ対応表)。
- `modules/<core|shared|designsystem|features>/<名前>/{spec.md, ios, android}`: 機能・基盤。`spec.md` が同期の基準。
- `modules/shared/*`: 複数 feature で使うモデルと処理。ルートの `specs/` は仕様・生成元で、共通実装とは分ける。
- `docs/adr/`: 設計判断の記録。

## コマンド
ツールのバージョンは `mise.toml`、検査スクリプトは Bun(TypeScript)。変更後は次を通す。
```
bun tools/generate.ts     # トークン・OpenAPI・routes からの再生成（変更したとき）
mise run check            # 整形・型検査・tools のテスト・依存ルール・生成物・spec の追従・シナリオ（CI と同じ）
mise run lint             # SwiftLint・ktlint(Swift/Kotlin を変更したとき)
mise run fmt              # tools/ の TypeScript を変更したとき(bun fmt)
```
追従の記録: `bun tools/check-spec-sync.ts --update both <モジュールパス>`(実装していない OS は記録しない)。

## 手順はスキルを使う
- 機能・基盤の追加や変更 → `sync-feature`(spec 先行 → 一方の OS → もう一方を同じ型名・同じ構造で → 検査 → 1 つの PR)
- モジュールの新規追加 → `add-module`
- 外部ライブラリの追加 → `add-third-party`

## ルール
- `Generated/` と `generated/` 配下は手で編集しない。`tools/generate.ts` で再生成する。
- 生成された DTO・APIClient は `core/network` の外に出さない。手書きのラッパー(例: `DefaultGitHubAPI`)でドメインモデルに変換して公開する。
- 画面遷移はすべて `core/navigation` の `Navigator`(`push`・`present` など)で行う。`NavigationLink` などで画面を直接開かない。画面を足すときは `specs/routes.yaml` に足し、その機能の spec.md の「画面遷移」に書き、アプリ層の `Route` → 画面の対応に足す(`docs/adr/0002`)。
- OS 間の意図した違いは `spec.md` の「OS 固有の差分」に書く。書かれていない違いはバグ。
- `specs/scenarios` の各 step は、両 OS のテストに `// @scenario <シナリオ名>/<id>` を書いて実装する。
- 依存: `core/utils` は何にも依存しない / `core/*` は他の `core/*` のみ / `designsystem` は `core/utils` のみ / `shared/*` は `core/*` のみ / `features/*` は `shared/*`・`core/*` と `designsystem` のみ(features 同士は禁止)。
- 外部ライブラリは `core/*` のラッパー経由で使い、`specs/third-party.yaml` に両 OS の対応を書く。AndroidX・Kotlin・kotlinx は公式基盤として `core/utils` 以外から直接使え、対応表の対象外。
- iOS のモジュールと外部依存(`.product(name:package:)`)は `modules/Package.swift` に、Android は `apps/android/settings.gradle.kts` に追加する(理由: `docs/adr/0001`)。
- Android のビルドは `apps/android` の `./gradlew` で行う(Gradle 8.14.3 を `gradle/wrapper` で固定。JDK 17 が必要)。Gradle のバージョンは `./gradlew wrapper --gradle-version <版>` で上げ、手元の `gradle` は使わない。
- テストは OS 標準の場所に置く(iOS: `ios/Tests`、Android: `android/src/test`)。
- 両 OS の変更は 1 つの PR にまとめる。
- ワークフローの `uses:` は正確なバージョン(`@v4.4.0`)で書き、変えたら `gh actions-lock` で `.github/workflows/actions.lock` を更新する(手で編集しない)。

## スキルの管理(apm)
- 原本は `.apm/skills/<名前>/SKILL.md`。配置先の `.claude/skills/`・`.agents/skills/` はコミットせず、手で編集しない。原本を直したら `apm install` をやり直す。
- clone 後は `mise install` と `mise run setup`。mise・apm のインストールは利用者が行う(README の「セットアップ」)。エージェントは mise・apm をインストールしない。
- `apm compile` は実行しない(この AGENTS.md は手書きのため)。
- スキルは短く保ち、`description` に「何をするか」と「いつ使うか」を書く。
