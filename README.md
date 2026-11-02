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

## 各プラットフォームのアーキテクチャ

両 OS とも「MVVM + 単方向データフロー」と、Presentation / Domain / Data の 3 層で揃えます。TCA のような片方の OS にしかないフレームワークは使わず、両 OS の公式機能で同じ形を作ります。対応関係が 1 対 1 であるほど、AI による移植とレビューが正確になります。

### モジュール内のレイヤー

| レイヤー | 責務 | iOS の型 | Android の型 |
| --- | --- | --- | --- |
| Presentation | 画面と画面状態 | `ItemsView` + `ItemsViewModel`（`@Observable`） | `ItemsScreen` + `ItemsViewModel`（`StateFlow`） |
| Domain | ビジネスルール、モデル、Repository の契約 | `ItemsUseCase`, `protocol ItemsRepository` | `ItemsUseCase`, `interface ItemsRepository` |
| Data | Repository の実装、API/DB 型との変換 | `ItemsRepositoryImpl` | `ItemsRepositoryImpl` |

型名とファイル名は両 OS で同じにします。各レイヤーが依存できるのは自分より内側（Presentation → Domain ← Data）だけです。サンプルの `features/items` は小さいため、現状は ViewModel と View だけで構成しています。

### モジュール間の依存ルール

| モジュール | 依存してよい先 |
| --- | --- |
| `apps/*` | すべて（DI の組み立てと画面遷移の配線だけを持つ） |
| `features/*` | `core/*`、`designsystem`（**features 同士は禁止**。画面遷移は `core/navigation` のルート定義を経由） |
| `core/*` | 他の `core/*`（循環禁止） |
| `core/utils` | なし（標準ライブラリの拡張など、機能を知らないコードだけ） |
| `designsystem` | `core/utils` |

`python3 tools/check_deps.py` がこのルールと、iOS と Android の依存関係が一致していることを検査します（CI でも実行）。

### 技術スタック

| 領域 | iOS | Android |
| --- | --- | --- |
| UI | SwiftUI（必要な箇所だけ UIKit） | Jetpack Compose |
| 状態管理 | Observation（`@Observable`） | ViewModel + StateFlow |
| 非同期 | Swift Concurrency | Kotlin Coroutines / Flow |
| DI | イニシャライザ注入（必要なら swift-dependencies） | Hilt |
| 通信 | 生成クライアント + URLSession | 生成クライアント + OkHttp |
| 永続化 | SwiftData | Room |
| ユニットテスト | Swift Testing | JUnit + kotlinx-coroutines-test |
| UI テスト | XCUITest | Compose UI Test |
| 静的解析 | SwiftLint / swift-format | ktlint / detekt |

DI・通信・永続化・静的解析は方針で、テンプレートにはまだ組み込んでいません。

### 最低対応バージョン（案）

- **iOS 18 以上**：Observation と SwiftData を前提にできる。Liquid Glass（iOS 26）などの新 API は `#available` で分岐し、`designsystem` の中に閉じ込める。
- **Android minSdk 28 以上**、targetSdk は常に最新。
- リリース時期が決まった時点で、利用者の OS 分布を見て見直す。

## セットアップ

clone 後に一度だけ実行します。

検査ツールには Python 3.11 以降と PyYAML が必要です。先に `python3 -m venv .venv`、`source .venv/bin/activate`、`python3 -m pip install -r tools/requirements.txt` を実行してください。

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
| トークンと OpenAPI から Swift/Kotlin を生成 | `python3 tools/generate.py` |
| spec と両 OS の同期を検査 | `python3 tools/check_spec_sync.py` |
| 追従を記録 | `python3 tools/check_spec_sync.py --update both modules/features/items` |
| モジュール依存ルールを検査 | `python3 tools/check_deps.py` |
| 共通シナリオと両 OS のテストの対応を検査 | `python3 tools/check_scenarios.py` |
| tools/ 自体のテスト | `python3 -m unittest discover -s tools/tests` |
| iOS のテスト | `cd modules && xcodebuild test -scheme Modules-Package -destination 'platform=iOS Simulator,name=iPhone 16'`（Xcode / macOS） |
| Android のテスト | `cd apps/android && gradle test`（Android SDK が必要） |
| iOS アプリのプロジェクト生成 | `cd apps/ios && xcodegen generate` |
| スキルの原本を変更したあとに再配置 | `apm install` |

作業の進め方は [AGENTS.md](AGENTS.md) を参照してください。

iOS のテストは `Native build` ワークフローと同じく iOS Simulator 上で実行します。Xcode に iPhone 16 の Simulator がない場合は、`xcrun simctl list devices available` で利用可能な機種名を確認し、`name=` を置き換えてください。`swift test` はホストの macOS 向けにビルドするため、iOS 向けのこのパッケージのテストには使いません。

## 現状
- 追加済み: 共通定義（OpenAPI サンプル、トークン、シナリオ）、トークンと OpenAPI からの Swift/Kotlin 生成、サンプル機能 `items`、同期/依存/生成差分/シナリオの検査、CI の雛形。
- 未実装: Firebase 用ラッパーモジュール。アプリは API サーバーが無いためフェイクの `ItemsAPI` を使っています（実サーバーができたら `DefaultItemsAPI(transport: URLSessionTransport(baseURL:))` / `DefaultItemsApi(UrlConnectionTransport(baseUrl))` に差し替えます）。

## 共通定義からの生成と検査

### OpenAPI → API クライアント
`python3 tools/generate.py` が `shared/openapi/openapi.yaml` から、`core/network` に次を生成します（`Generated/`・`generated/` は手で編集しません）。

| 生成物 | iOS | Android |
| --- | --- | --- |
| schema ごとの DTO | `struct ItemDTO: Codable` | `@Serializable data class ItemDTO` |
| operation ごとのメソッド | `APIClient.listItems()` | `ApiClient.listItems()` |

DTO とクライアントは `core/network` の内部（`internal`）に閉じ、手書きの `DefaultItemsAPI` / `DefaultItemsApi` がドメインモデル（`Item`）に変換して公開します。通信は `HTTPTransport` / `HttpTransport` に差し替えられ、テストではスタブを渡します。生成器が対応する OpenAPI の範囲は `tools/openapi_codegen.py` の先頭に書いてあり、範囲外の構文は生成時にエラーになります。

### 共通シナリオ → 両 OS のテスト
`shared/scenarios/*.yaml` の各 step に `id` を付け、両 OS のテストに `// @scenario <シナリオ名>/<id>` と書きます。`python3 tools/check_scenarios.py` は次を検査します。

- すべての step が iOS と Android の両方のテストから参照されている
- `given` の operationId が OpenAPI にあり、2xx の `body` がレスポンスの schema に合っている
- 機能の `spec.md` がシナリオを参照している
