# ADR 0001: モノレポで仕様だけを共有する

- 状態: 採用
- 決定: iOS と Android を 1 つのモノレポに置き、共有するのは API スキーマ・デザイントークン・機能仕様（spec.md）だけにする。ロジックは各 OS で書く。
- 理由: AI により 2 プラットフォーム分を書くコストが下がった。OS の新機能（Liquid Glass など）に公式フレームワークで即日追随したい。
- 結果: 仕様と両実装を同じ PR で変更できる。ズレは `tools/check-spec-sync.ts` で検知する。

## 補足: iOS のパッケージ構成
各モジュールの `ios/` を個別のローカル Swift Package にすると、同名ディレクトリ（`ios`）が SwiftPM で同一 ID として扱われ衝突する。
そのため `modules/Package.swift` に全 iOS モジュールを target として集約し、各 target の `path` で各モジュールの `ios/` を指す。
