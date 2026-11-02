#!/usr/bin/env python3
"""shared/scenarios の共通シナリオを検査する。

- 形式: name（ファイル名と一致）・feature・steps（id・name・given・when・then）
- given のキーは shared/openapi の operationId。2xx の body はレスポンスの schema に合うこと
- feature の spec.md がシナリオを参照していること
- 各 step が iOS と Android の両方のテストで `@scenario <name>/<step id>` として参照されていること
"""
import pathlib
import re
import sys

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import openapi_codegen  # noqa: E402

MARKER = re.compile(r"@scenario\s+([\w-]+)/([\w-]+)")
STEP_ID = re.compile(r"[a-z0-9]+(?:-[a-z0-9]+)*")
TEST_GLOBS = {"ios": ("ios/Tests/**/*.swift",), "android": ("android/src/test/**/*.kt", "android/src/androidTest/**/*.kt")}


def markers(module_dir, platform):
    found = {}
    for pattern in TEST_GLOBS[platform]:
        for path in sorted(module_dir.glob(pattern)):
            for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                for scenario, step in MARKER.findall(line):
                    found.setdefault((scenario, step), f"{path.relative_to(ROOT)}:{lineno}")
    return found


def check_given(spec, given, where):
    ops = {op["id"]: op for op in spec.operations}
    if not isinstance(given, dict) or not given:
        return [f"{where}.given: operationId をキーにした辞書が必要です"]
    errors = []
    for op_id, stub in given.items():
        w = f"{where}.given.{op_id}"
        if op_id not in ops:
            errors.append(f"{w}: shared/openapi に無い operationId です")
            continue
        if not isinstance(stub, dict):
            errors.append(f"{w}: status か pending が必要です")
            continue
        unknown = set(stub) - {"status", "body", "pending"}
        if unknown:
            errors.append(f"{w}: 未知のキーです: {sorted(unknown)}")
        if stub.get("pending") is True:
            if set(stub) != {"pending"}:
                errors.append(f"{w}: pending: true は単独で書いてください")
            continue
        status = stub.get("status")
        if not isinstance(status, int) or isinstance(status, bool) or not 100 <= status <= 599:
            errors.append(f"{w}.status: HTTP ステータスが必要です")
            continue
        returns = ops[op_id]["returns"]
        if 200 <= status < 300 and returns:
            if "body" not in stub:
                errors.append(f"{w}.body: 2xx のレスポンス body が必要です")
                continue
            try:
                openapi_codegen.validate_value(spec, returns, stub["body"], f"{w}.body")
            except ValueError as e:
                errors.append(str(e))
        elif "body" in stub:
            errors.append(f"{w}.body: この status では body を検査できません。エラー body は書かないでください")
    return errors


def check_file(path, spec):
    errors = []
    rel = path.relative_to(ROOT)
    try:
        doc = yaml.safe_load(path.read_text(encoding="utf-8"))
    except yaml.YAMLError as e:
        return [f"{rel}: YAML として読めません: {e}"], None
    if not isinstance(doc, dict):
        return [f"{rel}: 辞書が必要です"], None
    if doc.get("name") != path.stem:
        errors.append(f"{rel}: name はファイル名（{path.stem}）と同じにしてください")
    feature = doc.get("feature")
    module_dir = ROOT / "modules" / str(feature)
    if not isinstance(feature, str) or not (module_dir / "spec.md").is_file():
        errors.append(f"{rel}: feature は spec.md のあるモジュールのパスにしてください: {feature!r}")
        module_dir = None
    elif f"shared/scenarios/{path.name}" not in (module_dir / "spec.md").read_text(encoding="utf-8"):
        errors.append(f"modules/{feature}/spec.md: shared/scenarios/{path.name} を参照してください")
    steps = doc.get("steps")
    if not isinstance(steps, list) or not steps:
        errors.append(f"{rel}: steps が必要です")
        return errors, None
    ids = []
    for index, step in enumerate(steps):
        where = f"{rel}: steps[{index}]"
        if not isinstance(step, dict):
            errors.append(f"{where}: 辞書が必要です")
            continue
        missing = {"id", "name", "given", "when", "then"} - step.keys()
        if missing:
            errors.append(f"{where}: {sorted(missing)} が必要です")
        step_id = step.get("id")
        if not isinstance(step_id, str) or not STEP_ID.fullmatch(step_id):
            errors.append(f"{where}.id: 小文字と数字のケバブケースにしてください: {step_id!r}")
        elif step_id in ids:
            errors.append(f"{where}.id: 重複しています: {step_id}")
        else:
            ids.append(step_id)
        if "given" in step:
            errors += check_given(spec, step["given"], where)
        if "then" in step and not isinstance(step["then"], dict):
            errors.append(f"{where}.then: 辞書が必要です")
    return errors, (doc.get("name"), module_dir, ids)


def main():
    spec = openapi_codegen.Spec(yaml.safe_load((ROOT / "shared/openapi/openapi.yaml").read_text(encoding="utf-8")))
    errors = []
    scenarios = {}
    for path in sorted((ROOT / "shared/scenarios").glob("*.yaml")):
        file_errors, info = check_file(path, spec)
        errors += file_errors
        if info:
            scenarios[info[0]] = info
    for name, module_dir, ids in scenarios.values():
        if module_dir is None:
            continue
        for platform in ("ios", "android"):
            found = markers(module_dir, platform)
            for step_id in ids:
                if (name, step_id) not in found:
                    errors.append(f"{module_dir.relative_to(ROOT)}/{platform}: `@scenario {name}/{step_id}` を参照するテストがありません")
    known = {(name, step_id) for name, _, ids in scenarios.values() for step_id in ids}
    for spec_md in sorted((ROOT / "modules").glob("**/spec.md")):
        for ref in re.findall(r"shared/scenarios/([\w-]+\.yaml)", spec_md.read_text(encoding="utf-8")):
            if not (ROOT / "shared/scenarios" / ref).is_file():
                errors.append(f"{spec_md.relative_to(ROOT)}: 存在しないシナリオを参照しています: {ref}")
        for platform in ("ios", "android"):
            for (scenario, step_id), location in markers(spec_md.parent, platform).items():
                if (scenario, step_id) not in known:
                    errors.append(f"{location}: 存在しないシナリオ step を参照しています: {scenario}/{step_id}")
    for e in errors:
        print(e)
    if errors:
        return 1
    print(f"OK: {sum(len(ids) for _, _, ids in scenarios.values())} steps in {len(scenarios)} scenarios")
    return 0


if __name__ == "__main__":
    sys.exit(main())
