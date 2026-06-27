const fs = require('fs');
const path = require('path');
const dir = __dirname;
const root = path.dirname(dir);
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const js = fs.readFileSync(path.join(dir, 'main.js'), 'utf8');

// 1. DOM id 引用检查
const re = /\$\(['"]#([\w-]+)/g;
let m, set = new Set();
while ((m = re.exec(js))) set.add(m[1]);
const ids = [...set];
let miss = 0;
ids.forEach(id => {
  if (!html.includes('id="' + id + '"')) { miss++; console.log('  MISS ' + id); }
});
if (miss === 0) console.log('OK: 全部 ' + ids.length + ' 个 id 引用正确');

// 2. data-stagger 引用检查
const staggerEls = (html.match(/data-stagger/g) || []).length;
console.log('OK: HTML 中有 ' + staggerEls + ' 处 data-stagger 错峰入场');

// 3. router 逻辑回归
global.window = {};
require('./data.js');
require('./router.js');
const R = global.window.SPEC_ROUTER;
const r1 = R.resolve(['arch-ddd', 'persistence-mybatis-plus', 'persistence-redis'], []);
console.log(r1.specCount === 16 && r1.ruleCount === 146 && r1.conflicts.length === 0
  ? 'OK: DDD+MBP+Redis → 16 specs / 146 rules / 0 conflicts'
  : 'FAIL: router regression');
const r2 = R.resolve(['arch-mvc', 'arch-ddd'], []);
console.log(r2.conflicts.length === 1 ? 'OK: arch-mvc+arch-ddd → 1 conflict' : 'FAIL: conflict detection');

// 4. GSAP vendor 存在性
const gsapExists = fs.existsSync(path.join(root, 'vendor/gsap/gsap.min.js'));
const stExists   = fs.existsSync(path.join(root, 'vendor/gsap/ScrollTrigger.min.js'));
if (gsapExists && stExists) {
  console.log('OK: GSAP vendor 本地加载就绪 (gsap.min.js + ScrollTrigger.min.js)');
} else {
  console.log('FAIL: vendor 缺失', { gsapExists, stExists });
}

// 5. 新增动效入口节点（用 id 形式而非 #id 形式）
const must = ['id="scrollProgress"', 'id="backToTop"', 'class="grain"', 'vendor/gsap/gsap.min.js', 'vendor/gsap/ScrollTrigger.min.js', 'scripts/motion.js'];
const missing = must.filter(s => !html.includes(s));
if (missing.length === 0) console.log('OK: 5 个新增节点全部引入 (progress / backToTop / grain / vendor×2 / motion.js)');
else console.log('FAIL: 缺失', missing);

// 6. em-dash 检查（hero / footer / body copy）
const emDash = html.match(/—/g) || [];
console.log(emDash.length === 0 ? 'OK: 全站无 em-dash' : 'WARN: 检测到 ' + emDash.length + ' 处 em-dash');

