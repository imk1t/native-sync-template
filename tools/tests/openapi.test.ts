/** OpenAPI 生成器の対応範囲と、未対応構文で失敗することを検証する。 */
import { describe, expect, test } from "bun:test";
import * as gen from "../lib/openapi";
import type { Dict } from "../lib/util";

function doc(paths: Dict = {}, schemas?: Dict): Dict {
  return {
    openapi: "3.1.0",
    paths,
    components: {
      schemas: schemas ?? {
        Item: {
          type: "object",
          required: ["id", "created_at"],
          properties: {
            id: { type: "string" },
            created_at: { type: "integer", format: "int64" },
            tags: { type: "array", items: { type: "string" } },
            default: { type: "boolean" },
          },
        },
      },
    },
  };
}

const GET_ITEM = {
  "/items/{item_id}": {
    get: {
      operationId: "getItem",
      parameters: [
        { name: "item_id", in: "path", required: true, schema: { type: "string" } },
        { name: "page_size", in: "query", schema: { type: "integer" } },
      ],
      responses: {
        "200": {
          description: "OK",
          content: { "application/json": { schema: { $ref: "#/components/schemas/Item" } } },
        },
      },
    },
  },
};
const DELETE_ITEM = {
  "/items/{id}": {
    delete: {
      operationId: "delete-item",
      parameters: [{ name: "id", in: "path", required: true, schema: { type: "string" } }],
      responses: { "204": { description: "No Content" } },
    },
  },
};

describe("models", () => {
  test("snake_case のキーを両 OS で対応付ける", () => {
    const spec = new gen.Spec(doc());
    const swift = gen.swiftModels(spec);
    const kotlin = gen.kotlinModels(spec, "p");
    expect(swift).toContain("var createdAt: Int64");
    expect(swift).toContain('case createdAt = "created_at"');
    expect(swift).toContain("var tags: [String]?");
    expect(swift).toContain("var `default`: Bool?");
    expect(kotlin).toContain('@SerialName("created_at") val createdAt: Long,');
    expect(kotlin).toContain("val tags: List<String>? = null,");
    expect(kotlin).toContain("internal data class ItemDTO(");
  });

  test.each([[{ oneOf: [] }], [{ type: "object" }], [{ type: ["string", "null"] }]])(
    "未対応の schema は失敗する: %j",
    (prop) => {
      expect(() => new gen.Spec(doc({}, { X: { type: "object", properties: { p: prop } } }))).toThrow();
    },
  );

  test("解決できない $ref は失敗する", () => {
    const schemas = { X: { type: "object", properties: { p: { $ref: "#/components/schemas/Missing" } } } };
    expect(() => new gen.Spec(doc({}, schemas))).toThrow();
  });
});

describe("client", () => {
  test("path と query のパラメータ", () => {
    const spec = new gen.Spec(doc(GET_ITEM));
    const swift = gen.swiftClient(spec);
    const kotlin = gen.kotlinClient(spec, "p");
    expect(swift).toContain("func getItem(itemId: String, pageSize: Int? = nil) async throws -> APIResponse<ItemDTO>");
    expect(swift).toContain('path: "/items/\\(pathSegment(itemId))"');
    expect(swift).toContain(
      'if let pageSize { query.append(URLQueryItem(name: "page_size", value: String(pageSize))) }',
    );
    expect(kotlin).toContain("suspend fun getItem(itemId: String, pageSize: Int? = null): ApiResponse<ItemDTO>");
    expect(kotlin).toContain('path = "/items/${pathSegment(itemId.toString())}"');
    expect(kotlin).toContain('if (pageSize != null) query.add("page_size" to pageSize.toString())');
  });

  test("content の無いレスポンスは戻り値なし", () => {
    const spec = new gen.Spec(doc(DELETE_ITEM));
    expect(gen.swiftClient(spec)).toContain("func deleteItem(id: String) async throws {");
    expect(gen.swiftClient(spec)).toContain("try await transport.call(request)");
    expect(gen.kotlinClient(spec, "p")).toContain("suspend fun deleteItem(id: String) {");
    expect(gen.swiftClient(spec)).toContain("let query: [URLQueryItem] = []");
  });

  test("JSON の requestBody", () => {
    const ref = { schema: { $ref: "#/components/schemas/Item" } };
    const paths = {
      "/items": {
        post: {
          operationId: "createItem",
          requestBody: { required: true, content: { "application/json": ref } },
          responses: { "201": { description: "Created", content: { "application/json": ref } } },
        },
      },
    };
    const spec = new gen.Spec(doc(paths));
    expect(gen.swiftClient(spec)).toContain("body: try JSONEncoder().encode(body)");
    const kotlin = gen.kotlinClient(spec, "p");
    expect(kotlin).toContain("import kotlinx.serialization.encodeToString");
    expect(kotlin).toContain("body = ApiJson.encodeToString(body)");
  });

  const ok = { "200": { description: "OK" } };
  test.each([
    ["operationId なし", { "/a": { get: { responses: ok } } }],
    ["path パラメータ未定義", { "/a/{id}": { get: { operationId: "a", responses: ok } } }],
    [
      "header パラメータ",
      {
        "/a": {
          get: {
            operationId: "a",
            parameters: [{ name: "h", in: "header", schema: { type: "string" } }],
            responses: ok,
          },
        },
      },
    ],
    ["2xx なし", { "/a": { get: { operationId: "a", responses: { "404": { description: "NG" } } } } }],
    [
      "JSON 以外",
      {
        "/a": {
          get: { operationId: "a", responses: { "200": { description: "OK", content: { "text/plain": {} } } } },
        },
      },
    ],
    [
      "operationId の重複",
      { "/a": { get: { operationId: "a", responses: ok } }, "/b": { get: { operationId: "a", responses: ok } } },
    ],
  ])("未対応の operation は失敗する: %s", (_, paths) => {
    expect(() => new gen.Spec(doc(paths))).toThrow();
  });
});

describe("validateValue", () => {
  const spec = new gen.Spec(doc());
  const item: gen.ApiType = { kind: "ref", name: "Item" };

  test("schema に合う値は通る", () => {
    gen.validateValue(spec, item, { id: "1", created_at: 1 }, "x");
  });

  test.each([
    [{ id: "1" }],
    [{ id: 1, created_at: 1 }],
    [{ id: "1", created_at: 1, extra: 0 }],
    [{ id: "1", created_at: true }],
    [[]],
    [null],
  ])("schema に合わない値は失敗する: %j", (value) => {
    expect(() => gen.validateValue(spec, item, value, "x")).toThrow();
  });
});
