/** 明示的な上流更新。bun tools/sync-github-openapi.ts <40桁のコミットSHA> */
import { writeFileSync } from "node:fs";
import { digest, extract, META, project, SELECTION, SOURCE, type Selection } from "./lib/github-openapi";
import { join, readText, ROOT } from "./lib/util";

const commit = process.argv[2];
if (!commit || !/^[a-f0-9]{40}$/.test(commit)) throw new Error("公式定義の固定コミット SHA（40桁）を指定してください");
const path = "descriptions/api.github.com/api.github.com.2026-03-10.json";
const base = `https://raw.githubusercontent.com/github/rest-api-description/${commit}`;
const response = await fetch(`${base}/${path}`);
if (!response.ok) throw new Error(`公式定義の取得に失敗: ${response.status}`);
const raw = await response.text();
const selection = JSON.parse(readText(join(ROOT, SELECTION))) as Selection;
const snapshot = extract(JSON.parse(raw), selection);
project(snapshot, selection); // 対応範囲を検証してから保存する
const text = `${JSON.stringify(snapshot, null, 2)}\n`;
const licenseResponse = await fetch(`${base}/LICENSE.md`);
if (!licenseResponse.ok) throw new Error(`ライセンスの取得に失敗: ${licenseResponse.status}`);
const license = await licenseResponse.text();
writeFileSync(join(ROOT, SOURCE), text);
writeFileSync(
  join(ROOT, META),
  `${JSON.stringify({ repository: "github/rest-api-description", commit, path, apiVersion: "2026-03-10", sha256: digest(raw), snapshotSha256: digest(text) }, null, 2)}\n`,
);
writeFileSync(join(ROOT, "shared/openapi/GITHUB-LICENSE.md"), license);
console.log(`GitHub OpenAPI を ${commit} に更新しました。bun tools/generate.ts で再生成してください。`);
