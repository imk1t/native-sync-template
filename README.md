# native-sync-template

Swift (iOS) と Kotlin (Android) をそれぞれネイティブで実装し、**仕様だけを共有して同期を保つ**ためのモノレポ雛形です。

```
apps/ios, apps/android          アプリ本体（組み立てだけ）
shared/openapi, tokens, scenarios  共通で管理する定義
modules/core/<x>                基盤（utils, network）  spec.md + ios/ + android/
modules/designsystem            トークンから生成した値と共通 UI
modules/features/<x>            機能（サンプル: items）
tools/                          コード生成・同期/依存チェック
```

## セットアップ

clone 後に一度だけ実行します。

1. apm（Agent Package Manager）をインストールする（どれか 1 つ）。

   ```bash
   brew install apm          # macOS
   pip install apm-cli       # Python がある環境
   ```

2. エージェント用スキルを配置する。

   ```bash
   bash tools/setup.sh       # 中身は apm install
   ```

   `.apm/skills/` の原本が `.claude/skills/` と `.agents/skills/` に配置されます（どちらもコミットしません）。
   Claude Code では、apm が入っていればセッション開始時に同じ処理が自動で走ります。

## よく使うコマンド
| 目的 | コマンド |
| --- | --- |
| トークンから Swift/Kotlin を生成 | `python3 tools/generate.py` |
| spec と両 OS の同期を検査 | `python3 tools/check_spec_sync.py` |
| 追従を記録 | `python3 tools/check_spec_sync.py --update both modules/features/items` |
| モジュール依存ルールを検査 | `python3 tools/check_deps.py` |
| iOS のテスト | `cd modules && swift test`（Xcode / macOS） |
| Android のテスト | `cd apps/android && gradle test`（Android SDK が必要） |
| iOS アプリのプロジェクト生成 | `cd apps/ios && xcodegen generate` |
| スキルの原本を変更したあとに再配置 | `apm install` |

作業の進め方は [AGENTS.md](AGENTS.md) を参照してください。

## 現状
- 追加済み: 共通定義（OpenAPI サンプル、トークン）、トークンの Swift/Kotlin 生成、サンプル機能 `items`、同期/依存/生成差分の検査、CI の雛形。
- 未実装: OpenAPI からの API クライアント生成（現状は手書きのインターフェースとフェイク）、共通シナリオを読むテストランナー（現状はシナリオと同じ内容を各 OS のテストに手書き）、Firebase 用ラッパーモジュール。
