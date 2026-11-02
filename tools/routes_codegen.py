"""shared/routes.yaml から Swift / Kotlin の Route と AppTab を生成する（docs/adr/0002）。

未対応の書き方は黙って無視せず ValueError で失敗する。
- routes: name（lowerCamelCase）・feature・params（string / integer）・deepLink
- tabs: id（lowerCamelCase）・title・root（引数の無い route）
- feature は modules/ 配下の spec.md のあるモジュールで、その spec.md が `name` を参照していること
"""
import re

IDENT = re.compile(r"[a-z][A-Za-z0-9]*")
PARAM = re.compile(r"\{([A-Za-z][A-Za-z0-9]*)\}")
SEGMENT = re.compile(r"[A-Za-z0-9._~-]+")
TYPES = {"string": ("String", "String"), "integer": ("Int", "Long")}


def cap(name):
    return name[0].upper() + name[1:]


def _ident(value, where):
    if not isinstance(value, str) or not IDENT.fullmatch(value):
        raise ValueError(f"{where}: lowerCamelCase の英数字にしてください: {value!r}")
    return value


def load(doc, root=None):
    """routes.yaml を検証して {"routes": [...], "tabs": [...]} を返す。root を渡すと feature と spec.md も検査する。"""
    if not isinstance(doc, dict) or set(doc) - {"routes", "tabs"}:
        raise ValueError("routes.yaml は routes と tabs だけを持つ辞書にしてください")
    raw_routes = doc.get("routes")
    if not isinstance(raw_routes, list) or not raw_routes:
        raise ValueError("routes が必要です")
    routes, names, patterns = [], set(), set()
    for index, raw in enumerate(raw_routes):
        where = f"routes[{index}]"
        if not isinstance(raw, dict) or set(raw) - {"name", "feature", "params", "deepLink"}:
            raise ValueError(f"{where}: name・feature・params・deepLink だけを書いてください")
        name = _ident(raw.get("name"), f"{where}.name")
        if name in names:
            raise ValueError(f"{where}.name: 重複しています: {name}")
        names.add(name)
        feature = raw.get("feature")
        if not isinstance(feature, str) or not feature.startswith("features/"):
            raise ValueError(f"{where}.feature: features/<名前> を書いてください: {feature!r}")
        if root is not None:
            spec = root / "modules" / feature / "spec.md"
            if not spec.is_file():
                raise ValueError(f"{where}.feature: spec.md のあるモジュールではありません: {feature}")
            if f"`{name}`" not in spec.read_text(encoding="utf-8"):
                raise ValueError(f"modules/{feature}/spec.md: 画面遷移の節で `{name}` を参照してください")
        params = raw.get("params") or {}
        if not isinstance(params, dict):
            raise ValueError(f"{where}.params: 引数名をキーにした辞書にしてください")
        for key, kind in params.items():
            _ident(key, f"{where}.params")
            if kind not in TYPES:
                raise ValueError(f"{where}.params.{key}: string か integer にしてください: {kind!r}")
        segments = None
        if "deepLink" in raw:
            link = raw["deepLink"]
            if not isinstance(link, str) or not link.startswith("/"):
                raise ValueError(f"{where}.deepLink: / で始まるパスにしてください: {link!r}")
            segments = [s for s in link.split("/") if s]
            used = []
            for segment in segments:
                param = PARAM.fullmatch(segment)
                if param:
                    used.append(param.group(1))
                elif not SEGMENT.fullmatch(segment):
                    raise ValueError(f"{where}.deepLink: 使えない区切りです: {segment!r}")
            if sorted(used) != sorted(params) or len(set(used)) != len(used):
                raise ValueError(f"{where}.deepLink: {{引数}} は params と 1 対 1 にしてください")
            shape = tuple("{}" if PARAM.fullmatch(s) else s for s in segments)
            if shape in patterns:
                raise ValueError(f"{where}.deepLink: 他の route と同じ形です: {link}")
            patterns.add(shape)
        routes.append({"name": name, "feature": feature, "params": list(params.items()), "segments": segments})

    raw_tabs = doc.get("tabs")
    if not isinstance(raw_tabs, list) or not raw_tabs:
        raise ValueError("tabs が必要です（タブを使わない場合も 1 つ書く）")
    tabs, ids = [], set()
    by_name = {r["name"]: r for r in routes}
    for index, raw in enumerate(raw_tabs):
        where = f"tabs[{index}]"
        if not isinstance(raw, dict) or set(raw) != {"id", "title", "root"}:
            raise ValueError(f"{where}: id・title・root を書いてください")
        tab_id = _ident(raw["id"], f"{where}.id")
        if tab_id in ids:
            raise ValueError(f"{where}.id: 重複しています: {tab_id}")
        ids.add(tab_id)
        if not isinstance(raw["title"], str) or not raw["title"] or '"' in raw["title"] or "\\" in raw["title"]:
            raise ValueError(f"{where}.title: 空でない文字列にしてください（\" と \\ は使えません）")
        root_route = by_name.get(raw["root"])
        if root_route is None or root_route["params"]:
            raise ValueError(f"{where}.root: 引数の無い route の name にしてください: {raw['root']!r}")
        tabs.append({"id": tab_id, "title": raw["title"], "root": raw["root"]})
    return {"routes": routes, "tabs": tabs}


