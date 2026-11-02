"""shared/openapi から Swift / Kotlin の DTO と API クライアントを生成する。

対応するのはテンプレートで使う範囲だけで、未対応の構文は黙って無視せず ValueError で失敗する。
- components.schemas: type: object（properties / required）
- 型: string / integer（int64 は 64bit）/ number / boolean / array / $ref
- operation: get / post / put / patch / delete、operationId 必須
- parameters: in: path / query（プリミティブ型のみ）
- requestBody: application/json
- responses: 最初の 2xx。application/json なら戻り値、content が無ければ戻り値なし
"""
import re

METHODS = ("get", "post", "put", "patch", "delete")
PRIMITIVES = {"string", "integer", "number", "boolean"}
SWIFT_KEYWORDS = {"associatedtype", "class", "default", "deinit", "enum", "extension", "func", "import", "init",
                  "inout", "internal", "let", "operator", "private", "protocol", "public", "repeat", "return",
                  "self", "static", "struct", "subscript", "switch", "true", "false", "type", "var", "where", "while"}
KOTLIN_KEYWORDS = {"as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface",
                   "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try",
                   "typealias", "typeof", "val", "var", "when", "while"}


def camel(name):
    parts = [p for p in re.split(r"[^0-9A-Za-z]+", name) if p]
    if not parts:
        raise ValueError(f"識別子にできない名前: {name!r}")
    head, *rest = parts
    text = head[0].lower() + head[1:] + "".join(p[0].upper() + p[1:] for p in rest)
    if text[0].isdigit():
        raise ValueError(f"数字で始まる名前は使えません: {name!r}")
    return text


def type_name(name):
    text = camel(name)
    return text[0].upper() + text[1:] + "DTO"


def swift_ident(name):
    return f"`{name}`" if name in SWIFT_KEYWORDS else name


def kotlin_ident(name):
    return f"`{name}`" if name in KOTLIN_KEYWORDS else name


