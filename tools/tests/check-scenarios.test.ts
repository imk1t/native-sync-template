/** 共通シナリオの検査を一時コピーで検証する。 */
import { afterEach, beforeEach, describe, expect, test } from "bun:test";
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { main } from "../check-scenarios";
import { capture, copyRepo, replace } from "./helpers";

const SCENARIO = "shared/scenarios/items-list.yaml";
const IOS_TEST = "modules/features/items/ios/Tests/FeatureItemsTests/ItemsViewModelTests.swift";
const ANDROID_TEST = "modules/features/items/android/src/test/kotlin/com/example/featureitems/ItemsViewModelTest.kt";

describe("check-scenarios", () => {
  let root: string;
  let cleanup: () => void;
  beforeEach(() => {
    ({ root, cleanup } = copyRepo(["shared", "modules"]));
  });
  afterEach(() => cleanup());

  function expectFails(message: string) {
    const { code, output } = capture(main, root);
    expect(output).toContain(message);
    expect(code).toBe(1);
  }

  test("現在のリポジトリは通る", () => {
    const { code, output } = capture(main, root);
    expect(code, output).toBe(0);
  });

  test.each([
    [IOS_TEST, "ios"],
    [ANDROID_TEST, "android"],
  ])("片方の OS に step が無いと失敗する: %s", (file, platform) => {
    const original = readFileSync(join(root, file), "utf-8");
    replace(root, file, "// @scenario items-list/load-fails\n", "");
    expectFails(`features/items/${platform}: \`@scenario items-list/load-fails\``);
    writeFileSync(join(root, file), original);
  });

  test("存在しない step への参照は失敗する", () => {
    replace(
      root,
      IOS_TEST,
      "// @scenario items-list/load-fails",
      "// @scenario items-list/load-fails\n    // @scenario items-list/typo",
    );
    expectFails("存在しないシナリオ step を参照しています: items-list/typo");
  });

  test("存在しない operationId は失敗する", () => {
    replace(root, SCENARIO, "listItems:", "listThings:");
    expectFails("shared/openapi に無い operationId です");
  });

  test("schema に合わない body は失敗する", () => {
    replace(root, SCENARIO, '{ id: "1", title: "First" }', '{ id: 1, title: "First" }');
    expectFails("string ではありません");
  });

  test("step id の重複は失敗する", () => {
    replace(root, SCENARIO, "id: load-fails", "id: load-succeeds");
    expectFails("重複しています: load-succeeds");
  });

  test("spec.md がシナリオを参照していること", () => {
    replace(root, "modules/features/items/spec.md", "`shared/scenarios/items-list.yaml`", "なし");
    expectFails("shared/scenarios/items-list.yaml を参照してください");
  });
});
