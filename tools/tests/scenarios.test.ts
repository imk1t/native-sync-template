/** シナリオの given からテスト用データを作る処理の検証。 */
import { describe, expect, test } from "bun:test";
import { join } from "node:path";
import { Spec } from "../lib/openapi";
import * as scenarios from "../lib/scenarios";
import { type Dict, isDict, readYaml, ROOT } from "../lib/util";

const api = readYaml(join(ROOT, "specs/openapi/openapi.yaml"));
const spec = new Spec(isDict(api) ? api : ({} as Dict));
const loaded = scenarios.load(ROOT, spec);
const find = (name: string, id: string) => {
  const step = loaded.find((s) => s.name === name)?.steps.find((s) => s.id === id);
  if (!step) throw new Error(`${name}/${id} がありません`);
  return step;
};

describe("scenarios", () => {
  test("名前を識別子にする", () => {
    expect(scenarios.memberName("load-succeeds")).toBe("loadSucceeds");
    expect(scenarios.memberName("not-found")).toBe("notFound");
    expect(scenarios.typeName("github-search")).toBe("GithubSearch");
  });

  test("API を使わない step は含めない", () => {
    expect(() => find("github-search", "blank")).toThrow();
    expect(() => find("navigation", "pop-to-root")).toThrow();
  });

  test("配列の given は書いた順にレスポンスを並べる", () => {
    const step = find("github-api", "rate-limit");
    expect(step.stubs.map((s) => s.status)).toEqual([403, 429, 403]);
    expect(step.stubs[1]?.headers).toEqual([["Retry-After", "120"]]);
  });

  test("operationId から method と path を引く", () => {
    const [stub] = find("github-api", "path-encoding").stubs;
    expect(stub).toMatchObject({ operation: "repos/get", method: "GET", path: "/repos/{owner}/{repo}" });
    expect(JSON.parse(stub?.body ?? "")).toMatchObject({ full_name: "octocat/Hello-World" });
  });

  test("保留の step はレスポンスとして使わない", () => {
    const step = find("github-search", "stale-response");
    expect(step.stubs.every((s) => s.pending)).toBe(true);
    expect(scenarios.servesResponses(step)).toBe(false);
  });

  test("出力先は feature のテストディレクトリ", () => {
    const files = Object.keys(scenarios.outputs(ROOT, spec));
    expect(files).toContain("modules/shared/github-rest/ios/Tests/Shared/GitHubREST/Generated/ScenarioFixtures.swift");
    expect(files).toContain(
      "modules/features/search/android/src/test/kotlin/com/example/features/search/generated/ScenarioFixtures.kt",
    );
    expect(files).toHaveLength(4);
  });

  test("両 OS に同じ step・同じ本文を出す", () => {
    const swift = scenarios.swift(loaded.filter((s) => s.name === "github-detail"));
    const kotlin = scenarios.kotlin(
      loaded.filter((s) => s.name === "github-detail"),
      "com.example.test",
    );
    for (const text of [swift, kotlin]) {
      expect(text).toContain("github-detail/not-found");
      expect(text).toContain('\\"message\\":\\"Not Found\\"');
    }
    expect(kotlin).toContain("package com.example.test");
    expect(swift).toContain("enum GithubDetail");
    expect(kotlin).toContain("object GithubDetail");
  });

  test("Kotlin では $ を、どちらでも制御文字をエスケープする", () => {
    const stub = { operation: "o", method: "GET", path: "/a", status: 200, headers: [], pending: false };
    const step = { id: "x", member: "x", stubs: [{ ...stub, body: '"$a\u0001"' }] };
    const scenario = [{ name: "demo", type: "Demo", feature: "shared/demo", steps: [step] }];
    expect(scenarios.kotlin(scenario, "p")).toContain("\\$a");
    expect(scenarios.swift(scenario)).not.toContain("\\$a");
    expect(scenarios.swift(scenario)).toContain("\\u{0001}");
  });
});