class Spec:
    def __init__(self, doc):
        self.doc = doc
        if not str(doc.get("openapi", "")).startswith("3."):
            raise ValueError("OpenAPI 3.x のみ対応しています")
        self.schemas = self._schemas()
        self.operations = self._operations()

    # ---- 型 ----
    def resolve_ref(self, ref):
        prefix = "#/components/schemas/"
        if not ref.startswith(prefix) or ref[len(prefix):] not in self.doc.get("components", {}).get("schemas", {}):
            raise ValueError(f"解決できない $ref: {ref}")
        return ref[len(prefix):]

    def type_of(self, schema, where):
        """言語に依存しない型表現: ("ref", 名前) / ("array", 要素) / ("string"|"int32"|"int64"|"double"|"bool",)"""
        if not isinstance(schema, dict):
            raise ValueError(f"{where}: schema がありません")
        if "$ref" in schema:
            return ("ref", self.resolve_ref(schema["$ref"]))
        kind = schema.get("type")
        unsupported = {"enum", "oneOf", "anyOf", "allOf", "additionalProperties"} & schema.keys()
        if unsupported or isinstance(kind, list):
            raise ValueError(f"{where}: 未対応の schema 構文です: {sorted(unsupported) or kind}")
        if kind == "array":
            return ("array", self.type_of(schema.get("items"), f"{where}.items"))
        if kind == "string":
            return ("string",)
        if kind == "integer":
            return ("int64",) if schema.get("format") == "int64" else ("int32",)
        if kind == "number":
            return ("double",)
        if kind == "boolean":
            return ("bool",)
        raise ValueError(f"{where}: 未対応の型です: {kind!r}（object は components.schemas に定義して $ref で参照してください）")

    def _schemas(self):
        result = []
        for name, schema in sorted(self.doc.get("components", {}).get("schemas", {}).items()):
            where = f"components.schemas.{name}"
            if schema.get("type") != "object" or not isinstance(schema.get("properties"), dict):
                raise ValueError(f"{where}: type: object と properties が必要です")
            required = set(schema.get("required", []))
            unknown = required - schema["properties"].keys()
            if unknown:
                raise ValueError(f"{where}: required に未定義のプロパティがあります: {sorted(unknown)}")
            props = []
            for key, prop in schema["properties"].items():
                props.append({"key": key, "name": camel(key), "type": self.type_of(prop, f"{where}.{key}"),
                              "optional": key not in required})
            names = [p["name"] for p in props]
            if len(set(names)) != len(names):
                raise ValueError(f"{where}: 変換後のプロパティ名が重複します: {names}")
            result.append({"name": name, "type": type_name(name), "props": props})
        return result

    # ---- operation ----
    def _operations(self):
        result = []
        seen = set()
        for path, item in self.doc.get("paths", {}).items():
            extra = set(item) - set(METHODS)
            if extra:
                raise ValueError(f"paths.{path}: 未対応のキーです: {sorted(extra)}")
            for method in METHODS:
                if method not in item:
                    continue
                op = item[method]
                where = f"paths.{path}.{method}"
                op_id = op.get("operationId")
                if not op_id:
                    raise ValueError(f"{where}: operationId が必要です")
                name = camel(op_id)
                if name in seen:
                    raise ValueError(f"{where}: operationId が重複しています: {op_id}")
                seen.add(name)
                params = []
                for param in op.get("parameters", []):
                    if "$ref" in param or param.get("in") not in ("path", "query"):
                        raise ValueError(f"{where}: parameters は in: path / query のみ対応しています")
                    ptype = self.type_of(param.get("schema"), f"{where}.parameters.{param.get('name')}")
                    if ptype[0] in ("ref", "array"):
                        raise ValueError(f"{where}: パラメータはプリミティブ型のみ対応しています")
                    params.append({"key": param["name"], "name": camel(param["name"]), "in": param["in"],
                                   "type": ptype, "optional": param["in"] == "query" and not param.get("required", False)})
                placeholders = re.findall(r"\{([^}]+)\}", path)
                path_keys = [p["key"] for p in params if p["in"] == "path"]
                if sorted(placeholders) != sorted(path_keys):
                    raise ValueError(f"{where}: パスの {{...}} と in: path のパラメータが一致しません")
                body = None
                if "requestBody" in op:
                    content = op["requestBody"].get("content", {})
                    if set(content) != {"application/json"}:
                        raise ValueError(f"{where}: requestBody は application/json のみ対応しています")
                    body = {"type": self.type_of(content["application/json"].get("schema"), f"{where}.requestBody"),
                            "optional": not op["requestBody"].get("required", False)}
                    if body["optional"]:
                        raise ValueError(f"{where}: requestBody は required: true にしてください")
                success = sorted(code for code in op.get("responses", {}) if str(code).startswith("2"))
                if not success:
                    raise ValueError(f"{where}: 2xx のレスポンスが必要です")
                response = op["responses"][success[0]]
                content = response.get("content")
                returns = None
                if content:
                    if set(content) != {"application/json"}:
                        raise ValueError(f"{where}: レスポンスは application/json のみ対応しています")
                    returns = self.type_of(content["application/json"].get("schema"), f"{where}.responses.{success[0]}")
                result.append({"id": op_id, "name": name, "method": method.upper(), "path": path,
                               "params": params, "body": body, "returns": returns})
        return result


HEADER = "// Generated by tools/generate.py from shared/openapi/openapi.yaml. Do not edit."


# ---------------- Swift ----------------
def swift_type(t):
    if t[0] == "ref":
        return type_name(t[1])
    if t[0] == "array":
        return f"[{swift_type(t[1])}]"
    return {"string": "String", "int32": "Int", "int64": "Int64", "double": "Double", "bool": "Bool"}[t[0]]


