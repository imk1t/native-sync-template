# core/utils

## 概要
専用の型で提供する、機能を知らない汎用処理を置く。内部依存は必要な場合の `core/extensions` のみ許可し、外部ライブラリには依存しない。標準ライブラリ・Foundation・JDK は利用できる。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `DateTimeUtils.formatTime(date, locale, timeZone)` | 指定された日時を、ロケールとタイムゾーンに応じた秒までの時刻に変換する |

書式は OS の標準的な時刻表示（medium）を使い、12/24 時間表記・午前午後の表現はロケールに従う。ロケールとタイムゾーンは呼び出し側が表示時に渡す。日付・タイムゾーン名・ミリ秒は表示しない。翻訳リソース、UI、現在時刻には依存せず、変更可能な formatter を共有しない。

文字列の汎用拡張は `core/extensions` に移管する。数値を含む翻訳文と複数形は `core/localization` の Message を使い、ここでは扱わない。

## OS 固有の差分
モジュール名は iOS が `CoreUtils`、Android のパッケージは `com.example.core.utils`。 iOS の実装は `Sources/Core/Utils`、テストは `Tests/Core/Utils` に配置する。

日時型は Swift の Date / Kotlin の Instant、タイムゾーン型は TimeZone / ZoneId。表示は Foundation の DateFormatter / JDK の DateTimeFormatter を使う。ローカライズされた空白や句読点などの細部は OS の書式に従う。

## 検証
同じ日時を日本語・英語のロケールと異なるタイムゾーンで表示する。日付をまたぐ時差と夏時間も固定日時で検証する。
