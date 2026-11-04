/**
 * specs/openapi から Swift / Kotlin の DTO と API クライアントを生成する。
 *
 * 対応するのはテンプレートで使う範囲だけで、未対応の構文は黙って無視せず Error で失敗する。
 * - components.schemas: type: object（properties / required）
 * - 型: string / integer（int64 は 64bit）/ number / boolean / array / $ref
 * - nullable と省略可能を区別。string enum は契約検査に保持
 * - operation: get / post / put / patch / delete、operationId 必須
 * - parameters: in: path / query（プリミティブ型のみ）
 * - requestBody: application/json
 * - responses: 最初の 2xx。application/json なら戻り値、content が無ければ戻り値なし
 */
import { compare, type Dict, isDict, sorted } from "./util";

const METHODS = ["get", "post", "put", "patch", "delete"] as const;
const SWIFT_KEYWORDS = new Set([
  "associatedtype",
  "class",
  "default",
  "deinit",
  "enum",
  "extension",
  "func",
  "import",
  "init",
  "inout",
  "internal",
  "let",
  "operator",
  "private",
  "protocol",
  "public",
  "repeat",
  "return",
  "self",
  "static",
  "struct",
  "subscript",
  "switch",
  "true",
  "false",
  "type",
  "var",
  "where",
  "while",
]);
const KOTLIN_KEYWORDS = new Set([
  "as",
  "break",
  "class",
  "continue",
  "do",
  "else",
  "false",
  "for",
  "fun",
  "if",
  "in",
  "interface",
  "is",
  "null",
  "object",
  "package",
  "return",
  "super",
  "this",
  "throw",
  "true",
  "try",
  "typealias",
  "typeof",
  "val",
  "var",
  "when",
  "while",
]);

/** 言語に依存しない型表現。 */
export type ApiType = (
  | { kind: "ref"; name: string }
  | { kind: "array"; items: ApiType }
  | { kind: "string" | "int32" | "int64" | "double" | "bool" }
) & { nullable?: boolean; values?: unknown[] };

export type Prop = { key: string; name: string; type: ApiType; optional: boolean };
export type Schema = { name: string; type: string; props: Prop[] };
export type Param = {
  key: string;
  name: string;
  in: "path" | "query";
  type: ApiType;
  optional: boolean;
};
export type Operation = {
  id: string;
  name: string;
  method: string;
  path: string;
  params: Param[];
  body: { type: ApiType; optional: boolean } | null;
  returns: ApiType | null;
};

export function camel(name: string): string {
  const parts = name.split(/[^0-9A-Za-z]+/).filter((p) => p);
  const [head, ...rest] = parts;
  if (head === undefined) throw new Error(`識別子にできない名前: ${JSON.stringify(name)}`);
  const text =
    head.charAt(0).toLowerCase() + head.slice(1) + rest.map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
  if (/^[0-9]/.test(text)) throw new Error(`数字で始まる名前は使えません: ${JSON.stringify(name)}`);
  return text;
}

export function typeName(name: string): string {
  const text = camel(name);
  return `${text.charAt(0).toUpperCase()}${text.slice(1)}DTO`;
}

function swiftIdent(name: string): string {
  return SWIFT_KEYWORDS.has(name) ? `\`${name}\`` : name;
}

function kotlinIdent(name: string): string {
  return KOTLIN_KEYWORDS.has(name) ? `\`${name}\`` : name;
}

function dict(value: unknown): Dict {
  return isDict(value) ? value : {};
}

export class Spec {
  readonly schemas: Schema[];
  readonly operations: Operation[];

  constructor(readonly doc: Dict) {
    if (!String(doc.openapi ?? "").startsWith("3.")) {
      throw new Error("OpenAPI 3.x のみ対応しています");
    }
    this.schemas = this.readSchemas();
    this.operations = this.readOperations();
  }

  private componentSchemas(): Dict {
    return dict(dict(this.doc.components).schemas);
  }

  // ---- 型 ----
  resolveRef(ref: string): string {
    const prefix = "#/components/schemas/";
    const name = ref.slice(prefix.length);
    if (!ref.startsWith(prefix) || !(name in this.componentSchemas())) {
      throw new Error(`解決できない $ref: ${ref}`);
    }
    return name;
  }