def swift_models(spec):
    out = [HEADER, "import Foundation", ""]
    for s in spec.schemas:
        out.append(f"struct {s['type']}: Codable, Equatable, Sendable {{")
        for p in s["props"]:
            out.append(f"    var {swift_ident(p['name'])}: {swift_type(p['type'])}{'?' if p['optional'] else ''}")
        args = ", ".join(f"{p['name']}: {swift_type(p['type'])}{'? = nil' if p['optional'] else ''}" for p in s["props"])
        out.append("")
        out.append(f"    init({args}) {{")
        for p in s["props"]:
            out.append(f"        self.{p['name']} = {swift_ident(p['name'])}")
        out.append("    }")
        if any(p["name"] != p["key"] for p in s["props"]):
            out.append("")
            out.append("    enum CodingKeys: String, CodingKey {")
            for p in s["props"]:
                suffix = f' = "{p["key"]}"' if p["name"] != p["key"] else ""
                out.append(f"        case {swift_ident(p['name'])}{suffix}")
            out.append("    }")
        out.append("}")
        out.append("")
    return "\n".join(out)


def swift_client(spec):
    out = [HEADER, "import Foundation", "",
           "/// shared/openapi の operation を 1 メソッドずつ持つ。通信は HTTPTransport に任せる。",
           "struct APIClient: Sendable {",
           "    let transport: any HTTPTransport", ""]
    for op in spec.operations:
        args = [f"{p['name']}: {swift_type(p['type'])}{'? = nil' if p['optional'] else ''}" for p in op["params"]]
        if op["body"]:
            args.append(f"body: {swift_type(op['body']['type'])}")
        ret = f" -> {swift_type(op['returns'])}" if op["returns"] else ""
        path = re.sub(r"\{([^}]+)\}", lambda m: f"\\(pathSegment({camel(m.group(1))}))", op["path"])
        out.append(f"    /// {op['method']} {op['path']}（operationId: {op['id']}）")
        out.append(f"    func {op['name']}({', '.join(args)}) async throws{ret} {{")
        has_query = any(p["in"] == "query" for p in op["params"])
        out.append(f"        {'var' if has_query else 'let'} query: [URLQueryItem] = []")
        for p in op["params"]:
            if p["in"] != "query":
                continue
            if p["optional"]:
                out.append(f"        if let {p['name']} {{ query.append(URLQueryItem(name: \"{p['key']}\", value: String({p['name']}))) }}")
            else:
                out.append(f"        query.append(URLQueryItem(name: \"{p['key']}\", value: String({p['name']})))")
        body = "try JSONEncoder().encode(body)" if op["body"] else "nil"
        out.append(f"        let request = HTTPRequest(method: \"{op['method']}\", path: \"{path}\", query: query, body: {body})")
        if op["returns"]:
            out.append(f"        return try await transport.call(request, decoding: {swift_type(op['returns'])}.self)")
        else:
            out.append("        try await transport.call(request)")
        out.append("    }")
        out.append("")
    if out[-1] == "":
        out.pop()
    out += ["}", ""]
    return "\n".join(out)


# ---------------- Kotlin ----------------
def kotlin_type(t):
    if t[0] == "ref":
        return type_name(t[1])
    if t[0] == "array":
        return f"List<{kotlin_type(t[1])}>"
    return {"string": "String", "int32": "Int", "int64": "Long", "double": "Double", "bool": "Boolean"}[t[0]]


def kotlin_models(spec, package):
    renamed = any(p["name"] != p["key"] for s in spec.schemas for p in s["props"])
    imports = (["import kotlinx.serialization.SerialName"] if renamed else []) + ["import kotlinx.serialization.Serializable"]
    out = [HEADER, f"package {package}", "", *imports, ""]
    for s in spec.schemas:
        out.append("@Serializable")
        out.append(f"internal data class {s['type']}(")
        for p in s["props"]:
            serial = f"@SerialName(\"{p['key']}\") " if p["name"] != p["key"] else ""
            default = "? = null" if p["optional"] else ""
            out.append(f"    {serial}val {kotlin_ident(p['name'])}: {kotlin_type(p['type'])}{default},")
        out.append(")")
        out.append("")
    return "\n".join(out)


