# このリポジトリで AI エージェントが守ること

## 構成
- `apps/{ios,android}`: 組み立てだけ。ロジックは置かない。
- `shared/`: API スキーマ(openapi)、デザイントークン(tokens)、共通シナリオ(scenarios)、外部ライブラリ対応表(third-party.yaml)。単一の正。
- `modules/<core|designsystem|features>/<名前>/{spec.md, ios, android}`: 機能・基盤。`spec.md` が同期の基準。

## 機能を追加・変更するとき（必ずこの順）
1. `modules/**/spec.md` を更新する（必要なら `shared/openapi`、`shared/tokens`、`shared/scenarios` も）。
2. 片方の OS を実装する。
3. もう片方を**同じ型名・同じ構造**で実装する。違いは spec.md の「OS 固有の差分」に書く。
4. `python3 tools/check_spec_sync.py --update both <モジュールパス>` で追従を記録する。
5. `python3 tools/check_deps.py`、`bash tools/check_generated.sh`、`python3 tools/check_spec_sync.py` を通す。
6. 両 OS の変更を 1 つの PR にまとめる。

## 守ること
- `Generated/` と `generated/` 配下は手で編集しない。`python3 tools/generate.py` で再生成する。
- 依存ルール: `core/utils` は何にも依存しない / `core/*` は他の `core/*` のみ / `designsystem` は `core/utils` のみ / `features/*` は `core/*` と `designsystem` のみ（features 同士は禁止）。
- 外部ライブラリは `core/*` のラッパー経由で使い、`shared/third-party.yaml` に両 OS の対応を書く。
- iOS のモジュールは `modules/Package.swift` に追加する（理由は docs/adr/0001）。Android は `apps/android/settings.gradle.kts` に追加する。

## スキル（apm で管理）
- スキルの原本は `.apm/skills/<名前>/SKILL.md`。`apm.yml` の targets に従って `.claude/skills/` と `.agents/skills/` に配置される。配置先は**コミットしない**（.gitignore 済み）。
- clone 後は `bash tools/setup.sh`（中身は `apm install`。apm が無ければ `pip install apm-cli` で入れる）。Claude Code では SessionStart フック（`.claude/hooks/session-start.sh`）が同じことを自動で行う。
- 原本を直したら `apm install` をやり直す。配置先は手で編集しない。
- `apm compile` は実行しない（AGENTS.md を生成する機能で、この AGENTS.md は手書きのため）。
- 現在のスキル: `sync-feature`（機能の追加・変更）、`add-module`（モジュール追加）、`add-third-party`（外部ライブラリ追加）。
