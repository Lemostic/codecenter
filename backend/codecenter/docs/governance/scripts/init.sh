#!/usr/bin/env bash
# ============================================================
# init.sh — 规范体系初始化脚本
# Enterprise Coding Standards Initialization Script
# ============================================================
# 供外部团队克隆仓库后首次运行时使用。
# 功能:
#   1. 检查目录结构完整性
#   2. 验证 registry.yaml 与实际文件的一致性
#   3. 生成初始 CHANGELOG 条目
#   4. 配置 Git hooks（可选）
#   5. 输出接入指南
#
# 用法:
#   bash governance/scripts/init.sh [--skip-hooks]
# ============================================================

set -euo pipefail

# ============================================================
# 全局变量
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GOVERNANCE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCS_DIR="$(cd "$GOVERNANCE_DIR/.." && pwd)"
SKIP_HOOKS=false
ERRORS=0

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================================
# 参数解析
# ============================================================
for arg in "$@"; do
  case "$arg" in
    --skip-hooks) SKIP_HOOKS=true ;;
    --help|-h)
      echo "用法: bash governance/scripts/init.sh [--skip-hooks]"
      echo ""
      echo "选项:"
      echo "  --skip-hooks  跳过 Git hooks 安装"
      echo "  --help        显示帮助信息"
      exit 0
      ;;
  esac
done

