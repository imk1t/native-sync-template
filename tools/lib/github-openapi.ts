/** GitHub 公式定義の更新時に、利用する契約へ直接変換する。原本の中間ファイルは作らない。 */
import { createHash } from "node:crypto";
import { Spec } from "./openapi";
import { type Dict, isDict, sorted } from "./util";

export type Selection = {
  operations: Record<string, { parameters: string[]; response: string; array?: boolean }>;
  schemas: Record<string, { source: string; fields: string[]; references: Record<string, string> }>;
};
export const CONTRACT = "shared/openapi/openapi.yaml";
export const SELECTION = "shared/openapi/github-selection.json";
export const REPOSITORY = "github/rest-api-description";
export const API_VERSION = "2026-03-10";
export const UPSTREAM_PATH = `descriptions/api.github.com/api.github.com.${API_VERSION}.json`;
export const digest = (text: string) => createHash("sha256").update(text).digest("hex");

export function object(value: unknown, where: string): Dict {
  if (!isDict(value)) throw new Error(`${where}: オブジェクトが必要です`);
  return value;
}
export function pointer(doc: Dict, ref: string): Dict {
  if (!ref.startsWith("#/")) throw new Error(`外部参照には対応しません: ${ref}`);
  let current: unknown = doc;
  for (const part of ref.slice(2).split("/"))
    current = object(current, ref)[part.replaceAll("~1", "/").replaceAll("~0", "~")];
  return object(current, ref);
}
function resolve(doc: Dict, raw: unknown): Dict {
  const value = object(raw, "schema");
  return typeof value.$ref === "string" ? { ...pointer(doc, value.$ref), ...value } : value;
}

export function project(doc: Dict, selection: Selection): Dict {
  const schemas: Dict = {};
  for (const name of sorted(Object.keys(selection.schemas))) {
    const shape = selection.schemas[name]!;
    const original = pointer(doc, shape.source);
    const properties = object(original.properties, shape.source);
    const required = new Set((original.required as string[]) ?? []);
    const props: Dict = {};
    for (const field of shape.fields) {
      const raw = resolve(doc, properties[field]);
      for (const unsupported of ["oneOf", "anyOf", "allOf", "additionalProperties"]) {
        if (unsupported in raw) throw new Error(`${name}.${field}: 未対応: ${unsupported}`);
      }
      const target = shape.references[field];
      const normalized: Dict = {};
      if (target) {
        if (!(target in selection.schemas)) throw new Error(`不明な投影先: ${target}`);
        const nested = raw.type === "array" ? resolve(doc, raw.items) : raw;
        const targetShape = selection.schemas[target]!;
        const nestedProperties = object(nested.properties, `${name}.${field}`);
        const targetOriginal = pointer(doc, targetShape.source);
        const targetProperties = object(targetOriginal.properties, target);
        for (const key of targetShape.fields) {
          const sourceProperty = resolve(doc, nestedProperties[key]);
          const targetProperty = resolve(doc, targetProperties[key]);
          if (
            sourceProperty.type !== targetProperty.type ||
            Boolean(sourceProperty.nullable) !== Boolean(targetProperty.nullable)
          ) {
            throw new Error(`${name}.${field}.${key}: 投影先と型が一致しません`);
          }
          if (
            ((nested.required as string[]) ?? []).includes(key) !==
            ((targetOriginal.required as string[]) ?? []).includes(key)
          ) {
            throw new Error(`${name}.${field}.${key}: 投影先と必須性が一致しません`);
          }
        }
        const ref = { $ref: `#/components/schemas/${target}` };
        Object.assign(normalized, raw.type === "array" ? { type: "array", items: ref } : ref);
      } else {
        for (const key of ["type", "format", "items", "enum"]) if (key in raw) normalized[key] = raw[key];
        // GitHub の検索 ID は format を持たない integer。両 OS とも 64bit で受け取る。
        if (field === "id" && raw.type === "integer") normalized.format = "int64";
      }
      if (raw.nullable === true) normalized.nullable = true;
      props[field] = normalized;
    }
    schemas[name] = { type: "object", required: shape.fields.filter((f) => required.has(f)), properties: props };
  }
  const paths: Dict = {};
  for (const path of sorted(Object.keys(selection.operations))) {
    const selected = selection.operations[path]!;
    const op = pointer(doc, `#/paths/${path.replaceAll("~", "~0").replaceAll("/", "~1")}/get`);
    const parameters = (op.parameters as unknown[])
      .map((p) => resolve(doc, p))
      .filter((p) => selected.parameters.includes(String(p.name)))
      .map((p) => ({ name: p.name, in: p.in, required: p.required ?? false, schema: p.schema }));
    if (parameters.length !== selected.parameters.length) throw new Error(`${path}: パラメータが不足しています`);
    const schema = { $ref: `#/components/schemas/${selected.response}` };
    paths[path] = {
      get: {
        operationId: op.operationId,
        parameters,
        responses: {
          "200": {
            description: "OK",
            content: { "application/json": { schema: selected.array ? { type: "array", items: schema } : schema } },
          },
        },
      },
    };
  }
  return {
    openapi: "3.0.3",
    info: { title: "GitHub sample API", version: API_VERSION },
    paths,
    components: { schemas },
  };
}

/** 固定元とチェックサムを契約自身に記録する。出力がコード生成可能かも保存前に検証する。 */
export function githubContract(raw: string, selection: Selection, commit: string): string {
  if (!/^[a-f0-9]{40}$/.test(commit)) throw new Error("公式定義の固定コミット SHA（40桁）を指定してください");
  const contract = project(object(JSON.parse(raw), "GitHub OpenAPI"), selection);
  new Spec(contract);
  contract["x-github-source"] = {
    repository: REPOSITORY,
    commit,
    path: UPSTREAM_PATH,
    sha256: digest(raw),
    selectionSha256: digest(JSON.stringify(selection)),
  };
  return `# Generated by tools/sync-github-openapi.ts. Do not edit.\n${Bun.YAML.stringify(contract, null, 2)}\n`;
}

/** 選択ルールだけ変わり、更新コマンドを実行し忘れた状態を CI で検出する。 */
export function checkSelection(contract: Dict, selection: Selection): void {
  const source = object(contract["x-github-source"], "x-github-source");
  if (source.selectionSha256 !== digest(JSON.stringify(selection))) {
    throw new Error("GitHub の選択ルールが契約に反映されていません。sync-github-openapi.ts で契約を更新してください");
  }
}
