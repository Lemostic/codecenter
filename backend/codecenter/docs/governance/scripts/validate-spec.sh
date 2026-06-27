#!/usr/bin/env bash
# ============================================================
# validate-spec.sh — 规范体系自动校验脚本
# Enterprise Coding Standards Validation Script
# ============================================================
# 用法:
#   bash governance/scripts/validate-spec.sh [--full] [--ci]
#
# 选项:
#   --full   全量扫描（含定时巡检项目）
#   --ci     CI 模式（输出 GitHub Actions 格式注解）
#   (无参数)  增量校验（仅检查最近变更文件）
#
# 退出码:
#   0 = 全部通过
#   1 = 存在 ERROR 级校验失败
#   2 = 仅 WARN 级警告
# ============================================================

set -euo pipefail

# ============================================================
# 全局变量
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GOVERNANCE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCS_DIR="$(cd "$GOVERNANCE_DIR/.." && pwd)"

MODE="${1:-incremental}"
CI_MODE=false
FULL_SCAN=false
ERRORS=0
WARNINGS=0
CHECKS_PASSED=0
CHECKS_TOTAL=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================
# 参数解析
# ============================================================
for arg in "$@"; do
  case "$arg" in
    --full)  FULL_SCAN=true ;;
    --ci)    CI_MODE=true ;;
    --help|-h)
      echo "用法: bash validate-spec.sh [--full] [--ci]"
      echo ""
      echo "选项:"
      echo "  --full   全量扫描（含定时巡检项目）"
      echo "  --ci     CI 模式（输出 GitHub Actions 格式注解）"
      echo "  --help   显示帮助信息"
      exit 0
      ;;
  esac
done

# ============================================================
# 工具函数
# ============================================================

log_pass() {
  CHECKS_PASSED=$((CHECKS_PASSED + 1))
  CHECKS_TOTAL=$((CHECKS_TOTAL + 1))
  if $CI_MODE; then
    echo "::notice::$1"
  else
    echo -e "  ${GREEN}✓${NC} $1"
  fi
}

log_fail() {
  ERRORS=$((ERRORS + 1))
  CHECKS_TOTAL=$((CHECKS_TOTAL + 1))
  if $CI_MODE; then
    echo "::error::$1"
  else
    echo -e "  ${RED}✗${NC} $1"
  fi
}

log_warn() {
  WARNINGS=$((WARNINGS + 1))
  CHECKS_TOTAL=$((CHECKS_TOTAL + 1))
  if $CI_MODE; then
    echo "::warning::$1"
  else
    echo -e "  ${YELLOW}⚠${NC} $1"
  fi
}

log_section() {
  if $CI_MODE; then
    echo "::group::$1"
  else
    echo ""
    echo -e "${BLUE}━━━ $1 ━━━${NC}"
  fi
}

log_section_end() {
  if $CI_MODE; then
    echo "::endgroup::"
  fi
}

# 检查命令是否可用
require_cmd() {
  if ! command -v "$1" &>/dev/null; then
    echo -e "${RED}错误: 需要安装 '$1' 但未找到${NC}"
    echo "请先安装: $2"
    exit 1
  fi
}

# ============================================================
# 前置检查
# ============================================================
echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║   规范体系自动校验 (validate-spec.sh)        ║"
echo "║   Enterprise Coding Standards Validator      ║"
echo "╚══════════════════════════════════════════════╝"
echo ""
echo "  模式: $(if $FULL_SCAN; then echo '全量扫描'; else echo '增量校验'; fi)"
echo "  目录: $DOCS_DIR"
echo ""

require_cmd python3 "Python 3.8+ (https://python.org)"

# 导出路径为环境变量，避免中文路径在 shell→Python 传递时编码丢失
export DOCS_DIR GOVERNANCE_DIR

