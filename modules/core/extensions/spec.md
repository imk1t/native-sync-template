# core/extensions

## 概要
標準型に対する、機能を知らない汎用 Extension を提供する。他モジュール・外部ライブラリには依存しない。標準ライブラリ・Foundation・JDK は利用できる。

## 画面遷移
なし。

## 画面と状態
画面や状態を持たない。入力から値を計算し、元の文字列を変更しない。

## ユーザー操作と結果
| 名前 | 内容 |
| --- | --- |
| `String.isBlankOrEmpty` | 空文字または定義した空白だけなら true |
| `String.codePointCount` | Unicode コードポイント数。見た目の文字数や UTF-16 の長さとは区別する |
| `String.trimmedWhitespace()` | 前後の空白だけを除去する。内部の空白・改行は保持する |

空白判定と除去は同じ集合を使う。対象は U+0009–000D、U+0020、U+0085、U+00A0、U+1680、U+2000–200A、U+2028、U+2029、U+202F、U+205F、U+3000。U+001C–001F、U+200B、U+FEFF は空白として扱わない。OS や標準 API の空白定義に左右されない。

結合文字・絵文字を正規化したり切り詰めたりしない。例えば `😀` は 1、`e` + U+0301 は 2、`👨‍👩‍👧‍👦` は 7 コードポイント。

## 使う API とトークン
なし。値の参照・判定はプロパティ、変換は関数にする。同じ処理の Utils API は重複して公開しない。Message・Route・業務モデルなど、特定モジュールの知識を持つ Extension はそのモジュールに置く。

## OS 固有の差分
iOS のモジュール名は `CoreExtensions`、実装は `Sources/Core/Extensions`、テストは `Tests/Core/Extensions`。Android のパッケージは `com.example.core.extensions`。Swift は String の extension、Kotlin はトップレベルの拡張関数・拡張プロパティとして公開する。コードポイント数は Swift の Unicode scalar 数、Kotlin/JVM の codePointCount で計算する。

## シナリオ
単体テストで空白集合、内部空白の保持、結合文字・絵文字の計数を両 OS で検証する。利用側のシナリオは `specs/scenarios/github-search.yaml` と `specs/scenarios/bookmark-note.yaml`。
