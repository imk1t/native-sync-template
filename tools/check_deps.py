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

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODULES = ROOT / "modules"


def ios_graph():
    """modules/Package.swift の target 行から {モジュール: {依存モジュール}} を作る。"""
    text = (MODULES / "Package.swift").read_text(encoding="utf-8")
    target_to_mod, deps = {}, {}
    rows = re.findall(r"\.target\(name: \"(\w+)\"(?:, dependencies: \[([^\]]*)\])?, path: \"([^\"]+)\"\)", text)
    for name, _, path in rows:
        target_to_mod[name] = path.split("/ios/")[0]
    for name, d, _ in rows:
        deps[target_to_mod[name]] = {target_to_mod[x] for x in re.findall(r"\"(\w+)\"", d or "")}
    return deps


def android_graph():
    deps = {}
    for f in MODULES.rglob("android/build.gradle.kts"):
        mod = str(f.parent.parent.relative_to(MODULES))
        found = re.findall(r"project\(\"(:[\w:]+)\"\)", f.read_text(encoding="utf-8"))
        deps[mod] = {p.strip(":").replace(":", "/") for p in found}
    return deps


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
    graphs = {"ios": ios_graph(), "android": android_graph()}
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