  typeOf(schema: unknown, where: string): ApiType {
    if (!isDict(schema)) throw new Error(`${where}: schema がありません`);
    const unsupported = ["additionalProperties", "allOf", "anyOf", "oneOf"].filter((key) => key in schema);
    if (unsupported.length > 0 || Array.isArray(schema.type)) {
      throw new Error(
        `${where}: 未対応の schema 構文です: ${JSON.stringify(unsupported.length ? unsupported : schema.type)}`,
      );
    }
    let type: ApiType;
    if ("$ref" in schema) {
      type = { kind: "ref", name: this.resolveRef(String(schema.$ref)) };
    } else {
      switch (schema.type) {
        case "array":
          type = { kind: "array", items: this.typeOf(schema.items, `${where}.items`) };
          break;
        case "string":
          type = { kind: "string" };
          break;
        case "integer":
          type = { kind: schema.format === "int64" ? "int64" : "int32" };
          break;
        case "number":
          type = { kind: "double" };
          break;
        case "boolean":
          type = { kind: "bool" };
          break;
        default:
          throw new Error(
            `${where}: 未対応の型です: ${JSON.stringify(schema.type ?? null)}（object は components.schemas に定義して $ref で参照してください）`,
          );
      }
    }
    if (schema.nullable === true) type.nullable = true;
    if ("enum" in schema) {
      if (type.kind !== "string" || !Array.isArray(schema.enum) || !schema.enum.every((v) => typeof v === "string")) {
        throw new Error(`${where}: enum は文字列の配列のみ対応しています`);
      }
      type.values = schema.enum;
    }
    return type;
  }

  private readSchemas(): Schema[] {
    const result: Schema[] = [];
    const schemas = this.componentSchemas();
    for (const name of sorted(Object.keys(schemas))) {
      const schema = dict(schemas[name]);
      const where = `components.schemas.${name}`;
      if (schema.type !== "object" || !isDict(schema.properties)) {
        throw new Error(`${where}: type: object と properties が必要です`);
      }
      const properties = schema.properties;
      const required = new Set(Array.isArray(schema.required) ? schema.required.map(String) : []);
      const unknown = sorted([...required].filter((key) => !(key in properties)));
      if (unknown.length > 0) {
        throw new Error(`${where}: required に未定義のプロパティがあります: ${JSON.stringify(unknown)}`);
      }
      const props = Object.entries(properties).map(([key, prop]) => ({
        key,
        name: camel(key),
        type: this.typeOf(prop, `${where}.${key}`),
        optional: !required.has(key),
      }));
      const names = props.map((p) => p.name);
      if (new Set(names).size !== names.length) {
        throw new Error(`${where}: 変換後のプロパティ名が重複します: ${JSON.stringify(names)}`);
      }
      result.push({ name, type: typeName(name), props });
    }
    return result;
  }

