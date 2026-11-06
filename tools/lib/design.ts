/** DESIGN.md の front matter からデザイントークンを読む。色の暗い値は `colorsDark` に書く（DESIGN.md 仕様にモードはないため独自セクション）。 */
import { compare, type Dict, isDict } from "./util";

export interface Tokens {
  /** 名前 → [ライトの値, 暗いときの値]（`#RRGGBB`）。名前順。 */
  colors: [string, string, string][];
  rounded: [string, number][];
  spacing: [string, number][];
}

const COLOR = /^#[0-9A-Fa-f]{6}$/;
const DIMENSION = /^(\d+(?:\.\d+)?)(?:px|dp)?$/;

export function frontMatter(text: string): Dict {
  const match = /^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/.exec(text);
  if (!match) throw new Error("DESIGN.md の先頭に YAML front matter（--- で囲む）がありません");
  const value = Bun.YAML.parse(match[1] as string);
  if (!isDict(value)) throw new Error("DESIGN.md の front matter はマッピングにしてください");
  return value;
}

function section(doc: Dict, name: string): Dict {
  const value = doc[name];
  if (!isDict(value) || Object.keys(value).length === 0) {
    throw new Error(`DESIGN.md の ${name} が空か、マッピングではありません`);
  }
  return value;
}

function dimensions(doc: Dict, name: string): [string, number][] {
  return Object.entries(section(doc, name))
    .map(([key, raw]): [string, number] => {
      const match = DIMENSION.exec(String(raw));
      if (!match) throw new Error(`${name}.${key}: "${String(raw)}" は 8 / 8px / 8dp の形で書いてください`);
      return [key, Number(match[1])];
    })
    .sort(([a], [b]) => compare(a, b));
}

export function parse(text: string): Tokens {
  const doc = frontMatter(text);
  const light = section(doc, "colors");
  const dark = section(doc, "colorsDark");
  const colors = Object.entries(light)
    .map(([key, value]): [string, string, string] => {
      const darkValue = dark[key];
      if (darkValue === undefined) throw new Error(`colors.${key} に対応する colorsDark.${key} がありません`);
      for (const [where, v] of [
        [`colors.${key}`, value],
        [`colorsDark.${key}`, darkValue],
      ] as const) {
        if (!COLOR.test(String(v))) throw new Error(`${where}: "${String(v)}" は #RRGGBB で書いてください`);
      }
      return [key, String(value), String(darkValue)];
    })
    .sort(([a], [b]) => compare(a, b));
  for (const key of Object.keys(dark)) {
    if (!(key in light)) throw new Error(`colorsDark.${key} に対応する colors.${key} がありません`);
  }
  return { colors, rounded: dimensions(doc, "rounded"), spacing: dimensions(doc, "spacing") };
}
