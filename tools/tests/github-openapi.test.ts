import { expect, test } from "bun:test";
import { extract, project, type Selection } from "../lib/github-openapi";
import { Spec, validateValue, type ApiType, swiftModels, kotlinModels } from "../lib/openapi";
import { readText, ROOT, join } from "../lib/util";
const source = () => JSON.parse(readText(join(ROOT, "shared/openapi/github-source.json")));
const selection = () => JSON.parse(readText(join(ROOT, "shared/openapi/github-selection.json"))) as Selection;

test("固定した原本からの抽出と投影は決定的", () => {
  const doc = source();
  const chosen = selection();
  expect(project(extract(doc, chosen), chosen)).toEqual(project(doc, chosen));
  expect(new Spec(project(doc, chosen)).operations.map((op) => op.id).sort()).toEqual([
    "issues/get",
    "issues/list-for-repo",
    "repos/get",
    "search/repos",
  ]);
});
test("利用する上流フィールドが無くなれば生成を中止する", () => {
  const doc = source();
  delete doc.components.schemas["repo-search-result-item"].properties.description;
  expect(() => project(doc, selection())).toThrow();
});
test("投影先と元の参照先の型・必須性が違えば失敗する", () => {
  const doc = source();
  doc.paths["/search/repositories"].get.responses["200"].content["application/json"].schema.properties.items.items = {
    type: "object",
    required: [],
    properties: doc.components.schemas["repo-search-result-item"].properties,
  };
  expect(() => project(doc, selection())).toThrow("必須性");
});
test("選択フィールドに未対応の合成型が加われば失敗する", () => {
  const doc = source();
  doc.components.schemas["repo-search-result-item"].properties.description = { oneOf: [{ type: "string" }] };
  expect(() => project(doc, selection())).toThrow("未対応");
});
test("必須 nullable と省略可能を両 OS と fixture 検査で区別する", () => {
  const spec = new Spec(project(source(), selection()));
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
  const spec = new Spec(project(source(), selection()));
  const operation = spec.operations.find((op) => op.id === "search/repos")!;
  const sort = operation.params.find((p) => p.name === "sort")!;
  expect(() => validateValue(spec, sort.type, "stars", "sort")).not.toThrow();
  expect(() => validateValue(spec, sort.type, "unknown", "sort")).toThrow();
});
