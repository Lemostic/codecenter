/* ============================================================
 * main.js — 页面交互编排
 *   - 渲染数据驱动区块（三层模型、引擎九阶段、包全景、看板、模拟器、治理）
 *   - 指纹匹配模拟器（核心交互）
 *   - 终端打字 / 复制 manifest / 三级加载按钮
 *   - 动效全部委托给 motion.js（GSAP 驱动）
 * ============================================================ */

(function () {
  const D = window.SPEC_DATA;
  const R = window.SPEC_ROUTER;
  const M = window.SPEC_MOTION || {};
  const $ = (s, c) => (c || document).querySelector(s);
  const $$ = (s, c) => Array.from((c || document).querySelectorAll(s));

  /* ----------------------------------------
   * 一、导航锚点
   * ---------------------------------------- */
  function initNav() {
    const nav = $('#nav');
    let raf = 0;
    function onScroll() {
      nav.classList.toggle('scrolled', window.scrollY > 40);
      raf = 0;
    }
    window.addEventListener('scroll', () => {
      if (raf) return;
      raf = requestAnimationFrame(onScroll);
    }, { passive: true });
    $$('.nav-link').forEach(a => {
      a.addEventListener('click', e => {
        const href = a.getAttribute('href');
        if (href && href.startsWith('#')) {
          e.preventDefault();
          const t = $(href);
          if (t) {
            // 用 GSAP 软滚动到目标（统一手感）
            const top = t.getBoundingClientRect().top + window.scrollY - 56;
            window.scrollTo({ top, behavior: 'smooth' });
          }
        }
      });
    });
  }

  /* ----------------------------------------
   * 二、三级加载机制（点击逐级点亮）
   * ---------------------------------------- */
  function initBootFlow() {
    const levels = $$('.boot-level');
    const btn = $('#bootTrigger');
    if (!btn || !levels.length) return;
    function lightUp(to) {
      levels.forEach((lv, i) => lv.classList.toggle('lit', i <= to));
    }
    let step = -1;
    function next() {
      step++;
      if (step >= levels.length) {
        step = -1;
        levels.forEach(lv => lv.classList.remove('lit'));
        btn.textContent = '▶ 模拟链式加载';
        return;
      }
      lightUp(step);
      if (step === levels.length - 1) btn.textContent = '↻ 重置';
      setTimeout(next, 850);
    }
    btn.addEventListener('click', () => { step = -1; next(); });
  }

  /* ----------------------------------------
   * 三、渲染三层规范模型
   * ---------------------------------------- */
  function renderLayers() {
    const l0 = $('#l0List');
    if (l0) {
      l0.innerHTML = D.L0_SPECS.map(s =>
        `<li><span class="tag tag-l0">L0</span><span class="content"><span class="nm">${s.name}</span><span class="ds">${s.desc}</span></span></li>`
      ).join('');
    }
    const spi = $('#spiList');
    if (spi) {
      spi.innerHTML = D.L2_SPI.map(s =>
        `<li><span class="tag tag-l2">L2</span><span class="content"><span class="nm">${s.name}</span><span class="ds">${s.desc}</span></span></li>`
      ).join('');
    }
  }

  /* ----------------------------------------
   * 四、渲染引擎九阶段（bento 1+8 排版）
   *   第 1 阶段（读取项目清单）是入口，占左侧大格
   *   其余 8 个阶段填右侧 2×4
   * ---------------------------------------- */
  function renderEngine() {
    const wrap = $('#engineFlow');
    if (!wrap) return;
    const stages = D.ENGINE_STAGES;
    const lead = stages[0];
    const rest = stages.slice(1);
    wrap.innerHTML = `
      <div class="stage stage-lead reveal" data-no="${lead.no}">
        <div class="stage-no">${String(lead.no).padStart(2, '0')}</div>
        <h3 class="stage-title">${lead.title}</h3>
        <p class="stage-detail">${lead.detail}</p>
        <div class="stage-tagline">入口：从这一张清单开始，引擎才知道要加载什么</div>
      </div>
      <div class="stage-grid">
        ${rest.map(st => `
          <div class="stage reveal" data-no="${st.no}">
            <div class="stage-no">${String(st.no).padStart(2, '0')}</div>
            <h4 class="stage-title">${st.title}</h4>
            <p class="stage-detail">${st.detail}</p>
          </div>`).join('')}
      </div>
    `;
    if (M.initEngineFlow) M.initEngineFlow();
  }

  /* ----------------------------------------
   * 五、架构包全景（masonry 排版 + tab 切换）
   *   前 4 个包标记为 featured（大格）
   * ---------------------------------------- */
  function renderPackages() {
    const tabs = [
      { key: 'framework-base', label: '框架基座' },
      { key: 'architecture',   label: '架构风格' },
      { key: 'persistence',    label: '持久化' },
      { key: 'frontend',       label: '前端' }
    ];
    const bar = $('#pkgTabs');
    const body = $('#pkgBody');
    if (!bar || !body) return;

    bar.innerHTML = tabs.map((t, i) =>
      `<button class="pkg-tab${i === 0 ? ' active' : ''}" data-key="${t.key}">${t.label}</button>`
    ).join('');

    function paint(key) {
      const list = D.L1_PACKAGES.filter(p => p.type === key);
      // 前 4 个标记为 featured（视觉上更突出）
      body.innerHTML = list.map((p, i) => `
        <div class="pkg-card${i < 4 ? ' featured' : ''}">
          <div class="pkg-head">
            <span class="pkg-name">${p.name}</span>
            <span class="pkg-rules">${p.rules} <em>条规则</em></span>
          </div>
          <div class="pkg-id">${p.packageId}</div>
          <p class="pkg-desc">${p.desc}</p>
          <div class="pkg-specs">
            ${p.specs.map(s => `<span class="spec-chip">${s.name}</span>`).join('')}
          </div>
        </div>
      `).join('');
    }
    paint('framework-base');

    $$('.pkg-tab', bar).forEach(btn => {
      btn.addEventListener('click', () => {
        $$('.pkg-tab', bar).forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        paint(btn.dataset.key);
      });
    });
  }

  /* ----------------------------------------
   * 六、迭代管理（角色 1+3 + 校验 + 变更类型）
   *   Lead Maintainer 单独占大格（最重要）
   * ---------------------------------------- */
  function renderGovernance() {
    const roles = $('#govRoles');
    if (roles) {
      const lead = D.GOV_ROLES.find(r => r.code === 'Lead Maintainer');
      const rest = D.GOV_ROLES.filter(r => r.code !== 'Lead Maintainer');
      roles.innerHTML = `
        <div class="gov-role gov-role-lead">
          <div class="gov-role-badge">管理委员会主席</div>
          <div class="gov-role-name">${lead.name}</div>
          <div class="gov-role-code">${lead.code}</div>
          <p>${lead.desc}</p>
        </div>
        <div class="gov-role-stack">
          ${rest.map(r => `
            <div class="gov-role">
              <div class="gov-role-name">${r.name}</div>
              <div class="gov-role-code">${r.code}</div>
              <p>${r.desc}</p>
            </div>`).join('')}
        </div>
      `;
    }
    const checks = $('#govChecks');
    if (checks) {
      checks.innerHTML = D.GOV_CHECKS.map(c => `
        <div class="chk">
          <span class="chk-level ${c.level === 'ERROR' ? 'err' : 'warn'}">${c.level}</span>
          <span class="chk-cat">${c.cat}</span>
          <span class="chk-name">${c.name}</span>
        </div>
      `).join('');
    }
    const types = $('#govTypes');
    if (types) {
      types.innerHTML = D.GOV_CHANGE_TYPES.map(t => `
        <div class="ctype">
          <div class="ctype-tag ctype-${t.type}">${t.type}</div>
          <div class="ctype-body">
            <div><b>触发</b>${t.trigger}</div>
            <div><b>审批</b>${t.approval}</div>
            <div><b>执行</b>${t.executor}</div>
          </div>
        </div>
      `).join('');
    }
  }

  /* ----------------------------------------
   * 七、数据看板（数字滚动）
   *   stat 由 motion.js 负责 reveal
   * ---------------------------------------- */
  function renderStats() {
    const box = $('#stats');
    if (!box) return;
    box.innerHTML = D.STATS.map((s, i) => `
      <div class="stat reveal" data-delay="${i * 80}">
        <div class="stat-num"><span class="num" data-target="${s.num}">0</span><span class="unit">${s.unit}</span></div>
        <div class="stat-label">${s.label}</div>
      </div>
    `).join('');

    // 数字 countUp：进入视区后触发
    const io = new IntersectionObserver(entries => {
      entries.forEach(en => {
        if (en.isIntersecting) {
          const num = $('.num', en.target);
          const target = parseInt(num.dataset.target, 10);
          M.countUp(num, target);
          io.unobserve(en.target);
        }
      });
    }, { threshold: 0.5 });
    $$('.stat', box).forEach(n => io.observe(n));
  }

  /* ========================================
   * 八、★ 指纹匹配模拟器（核心）
   * ======================================== */
  function initSimulator() {
    const selBox = $('#simSelect');
    const spiBox = $('#simSpi');
    const presetBox = $('#simPresets');
    const outSpecs = $('#simSpecs');
    const outSummary = $('#simSummary');
    const outManifest = $('#simManifest');
    const outConflicts = $('#simConflicts');
    if (!selBox) return;

    const state = { selected: new Set(), spi: new Set() };

    const selectable = D.L1_PACKAGES.filter(p => p.selectable);
    const groups = {};
    selectable.forEach(p => { (groups[p.type] = groups[p.type] || []).push(p); });
    const groupLabel = {
      architecture: '架构风格（二选一）',
      persistence: '持久化技术（可多选）',
      frontend: '前端框架'
    };

    selBox.innerHTML = Object.keys(groups).map(type => `
      <div class="sim-group">
        <div class="sim-group-title">${groupLabel[type] || type}</div>
        <div class="sim-chips" data-type="${type}">
          ${groups[type].map(p => `
            <button class="chip" data-id="${p.packageId}" data-type="${type}">
              <span class="chip-name">${p.name}</span>
              <span class="chip-rules">${p.rules}条</span>
            </button>
          `).join('')}
        </div>
      </div>
    `).join('');

    spiBox.innerHTML = D.L2_SPI.map(s => `
      <button class="chip chip-spi" data-domain="${s.domain}">
        <span class="chip-name">${s.name}</span>
        <span class="chip-rules">L2 插件</span>
      </button>
    `).join('');

    presetBox.innerHTML = `<div class="sim-group-title">组合预设（一键填入）</div>` +
      D.PRESETS.map(p => `<button class="preset" data-id="${p.id}">${p.name}</button>`).join('') +
      `<button class="preset preset-reset">↻ 清空</button>`;

    function toggleChip(id, type, forceState) {
      const pkg = D.L1_PACKAGES.find(p => p.packageId === id);
      const willSelect = forceState != null ? forceState : !state.selected.has(id);

      if (willSelect) {
        if (type === 'architecture') {
          groups[type].forEach(p => {
            if (p.packageId !== id) state.selected.delete(p.packageId);
          });
        }
        state.selected.add(id);
      } else {
        state.selected.delete(id);
      }
      render();
    }

    selBox.addEventListener('click', e => {
      const c = e.target.closest('.chip');
      if (c) toggleChip(c.dataset.id, c.dataset.type);
    });
    spiBox.addEventListener('click', e => {
      const c = e.target.closest('.chip-spi');
      if (!c) return;
      const d = c.dataset.domain;
      if (state.spi.has(d)) state.spi.delete(d); else state.spi.add(d);
      render();
    });
    presetBox.addEventListener('click', e => {
      const btn = e.target.closest('.preset');
      if (!btn) return;
      if (btn.classList.contains('preset-reset')) {
        state.selected.clear(); state.spi.clear(); render(); return;
      }
      const ids = R.presetToSelection(btn.dataset.id);
      state.selected.clear(); state.spi.clear();
      ids.forEach(id => state.selected.add(id));
      render();
    });

    function render() {
      $$('.chip', selBox).forEach(c => {
        const id = c.dataset.id;
        const pkg = D.L1_PACKAGES.find(p => p.packageId === id);
        const isSel = state.selected.has(id);
        let blocked = false;
        if (!isSel && pkg.conflicts) {
          blocked = pkg.conflicts.some(x => state.selected.has(x));
        }
        c.classList.toggle('on', isSel);
        c.classList.toggle('blocked', blocked);
        c.disabled = blocked;
      });
      $$('.chip-spi', spiBox).forEach(c => {
        c.classList.toggle('on', state.spi.has(c.dataset.domain));
      });

      const result = R.resolve([...state.selected], [...state.spi]);

      const ok = result.conflicts.length === 0;
      // 概要：1 大（份规范） + 2 小（条规则 / 自动引入）
      outSummary.innerHTML = `
        <div class="sum-line ${ok ? 'ok' : 'warn'}">
          <span class="sum-dot"></span>
          ${ok ? '校验通过：当前组合可被引擎加载' : `检测到 ${result.conflicts.length} 个互斥冲突，CI 会阻止加载`}
        </div>
        <div class="sum-nums sum-nums-bento">
          <div class="sum-num sum-num-lead"><b>${result.specCount}</b><span>份规范</span></div>
          <div class="sum-num"><b>${result.ruleCount}</b><span>条架构规则</span></div>
          <div class="sum-num"><b>${result.autoIntroduced.length}</b><span>个自动引入包</span></div>
        </div>`;

      outConflicts.innerHTML = result.conflicts.length
        ? result.conflicts.map(c => `<div class="conflict">⚠ 互斥：<code>${c.a}</code> ⟷ <code>${c.b}</code><p>${c.reason}</p></div>`).join('')
        : '';

      const byLevel = { L0: [], L1: [], L2: [] };
      result.specList.forEach(s => byLevel[s.level].push(s));
      outSpecs.innerHTML = ['L0', 'L1', 'L2'].map(lv => {
        if (!byLevel[lv].length) return '';
        return `
          <div class="lv-group">
            <div class="lv-head"><span class="tag tag-${lv.toLowerCase()}">${lv}</span> ${levelTitle(lv)}</div>
            <div class="lv-items">
              ${byLevel[lv].map(s => `
                <div class="lv-item${s.auto ? ' auto' : ''}">
                  <span class="lv-name">${s.name}</span>
                  ${s.auto ? '<span class="lv-flag">自动加载</span>' : ''}
                  ${s.auto && s.pkg ? `<span class="lv-from">← ${s.pkg}</span>` : ''}
                </div>`).join('')}
            </div>
          </div>`;
      }).join('');

      outManifest.textContent = result.manifestYaml;
    }

    function levelTitle(lv) {
      return { L0: '通用规范（始终加载）', L1: '架构配套方案', L2: '领域扩展插件' }[lv];
    }

    R.presetToSelection('ddd-mybatis-plus-redis').forEach(id => state.selected.add(id));
    render();
  }

  /* ----------------------------------------
   * 九、模拟终端（接入流程）
   * ---------------------------------------- */
  function initTerminal() {
    const body = $('#termBody');
    if (!body) return;
    const lines = [
      { text: '# 第 1 步：在你的项目里引入 docs 目录（作为子模块）', type: 'note' },
      { text: 'git submodule add <standards-repo-url> docs', type: 'cmd' },
      { text: '✓ docs/ 已就绪，包含规范引擎与全部规范库', type: 'ok', pause: 500 },

      { text: '# 第 2 步：运行引导脚本（自动探测技术栈、生成清单）', type: 'note' },
      { text: 'bash docs/governance/scripts/bootstrap.sh', type: 'cmd' },
      { text: '[探测] 发现 pom.xml → spring-boot-starter-web', type: 'out' },
      { text: '[探测] 发现 mybatis-plus → persistence-mybatis-plus', type: 'out' },
      { text: '[探测] 目录命中 adapter/ domain/ → 推断 DDD 四层架构', type: 'out' },
      { text: '[生成] .spec/spec-manifest.yaml ✔', type: 'ok' },
      { text: '[生成] .spec/project-inventory.yaml ✔  .spec/glossary.yaml ✔', type: 'ok' },
      { text: '[生成] CLAUDE.md ✔  已指向 docs/skill/SKILL.md 引擎', type: 'ok', pause: 600 },

      { text: '# 第 3 步：像往常一样和 AI 对话', type: 'note' },
      { text: '你: 帮我创建一个订单（Order）的核心业务对象', type: 'cmd' },
      { text: 'AI: [加载 ddd-domain-model] 生成聚合根 + 实体 + 仓储接口，遵循团队规范 ✔', type: 'ok', pause: 800 }
    ];

    let played = false;
    const io = new IntersectionObserver(entries => {
      entries.forEach(en => {
        if (en.isIntersecting && !played) {
          played = true;
          window.SPEC_TERMINAL.playTerminal(body, lines);
          io.unobserve(en.target);
        }
      });
    }, { threshold: 0.35 });
    io.observe(body);
  }

  /* ----------------------------------------
   * 十、复制 manifest 按钮（SVG check-mark 翻转）
   * ---------------------------------------- */
  function initCopy() {
    const btn = $('#copyManifest');
    const code = $('#simManifest');
    if (!btn || !code) return;
    const label = btn.querySelector('.copy-label');
    btn.addEventListener('click', () => {
      const txt = code.textContent;
      const flash = () => {
        btn.classList.add('copied');
        if (label) label.textContent = '已复制';
        setTimeout(() => {
          btn.classList.remove('copied');
          if (label) label.textContent = '复制';
        }, 1800);
      };
      if (navigator.clipboard) {
        navigator.clipboard.writeText(txt).then(flash, flash);
      } else {
        flash();
      }
    });
  }

  /* ----------------------------------------
   * 启动
   * ---------------------------------------- */
  document.addEventListener('DOMContentLoaded', () => {
    initNav();
    initBootFlow();
    renderLayers();
    renderEngine();
    renderPackages();
    renderGovernance();
    renderStats();
    initSimulator();
    initTerminal();
    initCopy();

    // 动效层（在数据渲染完之后跑）
    if (M.initScrollProgress) M.initScrollProgress();
    if (M.initScrollSpy) M.initScrollSpy();
    if (M.initReveal) M.initReveal();
    if (M.initKineticH1) M.initKineticH1();
    if (M.initBackToTop) M.initBackToTop();
    if (M.initSpotlight) M.initSpotlight();
  });
})();
