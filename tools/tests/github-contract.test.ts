import { expect, test } from "bun:test";
import { Spec, validateValue, type ApiType, swiftModels, kotlinModels } from "../lib/openapi";
import { type Dict, readYaml, ROOT, join } from "../lib/util";

const contract = () => readYaml(join(ROOT, "specs/openapi/openapi.yaml")) as Dict;

test("API 契約がサンプルで使用する4操作を定義する", () => {
  const spec = new Spec(contract());
  expect(spec.operations.map((op) => op.id).sort()).toEqual([
    "issues/get",
    "issues/list-for-repo",
    "repos/get",
    "search/repos",
  ]);
});
test("必須 nullable と省略可能を両 OS と fixture 検査で区別する", () => {
  const spec = new Spec(contract());
  const type: ApiType = { kind: "ref", name: "Issue" };
  const issue = { id: 5_000_000_001, number: 1, title: "a", state: "open", user: null, updated_at: "2026-11-03" };
  expect(() => validateValue(spec, type, issue, "issue")).not.toThrow();
  const { user, ...missing } = issue;
  expect(() => validateValue(spec, type, missing, "issue")).toThrow();
  expect(swiftModels(spec)).toContain("contains(.user)");
  expect(kotlinModels(spec, "p")).toContain("val user: OwnerDTO?,");
  expect(kotlinModels(spec, "p")).toContain("val body: String? = null,");
});
test("パラメータ enum の契約を検証する", () => {
  const spec = new Spec(contract());
  const operation = spec.operations.find((op) => op.id === "search/repos")!;
  const sort = operation.params.find((p) => p.name === "sort")!;
  expect(() => validateValue(spec, sort.type, "stars", "sort")).not.toThrow();
  expect(() => validateValue(spec, sort.type, "unknown", "sort")).toThrow();
});
