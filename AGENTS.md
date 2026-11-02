# AGENTS.md

iOS(Swift)と Android(Kotlin)をネイティブで同時に開発するモノレポ。両 OS の整合は `modules/**/spec.md` を基準に保つ。

## 構成
- `apps/{ios,android}`: 組み立てだけ。ロジックは置かない。
- `shared/`: 単一の正。`openapi`(API)、`tokens`(デザイントークン)、`scenarios`(共通シナリオ)、`third-party.yaml`(外部ライブラリ対応表)。
- `modules/<core|designsystem|features>/<名前>/{spec.md, ios, android}`: 機能・基盤。`spec.md` が同期の基準。
- `docs/adr/`: 設計判断の記録。

## コマンド
変更後は次をすべて通す。
```
python3 tools/generate.py            # トークン・OpenAPI からの再生成（変更したとき）
python3 tools/check_deps.py          # 依存ルール
bash tools/check_generated.sh        # 生成物が最新か
python3 tools/check_spec_sync.py     # spec と両 OS の追従
python3 tools/check_scenarios.py     # シナリオと @scenario マーカー
```
追従の記録: `python3 tools/check_spec_sync.py --update both <モジュールパス>`(実装していない OS は記録しない)。

## 手順はスキルを使う
- 機能・基盤の追加や変更 → `sync-feature`(spec 先行 → 一方の OS → もう一方を同じ型名・同じ構造で → 検査 → 1 つの PR)
- モジュールの新規追加 → `add-module`
- 外部ライブラリの追加 → `add-third-party`

## ルール
- `Generated/` と `generated/` 配下は手で編集しない。`tools/generate.py` で再生成する。
- 生成された DTO・APIClient は `core/network` の外に出さない。手書きのラッパー(例: `DefaultItemsAPI`)でドメインモデルに変換して公開する。
- OS 間の意図した違いは `spec.md` の「OS 固有の差分」に書く。書かれていない違いはバグ。
- `shared/scenarios` の各 step は、両 OS のテストに `// @scenario <シナリオ名>/<id>` を書いて実装する。
- 依存: `core/utils` は何にも依存しない / `core/*` は他の `core/*` のみ / `designsystem` は `core/utils` のみ / `features/*` は `core/*` と `designsystem` のみ(features 同士は禁止)。
- 外部ライブラリは `core/*` のラッパー経由で使い、`shared/third-party.yaml` に両 OS の対応を書く。AndroidX・Kotlin・kotlinx は公式基盤として `core/utils` 以外から直接使え、対応表の対象外。
- iOS のモジュールと外部依存(`.product(name:package:)`)は `modules/Package.swift` に、Android は `apps/android/settings.gradle.kts` に追加する(理由: `docs/adr/0001`)。
- テストは OS 標準の場所に置く(iOS: `ios/Tests`、Android: `android/src/test`)。
- 両 OS の変更は 1 つの PR にまとめる。

## スキルの管理(apm)
- 原本は `.apm/skills/<名前>/SKILL.md`。配置先の `.claude/skills/`・`.agents/skills/` はコミットせず、手で編集しない。原本を直したら `apm install` をやり直す。
- clone 後は `bash tools/setup.sh`。apm のインストールは利用者が行う(README の「セットアップ」)。エージェントは apm をインストールしない。
- `apm compile` は実行しない(この AGENTS.md は手書きのため)。
- スキルは短く保ち、`description` に「何をするか」と「いつ使うか」を書く。