  // ---- operation ----
  private readOperations(): Operation[] {
    const result: Operation[] = [];
    const seen = new Set<string>();
    for (const [path, rawItem] of Object.entries(dict(this.doc.paths))) {
      const item = dict(rawItem);
      const extra = sorted(Object.keys(item).filter((key) => !(METHODS as readonly string[]).includes(key)));
      if (extra.length > 0) {
        throw new Error(`paths.${path}: 未対応のキーです: ${JSON.stringify(extra)}`);
      }
      for (const method of METHODS) {
        if (!(method in item)) continue;
        const op = dict(item[method]);
        const where = `paths.${path}.${method}`;
        const opId = op.operationId;
        if (!opId) throw new Error(`${where}: operationId が必要です`);
        const name = camel(String(opId));
        if (seen.has(name)) throw new Error(`${where}: operationId が重複しています: ${opId}`);
        seen.add(name);

        const params: Param[] = [];
        for (const rawParam of Array.isArray(op.parameters) ? op.parameters : []) {
          const param = dict(rawParam);
          if ("$ref" in param || (param.in !== "path" && param.in !== "query")) {
            throw new Error(`${where}: parameters は in: path / query のみ対応しています`);
          }
          const ptype = this.typeOf(param.schema, `${where}.parameters.${param.name}`);
          if (ptype.kind === "ref" || ptype.kind === "array") {
            throw new Error(`${where}: パラメータはプリミティブ型のみ対応しています`);
          }
          const key = String(param.name);
          params.push({
            key,
            name: camel(key),
            in: param.in,
            type: ptype,
            optional: param.in === "query" && !param.required,
          });
        }
        const placeholders = [...path.matchAll(/\{([^}]+)\}/g)].map((m) => m[1] as string);
        const pathKeys = params.filter((p) => p.in === "path").map((p) => p.key);
        if (sorted(placeholders).join("\0") !== sorted(pathKeys).join("\0")) {
          throw new Error(`${where}: パスの {...} と in: path のパラメータが一致しません`);
        }

        let body: Operation["body"] = null;
        if ("requestBody" in op) {
          const requestBody = dict(op.requestBody);
          const content = dict(requestBody.content);
          if (Object.keys(content).join() !== "application/json") {
            throw new Error(`${where}: requestBody は application/json のみ対応しています`);
          }
          body = {
            type: this.typeOf(dict(content["application/json"]).schema, `${where}.requestBody`),
            optional: !requestBody.required,
          };
          if (body.optional) throw new Error(`${where}: requestBody は required: true にしてください`);
        }

        const responses = dict(op.responses);
        const success = Object.keys(responses)
          .filter((code) => code.startsWith("2"))
          .sort(compare);
        const first = success[0];
        if (first === undefined) throw new Error(`${where}: 2xx のレスポンスが必要です`);
        const content = dict(responses[first]).content;
        let returns: ApiType | null = null;
        if (isDict(content) && Object.keys(content).length > 0) {
          if (Object.keys(content).join() !== "application/json") {
            throw new Error(`${where}: レスポンスは application/json のみ対応しています`);
          }
          returns = this.typeOf(dict(content["application/json"]).schema, `${where}.responses.${first}`);
        }
        result.push({
          id: String(opId),
          name,
          method: method.toUpperCase(),
          path,
          params,
          body,
          returns,
        });
      }
    }
    return result;
  }
}

const HEADER = "// Generated by tools/generate.ts from specs/openapi/openapi.yaml. Do not edit.";

// ---------------- Swift ----------------
function swiftType(t: ApiType): string {
  switch (t.kind) {
    case "ref":
      return typeName(t.name);
    case "array":
      return `[${swiftType(t.items)}]`;
    default:
      return { string: "String", int32: "Int", int64: "Int64", double: "Double", bool: "Bool" }[t.kind];
  }
}

export function swiftModels(spec: Spec): string {
  const out = [HEADER, "import Foundation", ""];
  for (const s of spec.schemas) {
    out.push(`struct ${s.type}: Codable, Equatable, Sendable {`);
    for (const p of s.props) {
      out.push(`    var ${swiftIdent(p.name)}: ${swiftType(p.type)}${p.optional || p.type.nullable ? "?" : ""}`);
    }
    const args = s.props
      .map((p) => `${p.name}: ${swiftType(p.type)}${p.optional ? "? = nil" : p.type.nullable ? "?" : ""}`)
      .join(", ");
    out.push("");
    out.push(`    init(${args}) {`);
    for (const p of s.props) out.push(`        self.${p.name} = ${swiftIdent(p.name)}`);
    out.push("    }");
    if (s.props.some((p) => p.name !== p.key) || s.props.some((p) => p.type.nullable)) {
      out.push("");
      out.push("    enum CodingKeys: String, CodingKey {");
      for (const p of s.props) {
        const suffix = p.name !== p.key ? ` = "${p.key}"` : "";
        out.push(`        case ${swiftIdent(p.name)}${suffix}`);
      }
      out.push("    }");
    }
    if (s.props.some((p) => p.type.nullable)) {
      out.push(
        "",
        "    init(from decoder: any Decoder) throws {",
        "        let values = try decoder.container(keyedBy: CodingKeys.self)",
      );
      for (const p of s.props) {
        const key = swiftIdent(p.name);
        if (!p.optional && p.type.nullable) {
          out.push(
            `        guard values.contains(.${key}) else { throw DecodingError.keyNotFound(CodingKeys.${key}, .init(codingPath: decoder.codingPath, debugDescription: "Missing required property")) }`,
          );
        }
        const method = p.optional || p.type.nullable ? "decodeIfPresent" : "decode";
        out.push(`        ${key} = try values.${method}(${swiftType(p.type)}.self, forKey: .${key})`);
      }
      out.push("    }");
    }
    out.push("}");
    out.push("");
  }
  return out.join("\n");
}

