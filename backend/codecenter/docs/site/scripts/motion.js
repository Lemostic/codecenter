/* ============================================================
 * motion.js — 动效工具层（基于 GSAP 3）
 *  - 暴露 window.SPEC_MOTION 工具集
 *  - 全部使用 spring / cubic-bezier(.16,1,.3,1) 缓动
 *  - 尊重 prefers-reduced-motion（自动降级为同步状态）
 *  - 主动清理 ScrollTrigger 实例（无内存泄漏）
 * ============================================================ */

(function () {
  const REDUCE_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const EASE_OUT = 'power3.out';
  const EASE_OUT_SOFT = 'power2.out';
  const EASE_IN_OUT = 'power2.inOut';
  const EASE_BACK = 'back.out(1.2)';

  /**
   * 滚动进度条：顶部 2px 横线
   * 挂在 #scrollProgress 上
   */
  function initScrollProgress() {
    const bar = document.getElementById('scrollProgress');
    if (!bar) return;
    if (REDUCE_MOTION) { bar.style.display = 'none'; return; }

    let raf = 0;
    function update() {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      const p = max > 0 ? Math.min(1, window.scrollY / max) : 0;
      bar.style.transform = `scaleX(${p})`;
      raf = 0;
    }
    window.addEventListener('scroll', () => {
      if (raf) return;
      raf = requestAnimationFrame(update);
    }, { passive: true });
    update();
  }

  /**
   * 导航高亮 scroll-spy：哪个 section 进入视口中部，nav-link 加 .active
   * 替代上一版纯 IntersectionObserver（避免多重触发）
   */
  function initScrollSpy() {
    if (REDUCE_MOTION) return;
    const links = Array.from(document.querySelectorAll('.nav-link'));
    if (!links.length) return;
    const map = new Map();
    links.forEach(a => {
      const id = a.getAttribute('href') || '';
      if (id.startsWith('#')) {
        const el = document.querySelector(id);
        if (el) map.set(el, a);
      }
    });
    if (!map.size) return;

    const sections = Array.from(map.keys());
    function onScroll() {
      const triggerY = window.innerHeight * 0.35;
      let active = null;
      for (const s of sections) {
        const r = s.getBoundingClientRect();
        if (r.top <= triggerY && r.bottom > triggerY) { active = s; break; }
      }
      if (!active && sections.length) {
        // 滚到顶部之前/之后兜底
        const first = sections[0];
        const last = sections[sections.length - 1];
        if (window.scrollY < first.offsetTop + 100) active = first;
        else if (window.scrollY + window.innerHeight >= last.offsetTop + last.offsetHeight) active = last;
      }
      links.forEach(a => a.classList.remove('active'));
      if (active && map.get(active)) map.get(active).classList.add('active');
    }
    let raf = 0;
    window.addEventListener('scroll', () => {
      if (raf) return;
      raf = requestAnimationFrame(() => { onScroll(); raf = 0; });
    }, { passive: true });
    onScroll();
  }

  /**
   * Hero h1 字符级 fade-in：把 h1 拆成字符，逐一延迟出现
   * 整段控制在 ~800ms 内；动画完成后把拆出的 span 节点拆掉，
   * 还原成原始 innerHTML（避免 "AI 自动遵循的..." 显示成裸 span 文本）
   */
  function initKineticH1() {
    if (REDUCE_MOTION) return;
    const h1 = document.querySelector('.hero h1');
    if (!h1 || !window.gsap) return;

    // 保存原始 innerHTML 用于还原
    const origHTML = h1.innerHTML;

    // 拆字（保留 <br>）
    const parts = origHTML.split(/(<br\s*\/?>)/gi);
    let out = '';
    let idx = 0;
    for (const p of parts) {
      if (/^<br/i.test(p)) { out += p; continue; }
      out += Array.from(p).map(ch => {
        if (/\s/.test(ch)) return ch;
        return `<span class="kin">${ch}</span>`;
      }).join('');
    }
    h1.innerHTML = out;

    gsap.fromTo(h1.querySelectorAll('.kin'),
      { y: '0.4em', opacity: 0, display: 'inline-block' },
      {
        y: 0, opacity: 1, duration: 0.7, ease: EASE_OUT,
        stagger: 0.022, delay: 0.15,
        onComplete: () => {
          // 字符淡入完成后：拆掉所有 .kin span 节点，把 h1 还原成原始结构
          // 这样页面在 DevTools / 选区 / 屏幕阅读器看到的是正常文本
          h1.innerHTML = origHTML;
        }
      });
  }

  /**
   * 元素入场：升级版 reveal
   *  - 用 GSAP 替代 CSS transition（更可控的 stagger 与 timing）
   *  - data-stagger 仍然生效
   */
  function initReveal() {
    if (REDUCE_MOTION) {
      // 直接全部 in
      document.querySelectorAll('.reveal').forEach(n => n.classList.add('in'));
      return;
    }
    if (!window.gsap || !window.ScrollTrigger) {
      // GSAP 没加载好，回退到原 CSS
      const io = new IntersectionObserver(entries => {
        entries.forEach(en => {
          if (en.isIntersecting) {
            const d = parseInt(en.target.dataset.stagger || '0', 10);
            setTimeout(() => en.target.classList.add('in'), d);
            io.unobserve(en.target);
          }
        });
      }, { threshold: 0.12 });
      document.querySelectorAll('.reveal').forEach(n => io.observe(n));
      return;
    }
    gsap.registerPlugin(ScrollTrigger);
    document.querySelectorAll('.reveal').forEach(n => {
      const delay = parseInt(n.dataset.stagger || '0', 10) / 1000;
      gsap.fromTo(n,
        { y: 28, opacity: 0 },
        {
          y: 0, opacity: 1, duration: 0.85, ease: EASE_OUT, delay,
          scrollTrigger: { trigger: n, start: 'top 88%', once: true }
        });
    });
  }

  /**
   * 引擎九阶段：进入视区后逐个点亮
   * 替代原 CSS setTimeout 链（更柔顺）
   */
  function initEngineFlow() {
    if (REDUCE_MOTION) {
      document.querySelectorAll('.stage').forEach(s => s.classList.add('lit'));
      return;
    }
    if (!window.gsap || !window.ScrollTrigger) {
      // 回退原 JS
      const wrap = document.getElementById('engineFlow');
      if (!wrap) return;
      const io = new IntersectionObserver(entries => {
        entries.forEach(en => {
          if (en.isIntersecting) {
            const nodes = wrap.querySelectorAll('.stage');
            nodes.forEach((n, i) => setTimeout(() => n.classList.add('lit'), i * 220));
            io.unobserve(en.target);
          }
        });
      }, { threshold: 0.2 });
      io.observe(wrap);
      return;
    }
    const wrap = document.getElementById('engineFlow');
    if (!wrap) return;
    const stages = wrap.querySelectorAll('.stage');
    ScrollTrigger.create({
      trigger: wrap, start: 'top 78%', once: true,
      onEnter: () => {
        gsap.to(stages, {
          opacity: 1, duration: 0.6, ease: EASE_OUT, stagger: 0.08
        });
        stages.forEach(s => s.classList.add('lit'));
      }
    });
  }

  /**
   * Stat 数字 count-up：用 GSAP tween 实现 ease-out-back
   * 增强"弹一下"的高级感
   */
  function countUp(node, target) {
    if (REDUCE_MOTION || !window.gsap) {
      node.textContent = String(target);
      return;
    }
    const obj = { v: 0 };
    gsap.to(obj, {
      v: target, duration: 1.1, ease: EASE_BACK,
      onUpdate: () => { node.textContent = String(Math.round(obj.v)); },
      onComplete: () => { node.textContent = String(target); }
    });
  }

  /**
   * 返回顶部按钮
   */
  function initBackToTop() {
    const btn = document.getElementById('backToTop');
    if (!btn) return;
    if (REDUCE_MOTION) { btn.style.display = 'none'; return; }

    let raf = 0;
    function update() {
      btn.classList.toggle('show', window.scrollY > window.innerHeight * 0.4);
      raf = 0;
    }
    window.addEventListener('scroll', () => {
      if (raf) return;
      raf = requestAnimationFrame(update);
    }, { passive: true });
    btn.addEventListener('click', () => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
    update();
  }

  /**
   * 鼠标光标 spotlight：把光标坐标写入元素 --mx/--my
   * 用于 .layer-card / .pkg-card / .stage / .gov-role / .btn 的 ::after 内描边亮起
   */
  function initSpotlight() {
    if (REDUCE_MOTION) return;
    const targets = document.querySelectorAll('.layer-card, .pkg-card, .stage.lit, .gov-role, .btn');
    targets.forEach(el => {
      el.addEventListener('mousemove', e => {
        const r = el.getBoundingClientRect();
        el.style.setProperty('--mx', `${e.clientX - r.left}px`);
        el.style.setProperty('--my', `${e.clientY - r.top}px`);
      });
    });
  }

  // 暴露
  window.SPEC_MOTION = {
    initScrollProgress,
    initScrollSpy,
    initKineticH1,
    initReveal,
    initEngineFlow,
    countUp,
    initBackToTop,
    initSpotlight
  };
})();
