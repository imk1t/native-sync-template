/** 一時 Git リポジトリで生成差分チェックの見逃しを検証する。 */
import { afterEach, beforeEach, describe, expect, test } from "bun:test";
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { OUTPUTS } from "../generate";
import { copyRepo } from "./helpers";

const FILES = [
  "tools/generate.ts",
  "tools/check-generated.ts",
  "tools/lib",
  "shared/tokens/tokens.json",
  "shared/openapi",
  "shared/fixtures",
  "shared/routes.yaml",
  "modules/features/github/spec.md",
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
    const path = join(repo, "shared/tokens/tokens.json");
    const tokens = JSON.parse(readFileSync(path, "utf-8"));
    tokens.spacing.medium.$value = "20";
    writeFileSync(path, JSON.stringify(tokens));
    git("add", ".");
    commit();
    expect(checkGenerated().code).toBe(1);
  });

  test("OpenAPI を変えて再生成しないと失敗する", () => {
    const path = join(repo, "shared/openapi/openapi.yaml");
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
    const path = join(repo, "shared/openapi/openapi.yaml");
    const before = readFileSync(path, "utf-8");
    const result = Bun.spawnSync(["bun", "tools/generate.ts"], { cwd: repo });
    expect(result.exitCode, result.stderr.toString()).toBe(0);
    expect(readFileSync(path, "utf-8")).toBe(before);
  });

  test("どの生成物を消してコミットしても失敗する", () => {
    for (const output of Object.values(OUTPUTS)) {
      const baseline = git("rev-parse", "HEAD").trim();
      git("rm", output);
      commit();
      const result = checkGenerated();
      expect(result.code, result.output).toBe(1);
      expect(result.output).toContain(`?? ${output}`);
      git("reset", "--hard", baseline);
    }
  });

  test("生成器が作業ツリーを直しても、ステージ済みの破損は失敗する", () => {
    const output = OUTPUTS.iosTokens;
    writeFileSync(join(repo, output), "// stale output\n");
    git("add", output);
    expect(checkGenerated().code).toBe(1);
  });
});