export function swiftClient(spec: Spec): string {
  const out = [
    HEADER,
    "import Foundation",
    "",
    "/// specs/openapi の operation を 1 メソッドずつ持つ。通信は HTTPTransport に任せる。",
    "struct APIClient: Sendable {",
    "    let transport: any HTTPTransport",
    "",
  ];
  for (const op of spec.operations) {
    const args = op.params.map(
      (p) => `${p.name}: ${swiftType(p.type)}${p.optional ? "? = nil" : p.type.nullable ? "?" : ""}`,
    );
    if (op.body) args.push(`body: ${swiftType(op.body.type)}`);
    const ret = op.returns ? ` -> APIResponse<${swiftType(op.returns)}>` : "";
    const path = op.path.replace(/\{([^}]+)\}/g, (_, key: string) => `\\(pathSegment(${camel(key)}))`);
    out.push(`    /// ${op.method} ${op.path}（operationId: ${op.id}）`);
    out.push(`    func ${op.name}(${args.join(", ")}) async throws${ret} {`);
    const hasQuery = op.params.some((p) => p.in === "query");
    out.push(`        ${hasQuery ? "var" : "let"} query: [URLQueryItem] = []`);
    for (const p of op.params) {
      if (p.in !== "query") continue;
      if (p.optional) {
        out.push(`        if let ${p.name} { query.append(URLQueryItem(name: "${p.key}", value: String(${p.name}))) }`);
      } else {
        out.push(`        query.append(URLQueryItem(name: "${p.key}", value: String(${p.name})))`);
      }
    }
    const body = op.body ? "try JSONEncoder().encode(body)" : "nil";
    out.push(`        let request = HTTPRequest(method: "${op.method}", path: "${path}", query: query, body: ${body})`);
    if (op.returns) {
      out.push(`        return try await transport.call(request, decoding: ${swiftType(op.returns)}.self)`);
    } else {
      out.push("        try await transport.call(request)");
    }
    out.push("    }");
    out.push("");
  }
  if (out[out.length - 1] === "") out.pop();
  out.push("}", "");
  return out.join("\n");
}

// ---------------- Kotlin ----------------
function kotlinType(t: ApiType): string {
  switch (t.kind) {
    case "ref":
      return typeName(t.name);
    case "array":
      return `List<${kotlinType(t.items)}>`;
    default:
      return { string: "String", int32: "Int", int64: "Long", double: "Double", bool: "Boolean" }[t.kind];
  }
}

export function kotlinModels(spec: Spec, pkg: string): string {
  const renamed = spec.schemas.some((s) => s.props.some((p) => p.name !== p.key));
  const imports = [
    ...(renamed ? ["import kotlinx.serialization.SerialName"] : []),
    "import kotlinx.serialization.Serializable",
  ];
  const out = [HEADER, `package ${pkg}`, "", ...imports, ""];
  for (const s of spec.schemas) {
    out.push("@Serializable");
    out.push(`internal data class ${s.type}(`);
    for (const p of s.props) {
      const serial = p.name !== p.key ? `@SerialName("${p.key}") ` : "";
      const fallback = p.optional ? "? = null" : p.type.nullable ? "?" : "";
      out.push(`    ${serial}val ${kotlinIdent(p.name)}: ${kotlinType(p.type)}${fallback},`);
    }
    out.push(")");
    out.push("");
  }
  return out.join("\n");
}

