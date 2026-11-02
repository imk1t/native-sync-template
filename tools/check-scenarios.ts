/**
 * shared/scenarios の共通シナリオを検査する。
 *
 * - 形式: name（ファイル名と一致）・feature・steps（id・name・given・when・then）
 * - given のキーは shared/openapi の operationId。2xx の body はレスポンスの schema に合うこと
 *   （`navigation` だけは画面遷移の初期状態として予約。辞書で書く）
 * - feature の spec.md がシナリオを参照していること
 * - 各 step が iOS と Android の両方のテストで `@scenario <name>/<step id>` として参照されていること
 */
import { basename, relative } from "node:path";
import { Spec, validateValue } from "./lib/openapi";
import { type Dict, glob, isDict, isFile, join, readText, readYaml, ROOT, sorted } from "./lib/util";

const MARKER = /@scenario\s+([\w-]+)\/([\w-]+)/g;
const STEP_ID = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const NAVIGATION = "navigation";
const TEST_GLOBS = {
  ios: ["ios/Tests/**/*.swift"],
  android: ["android/src/test/**/*.kt", "android/src/androidTest/**/*.kt"],
} as const;
const PLATFORMS = ["ios", "android"] as const;
type Platform = (typeof PLATFORMS)[number];

type Scenario = { name: string; moduleDir: string | null; ids: string[] };

/** モジュールのテストにある `@scenario` を `<シナリオ>/<step>` → 最初の場所で返す。 */
function markers(root: string, moduleDir: string, platform: Platform): Map<string, string> {
  const found = new Map<string, string>();
  for (const pattern of TEST_GLOBS[platform]) {
    for (const path of glob(moduleDir, pattern)) {
      const full = join(moduleDir, path);
      readText(full)
        .split(/\r?\n/)
        .forEach((line, index) => {
          for (const [, scenario, step] of line.matchAll(MARKER)) {
            const key = `${scenario}/${step}`;
            if (!found.has(key)) found.set(key, `${relative(root, full)}:${index + 1}`);
          }
        });
    }
  }
  return found;
}

function checkGiven(spec: Spec, given: unknown, where: string): string[] {
  const ops = new Map(spec.operations.map((op) => [op.id, op]));
  if (!isDict(given) || Object.keys(given).length === 0) {
    return [`${where}.given: operationId をキーにした辞書が必要です`];
  }
  const errors: string[] = [];
  for (const [opId, stub] of Object.entries(given)) {
    const w = `${where}.given.${opId}`;
    if (opId === NAVIGATION) {
      if (!isDict(stub)) errors.push(`${w}: 画面遷移の初期状態を辞書で書いてください`);
      continue;
    }
    const op = ops.get(opId);
    if (!op) {
      errors.push(`${w}: shared/openapi に無い operationId です`);
      continue;
    }
    if (!isDict(stub)) {
      errors.push(`${w}: status か pending が必要です`);
      continue;
    }
    const unknown = sorted(Object.keys(stub).filter((key) => !["status", "body", "pending"].includes(key)));
    if (unknown.length > 0) errors.push(`${w}: 未知のキーです: ${JSON.stringify(unknown)}`);
    if (stub.pending === true) {
      if (Object.keys(stub).join() !== "pending") errors.push(`${w}: pending: true は単独で書いてください`);
      continue;
    }
    const status = stub.status;
    if (typeof status !== "number" || !Number.isInteger(status) || status < 100 || status > 599) {
      errors.push(`${w}.status: HTTP ステータスが必要です`);
      continue;
    }
    if (status >= 200 && status < 300 && op.returns) {
      if (!("body" in stub)) {
        errors.push(`${w}.body: 2xx のレスポンス body が必要です`);
        continue;
      }
      try {
        validateValue(spec, op.returns, stub.body, `${w}.body`);
      } catch (error) {
        errors.push(error instanceof Error ? error.message : String(error));
      }
    } else if ("body" in stub) {
      errors.push(`${w}.body: この status では body を検査できません。エラー body は書かないでください`);
    }
  }
  return errors;
}

