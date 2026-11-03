/** 固定した GitHub 公式定義を、アプリが使うフィールドに絞る。通常の生成では通信しない。 */
import { createHash } from "node:crypto";
import { type Dict, isDict, join, readText, sorted } from "./util";

export type Selection = {
  operations: Record<string, { parameters: string[]; response: string; array?: boolean }>;
  schemas: Record<string, { source: string; fields: string[]; references: Record<string, string> }>;
};
export const SOURCE = "shared/openapi/github-source.json";
export const SELECTION = "shared/openapi/github-selection.json";
export const META = "shared/openapi/github-source-meta.json";
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
    info: { title: "GitHub sample API", version: "2026-03-10" },
    paths,
    components: { schemas },
  };
}

export function githubOpenAPI(root: string): string {
  const text = readText(join(root, SOURCE));
  const meta = JSON.parse(readText(join(root, META))) as { snapshotSha256: string };
  if (digest(text) !== meta.snapshotSha256)
    throw new Error("GitHub 原本のチェックサムが一致しません。sync-github-openapi.ts で更新してください");
  const result = project(JSON.parse(text), JSON.parse(readText(join(root, SELECTION))));
  return `# Generated by tools/generate.ts from the pinned GitHub OpenAPI snapshot. Do not edit.\n${Bun.YAML.stringify(result)}\n`;
}

/** 選択 operation の 200 応答・パラメータと、参照先の原本を抽出する。 */
export function extract(doc: Dict, selection: Selection): Dict {
  const result: Dict = { openapi: doc.openapi, info: doc.info, paths: {}, components: {} };
  const visited = new Set<string>();
  function visit(value: unknown): void {
    if (Array.isArray(value)) {
      value.forEach(visit);
      return;
    }
    if (!isDict(value)) return;
    if (typeof value.$ref === "string" && !visited.has(value.$ref)) {
      visited.add(value.$ref);
      const parts = value.$ref
        .slice(2)
        .split("/")
        .map((p) => p.replaceAll("~1", "/").replaceAll("~0", "~"));
      const original = pointer(doc, value.$ref);
      let target = result;
      for (const part of parts.slice(0, -1)) target = object((target[part] ??= {}), part);
      target[parts.at(-1)!] = original;
      visit(original);
    }
    Object.values(value).forEach(visit);
  }
  for (const [path, selected] of Object.entries(selection.operations)) {
    const op = object(object(object(doc.paths, "paths")[path], path).get, "get");
    const chosen = {
      operationId: op.operationId,
      parameters: (op.parameters as unknown[]).filter((p) =>
        selected.parameters.includes(String(resolve(doc, p).name)),
      ),
      responses: { "200": object(op.responses, "responses")["200"] },
    };
    object(result.paths, "paths")[path] = { get: chosen };
    visit(chosen);
  }
  return result;
}