export function kotlinClient(spec: Spec, pkg: string): string {
  const imports = spec.operations.some((op) => op.body) ? ["import kotlinx.serialization.encodeToString", ""] : [];
  const out = [
    HEADER,
    `package ${pkg}`,
    "",
    ...imports,
    "/** specs/openapi の operation を 1 メソッドずつ持つ。通信は HttpTransport に任せる。 */",
    "internal class ApiClient(private val transport: HttpTransport) {",
  ];
  for (const op of spec.operations) {
    const args = op.params.map((p) => `${kotlinIdent(p.name)}: ${kotlinType(p.type)}${p.optional ? "? = null" : ""}`);
    if (op.body) args.push(`body: ${kotlinType(op.body.type)}`);
    const ret = op.returns ? `: ApiResponse<${kotlinType(op.returns)}>` : "";
    const path = op.path.replace(
      /\{([^}]+)\}/g,
      (_, key: string) => `\${pathSegment(${kotlinIdent(camel(key))}.toString())}`,
    );
    out.push(`    /** ${op.method} ${op.path}（operationId: ${op.id}） */`);
    out.push(`    suspend fun ${op.name}(${args.join(", ")})${ret} {`);
    const hasQuery = op.params.some((p) => p.in === "query");
    out.push(
      `        val query = ${hasQuery ? "mutableListOf<Pair<String, String>>()" : "emptyList<Pair<String, String>>()"}`,
    );
    for (const p of op.params) {
      if (p.in !== "query") continue;
      const ident = kotlinIdent(p.name);
      if (p.optional) {
        out.push(`        if (${ident} != null) query.add("${p.key}" to ${ident}.toString())`);
      } else {
        out.push(`        query.add("${p.key}" to ${ident}.toString())`);
      }
    }
    const body = op.body ? "ApiJson.encodeToString(body)" : "null";
    out.push(
      `        val request = HttpRequest(method = "${op.method}", path = "${path}", query = query, body = ${body})`,
    );
    if (op.returns) {
      out.push(`        return transport.call<${kotlinType(op.returns)}>(request)`);
    } else {
      out.push("        transport.callUnit(request)");
    }
    out.push("    }");
    out.push("");
  }
  if (out[out.length - 1] === "") out.pop();
  out.push("}", "");
  return out.join("\n");
}

// ---------------- シナリオ検証用 ----------------
/** シナリオの fixture が schema に合っているかを検査する。 */
export function validateValue(spec: Spec, t: ApiType, value: unknown, where: string, optional = false): void {
  if (value === null || value === undefined) {
    if (value === undefined ? optional : t.nullable) return;
    throw new Error(`${where}: null は許されていません`);
  }
  if (t.values && !t.values.includes(value)) throw new Error(`${where}: enum の許容値ではありません`);
  if (t.kind === "ref") {
    const schema = spec.schemas.find((s) => s.name === t.name);
    if (!schema) throw new Error(`${where}: 不明な schema です: ${t.name}`);
    if (!isDict(value)) throw new Error(`${where}: ${t.name} はオブジェクトです`);
    const keys = new Map(schema.props.map((p) => [p.key, p]));
    const unknown = sorted(Object.keys(value).filter((key) => !keys.has(key)));
    if (unknown.length > 0) {
      throw new Error(`${where}: ${t.name} に無いプロパティです: ${JSON.stringify(unknown)}`);
    }
    for (const [key, prop] of keys) {
      if (!(key in value) && !prop.optional) throw new Error(`${where}: ${t.name}.${key} は必須です`);
      if (key in value) validateValue(spec, prop.type, value[key], `${where}.${key}`, prop.optional);
    }
  } else if (t.kind === "array") {
    if (!Array.isArray(value)) throw new Error(`${where}: 配列が必要です`);
    value.forEach((v, i) => {
      validateValue(spec, t.items, v, `${where}[${i}]`);
    });
  } else {
    const ok = {
      string: typeof value === "string",
      int32: Number.isInteger(value),
      int64: Number.isInteger(value),
      double: typeof value === "number",
      bool: typeof value === "boolean",
    }[t.kind];
    if (!ok) throw new Error(`${where}: ${t.kind} ではありません: ${JSON.stringify(value)}`);
  }
}
