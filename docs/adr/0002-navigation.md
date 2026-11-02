# ADR 0002: 画面遷移は Navigator の状態として 1 通りに書く

- 状態: 採用
- 決定:
  - 画面の一覧（名前・引数・タブ・ディープリンク）を `shared/routes.yaml` に集約し、両 OS の `Route` と `AppTab` を `tools/generate.py` で生成する。
  - 画面遷移は、機能の中でも外でも、すべて `core/navigation` の `Navigator` で行う。`NavigationLink` などで画面を直接開かない。
  - 遷移は `NavigationState`（タブごとのスタックと、重なったモーダル。モーダルはそれぞれ自分のスタックを持つ）として持ち、`Navigator` の 7 つの操作（`push` / `pop` / `popToRoot` / `present` / `dismiss` / `selectTab` / `reset`）で書き換える。push かモーダルかは呼ぶ側が決める。
  - `Route` から画面への対応と、状態の描画はアプリ層（`apps/*`）が持つ。iOS は `TabView` + `NavigationStack(path:)` + `.sheet` / `.fullScreenCover`、Android は `NavigationSuiteScaffold` + Navigation 3 の `NavDisplay` + ボトムシート / 全画面ダイアログ。
- 理由:
  - やり方を 1 つにすると、どこで書くかの判断が要らず、機能を足しても書き方がぶれない。
  - features 同士の依存を禁止したまま（相互遷移で循環しない）、機能をまたぐ遷移とディープリンクを書ける。
  - 遷移の振る舞いを純粋な状態遷移にすると、両 OS 共通のシナリオでテストでき、OS 間の差が出にくい。
  - 描画は各 OS の公式部品に任せるので、Liquid Glass のタブバーや大画面の適応レイアウトに OS の更新だけで追随できる。
- 結果:
  - 画面を足すときは `routes.yaml` に 1 件足し、アプリ層の `Route` → 画面の対応に 1 行足す。登録し忘れると `switch` / `when` の網羅チェックでビルドが落ちる。
  - 画面間で結果を返す仕組みは作らない。結果はデータ層（`core/*`）を通して受け渡す。
  - プロセス終了後のバックスタック復元は対象外（必要になったら `NavigationState` を保存できる形にする）。

## 検討した他の案
- features 同士の依存を許可して直接開く: 相互遷移が循環依存になりビルドできない。変更の影響範囲が広がる。
- 機能内は直接、機能間だけ Route: やり方が 2 つになり、後から外部の入口にしたくなると書き換えが要る。
- コールバックで外に出してアプリが配線する: ディープリンクと両 OS の一致検査の拠り所がない。
