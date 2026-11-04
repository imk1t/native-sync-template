# ADR 0003: Gradle を wrapper で固定する

- 状態: 採用
- 決定: `apps/android` に Gradle Wrapper を置き、Gradle 8.14.3 に固定する。ビルドは `./gradlew` で行い、CI も同じ wrapper を使う。
- 理由: 手元の Gradle に依存すると、ローカルと CI でバージョンがずれる。実際に Gradle 9.x では `settings.gradle.kts` の `:core` が設定エラーになった。
- 結果: clone した人は誰でも同じバージョンでビルドできる。上げるときは `./gradlew wrapper --gradle-version <版>` で `gradle-wrapper.properties` を更新する。`distributionSha256Sum` を書いて取得物を検証する。