def kotlin_client(spec, package):
    imports = ["import kotlinx.serialization.encodeToString", ""] if any(op["body"] for op in spec.operations) else []
    out = [HEADER, f"package {package}", "", *imports,
           "/** shared/openapi の operation を 1 メソッドずつ持つ。通信は HttpTransport に任せる。 */",
           "internal class ApiClient(private val transport: HttpTransport) {"]
    for op in spec.operations:
        args = [f"{kotlin_ident(p['name'])}: {kotlin_type(p['type'])}{'? = null' if p['optional'] else ''}" for p in op["params"]]
        if op["body"]:
            args.append(f"body: {kotlin_type(op['body']['type'])}")
        ret = f": {kotlin_type(op['returns'])}" if op["returns"] else ""
        path = re.sub(r"\{([^}]+)\}", lambda m: f"${{pathSegment({kotlin_ident(camel(m.group(1)))}.toString())}}", op["path"])
        out.append(f"    /** {op['method']} {op['path']}（operationId: {op['id']}） */")
        out.append(f"    suspend fun {op['name']}({', '.join(args)}){ret} {{")
        has_query = any(p["in"] == "query" for p in op["params"])
        out.append("        val query = " + ("mutableListOf<Pair<String, String>>()" if has_query else "emptyList<Pair<String, String>>()"))
        for p in op["params"]:
            if p["in"] != "query":
                continue
            ident = kotlin_ident(p["name"])
            if p["optional"]:
                out.append(f"        if ({ident} != null) query.add(\"{p['key']}\" to {ident}.toString())")
            else:
                out.append(f"        query.add(\"{p['key']}\" to {ident}.toString())")
        body = "ApiJson.encodeToString(body)" if op["body"] else "null"
        out.append(f"        val request = HttpRequest(method = \"{op['method']}\", path = \"{path}\", query = query, body = {body})")
        if op["returns"]:
            out.append(f"        return transport.call<{kotlin_type(op['returns'])}>(request)")
        else:
            out.append("        transport.callUnit(request)")
        out.append("    }")
        out.append("")
    if out[-1] == "":
        out.pop()
    out += ["}", ""]
    return "\n".join(out)


# ---------------- シナリオ検証用 ----------------
def validate_value(spec, t, value, where, optional=False):
    """シナリオの fixture が schema に合っているかを検査する。"""
    if value is None:
        if optional:
            return
        raise ValueError(f"{where}: null は許されていません")
    kind = t[0]
    if kind == "ref":
        schema = next(s for s in spec.schemas if s["name"] == t[1])
        if not isinstance(value, dict):
            raise ValueError(f"{where}: {t[1]} はオブジェクトです")
        keys = {p["key"]: p for p in schema["props"]}
        unknown = set(value) - keys.keys()
        if unknown:
            raise ValueError(f"{where}: {t[1]} に無いプロパティです: {sorted(unknown)}")
        for key, prop in keys.items():
            if key not in value and not prop["optional"]:
                raise ValueError(f"{where}: {t[1]}.{key} は必須です")
            if key in value:
                validate_value(spec, prop["type"], value[key], f"{where}.{key}", prop["optional"])
    elif kind == "array":
        if not isinstance(value, list):
            raise ValueError(f"{where}: 配列が必要です")
        for i, v in enumerate(value):
            validate_value(spec, t[1], v, f"{where}[{i}]")
    else:
        ok = {"string": lambda v: isinstance(v, str),
              "int32": lambda v: isinstance(v, int) and not isinstance(v, bool),
              "int64": lambda v: isinstance(v, int) and not isinstance(v, bool),
              "double": lambda v: isinstance(v, (int, float)) and not isinstance(v, bool),
              "bool": lambda v: isinstance(v, bool)}[kind](value)
        if not ok:
            raise ValueError(f"{where}: {kind} ではありません: {value!r}")
