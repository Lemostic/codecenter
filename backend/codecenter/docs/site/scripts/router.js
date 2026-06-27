/* ============================================================
 * router.js — 指纹匹配模拟器核心引擎
 * 复现 routing-rules / composition-presets 的真实路由逻辑：
 *   1. 勾选的架构包 → 收集 specs
 *   2. 解析隐含依赖（implies）→ 自动引入框架基座
 *   3. 检测互斥冲突（conflicts）→ 输出告警
 *   4. 累加 L0（始终加载）+ L1 + L2 → 规范总数与规则总数
 *   5. 生成 spec-manifest YAML 片段
 * ============================================================ */

(function () {
  const D = window.SPEC_DATA;

  /**
   * 给定一组用户勾选的包 ID（不含隐含依赖），计算完整加载结果。
   * @param {string[]} selected 用户主动勾选的 L1 包 ID（不含框架基座）
   * @param {string[]} spiDomains 用户勾选的 L2 领域 ID
   * @returns {{
   *   selected: string[],
   *   resolved: object[],        // 最终生效的 L1 包对象（含自动引入的基座）
   *   autoIntroduced: string[],  // 自动引入的包 ID
   *   conflicts: object[],       // 冲突信息 {a,b,reason}
   *   specList: object[],        // 所有规范（L0 + L1 + L2）汇总
   *   specCount: number,
   *   ruleCount: number,
   *   manifestYaml: string
   * }}
   */
  function resolve(selected, spiDomains) {
    const selectedSet = new Set(selected);

    // 1. 解析隐含依赖：收集所有被选中包 implies 的包
    const autoIntroduced = new Set();
    const queue = [...selected];
    while (queue.length) {
      const pid = queue.shift();
      const pkg = D.L1_PACKAGES.find(p => p.packageId === pid);
      if (pkg && pkg.implies) {
        pkg.implies.forEach(dep => {
          if (!selectedSet.has(dep) && !autoIntroduced.has(dep)) {
            autoIntroduced.add(dep);
            queue.push(dep); // 基座也可能 implies 别的（链式）
          }
        });
      }
    }

    // 2. 合并：用户选择 + 自动引入
    const allIds = new Set([...selectedSet, ...autoIntroduced]);
    const resolved = D.L1_PACKAGES.filter(p => allIds.has(p.packageId));

    // 3. 检测互斥冲突
    const conflicts = [];
    const idArr = [...allIds];
    D.CONFLICTS.forEach(c => {
      if (allIds.has(c.a) && allIds.has(c.b)) {
        conflicts.push(c);
      }
    });

    // 4. 汇总规范清单（L0 始终全部加载）
    const specList = [];
    // L0
    D.L0_SPECS.forEach(s => specList.push({ ...s, level: 'L0', auto: true }));
    // L1
    resolved.forEach(pkg => {
      pkg.specs.forEach(s => specList.push({
        ...s,
        level: 'L1',
        pkg: pkg.name,
        auto: autoIntroduced.has(pkg.packageId)
      }));
    });
    // L2
    (spiDomains || []).forEach(domain => {
      const spi = D.L2_SPI.find(s => s.domain === domain);
      if (spi) specList.push({ id: spi.specId, name: spi.name, level: 'L2', auto: false });
    });

    // 5. 计数
    const specCount = specList.length;
    const ruleCount =
      D.L0_SPECS.length * 0 + // L0 规则数未单列，这里不计入（仅统计 L1/L2）
      resolved.reduce((sum, p) => sum + (p.rules || 0), 0);

    // 6. 生成 manifest YAML
    const manifestYaml = buildManifest([...selectedSet], [...spiDomains || []], conflicts);

    return {
      selected: [...selectedSet],
      resolved,
      autoIntroduced: [...autoIntroduced],
      conflicts,
      specList,
      specCount,
      ruleCount,
      manifestYaml
    };
  }

  /** 生成 spec-manifest.yaml 片段 */
  function buildManifest(profiles, domains, conflicts) {
    const now = '2026-06-17';
    const lines = [];
    lines.push('# .spec/spec-manifest.yaml');
    lines.push('version: "2.0"');
    lines.push('');
    lines.push('project:');
    lines.push('  name: "your-project"');
    lines.push('');
    lines.push('# 架构指纹 —— 引擎据此匹配规范');
    lines.push('fingerprint:');
    lines.push('  profiles:');
    profiles.forEach(p => lines.push('    - ' + p));
    if (domains && domains.length) {
      lines.push('  domains:');
      domains.forEach(d => lines.push('    - ' + d));
    } else {
      lines.push('  domains: []');
    }
    lines.push('  http_mode: "A"   # A=完整 HTTP 方法；B=只用 GET/POST');
    lines.push('');
    if (conflicts.length) {
      lines.push('# ⚠️ 存在互斥冲突，CI 校验会阻止加载，请修正：');
      conflicts.forEach(c => {
        lines.push('#   ' + c.a + '  ⟷  ' + c.b);
      });
      lines.push('');
    }
    lines.push('protection:');
    lines.push('  protected_globs:');
    lines.push('    - "**/*Test.java"');
    lines.push('    - "**/common/utils/**"');
    return lines.join('\n');
  }

  /** 给定一个预设，返回它对应的包选择集（用于预设按钮） */
  function presetToSelection(presetId) {
    const p = D.PRESETS.find(x => x.id === presetId);
    return p ? p.profiles.slice() : [];
  }

  window.SPEC_ROUTER = { resolve, presetToSelection };
})();
