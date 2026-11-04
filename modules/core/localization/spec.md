# core/localization

## 概要
共通・機能別の UI 文言を集約する。翻訳原本は `specs/localization/messages-<言語>.yaml`。日本語を基準言語とし、英語も提供する。対応言語はファイル名から検出し、別の言語一覧は管理しない。

## 提供するもの
生成した `Message` はキーと型付き引数を保持し、表示時にネイティブリソースで解決する。引数は string / integer、複数形は整数引数一つを対象とする。日本語側だけで引数と複数形の契約を宣言する。全言語のキー・引数一致、other 必須、未対応構文を生成前に検査する。

## 言語と表示
OS が選んだアプリ言語を使用する。未対応言語は日本語へフォールバックする。アプリ内切替は設けない。SwiftUI の Locale と Android のリソース Configuration を表示時に参照する。ユーザー入力、GitHub の本文・名前、検索条件の送信値は翻訳しない。数値・複数形は OS の規則に従う。

## リソース管理
iOS は一つの Localizable.xcstrings、Android は言語ごとの strings.xml を生成する。生成物はコミットし、手編集しない。アプリ名の OS メタデータと対応言語設定も同じ原本から生成する。新言語追加は全キーを持つファイルを追加して再生成する。削除時には古い生成リソースも除去する。

## 依存
他モジュールには依存しない。feature とアプリ層から参照する。designsystem には翻訳済みのラベルを渡す。

## OS 固有の差分
モジュール名は iOS が `CoreLocalization`、Android のパッケージは `com.example.core.localization`。

iOS は SwiftPM のリソース bundle と Foundation、Android は Context の getString / getQuantityString を使う。Android 13 以降は OS のアプリ言語設定に対応し、それ以前はシステム言語に従う。

## 検証
生成器の契約検証・エスケープ・引数順序・複数形・言語追加削除を tools のテストで確認する。両 OS のテストで日本語・英語・フォールバック・複数形・引数を確認する。Android は AndroidX Test の instrumentation で実リソースを解決する。ソースはリポジトリ規約の `src/test` に置き、このモジュールのビルド定義で androidTest に接続する。

## シナリオ
- `specs/scenarios/localization.yaml`
