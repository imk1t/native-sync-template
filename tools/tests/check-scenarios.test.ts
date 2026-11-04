/** 共通シナリオの検査を一時コピーで検証する。 */
import { afterEach, beforeEach, describe, expect, test } from "bun:test";
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { main } from "../check-scenarios";
import { capture, copyRepo, replace } from "./helpers";

const SCENARIO = "specs/scenarios/github-search.yaml";
const IOS_TEST = "modules/features/search/ios/Tests/FeatureSearchTests/RepositorySearchViewModelTests.swift";
const ANDROID_TEST =
  "modules/features/search/android/src/test/kotlin/com/example/featuresearch/RepositorySearchViewModelTest.kt";

describe("check-scenarios", () => {
  let root: string;
  let cleanup: () => void;
  beforeEach(() => {
    ({ root, cleanup } = copyRepo(["specs", "modules"]));
  });
  afterEach(() => cleanup());

  function expectFails(message: string) {
    const { code, output } = capture(main, root);
    expect(output).toContain(message);
    expect(code).toBe(1);
  }
  function updateGiven(given: unknown) {
    const path = join(root, SCENARIO);
    const doc = Bun.YAML.parse(readFileSync(path, "utf8")) as { steps: { given: unknown }[] };
    doc.steps[0]!.given = given;
    writeFileSync(path, Bun.YAML.stringify(doc));
  }
  test("現在のリポジトリは通る", () => {
    const { code, output } = capture(main, root);
    expect(code, output).toBe(0);
  });
  test.each([
    [IOS_TEST, "ios"],
    [ANDROID_TEST, "android"],
  ])("片方の OS に step が無いと失敗する: %s", (file, platform) => {
    replace(root, file, "// @scenario github-search/load-fails\n", "");
    expectFails(`features/search/${platform}: \`@scenario github-search/load-fails\``);
  });
  test("存在しない step への参照は失敗する", () => {
    replace(root, IOS_TEST, "// @scenario github-search/load-fails", "// @scenario github-search/typo");
    expectFails("存在しないシナリオ step を参照しています: github-search/typo");
  });
  test("存在しない operationId は失敗する", () => {
    updateGiven({ listThings: { status: 200, body: [] } });
    expectFails("specs/openapi に無い operationId です");
  });
  test("schema に合わない body は失敗する", () => {
    updateGiven({ "search/repos": { status: 200, body: { total_count: "1", incomplete_results: false, items: [] } } });
    expectFails("int32 ではありません");
  });
  test("step id の重複は失敗する", () => {
    replace(root, SCENARIO, "id: load-fails", "id: load-succeeds");
    expectFails("重複しています: load-succeeds");
  });
  test("spec.md がシナリオを参照していること", () => {
    replace(root, "modules/features/search/spec.md", "`specs/scenarios/github-search.yaml`", "なし");
    expectFails("specs/scenarios/github-search.yaml を参照してください");
  });
  test.each([
    [{ localState: { values: { key: 1 } } }, "保存キーと文字列"],
    [{ localState: { writeFails: "true" } }, "boolean が必要"],
    [{ localState: { unknown: true } }, "未知のキー"],
    [{ "search/repos": { status: 429, headers: { "Retry-After": 120 } } }, "文字列の辞書"],
    [{ "search/repos": { status: 403, body: "error" } }, "エラー body は辞書"],
    [{ "search/repos": { pending: true, headers: {} } }, "pending: true は単独"],
  ])("不正な追加形式を拒否する: %j", (given, error) => {
    updateGiven(given);
    expectFails(error as string);
  });
});