def _matchers(routes):
    """引数の少ない（固定の区切りが多い）route から順に照合する。"""
    linked = [r for r in routes if r["segments"] is not None]
    return sorted(linked, key=lambda r: len(r["params"]))


def swift(model):
    out = ["// Generated by tools/generate.py from shared/routes.yaml. Do not edit.", "import Foundation", "",
           "/// shared/routes.yaml の画面。", "public enum Route: Hashable, Sendable {"]
    for r in model["routes"]:
        out.append(f"    /// {r['feature']}")
        if r["params"]:
            args = ", ".join(f"{k}: {TYPES[t][0]}" for k, t in r["params"])
            out.append(f"    case {r['name']}({args})")
        else:
            out.append(f"    case {r['name']}")
    out += ["}", "", "/// shared/routes.yaml のタブ。", "public enum AppTab: String, CaseIterable, Hashable, Sendable {"]
    out += [f"    case {t['id']}" for t in model["tabs"]]
    out += ["", "    /// タブの先頭の画面", "    public var root: Route {", "        switch self {"]
    out += [f"        case .{t['id']}: .{t['root']}" for t in model["tabs"]]
    out += ["        }", "    }", "", "    public var title: String {", "        switch self {"]
    out += [f"        case .{t['id']}: \"{t['title']}\"" for t in model["tabs"]]
    out += ["        }", "    }", "}", "", "extension Route {",
            "    /// ディープリンクのパス（例: /items/1）から Route を作る。一致しなければ nil。",
            "    public init?(path: String) {",
            "        let segments = path.split(separator: \"/\").map { String($0).removingPercentEncoding ?? String($0) }"]
    for r in _matchers(model["routes"]):
        conds = [f"segments.count == {len(r['segments'])}"]
        values = {}
        for i, seg in enumerate(r["segments"]):
            param = PARAM.fullmatch(seg)
            if not param:
                conds.append(f"segments[{i}] == \"{seg}\"")
            elif dict(r["params"])[param.group(1)] == "integer":
                conds.append(f"let {param.group(1)} = Int(segments[{i}])")
                values[param.group(1)] = param.group(1)
            else:
                values[param.group(1)] = f"segments[{i}]"
        args = ", ".join(f"{k}: {values[k]}" for k, _ in r["params"])
        value = f".{r['name']}({args})" if r["params"] else f".{r['name']}"
        out.append(f"        if {', '.join(conds)} {{")
        out.append(f"            self = {value}")
        out.append("            return")
        out.append("        }")
    out += ["        return nil", "    }", "}", ""]
    return "\n".join(out)


def kotlin(model, package):
    out = ["// Generated by tools/generate.py from shared/routes.yaml. Do not edit.", f"package {package}", "",
           "import java.net.URLDecoder", "", "/** shared/routes.yaml の画面。 */", "sealed interface Route {"]
    for r in model["routes"]:
        out.append(f"    /** {r['feature']} */")
        if r["params"]:
            args = ", ".join(f"val {k}: {TYPES[t][1]}" for k, t in r["params"])
            out.append(f"    data class {cap(r['name'])}({args}) : Route")
        else:
            out.append(f"    data object {cap(r['name'])} : Route")
    out += ["", "    companion object {",
            "        /** ディープリンクのパス（例: /items/1）から Route を作る。一致しなければ null。 */",
            "        fun fromPath(path: String): Route? {",
            "            val segments = path.split(\"/\").filter { it.isNotEmpty() }.map(::decode)"]
    for r in _matchers(model["routes"]):
        conds = [f"segments.size == {len(r['segments'])}"]
        values = {}
        for i, seg in enumerate(r["segments"]):
            param = PARAM.fullmatch(seg)
            if not param:
                conds.append(f"segments[{i}] == \"{seg}\"")
            elif dict(r["params"])[param.group(1)] == "integer":
                conds.append(f"segments[{i}].toLongOrNull() != null")
                values[param.group(1)] = f"segments[{i}].toLong()"
            else:
                values[param.group(1)] = f"segments[{i}]"
        args = ", ".join(values[k] for k, _ in r["params"])
        value = f"{cap(r['name'])}({args})" if r["params"] else cap(r["name"])
        out.append(f"            if ({' && '.join(conds)}) return {value}")
    out += ["            return null", "        }", "",
            "        // パーセントエンコードを戻す。不正な値はそのまま使う（iOS の removingPercentEncoding と同じ）。",
            "        private fun decode(segment: String): String =",
            "            runCatching { URLDecoder.decode(segment.replace(\"+\", \"%2B\"), \"UTF-8\") }.getOrDefault(segment)",
            "    }", "}", "", "/** shared/routes.yaml のタブ。 */", "enum class AppTab(val root: Route, val title: String) {"]
    out += [f"    {cap(t['id'])}(Route.{cap(t['root'])}, \"{t['title']}\")," for t in model["tabs"]]
    out += ["}", ""]
    return "\n".join(out)
