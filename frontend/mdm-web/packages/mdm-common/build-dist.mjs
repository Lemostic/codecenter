// 临时构建脚本：用 esbuild 把 src 中的 .ts 编译到 dist/，保留目录结构。
// 替代 pnpm build（vue-tsc --noEmit），因为 dev 模式需要真实的 dist/。
// TODO: 后续在 build 脚本里集成此步骤。

import { build } from 'esbuild';
import { glob } from 'node:fs/promises';

const entries = [];
for await (const f of glob('src/**/*.ts')) entries.push(f);

await build({
  entryPoints: entries,
  outdir: 'dist',
  format: 'esm',
  platform: 'node',
  logLevel: 'info',
});
