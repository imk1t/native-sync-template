# ADR 0003 GitHub の公式 OpenAPI を必要な範囲に絞って使う

GitHub 閲覧サンプルでは、公式 github/rest-api-description の OpenAPI 3.0、API バージョン 2026-03-10 を使う。

アプリが使用する operation・パラメータ・フィールドは `github-selection.json` で管理する。更新スクリプトが固定コミットの公式定義を取得し、選択ルールを適用して直接 `openapi.yaml` へ保存する。原本や参照付き schema の中間スナップショットは保存しない。

通常の `tools/generate.ts` は `openapi.yaml` だけを API 契約として読み、Swift / Kotlin を生成する。契約自体は書き換えず、通常の生成・CI はネットワークに依存しない。公式定義の型・必須性・null 許容・enum を保持し、未対応構文は更新時に失敗させる。GitHub の全 API や SDK をアプリに持ち込まない。

取得元のリポジトリ・コミット SHA・パス・原本の SHA-256・選択ルールの SHA-256 は、契約の `x-github-source` にまとめる。CI は選択ルールと契約のチェックサムを照合し、契約の更新忘れを検出する。ライセンスは `GITHUB-LICENSE.md` として保存し、`shared/openapi` の管理対象はこの3ファイルに絞る。

生成 DTO とクライアントは core/network 内に閉じ、手書きの GitHubAPI ラッパーがドメインモデルとページ情報へ変換する。レスポンスヘッダーは各呼び出しの結果に保持し、Link とレート制限に利用する。

## 更新手順

固定元は `shared/openapi/openapi.yaml` の `x-github-source`、フィールド選択は `github-selection.json` に記録する。公式定義のバージョンを変えなくても、選択ルールを変更した場合は同じ固定コミットから更新コマンドを実行する。

```sh
# github/rest-api-description の更新対象コミットを指定
bun tools/sync-github-openapi.ts <40桁のコミットSHA>
bun tools/generate.ts
mise run check
```

取得元・APIバージョンは更新処理の `tools/lib/github-openapi.ts` に固定している。APIバージョンを変更するときは、抽出元パスと生成契約・両OSのGitHubTransportヘッダーも合わせて変更する。更新差分で型・必須性・null許容とライセンスを確認し、生成物を入力と同じ変更に含める。

この構成では原本の保管・同期・検査が不要になる一方、選択ルールの変更にも公式定義の再取得が必要になる。ネットワークが必要なのは明示的な更新コマンドだけで、日常の生成・ビルドには影響しない。
