/** 一時 Git リポジトリで生成差分チェックの見逃しを検証する。 */
import { afterEach, beforeEach, describe, expect, test } from "bun:test";
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { ROOTS, load, outputs } from "../lib/localization";
import { ROOT } from "../lib/util";
import { OUTPUTS } from "../generate";
import { copyRepo } from "./helpers";

const FILES = [
  "node_modules/yaml",
  "tools/generate.ts",
  "tools/check-generated.ts",
  "tools/lib",
  "specs/tokens/tokens.json",
  "specs/openapi",
  "specs/routes.yaml",
  "specs/localization",
  ...ROOTS,
  "modules/features/search/spec.md",
  "modules/features/bookmarks/spec.md",
  "modules/features/settings/spec.md",
  ...Object.values(OUTPUTS),
];

describe("check-generated", () => {
  let repo: string;
  let cleanup: () => void;

  const git = (...args: string[]) => {
    const result = Bun.spawnSync(
      [
        "git",
        "-c",
        "user.name=Test",
        "-c",
        "user.email=test@example.invalid",
        "-c",
        "commit.gpgsign=false",
        "-c",
        "core.hooksPath=/dev/null",
        ...args,
      ],
      { cwd: repo },
    );
    if (result.exitCode !== 0) throw new Error(result.stderr.toString());
    return result.stdout.toString();
  };
  const commit = () => git("commit", "-m", "Test fixture");
  const checkGenerated = () => {
    const result = Bun.spawnSync(["bun", "tools/check-generated.ts"], { cwd: repo });
    return { code: result.exitCode, output: result.stdout.toString() + result.stderr.toString() };
  };

  beforeEach(() => {
    ({ root: repo, cleanup } = copyRepo(FILES));
    git("init");
    git("add", ".");
    commit();
  });
  afterEach(() => cleanup());

  test("現在の生成物は通る", () => {
    const { code, output } = checkGenerated();
    expect(code, output).toBe(0);
  });

  test("トークンを変えて再生成しないと失敗する", () => {
    const path = join(repo, "specs/tokens/tokens.json");
    const tokens = JSON.parse(readFileSync(path, "utf-8"));
    tokens.spacing.medium.$value = "20";
    writeFileSync(path, JSON.stringify(tokens));
    git("add", ".");
    commit();
    expect(checkGenerated().code).toBe(1);
  });

  test("OpenAPI を変えて再生成しないと失敗する", () => {
    const path = join(repo, "specs/openapi/openapi.yaml");
    const api = Bun.YAML.parse(readFileSync(path, "utf-8")) as {
      components: { schemas: { Repository: { properties: Record<string, unknown> } } };
    };
    api.components.schemas.Repository.properties.archived = { type: "boolean" };
    writeFileSync(path, Bun.YAML.stringify(api));
    git("add", ".");
    commit();
    const { code, output } = checkGenerated();
    expect(code, output).toBe(1);
    expect(output).toContain("APIModels.swift");
  });

  test("生成は入力の OpenAPI 契約を書き換えない", () => {
    const path = join(repo, "specs/openapi/openapi.yaml");
    const before = readFileSync(path, "utf-8");
    const result = Bun.spawnSync(["bun", "tools/generate.ts"], { cwd: repo });
    expect(result.exitCode, result.stderr.toString()).toBe(0);
    expect(readFileSync(path, "utf-8")).toBe(before);
  });

  test("どの生成物を消してコミットしても失敗する", () => {
    for (const output of [...Object.values(OUTPUTS), ...Object.keys(outputs(load(ROOT)))]) {
      const baseline = git("rev-parse", "HEAD").trim();
      git("rm", output);
      commit();
      const result = checkGenerated();
      expect(result.code, result.output).toBe(1);
      expect(result.output).toContain(`?? ${output}`);
      git("reset", "--hard", baseline);
    }
  });

  test("言語を削除して生成物を残すと失敗する", () => {
    git("rm", "specs/localization/messages-en.yaml");
    commit();
    const result = checkGenerated();
    expect(result.code, result.output).toBe(1);
    expect(result.output).toContain("values-en/strings.xml");
  });

  test("新しい言語を追加して再生成しないと失敗する", () => {
    writeFileSync(
      join(repo, "specs/localization/messages-de.yaml"),
      readFileSync(join(repo, "specs/localization/messages-en.yaml")),
    );
    git("add", ".");
    commit();
    const result = checkGenerated();
    expect(result.code, result.output).toBe(1);
    expect(result.output).toContain("values-de/strings.xml");
  });

  test("生成器が作業ツリーを直しても、ステージ済みの破損は失敗する", () => {
    const output = OUTPUTS.iosTokens;
    writeFileSync(join(repo, output), "// stale output\n");
    git("add", output);
    expect(checkGenerated().code).toBe(1);
  });
});