function checkFile(root: string, path: string, spec: Spec): [string[], Scenario | null] {
  const errors: string[] = [];
  const rel = relative(root, path);
  const stem = basename(path, ".yaml");
  let doc: unknown;
  try {
    doc = readYaml(path);
  } catch (error) {
    return [[`${rel}: YAML として読めません: ${error instanceof Error ? error.message : error}`], null];
  }
  if (!isDict(doc)) return [[`${rel}: 辞書が必要です`], null];
  if (doc.name !== stem) errors.push(`${rel}: name はファイル名（${stem}）と同じにしてください`);
  const feature = doc.feature;
  let moduleDir: string | null = typeof feature === "string" ? join(root, "modules", feature) : null;
  if (moduleDir === null || !isFile(join(moduleDir, "spec.md"))) {
    errors.push(`${rel}: feature は spec.md のあるモジュールのパスにしてください: ${JSON.stringify(feature ?? null)}`);
    moduleDir = null;
  } else if (!readText(join(moduleDir, "spec.md")).includes(`shared/scenarios/${basename(path)}`)) {
    errors.push(`modules/${feature}/spec.md: shared/scenarios/${basename(path)} を参照してください`);
  }
  const steps = doc.steps;
  if (!Array.isArray(steps) || steps.length === 0) {
    errors.push(`${rel}: steps が必要です`);
    return [errors, null];
  }
  const ids: string[] = [];
  steps.forEach((step: unknown, index) => {
    const where = `${rel}: steps[${index}]`;
    if (!isDict(step)) {
      errors.push(`${where}: 辞書が必要です`);
      return;
    }
    const missing = sorted(["id", "name", "given", "when", "then"].filter((key) => !(key in step)));
    if (missing.length > 0) errors.push(`${where}: ${JSON.stringify(missing)} が必要です`);
    const stepId = step.id;
    if (typeof stepId !== "string" || !STEP_ID.test(stepId)) {
      errors.push(`${where}.id: 小文字と数字のケバブケースにしてください: ${JSON.stringify(stepId ?? null)}`);
    } else if (ids.includes(stepId)) {
      errors.push(`${where}.id: 重複しています: ${stepId}`);
    } else {
      ids.push(stepId);
    }
    if ("given" in step) errors.push(...checkGiven(spec, step.given, where));
    if ("then" in step && !isDict(step.then)) errors.push(`${where}.then: 辞書が必要です`);
  });
  return [errors, { name: String(doc.name), moduleDir, ids }];
}

export function main(root: string = ROOT, log: (line: string) => void = console.log): number {
  const api = readYaml(join(root, "shared/openapi/openapi.yaml"));
  const spec = new Spec(isDict(api) ? api : ({} as Dict));
  const errors: string[] = [];
  const scenarios = new Map<string, Scenario>();
  const dir = join(root, "shared/scenarios");
  for (const path of glob(dir, "*.yaml", { dot: true })) {
    const [fileErrors, info] = checkFile(root, join(dir, path), spec);
    errors.push(...fileErrors);
    if (info) scenarios.set(info.name, info);
  }
  for (const { name, moduleDir, ids } of scenarios.values()) {
    if (moduleDir === null) continue;
    for (const platform of PLATFORMS) {
      const found = markers(root, moduleDir, platform);
      for (const stepId of ids) {
        if (!found.has(`${name}/${stepId}`)) {
          errors.push(
            `${relative(root, moduleDir)}/${platform}: \`@scenario ${name}/${stepId}\` を参照するテストがありません`,
          );
        }
      }
    }
  }
  const known = new Set([...scenarios.values()].flatMap(({ name, ids }) => ids.map((id) => `${name}/${id}`)));
  const modules = join(root, "modules");
  for (const path of glob(modules, "**/spec.md")) {
    const specMd = join(modules, path);
    for (const [, ref] of readText(specMd).matchAll(/shared\/scenarios\/([\w-]+\.yaml)/g)) {
      if (!isFile(join(root, "shared/scenarios", ref as string))) {
        errors.push(`${relative(root, specMd)}: 存在しないシナリオを参照しています: ${ref}`);
      }
    }
    const moduleDir = join(specMd, "..");
    for (const platform of PLATFORMS) {
      for (const [key, location] of markers(root, moduleDir, platform)) {
        if (!known.has(key)) errors.push(`${location}: 存在しないシナリオ step を参照しています: ${key}`);
      }
    }
  }
  for (const e of errors) log(e);
  if (errors.length > 0) return 1;
  const steps = [...scenarios.values()].reduce((sum, s) => sum + s.ids.length, 0);
  log(`OK: ${steps} steps in ${scenarios.size} scenarios`);
  return 0;
}

if (import.meta.main) process.exit(main());
