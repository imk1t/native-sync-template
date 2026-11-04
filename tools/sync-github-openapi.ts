/** 明示的な上流更新。bun tools/sync-github-openapi.ts <40桁のコミットSHA> */
import { writeFileSync } from "node:fs";
import { CONTRACT, githubContract, REPOSITORY, SELECTION, UPSTREAM_PATH, type Selection } from "./lib/github-openapi";
import { join, readText, ROOT } from "./lib/util";

const commit = process.argv[2];
if (!commit || !/^[a-f0-9]{40}$/.test(commit)) throw new Error("公式定義の固定コミット SHA（40桁）を指定してください");
const base = `https://raw.githubusercontent.com/${REPOSITORY}/${commit}`;
const response = await fetch(`${base}/${UPSTREAM_PATH}`);
if (!response.ok) throw new Error(`公式定義の取得に失敗: ${response.status}`);
const raw = await response.text();
const selection = JSON.parse(readText(join(ROOT, SELECTION))) as Selection;
const contract = githubContract(raw, selection, commit);
const licenseResponse = await fetch(`${base}/LICENSE.md`);
if (!licenseResponse.ok) throw new Error(`ライセンスの取得に失敗: ${licenseResponse.status}`);
const license = await licenseResponse.text();
writeFileSync(join(ROOT, CONTRACT), contract);
writeFileSync(join(ROOT, "shared/openapi/GITHUB-LICENSE.md"), license);
console.log(`GitHub OpenAPI を ${commit} に更新しました。bun tools/generate.ts で再生成してください。`);
