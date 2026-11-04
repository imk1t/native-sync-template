# shared/designsystem

## 概要
`specs/tokens` から生成した値と、それを使う共通 UI 部品を置く。

`modules/shared/designsystem` に配置する。依存先は `core/utils` のみとし、他の `shared/*`、features、通信・保存処理には依存しない。他の `shared/*` からの依存も禁止する。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `Tokens` | 色・余白の定数（`tools/generate.ts` で生成。手で編集しない） |

## 共通 UI
LoadingIndicator、EmptyState、ErrorState、StatusBadge を提供する。エラーは読み上げ可能な文言と再試行操作を持つ。バッジは色だけに依存せず文字でも状態を示す。文字サイズは OS の設定に従う。

RepositoryRow は RepositoryRowData（文字列・数値だけの表示値）と画像取得処理を受け取り、検索・詳細・保存済みの行表示を共有する。API モデルや保存処理には依存しない。画像取得に失敗したら代替表示にする。

## テーマ
画面とシステムバーは OS のライト・ダーク設定に従い、背景と文字・アイコンのコントラストを保つ。起動中に OS の明暗設定を変更した場合も追従する。

## OS 固有の差分
モジュール名は iOS が `DesignSystem`、Android のパッケージは `com.example.shared.designsystem`。

iOS 26 以降は Liquid Glass を使う部品を `#available` で分岐し、このモジュールの中に閉じ込める。
- iOS は SwiftUI 標準の背景・文字色とシステムバーの自動切り替えを使用する。
- Android はアプリ層の組み立てで Material 3 のライト・ダーク配色を選び、`enableEdgeToEdge()` のシステムバーの明暗設定と揃える。起動ウィンドウのテーマも `values` / `values-night` で切り替える。

- 行の画像未取得時は iOS では人物アイコン、Android では所有者名の先頭文字を表示する。Star は iOS ではアイコン、Android では文字ラベルを使う。