# ============================================================
# 工具函数
# ============================================================
step_header() {
  echo ""
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

pass() { echo -e "  ${GREEN}✓${NC} $1"; }
fail() { echo -e "  ${RED}✗${NC} $1"; ERRORS=$((ERRORS + 1)); }
warn() { echo -e "  ${YELLOW}⚠${NC} $1"; }
info() { echo -e "  ${BLUE}ℹ${NC} $1"; }

# ============================================================
# 欢迎信息
# ============================================================
echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║                                                  ║"
echo "║   Enterprise Coding Standards — 初始化向导       ║"
echo "║                                                  ║"
echo "║   本脚本将帮助您在新团队中快速启动规范体系       ║"
echo "║                                                  ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""
echo "  文档根目录: $DOCS_DIR"
echo "  治理目录:   $GOVERNANCE_DIR"

# ============================================================
# Step 1: 检查目录结构
# ============================================================
step_header "Step 1/5: 检查目录结构完整性"

REQUIRED_DIRS=(
  "universal"
  "profiles"
  "profiles/spring-boot-mvc"
  "profiles/ddd"
  "profiles/react-vue-frontend"
  "spi"
  "skill"
  "guides"
  "governance"
  "governance/proposals"
  "governance/scripts"
)

for dir in "${REQUIRED_DIRS[@]}"; do
  if [ -d "$DOCS_DIR/$dir" ]; then
    pass "$dir/"
  else
    fail "$dir/ — 目录缺失"
  fi
done

# 检查核心文件
REQUIRED_FILES=(
  "ARCHITECTURE.md"
  "PLAN.md"
  "governance/GOVERNANCE.md"
  "governance/registry.yaml"
  "governance/permissions.yaml"
  "governance/validation-rules.yaml"
  "governance/CHANGELOG.md"
  "governance/proposals/_template.yaml"
  "skill/SKILL.md"
  "skill/routing-rules.yaml"
)

for file in "${REQUIRED_FILES[@]}"; do
  if [ -f "$DOCS_DIR/$file" ]; then
    pass "$file"
  else
    fail "$file — 文件缺失"
  fi
done

# ============================================================
# Step 2: 验证 registry.yaml 一致性
# ============================================================
step_header "Step 2/5: 验证 registry.yaml 与实际文件一致性"

if ! command -v python3 &>/dev/null; then
  warn "Python3 未安装，跳过 registry 一致性验证"
  warn "请安装 Python 3.8+ 后运行: pip install pyyaml"
else
  # 安装 PyYAML（如果缺失）
  if ! python3 -c "import yaml" 2>/dev/null; then
    info "正在安装 PyYAML..."
    pip3 install pyyaml --quiet 2>/dev/null || pip install pyyaml --quiet 2>/dev/null || \
      warn "PyYAML 安装失败，请手动运行: pip install pyyaml"
  fi

  if python3 -c "import yaml" 2>/dev/null; then
    REG_RESULT=$(python3 -c "
import yaml, os, sys
reg_path = '$GOVERNANCE_DIR/registry.yaml'
reg = yaml.safe_load(open(reg_path, encoding='utf-8'))
modules = reg.get('modules', [])
missing = []
for m in modules:
    fp = os.path.join('$DOCS_DIR', m.get('file_path', ''))
    if not os.path.exists(fp):
        missing.append(f\"  {m['module_id']}: {m['file_path']}\")
if missing:
    print('MISSING')
    for m in missing:
        print(m)
    sys.exit(1)
# 输出统计
active = sum(1 for m in modules if m.get('status') == 'active')
print(f'OK:{len(modules)}:{active}')
" 2>&1) || true

    if [[ "$REG_RESULT" == OK:* ]]; then
      IFS=':' read -r _ total active <<< "$REG_RESULT"
      pass "registry 一致性验证通过 ($total 模块注册, $active active)"
    elif [[ "$REG_RESULT" == "MISSING"* ]]; then
      warn "以下模块在 registry.yaml 中注册但文件不存在:"
      echo "$REG_RESULT" | grep -v "^MISSING" | while IFS= read -r line; do
        warn "  $line"
      done
    else
      fail "registry.yaml 解析失败: $REG_RESULT"
    fi
  fi
fi

# ============================================================
# Step 3: 生成初始 CHANGELOG 条目
# ============================================================
step_header "Step 3/5: 初始化 CHANGELOG"

CHANGELOG="$GOVERNANCE_DIR/CHANGELOG.md"
if [ -f "$CHANGELOG" ]; then
  # 检查是否已有条目
  EXISTING_ENTRIES=$(grep -c '^\## \[' "$CHANGELOG" 2>/dev/null || echo "0")
  if [ "$EXISTING_ENTRIES" -gt 0 ]; then
    info "CHANGELOG.md 已有 $EXISTING_ENTRIES 个版本条目，保留原有条目"
    pass "CHANGELOG.md 已存在且包含历史记录"
  else
    info "CHANGELOG.md 存在但无版本条目，追加初始条目"
    cat >> "$CHANGELOG" << 'CHANGELOG_ENTRY'

## [1.0.0] - 初始化

### Added
- 规范体系初始化，由 `init.sh` 自动生成
- 包含 L0 通用规范 (9 份)、L1 架构 Profile (3 套)、L2 SPI 插件模板
- 治理框架 (GOVERNANCE.md)、元数据注册表 (registry.yaml)
- 角色权限矩阵 (permissions.yaml)、自动化校验规则 (validation-rules.yaml)
- 变更提案系统 (proposals/) 与 CI/CD 流水线配置
CHANGELOG_ENTRY
    pass "已追加 [1.0.0] 初始条目"
  fi
else
  cat > "$CHANGELOG" << 'CHANGELOG_NEW'
# Changelog

> 本文件记录规范体系的所有变更历史。格式遵循 [Keep a Changelog](https://keepachangelog.com/)。

## [1.0.0] - 初始化

### Added
- 规范体系初始化，由 `init.sh` 自动生成
- 包含 L0 通用规范 (9 份)、L1 架构 Profile (3 套)、L2 SPI 插件模板
- 治理框架 (GOVERNANCE.md)、元数据注册表 (registry.yaml)
- 角色权限矩阵 (permissions.yaml)、自动化校验规则 (validation-rules.yaml)
- 变更提案系统 (proposals/) 与 CI/CD 流水线配置
CHANGELOG_NEW
  pass "已创建 CHANGELOG.md 并写入 [1.0.0] 初始条目"
fi

# ============================================================
# Step 4: 配置 Git hooks（可选）
# ============================================================
step_header "Step 4/5: 配置 Git pre-commit hook"

if $SKIP_HOOKS; then
  info "已跳过 Git hooks 安装 (--skip-hooks)"
elif ! git -C "$DOCS_DIR" rev-parse --git-dir &>/dev/null 2>&1 && \
     ! git -C "$(dirname "$DOCS_DIR")" rev-parse --git-dir &>/dev/null 2>&1; then
  warn "未检测到 Git 仓库，跳过 hooks 安装"
  info "初始化 Git 仓库后可重新运行本脚本"
else
  # 找到 .git 目录
  GIT_DIR=""
  if [ -d "$DOCS_DIR/.git" ]; then
    GIT_DIR="$DOCS_DIR/.git"
  elif [ -d "$(dirname "$DOCS_DIR")/.git" ]; then
    GIT_DIR="$(dirname "$DOCS_DIR")/.git"
  fi

  if [ -n "$GIT_DIR" ]; then
    HOOKS_DIR="$GIT_DIR/hooks"
    PRE_COMMIT="$HOOKS_DIR/pre-commit"

    if [ -f "$PRE_COMMIT" ]; then
      # 备份已有 hook
      cp "$PRE_COMMIT" "${PRE_COMMIT}.bak"
      info "已备份现有 pre-commit hook → pre-commit.bak"
    fi

    cat > "$PRE_COMMIT" << 'HOOK_SCRIPT'
#!/usr/bin/env bash
# ============================================================
# pre-commit hook: 规范体系快速校验
# 由 init.sh 自动安装
# ============================================================

# 获取变更文件列表
CHANGED=$(git diff --cached --name-only --diff-filter=ACM)

# 检查是否有 docs/ 目录下的文件变更
DOCS_CHANGED=$(echo "$CHANGED" | grep "^docs/" || true)
if [ -z "$DOCS_CHANGED" ]; then
  exit 0  # 无 docs 变更，跳过
fi

echo "检测到 docs/ 目录变更，运行规范体系快速校验..."

# 快速 YAML 语法检查
YAML_FILES=$(echo "$DOCS_CHANGED" | grep "\.yaml$" || true)
if [ -n "$YAML_FILES" ]; then
  while IFS= read -r f; do
    if ! python3 -c "import yaml; yaml.safe_load(open('$f'))" 2>/dev/null; then
      echo "ERROR: YAML 语法错误 — $f"
      exit 1
    fi
  done <<< "$YAML_FILES"
fi

# 检查冲突标记
if echo "$CHANGED" | grep -qE "\.(md|yaml)$"; then
  for f in $CHANGED; do
    if grep -qE '<<<<<<<|=======|>>>>>>>' "$f" 2>/dev/null; then
      echo "ERROR: 发现 Git 冲突标记 — $f"
      exit 1
    fi
  done
fi

# 检查是否直接编辑了受保护文件
PROTECTED=(
  "docs/governance/GOVERNANCE.md"
  "docs/governance/registry.yaml"
  "docs/governance/permissions.yaml"
  "docs/ARCHITECTURE.md"
  "docs/PLAN.md"
  "docs/skill/SKILL.md"
  "docs/skill/routing-rules.yaml"
)

for pf in "${PROTECTED[@]}"; do
  if echo "$CHANGED" | grep -qF "$pf"; then
    echo "WARNING: 受保护文件被修改 — $pf"
    echo "请确认已通过 governance/proposals/ 提交变更提案"
  fi
done

echo "快速校验通过"
exit 0
HOOK_SCRIPT

    chmod +x "$PRE_COMMIT"
    pass "pre-commit hook 已安装 → $PRE_COMMIT"
    info "每次 git commit 时将自动校验 docs/ 变更"
  fi
fi

# ============================================================
# Step 5: 输出接入指南
# ============================================================
step_header "Step 5/5: 接入指南"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║   初始化完成!  规范体系已就绪                    ║${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
echo ""

if [ "$ERRORS" -gt 0 ]; then
  echo -e "  ${YELLOW}发现 $ERRORS 个问题，请根据上方提示修复后再继续。${NC}"
  echo ""
fi

echo "  快速开始:"
echo "  ─────────────────────────────────────────────"
echo ""
echo "  1. 配置项目规范清单"
echo "     在项目根目录创建 spec-manifest.yaml，声明项目架构指纹:"
echo ""
echo "     cat > spec-manifest.yaml << 'EOF'"
echo "     project_name: \"my-project\""
echo "     architecture: \"ddd\"           # spring-boot-mvc | ddd | react-vue-frontend"
echo "     tech_stack: [\"java\", \"spring-boot\"]"
echo "     domains: []"
echo "     http_mode: \"A\""
echo "     EOF"
echo ""
echo "  2. 提交第一个规范变更"
echo "     a. 复制提案模板:"
echo "        cp governance/proposals/_template.yaml \\"
echo "           governance/proposals/PROP-2026-NNNN-your-change.proposal.yaml"
echo "     b. 编辑提案文件，填写变更内容"
echo "     c. 创建 PR，CI 将自动校验"
echo ""
echo "  3. 运行全量校验"
echo "     bash governance/scripts/validate-spec.sh --full"
echo ""
echo "  4. 查阅文档"
echo "     - 治理框架:   governance/GOVERNANCE.md"
echo "     - 入门指南:   guides/getting-started.md"
echo "     - 新增规范:   guides/how-to-add-spec.md"
echo "     - SPI 开发:   guides/how-to-write-spi.md"
echo ""
echo "  ─────────────────────────────────────────────"
echo ""
