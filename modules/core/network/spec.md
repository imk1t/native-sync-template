# core/network

## 概要
GitHub 公式 OpenAPI の選択定義から生成したクライアントを包み、公開リポジトリ・Issue・ページ情報へ変換する。生成 DTO と APIClient はモジュール外へ公開しない。

## 提供するもの
- GitHubAPI: searchRepositories(query, sort, page)、repository(owner, repo)、issues(owner, repo, state, page)、issue(owner, repo, number)。DefaultGitHubAPI が生成コードを呼ぶ。
- RepositorySummary、GitHubIssue、Page<Value>。ID は Int64 / Long、次ページは Link から取得。検索は 1,000 件を上限にし incomplete_results を公開する。
- HTTPRequest / HTTPResponse は body と headers を保持する。成功時も body と headers を同じ呼び出しの APIResponse に含め、並行呼び出しで共有しない。
- GitHubTransport が Accept、User-Agent、X-GitHub-Api-Version: 2026-03-10 を付ける。レスポンスヘッダー名は小文字化する。
- GitHub の 403 / 429 は残量・Retry-After・エラー body で分類し、制限時は再試行可能時刻を返す。通常の 403 と区別する。待機中は同じリソースへの再送を抑制する。
- ImageLoader は HTTPS 画像のデータを取得し、サイズを制限したメモリキャッシュを持つ。UI には依存しない。

## 生成とエラー
shared/openapi/github-source.json は固定コミットの公式定義から抽出した原本。github-selection.json のフィールド選択を適用して openapi.yaml を生成し、両 OS のコードへ変換する。通常の生成はオフラインで完結する。更新方法は docs/adr/0003-github-openapi.md。

省略可能と null 許容を別々に検証する。入力 enum の値は契約検査で確認する。レスポンスの未知のキーは無視し、状態文字列の未知値はそのまま表示する。空本文・不明な投稿者も表示可能。Issue 一覧から pull_request を除外するが、次ページ情報は除外前の HTTP ヘッダーに従う。

## OS 固有の差分
略語は Swift の API / HTTP と Kotlin の Api / Http。JSON は Codable / kotlinx.serialization。HTTP は URLSession / HttpURLConnection。Android は接続 15 秒・読み込み 30 秒のタイムアウトを持ち、キャンセル時は接続を切断・割り込みする。iOS は URLSession の task キャンセルに従う。画像データは Data / ByteArray。

## シナリオ
- `shared/scenarios/github-network.yaml`
