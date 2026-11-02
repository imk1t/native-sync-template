# ADR 0003: 生成・検査ツールを Bun（TypeScript）で書き、mise でバージョンを管理する

- 状態: 採用
- 決定: `tools/` のコード生成と検査を Python から Bun（TypeScript）に移し、Python への依存をなくす。Bun・ktlint・SwiftLint のバージョンは `mise.toml` で固定し、ローカルと CI で同じものを使う。整形は Bun 組み込みの `bun fmt`、型検査は `tsc --noEmit` で行う。
- 理由:
  - Python は 3.11 以上（`tomllib`）が必要で、macOS 標準の python3 では動かず、venv と pip の手順が要った。Bun は mise で 1 行で入る。
  - YAML・TOML の読み込みとテストランナーが Bun に組み込まれているため、実行時の npm 依存はない（`package.json` は型検査用の `typescript` と `@types/bun` だけ）。
  - 生成器は OpenAPI の型を組み合わせて扱うため、TypeScript の型検査が効く。
- 結果:
  - 生成される Swift / Kotlin は、先頭のコメント（生成元のファイル名）以外は移行前と同じ。
  - YAML は 1.2 として読む。PyYAML（1.1）と違い `yes`・`on` などは真偽値にならず文字列になる。真偽値は `true` / `false` で書く。
  - mise と apm のインストールは利用者が行う（README の「セットアップ」）。
