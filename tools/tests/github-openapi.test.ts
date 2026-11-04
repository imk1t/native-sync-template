import { expect, test } from "bun:test";
import {
  checkSelection,
  CONTRACT,
  digest,
  githubContract,
  project,
  SELECTION,
  type Selection,
} from "../lib/github-openapi";
import { Spec, validateValue, type ApiType, swiftModels, kotlinModels } from "../lib/openapi";
import { type Dict, readText, readYaml, ROOT, join } from "../lib/util";

const contract = () => readYaml(join(ROOT, CONTRACT)) as Dict;
const selection = () => JSON.parse(readText(join(ROOT, SELECTION))) as Selection;

// 公式定義と同じ参照構造を持つ小さな上流 fixture。ネットワークや原本ファイルに依存しない。
function upstream() {
  return {
    openapi: "3.0.3",
    info: { title: "Upstream", version: "1" },
    paths: {
      "/search/repositories": {
        get: {
          operationId: "search/repos",
          parameters: [{ $ref: "#/components/parameters/q" }],
          responses: {
            "200": {
              content: {
                "application/json": {
                  schema: {
                    type: "object",
                    required: ["items"],
                    properties: { items: { type: "array", items: { $ref: "#/components/schemas/repo" } as Dict } },
                  },
                },
              },
            },
          },
        },
      },
    },
    components: {
      parameters: { q: { name: "q", in: "query", required: true, schema: { type: "string" } } },
      schemas: {
        repo: {
          type: "object",
          required: ["id", "description", "owner"],
          properties: {
            id: { type: "integer" },
            description: { type: "string", nullable: true } as Dict,
            owner: { $ref: "#/components/schemas/user", nullable: true },
            unused: { allOf: [{ type: "string" }] },
          },
        },
        user: { type: "object", required: ["login"], properties: { login: { type: "string" } } },
      },
    },
  };
}
const chosen: Selection = {
  operations: { "/search/repositories": { parameters: ["q"], response: "SearchResult" } },
  schemas: {
    Repo: {
      source: "#/components/schemas/repo",
      fields: ["id", "description", "owner"],
      references: { owner: "Owner" },
    },
    Owner: { source: "#/components/schemas/user", fields: ["login"], references: {} },
    SearchResult: {
      source: "#/paths/~1search~1repositories/get/responses/200/content/application~1json/schema",
      fields: ["items"],
      references: { items: "Repo" },
    },
  },
};

test("上流から直接生成した契約は決定的で、取得元と選択ルールを自身に記録する", () => {
  const raw = JSON.stringify(upstream());
  const commit = "a".repeat(40);
  const text = githubContract(raw, chosen, commit);
  expect(githubContract(raw, chosen, commit)).toBe(text);
  const doc = Bun.YAML.parse(text) as Dict;
  expect(doc["x-github-source"]).toEqual({
    repository: "github/rest-api-description",
    commit,
    path: "descriptions/api.github.com/api.github.com.2026-03-10.json",
    sha256: digest(raw),
    selectionSha256: digest(JSON.stringify(chosen)),
  });
  const spec = new Spec(doc);
  expect(spec.operations.map((op) => op.id)).toEqual(["search/repos"]);
  expect(spec.schemas.find((s) => s.name === "Repo")!.props).toEqual([
    { key: "id", name: "id", optional: false, type: { kind: "int64" } },
    { key: "description", name: "description", optional: false, type: { kind: "string", nullable: true } },
    { key: "owner", name: "owner", optional: false, type: { kind: "ref", name: "Owner", nullable: true } },
  ]);
});
test("利用する上流フィールドが無くなれば生成を中止する", () => {
  const doc = upstream();
  Reflect.deleteProperty(doc.components.schemas.repo.properties, "description");
  expect(() => project(doc, chosen)).toThrow();
});
test("投影先と元の参照先の型・必須性が違えば失敗する", () => {
  const doc = upstream();
  doc.paths["/search/repositories"].get.responses["200"].content["application/json"].schema.properties.items.items = {
    type: "object",
    required: [],
    properties: doc.components.schemas.repo.properties,
  };
  expect(() => project(doc, chosen)).toThrow("必須性");
});
test("選択フィールドに未対応の合成型が加われば失敗する", () => {
  const doc = upstream();
  doc.components.schemas.repo.properties.description = { oneOf: [{ type: "string" }] };
  expect(() => project(doc, chosen)).toThrow("未対応");
});
test("更新し忘れた選択ルールを検出する", () => {
  const doc = contract();
  const changed = selection();
  expect(() => checkSelection(doc, changed)).not.toThrow();
  changed.schemas.Repository!.fields.push("archived");
  expect(() => checkSelection(doc, changed)).toThrow("契約に反映されていません");
});
test("コミットした契約が4操作と選択ルールに一致する", () => {
  const spec = new Spec(contract());
  expect(spec.operations.map((op) => op.id).sort()).toEqual([
    "issues/get",
    "issues/list-for-repo",
    "repos/get",
    "search/repos",
  ]);
  expect(() => checkSelection(spec.doc, selection())).not.toThrow();
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
