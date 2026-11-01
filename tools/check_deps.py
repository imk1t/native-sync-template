#!/usr/bin/env python3
"""モジュール間の依存ルールを検査する。

  core/utils      -> 何にも依存しない
  core/*          -> 他の core/* のみ（循環禁止）
  designsystem    -> core/utils のみ
  features/*      -> core/*, designsystem（features 同士は禁止）
"""
import pathlib
import re
import sys
import tomllib
import yaml
from module_manifest import android_modules, ios_targets

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODULES = ROOT / "modules"


def ios_dependencies():
    targets = ios_targets(ROOT)
    deps, external = {}, set()
    token = re.compile(
        r'\s*(?:"(?P<plain>[^"]+)"|\.target\(\s*name:\s*"(?P<target>[^"]+)"\s*\)'
        r'|\.product\(\s*name:\s*"[^"]+"\s*,\s*package:\s*"(?P<package>[^"]+)"\s*\))\s*(?:,|$)'
    )
    for _, (mod, raw) in targets.items():
        if mod in deps:
            raise ValueError(f"1モジュールに複数の iOS target があります: {mod}")
        deps[mod] = set()
        while raw.strip():
            match = token.match(raw)
            if not match:
                raise ValueError(f"{mod}: 未対応の iOS 依存構文: {raw}")
            if match["package"]:
                external.add((mod, match["package"].lower()))
            else:
                name = match["plain"] or match["target"]
                if name not in targets:
                    raise ValueError(f"{mod}: 不明な内部 target {name}。外部依存は .product(name: ..., package: ...) を使ってください")
                deps[mod].add(targets[name][0])
            raw = raw[match.end():]
    return deps, external


def android_dependencies():
    registered = android_modules(ROOT)
    deps, external = {}, set()
    catalog = tomllib.loads((ROOT / "apps/android/gradle/libs.versions.toml").read_text(encoding="utf-8"))["libraries"]
    actual = {str(f.parent.parent.relative_to(MODULES)) for f in MODULES.rglob("android/build.gradle.kts")}
    if actual != set(registered.values()):
        raise ValueError(f"Android の登録と build.gradle.kts が一致しません: registered={sorted(registered.values())}, actual={sorted(actual)}")
    for mod in registered.values():
        text = (MODULES / mod / "android/build.gradle.kts").read_text(encoding="utf-8")
        text = re.sub(r"//[^\n]*|/\*.*?\*/", "", text, flags=re.S)
        found = re.findall(r'project\(\s*(?:path\s*=\s*)?"(:[\w:]+)"\s*\)', text)
        if text.count("project(") != len(found):
            raise ValueError(f"{mod}: 未対応の project 依存構文")
        unknown = set(found) - registered.keys()
        if unknown:
            raise ValueError(f"{mod}: 不明な Android project: {sorted(unknown)}")
        deps[mod] = {registered[p] for p in found}
        if re.search(r'(?:api|implementation|compileOnly|runtimeOnly)\s*\(\s*(?:platform\(\s*)?["\']', text):
            raise ValueError(f"{mod}: 外部依存は libs.versions.toml に定義してください")
        aliases = re.findall(r'(?:api|implementation|compileOnly|runtimeOnly)\s*\(\s*(?:platform\(\s*)?libs\.([\w.]+)', text)
        for accessor in aliases:
            alias = accessor.replace(".", "-")
            if alias not in catalog:
                raise ValueError(f"{mod}: 不明な version catalog alias: {alias}")
            coordinate = catalog[alias]["module"]
            # OS・言語の公式ライブラリは UI / coroutine のために各層で利用できる。
            if mod != "core/utils" and coordinate.startswith(("androidx.", "org.jetbrains.kotlin:", "org.jetbrains.kotlinx:")):
                continue
            external.add((mod, alias))
    return deps, external


def check_external(used):
    document = yaml.safe_load((ROOT / "shared/third-party.yaml").read_text(encoding="utf-8"))
    if not isinstance(document, dict) or not isinstance(document.get("libraries"), list):
        raise ValueError("third-party.yaml の libraries は配列にしてください")
    declared = {"ios": set(), "android": set()}
    for entry in document["libraries"]:
        if not isinstance(entry, dict) or not all(isinstance(entry.get(key), str) and entry[key] for key in ("role", "module", "owner")):
            raise ValueError("外部ライブラリには role・module・owner を指定してください")
        for plat in declared:
            library = entry.get(plat)
            if not isinstance(library, dict) or not all(isinstance(library.get(key), str) and library[key] for key in ("name", "license")):
                raise ValueError(f"{entry['module']}: {plat} の name・license が必要です")
            name = library["name"].lower() if plat == "ios" else library["name"]
            declared[plat].add((entry["module"], name))
    errors = []
    for plat, dependencies in used.items():
        for mod, name in sorted(dependencies):
            if not mod.startswith("core/") or mod == "core/utils":
                errors.append(f"{plat}: {mod} は外部 SDK {name} に直接依存できません")
        for mod, name in sorted(dependencies - declared[plat]):
            errors.append(f"{plat}: {mod} の {name} が third-party.yaml に未登録です")
        for mod, name in sorted(declared[plat] - dependencies):
            errors.append(f"{plat}: third-party.yaml の {mod} / {name} が実際の依存にありません")
    return errors


def allowed(src, dst):
    if src == dst:
        return False
    if src == "core/utils":
        return False
    if src.startswith("core/"):
        return dst.startswith("core/")
    if src == "designsystem":
        return dst == "core/utils"
    if src.startswith("features/"):
        return dst.startswith("core/") or dst == "designsystem"
    return False


def has_cycle(g):
    state = {}

    def visit(n):
        if state.get(n) == 1:
            return True
        if state.get(n) == 2:
            return False
        state[n] = 1
        if any(visit(m) for m in g.get(n, ())):
            return True
        state[n] = 2
        return False

    return any(visit(n) for n in list(g))


def main():
    errors = []
    try:
        ios, ios_external = ios_dependencies()
        android, android_external = android_dependencies()
        errors.extend(check_external({"ios": ios_external, "android": android_external}))
    except (ValueError, OSError, KeyError, TypeError, yaml.YAMLError) as error:
        print(f"ERROR {error}")
        return 1
    graphs = {"ios": ios, "android": android}
    for plat, g in graphs.items():
        for src, dsts in sorted(g.items()):
            for dst in sorted(dsts):
                if not allowed(src, dst):
                    errors.append(f"{plat}: {src} は {dst} に依存できません")
        if has_cycle(g):
            errors.append(f"{plat}: モジュール間に循環依存があります")
    if graphs["ios"] != graphs["android"]:
        errors.append(f"iOS と Android の依存関係が一致しません: ios={graphs['ios']} android={graphs['android']}")
    for e in errors:
        print("ERROR", e)
    print("deps: OK" if not errors else f"deps: {len(errors)} problem(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