# 检查 python yaml 库
if ! python3 -c "import yaml" 2>/dev/null; then
  echo -e "${YELLOW}提示: 正在安装 PyYAML 依赖...${NC}"
  pip3 install pyyaml --quiet 2>/dev/null || pip install pyyaml --quiet
fi

# ============================================================
# V-002: YAML 语法检查
# ============================================================
check_yaml_syntax() {
  log_section "V-002: YAML 语法检查"

  local yaml_errors
  yaml_errors=$(python3 -c "
import yaml, glob, sys, os
errors = []
for f in glob.glob(os.environ['DOCS_DIR'] + '/**/*.yaml', recursive=True):
    try:
        with open(f, encoding='utf-8') as fh:
            yaml.safe_load(fh)
    except Exception as e:
        errors.append(f'{f}: {e}')
if errors:
    for e in errors:
        print(e)
    sys.exit(1)
" 2>&1) || true

  if [ -z "$yaml_errors" ]; then
    log_pass "所有 YAML 文件语法合法"
  else
    while IFS= read -r line; do
      log_fail "YAML 语法错误: $line"
    done <<< "$yaml_errors"
  fi

  log_section_end
}

# ============================================================
# V-003: 冲突标记检查
# ============================================================
check_conflict_markers() {
  log_section "V-003: 冲突标记检查"

  local markers
  markers=$(grep -rn -e '^<<<<<<<' -e '^=======$' -e '^>>>>>>>' "$DOCS_DIR" \
    --include="*.md" --include="*.yaml" 2>/dev/null || true)

  if [ -z "$markers" ]; then
    log_pass "无 Git 冲突标记"
  else
    while IFS= read -r line; do
      log_fail "发现冲突标记: $line"
    done <<< "$markers"
  fi

  log_section_end
}

# ============================================================
# V-101: registry 路径完整性
# ============================================================
check_registry_paths() {
  log_section "V-101: registry 路径完整性"

  local path_errors
  path_errors=$(python3 -c "
import yaml, os, sys
reg_path = os.environ['GOVERNANCE_DIR'] + '/registry.yaml'
if not os.path.exists(reg_path):
    print('registry.yaml 不存在')
    sys.exit(1)
reg = yaml.safe_load(open(reg_path, encoding='utf-8'))
errors = []
total = 0
for mod in reg.get('modules', []):
    total += 1
    fp = mod.get('file_path', '')
    full = os.path.join(os.environ['DOCS_DIR'], fp)
    if not os.path.exists(full):
        errors.append(f\"{mod['module_id']}: {fp} 不存在\")
if errors:
    for e in errors:
        print(e)
    sys.exit(1)
print(f'OK:{total}')
" 2>&1) || true

  if [[ "$path_errors" == OK:* ]]; then
    local count="${path_errors#OK:}"
    log_pass "registry 路径完整 ($count 个模块路径均有效)"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "路径缺失: $line"
    done <<< "$path_errors"
  fi

  log_section_end
}

# ============================================================
# V-102: Profile 模块完整性
# ============================================================
check_profile_modules() {
  log_section "V-102: Profile 模块完整性"

  local profile_errors
  profile_errors=$(python3 -c "
import yaml, os, sys, glob
errors = []
profiles_found = 0
for pf in glob.glob(os.environ['DOCS_DIR'] + '/profiles/**/_profile.yaml', recursive=True):
    profiles_found += 1
    profile = yaml.safe_load(open(pf, encoding='utf-8'))
    profile_dir = os.path.dirname(pf)
    for mod in profile.get('modules', []):
        md_path = os.path.join(profile_dir, f'{mod}.md')
        if not os.path.exists(md_path):
            errors.append(f'{pf}: module \"{mod}\" 缺少 {mod}.md')
if errors:
    for e in errors:
        print(e)
    sys.exit(1)
print(f'OK:{profiles_found}')
" 2>&1) || true

  if [[ "$profile_errors" == OK:* ]]; then
    local count="${profile_errors#OK:}"
    log_pass "Profile 模块完整 ($count 个 Profile 检查通过)"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "Profile 模块缺失: $line"
    done <<< "$profile_errors"
  fi

  log_section_end
}

# ============================================================
# V-103: 路由规则完整性
# ============================================================
check_routing_paths() {
  log_section "V-103: 路由规则完整性"

  local routing_file="$DOCS_DIR/skill/routing-rules.yaml"
  if [ ! -f "$routing_file" ]; then
    log_warn "routing-rules.yaml 不存在，跳过路由完整性检查"
    log_section_end
    return
  fi

  local routing_errors
  routing_errors=$(python3 -c "
import yaml, os, sys
routing = yaml.safe_load(open(os.environ['DOCS_DIR'] + '/skill/routing-rules.yaml', encoding='utf-8'))
errors = []
total = 0

# 检查 spec_registry 中的路径
for entry in routing.get('spec_registry', []):
    total += 1
    spec_path = entry.get('spec_path', '')
    if spec_path:
        full = os.path.join(os.environ['DOCS_DIR'], spec_path)
        if not os.path.exists(full):
            errors.append(f\"spec_registry: {entry.get('spec_id','?')} -> {spec_path} 不存在\")

if errors:
    for e in errors:
        print(e)
    sys.exit(1)
print(f'OK:{total}')
" 2>&1) || true

  if [[ "$routing_errors" == OK:* ]]; then
    local count="${routing_errors#OK:}"
    log_pass "路由规则完整 ($count 条路由均有效)"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "路由缺失: $line"
    done <<< "$routing_errors"
  fi

  log_section_end
}

# ============================================================
# V-104: 规则 ID 唯一性
# ============================================================
check_rule_id_uniqueness() {
  log_section "V-104: 规则 ID 唯一性"

  local id_errors
  id_errors=$(python3 -c "
import re, glob, sys, os
from collections import defaultdict
# 只匹配规则【定义】位置，忽略交叉引用和变更日志提及
# 格式1: ## 规范 UNI-AD-001: ... (heading 定义)
heading_def = re.compile(r'^## .*?(PROF-[A-Z]+-\d+|UNI-[A-Z]+-\d+|DG-\d+)', re.MULTILINE)
# 格式2: **PROF-DDD-005** ... (bold rule 定义，DDD profiles 风格)
bold_def = re.compile(r'^\*\*(PROF-[A-Z]+-\d+|UNI-[A-Z]+-\d+|DG-\d+)\*\*', re.MULTILINE)
id_map = defaultdict(list)
docs_dir = os.environ['DOCS_DIR']
for base in ['universal', 'profiles', 'spi']:
    for f in glob.glob(docs_dir + '/' + base + '/**/*.md', recursive=True):
        if any(skip in f for skip in ['EXTRACTION-REPORT', '_spi-guide', '_template', '_legacy']):
            continue
        content = open(f, encoding='utf-8').read()
        rel = os.path.relpath(f, docs_dir).replace(os.sep, '/')
        found = set()
        for pat in [heading_def, bold_def]:
            for m in pat.finditer(content):
                found.add(m.group(1))
        for rid in found:
            id_map[rid].append(rel)
dups = {k: list(set(v)) for k, v in id_map.items() if len(set(v)) > 1}
if dups:
    for rid, files in dups.items():
        print(f'{rid} 定义在: {files}')
    sys.exit(1)
print(f'OK:{len(id_map)}')
" 2>&1) || true

  if [[ "$id_errors" == OK:* ]]; then
    local count="${id_errors#OK:}"
    log_pass "规则 ID 唯一 ($count 个唯一 ID)"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "重复规则 ID: $line"
    done <<< "$id_errors"
  fi

  log_section_end
}

# ============================================================
# V-105: 双向引用一致性
# ============================================================
check_cross_reference() {
  log_section "V-105: 双向引用一致性"

  local routing_file="$DOCS_DIR/skill/routing-rules.yaml"
  if [ ! -f "$routing_file" ]; then
    log_warn "routing-rules.yaml 不存在，跳过双向引用检查"
    log_section_end
    return
  fi

  local xref_errors
  xref_errors=$(python3 -c "
import yaml, sys, os
routing = yaml.safe_load(open(os.environ['DOCS_DIR'] + '/skill/routing-rules.yaml', encoding='utf-8'))
errors = []

# 收集 package_registry 中引用的 spec_id (v2.0 格式)
package_spec_ids = set()
for pkg in routing.get('package_registry', []):
    for spec in pkg.get('specs', []):
        sid = spec.get('spec_id', '')
        if sid:
            package_spec_ids.add(sid)

# 兼容旧版 profile_rules (v1.0)
for rule in routing.get('profile_rules', []):
    for spec in rule.get('specs', []):
        sid = spec.get('spec_id', '')
        if sid:
            package_spec_ids.add(sid)

# 收集 spec_registry 中的 spec_id
registry_spec_ids = set()
for entry in routing.get('spec_registry', []):
    sid = entry.get('spec_id', '')
    if sid:
        registry_spec_ids.add(sid)

# 收集 universal_specs 中的 spec_id
universal_spec_ids = set()
for entry in routing.get('universal_specs', []):
    sid = entry.get('spec_id', '')
    if sid:
        universal_spec_ids.add(sid)

# 收集 spi 注册条目
spi_spec_ids = set()
for entry in routing.get('spec_registry', []):
    sid = entry.get('spec_id', '')
    if sid and sid.startswith('spi-'):
        spi_spec_ids.add(sid)

# 检查双向一致性
effective_package = package_spec_ids | universal_spec_ids | spi_spec_ids
only_in_package = effective_package - registry_spec_ids
only_in_registry = registry_spec_ids - effective_package

for sid in only_in_package:
    errors.append(f'{sid}: 仅在 package_registry 中存在，spec_registry 中缺失')
for sid in only_in_registry:
    errors.append(f'{sid}: 仅在 spec_registry 中存在，package_registry 中缺失')

if errors:
    for e in errors:
        print(e)
    sys.exit(1)
print(f'OK:{len(registry_spec_ids)}')
" 2>&1) || true

  if [[ "$xref_errors" == OK:* ]]; then
    local count="${xref_errors#OK:}"
    log_pass "双向引用一致 ($count 条 spec_id 双向匹配)"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "引用不一致: $line"
    done <<< "$xref_errors"
  fi

  log_section_end
}

# ============================================================
# V-201: 版本号合规（SemVer）
# ============================================================
check_semver() {
  log_section "V-201: 版本号合规"

  local ver_errors
  ver_errors=$(python3 -c "
import yaml, re, sys, os
reg = yaml.safe_load(open(os.environ['GOVERNANCE_DIR'] + '/registry.yaml', encoding='utf-8'))
pattern = re.compile(r'^\d+\.\d+(\.\d+)?$')
errors = []
for mod in reg.get('modules', []):
    v = str(mod.get('version', ''))
    if not pattern.match(v):
        errors.append(f\"{mod['module_id']}: 版本号 '{v}' 不符合 SemVer\")
if errors:
    for e in errors:
        print(e)
    sys.exit(1)
print(f'OK:{len(reg.get(\"modules\", []))}')
" 2>&1) || true

  if [[ "$ver_errors" == OK:* ]]; then
    local count="${ver_errors#OK:}"
    log_pass "版本号合规 ($count 个模块版本号格式正确)"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "版本号问题: $line"
    done <<< "$ver_errors"
  fi

  log_section_end
}

# ============================================================
# V-202: 依赖完整性
# ============================================================
check_dependencies() {
  log_section "V-202: 依赖完整性"

  local dep_errors
  dep_errors=$(python3 -c "
import yaml, sys, os
reg = yaml.safe_load(open(os.environ['GOVERNANCE_DIR'] + '/registry.yaml', encoding='utf-8'))
module_ids = {m['module_id'] for m in reg.get('modules', []) if m.get('status') == 'active'}
errors = []
for mod in reg.get('modules', []):
    for dep in mod.get('depends_on', []):
        if dep not in module_ids:
            errors.append(f\"{mod['module_id']}: 依赖 '{dep}' 不存在或非 active\")
if errors:
    for e in errors:
        print(e)
    sys.exit(1)
print('OK')
" 2>&1) || true

  if [[ "$dep_errors" == "OK" ]]; then
    log_pass "依赖关系完整 (所有 depends_on 引用均有效)"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "依赖悬空: $line"
    done <<< "$dep_errors"
  fi

  log_section_end
}

# ============================================================
# V-301: 受保护文件检查（仅 PR 模式）
# ============================================================
check_protected_files() {
  log_section "V-301: 受保护文件检查"

  # 获取受保护文件列表
  local protected_list
  protected_list=$(python3 -c "
import yaml, os
perms = yaml.safe_load(open(os.environ['GOVERNANCE_DIR'] + '/permissions.yaml', encoding='utf-8'))
for f in perms.get('protected_files', []):
    print(f['path'])
" 2>/dev/null) || true

  if [ -z "$protected_list" ]; then
    log_warn "无法读取受保护文件列表，跳过检查"
    log_section_end
    return
  fi

  # 检查 Git diff 中是否有受保护文件被修改
  if git -C "$DOCS_DIR" rev-parse --git-dir &>/dev/null; then
    local base_branch="${BASE_BRANCH:-main}"
    local changed_files
    changed_files=$(git -C "$DOCS_DIR" diff --name-only "origin/$base_branch" HEAD 2>/dev/null || true)

    local violations=0
    while IFS= read -r protected; do
      [ -z "$protected" ] && continue
      local full_path="docs/$protected"
      if echo "$changed_files" | grep -qF "$full_path" 2>/dev/null; then
        log_fail "受保护文件被直接修改: $protected (请通过提案系统变更)"
        violations=$((violations + 1))
      fi
    done <<< "$protected_list"

    if [ "$violations" -eq 0 ]; then
      log_pass "受保护文件未被直接修改"
    fi
  else
    log_warn "非 Git 仓库，跳过 PR diff 检查"
  fi

  log_section_end
}

# ============================================================
# V-302: 占位符完整性
# ============================================================
check_placeholders() {
  log_section "V-302: 占位符完整性"

  local ph_warnings
  ph_warnings=$(python3 -c "
import re, glob, os
pattern = re.compile(r'\{\{[a-z_]+\}\}')
total = 0
for f in glob.glob(os.environ['DOCS_DIR'] + '/**/*.md', recursive=True):
    content = open(f, encoding='utf-8').read()
    found = pattern.findall(content)
    total += len(found)
print(f'OK:{total}')
" 2>&1) || true

  if [[ "$ph_warnings" == OK:* ]]; then
    local count="${ph_warnings#OK:}"
    log_pass "占位符完整 (检测到 $count 个占位符)"
  else
    log_warn "占位符检查异常: $ph_warnings"
  fi

  log_section_end
}

# ============================================================
# V-303: 权限校验（提案角色与变更类型匹配）
# ============================================================
check_role_permissions() {
  log_section "V-303: 权限校验"

  local perm_errors
  perm_errors=$(python3 -c "
import yaml, glob, os, sys

perms_path = os.environ['GOVERNANCE_DIR'] + '/permissions.yaml'
if not os.path.exists(perms_path):
    print('permissions.yaml 不存在')
    sys.exit(1)

perms = yaml.safe_load(open(perms_path, encoding='utf-8'))
operations = perms.get('operations', {})

# 扫描所有提案文件，检查 type 与角色是否匹配
errors = []
proposal_files = glob.glob(os.environ['GOVERNANCE_DIR'] + '/proposals/**/*.proposal.yaml', recursive=True)
for pf in proposal_files:
    try:
        prop = yaml.safe_load(open(pf, encoding='utf-8'))
        proposal = prop.get('proposal', {})
        change_type = proposal.get('type', '')
        author_role = proposal.get('author_role', '')

        # 检查该角色是否有权提交该类型的提案
        op_key = f'submit_proposal_{change_type}'
        if op_key in operations:
            allowed = operations[op_key].get('allowed_roles', [])
            if author_role and author_role not in allowed:
                rel = pf.replace(os.environ['GOVERNANCE_DIR'] + '/', '')
                errors.append(f'{rel}: 角色 \"{author_role}\" 无权提交 {change_type} 级提案')
    except Exception as e:
        errors.append(f'{pf}: 解析失败 — {e}')

if errors:
    for e in errors:
        print(e)
    sys.exit(1)
print(f'OK:{len(proposal_files)}')
" 2>&1) || true

  if [[ "$perm_errors" == OK:* ]]; then
    local count="${perm_errors#OK:}"
    log_pass "权限校验通过 ($count 个提案角色权限合规)"
  elif [[ "$perm_errors" == *"不存在"* ]]; then
    log_warn "permissions.yaml 不存在，跳过权限校验"
  else
    while IFS= read -r line; do
      [ -n "$line" ] && log_fail "权限违规: $line"
    done <<< "$perm_errors"
  fi

  log_section_end
}

# ============================================================
# V-001: 提案文件存在性（仅 PR 模式）
# ============================================================
check_proposal_exists() {
  log_section "V-001: 提案文件存在性"

  if git -C "$DOCS_DIR" rev-parse --git-dir &>/dev/null; then
    local base_branch="${BASE_BRANCH:-main}"
    local proposals
    proposals=$(git -C "$DOCS_DIR" diff --name-only "origin/$base_branch" HEAD 2>/dev/null | \
      grep "governance/proposals/.*\.proposal\.yaml" || true)

    if [ -n "$proposals" ]; then
      log_pass "提案文件存在: $(echo "$proposals" | wc -l | tr -d ' ') 个提案"
    else
      log_fail "PR 必须包含至少一个 proposal.yaml 文件 (参考 governance/proposals/_template.yaml)"
    fi
  else
    log_warn "非 Git 仓库，跳过提案存在性检查"
  fi

  log_section_end
}

# ============================================================
# V-203: CHANGELOG 条目检查
# ============================================================
check_changelog() {
  log_section "V-203: CHANGELOG 条目"

  local changelog="$GOVERNANCE_DIR/CHANGELOG.md"
  if [ -f "$changelog" ]; then
    local entries
    entries=$(grep -c '^\## \[' "$changelog" 2>/dev/null || echo "0")
    if [ "$entries" -gt 0 ]; then
      log_pass "CHANGELOG 存在 ($entries 个版本条目)"
    else
      log_warn "CHANGELOG.md 存在但无版本条目"
    fi
  else
    log_warn "CHANGELOG.md 不存在"
  fi

  log_section_end
}

# ============================================================
# 全量巡检附加项
# ============================================================
full_health_check() {
  log_section "全量巡检: 体系健康度"

  # 统计模块状态
  python3 -c "
import yaml, os
reg = yaml.safe_load(open(os.environ['GOVERNANCE_DIR'] + '/registry.yaml', encoding='utf-8'))
modules = reg.get('modules', [])
active = sum(1 for m in modules if m.get('status') == 'active')
inactive = sum(1 for m in modules if m.get('status') == 'inactive')
deprecated = sum(1 for m in modules if m.get('status') == 'deprecated')
print(f'  模块统计: {len(modules)} 总计 | {active} active | {inactive} inactive | {deprecated} deprecated')
print(f'  体系版本: {reg.get(\"system\", {}).get(\"system_version\", \"unknown\")}')
print(f'  治理版本: {reg.get(\"system\", {}).get(\"governance_version\", \"unknown\")}')
" 2>/dev/null || echo "  无法读取 registry.yaml"

  # 检查孤立文件（存在于 docs/ 但未在 registry 注册）
  local orphan_files
  orphan_files=$(python3 -c "
import yaml, glob, os
docs = os.environ['DOCS_DIR']
reg = yaml.safe_load(open(os.environ['GOVERNANCE_DIR'] + '/registry.yaml', encoding='utf-8'))
registered = set()
for m in reg.get('modules', []):
    fp = m.get('file_path', '').replace('/', os.sep)
    registered.add(os.path.normpath(os.path.join(docs, fp)))

# 扫描实际 .md 文件（排除 governance 目录自身和 EXTRACTION-REPORT）
orphans = []
for f in glob.glob(docs + '/**/*.md', recursive=True):
    nf = os.path.normpath(f)
    if os.sep + 'governance' + os.sep in nf:
        continue
    if 'EXTRACTION-REPORT' in f:
        continue
    if os.sep + '_legacy' + os.sep in nf:
        continue
    if os.path.basename(nf) in ('PROFILES.md', 'PLAN.md', '_spi-guide.md'):
        continue
    if nf not in registered:
        rel = os.path.relpath(nf, docs).replace(os.sep, '/')
        orphans.append(rel)

if orphans:
    for o in orphans:
        print(o)
" 2>&1) || true

  if [ -n "$orphan_files" ]; then
    while IFS= read -r orphan; do
      [ -n "$orphan" ] && log_warn "未注册文件: $orphan (存在于磁盘但 registry.yaml 中无记录)"
    done <<< "$orphan_files"
  else
    log_pass "无孤立文件 (所有 .md 均已在 registry 注册)"
  fi

  log_section_end
}

# ============================================================
# 执行校验
# ============================================================

# 基础校验（每次必跑）
check_yaml_syntax
check_conflict_markers
check_registry_paths
check_profile_modules
check_routing_paths
check_rule_id_uniqueness
check_cross_reference
check_semver
check_dependencies
check_placeholders
check_role_permissions
check_changelog

# PR 模式附加校验（仅在增量/CI 模式下运行，--full 全量扫描不检查提案）
if [ "$MODE" != "--full" ]; then
  check_proposal_exists
  check_protected_files
fi

# 全量巡检附加项
if $FULL_SCAN; then
  full_health_check
fi

# ============================================================
# 输出报告
# ============================================================
echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║              校 验 报 告                     ║"
echo "╠══════════════════════════════════════════════╣"
echo -e "║  总检查项:  $(printf '%-32s' "$CHECKS_TOTAL")║"
echo -e "║  通过:      ${GREEN}$(printf '%-32s' "$CHECKS_PASSED")${NC}║"
echo -e "║  错误:      ${RED}$(printf '%-32s' "$ERRORS")${NC}║"
echo -e "║  警告:      ${YELLOW}$(printf '%-32s' "$WARNINGS")${NC}║"
echo "╠══════════════════════════════════════════════╣"

if [ "$ERRORS" -gt 0 ]; then
  echo -e "║  结果:  ${RED}$(printf '%-33s' "校验失败 — 请修正后重新提交")${NC}║"
  echo "╚══════════════════════════════════════════════╝"
  exit 1
elif [ "$WARNINGS" -gt 0 ]; then
  echo -e "║  结果:  ${YELLOW}$(printf '%-33s' "校验通过（有警告）")${NC}║"
  echo "╚══════════════════════════════════════════════╝"
  exit 0
else
  echo -e "║  结果:  ${GREEN}$(printf '%-33s' "全部通过")${NC}║"
  echo "╚══════════════════════════════════════════════╝"
  exit 0
fi
