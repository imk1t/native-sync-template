/** ビルド定義・spec・外部 SDK の境界を一時コピーで検証する。 */
import { afterEach, beforeEach, describe, expect, test } from "bun:test";
import { mkdirSync, readFileSync, rmSync, unlinkSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import * as deps from "../check-deps";
import { check } from "../check-spec-sync";
import { glob } from "../lib/util";
import { capture, copyRepo, replace } from "./helpers";

describe("repo checks", () => {
  let root: string;
  let cleanup: () => void;
  beforeEach(() => {
    ({ root, cleanup } = copyRepo([
      "modules",
      "apps/android/settings.gradle.kts",
      "apps/android/gradle/libs.versions.toml",
      "specs/third-party.yaml",
    ]));
  });
  afterEach(() => cleanup());

  const depsResult = () => capture(deps.main, root);
  const read = (name: string) => readFileSync(join(root, name), "utf-8");
  const write = (name: string, text: string) => writeFileSync(join(root, name), text);

  function addSdk(mod = "core/network", register = true) {
    const target = { "core/network": "CoreNetwork", "core/utils": "CoreUtils", "features/search": "FeatureSearch" }[
      mod
    ];
    const product = '.product(name: "ExampleSDK", package: "example-sdk")';
    const rows = read("modules/Package.swift")
      .split("\n")
      .map((row) => {
        if (!row.includes(`.target(name: "${target}",`)) return row;
        return row.includes("dependencies: [")
          ? row.replace("dependencies: [", `dependencies: [${product}, `)
          : row.replace("path:", `dependencies: [${product}], path:`);
      });
    write("modules/Package.swift", rows.join("\n"));
    const catalog = "apps/android/gradle/libs.versions.toml";
    write(
      catalog,
      read(catalog).replace(
        "[libraries]",
        '[libraries]\nexample-sdk = { module = "com.example:sdk", version = "1.0" }',
      ),
    );
    const build = `modules/${mod}/android/build.gradle.kts`;
    write(build, `${read(build)}\ndependencies { implementation(libs.example.sdk) }\n`);
    if (register) {
      const entry = {
        role: "example",
        module: mod,
        owner: "platform",
        ios: { name: "example-sdk", license: "Apache-2.0" },
        android: { name: "example-sdk", license: "Apache-2.0" },
      };
      write("specs/third-party.yaml", JSON.stringify({ libraries: [entry] }));
    }
  }

  test("現在のリポジトリは通る", () => {
    expect(depsResult().code).toBe(0);
    expect(check(root)).toEqual([]);
  });

  test("登録済みの SDK は内部の依存グラフと分けて扱う", () => {
    addSdk();
    const { code, output } = depsResult();
    expect(code, output).toBe(0);
    const [graph, external] = deps.iosDependencies(root);
    expect(graph.get("core/network")).toEqual(new Set(["core/utils"]));
    expect(external).toEqual(new Set(["core/network\texample-sdk"]));
  });

  test("SDK は両 OS とも対応表に登録が必要", () => {
    addSdk("core/network", false);
    const { code, output } = depsResult();
    expect(code).toBe(1);
    expect(output).toContain("ios: core/network");
    expect(output).toContain("android: core/network");
  });

  test.each(["features/search", "shared/bookmarks", "shared/designsystem", "core/utils"])(
    "%s は SDK に直接依存できない",
    (mod) => {
      const errors = deps.checkExternal({ ios: new Set([`${mod}\texample-sdk`]), android: new Set() }, root);
      expect(errors.some((error) => error.includes("直接依存できません"))).toBe(true);
    },
  );

  test.each([
    ["features/search", "shared/bookmarks", true],
    ["features/bookmarks", "shared/bookmarks", true],
    ["features/search", "shared/designsystem", true],
    ["shared/designsystem", "core/utils", true],
    ["shared/designsystem", "core/network", false],
    ["shared/designsystem", "core/storage", false],
    ["shared/designsystem", "core/navigation", false],
    ["core/network", "shared/designsystem", false],
    ["shared/bookmarks", "core/storage", true],
    ["shared/bookmarks", "core/network", true],
    ["core/storage", "shared/bookmarks", false],
    ["shared/designsystem", "shared/bookmarks", false],
    ["shared/designsystem", "features/search", false],
    ["shared/designsystem", "shared/designsystem", false],
    ["shared/bookmarks", "features/bookmarks", false],
    ["shared/bookmarks", "shared/designsystem", false],
    ["shared/bookmarks", "shared/other", false],
    ["shared/bookmarks", "shared/bookmarks", false],
    ["features/search", "features/bookmarks", false],
  ] as const)("依存境界 %s → %s は %s", (source, target, expected) => {
    expect(deps.allowed(source, target)).toBe(expected);
  });

  test("対応表にあっても Android の依存が無いと失敗する", () => {
    addSdk();
    replace(root, "modules/core/network/android/build.gradle.kts", "implementation(libs.example.sdk)", "", true);
    const { code, output } = depsResult();
    expect(code).toBe(1);
    expect(output).toContain("実際の依存にありません");
  });

  test("不明な内部 target は対処方法つきで失敗する", () => {
    replace(root, "modules/Package.swift", 'dependencies: ["CoreUtils"]', 'dependencies: ["Missing"]', true);
    const { code, output } = depsResult();
    expect(code).toBe(1);
    expect(output).toContain("不明な内部 target Missing");
  });

  test(".target(name:) 形式の内部依存は通る", () => {
    replace(
      root,
      "modules/Package.swift",
      'dependencies: ["CoreUtils"]',
      'dependencies: [.target(name: "CoreUtils")]',
      true,
    );
    expect(depsResult().code).toBe(0);
  });

  test("未対応の Swift の依存構文は黙って無視しない", () => {
    replace(root, "modules/Package.swift", 'dependencies: ["CoreUtils"]', "dependencies: externalDependencies", true);
    const { code, output } = depsResult();
    expect(code).toBe(1);
    expect(output).toContain("1行で記述");
  });

  test("Android の project 名は settings から解決する", () => {
    replace(root, "apps/android/settings.gradle.kts", '":core:utils"', '":utilities"', true);
    replace(
      root,
      "modules/core/network/android/build.gradle.kts",
      'project(":core:utils")',
      'project(path = ":utilities")',
      true,
    );
    expect(depsResult().code).toBe(0);
  });

  test("登録されていない build.gradle.kts は失敗する", () => {
    replace(root, "apps/android/settings.gradle.kts", 'module(":core:utils", "core/utils")', "", true);
    expect(depsResult().code).toBe(1);
  });

  test("core/utils は Android の公式ライブラリにも依存できない", () => {
    const build = "modules/core/utils/android/build.gradle.kts";
    write(build, `${read(build)}\ndependencies { implementation(libs.coroutines.core) }\n`);
    const { code, output } = depsResult();
    expect(code).toBe(1);
    expect(output).toContain("直接依存できません");
  });

  test("どのモジュールの spec.md を消しても検出する", () => {
    const specs = glob(join(root, "modules"), "**/spec.md");
    expect(specs.length).toBeGreaterThan(0);
    for (const spec of specs) {
      const path = join(root, "modules", spec);
      const original = readFileSync(path, "utf-8");
      unlinkSync(path);
      expect(check(root).some((error) => error.includes("spec.md がありません"))).toBe(true);
      writeFileSync(path, original);
    }
  });

  test("実装を消してもビルド定義に残るモジュールを検出する", () => {
    rmSync(join(root, "modules/features/search"), { recursive: true });
    expect(check(root)).toContain("modules/features/search: spec.md がありません");
  });

  test("登録されていない実装にも spec.md が必要", () => {
    mkdirSync(join(root, "modules/features/orphan/ios"), { recursive: true });
    expect(check(root)).toContain("modules/features/orphan: spec.md がありません");
  });

  test("spec の変更には両 OS の記録が必要", () => {
    const spec = "modules/features/search/spec.md";
    write(spec, `${read(spec)}\n変更\n`);
    expect(check(root).filter((error) => error.includes("追従していません"))).toHaveLength(2);
  });
});
