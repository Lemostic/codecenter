#!/usr/bin/env bash
# ============================================================
# bootstrap.sh — 多 AI Agent 一键注入脚本
# Enterprise Coding Standards — Agent Bootstrap Script
# ============================================================
# 将规范体系注入到项目根目录，自动配置 AI 编程助手接入。
#
# 用法:
#   bash docs/governance/scripts/bootstrap.sh [选项]
#
# 选项:
#   --project-name <name>   项目名称（不指定则交互式输入）
#   --profiles <list>       架构包 ID 列表（逗号分隔，推荐）
#   --arch <type>           架构类型（旧版兼容）: ddd | spring-boot-mvc | vue-frontend
#   --http-mode <mode>      HTTP 模式: 1 (全RESTful) | 2 (简化GET/POST)
#   --force                 覆盖已存在的配置文件
#   --dry-run               仅显示将要执行的操作，不实际写入
#   --skip-manifest         跳过 manifest 创建（已存在时使用）
#
# 退出码:
#   0 = 成功
#   1 = 错误
# ============================================================

set -euo pipefail

# ============================================================
# 全局变量
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCS_DIR="$(cd "$SCRIPT_DIR/../../" && pwd)"
PROJECT_ROOT="$(cd "$DOCS_DIR/.." && pwd)"

# 参数
PROJECT_NAME=""
ARCH_TYPE=""          # 旧版 --arch 参数（向后兼容）
PROFILES=""           # 新版 --profiles 参数（逗号分隔的 package ID 列表）
HTTP_MODE=""
FORCE=false
DRY_RUN=false
SKIP_MANIFEST=false

# 已知的 profiles 列表（用于验证）
KNOWN_PROFILES=(
  "spring-boot-base" "arch-ddd" "arch-mvc"
  "persistence-mybatis-plus" "persistence-mybatis" "persistence-jpa"
  "persistence-redis" "persistence-elasticsearch"
  "messaging-kafka" "messaging-rocketmq"
  "testing-jvm" "db-migration"
  "frontend-vue"
)

# --arch 到 --profiles 的向后兼容映射
declare -A ARCH_TO_PROFILES=(
  ["ddd"]="spring-boot-base,arch-ddd,persistence-mybatis-plus"
  ["spring-boot-mvc"]="spring-boot-base,arch-mvc,persistence-mybatis-plus"
  ["vue-frontend"]="frontend-vue"
)

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# 统计
CREATED=0
SKIPPED=0
UPDATED=0

# ============================================================
# 参数解析
# ============================================================
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project-name) PROJECT_NAME="$2"; shift 2 ;;
    --arch) ARCH_TYPE="$2"; shift 2 ;;
    --profiles) PROFILES="$2"; shift 2 ;;
    --http-mode) HTTP_MODE="$2"; shift 2 ;;
    --force) FORCE=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    --skip-manifest) SKIP_MANIFEST=true; shift ;;
    --help|-h)
      echo "用法: bash docs/governance/scripts/bootstrap.sh [选项]"
      echo ""
      echo "选项:"
      echo "  --project-name <name>   项目名称"
      echo "  --profiles <list>       架构包 ID 列表（逗号分隔，推荐）"
      echo "  --arch <type>           架构类型（旧版兼容）: ddd | spring-boot-mvc | vue-frontend"
      echo "  --http-mode <mode>      HTTP 模式: 1 | 2"
      echo "  --force                 覆盖已存在的配置文件"
      echo "  --dry-run               仅预览，不实际写入"
      echo "  --skip-manifest         跳过 manifest 创建"
      echo ""
      echo "可用架构包 ID（--profiles）:"
      echo "  基础层:     spring-boot-base"
      echo "  架构层:     arch-ddd, arch-mvc"
      echo "  持久层:     persistence-mybatis-plus, persistence-mybatis, persistence-jpa"
      echo "              persistence-redis, persistence-elasticsearch"
      echo "  消息队列:   messaging-kafka, messaging-rocketmq (二选一)"
      echo "  测试框架:   testing-jvm"
      echo "  数据库迁移: db-migration"
      echo "  前端:       frontend-vue"
      echo ""
      echo "示例:"
      echo "  --profiles spring-boot-base,arch-ddd,persistence-mybatis-plus"
      echo "  --arch ddd   # 等价于上述组合（向后兼容）"
      exit 0
      ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

# ============================================================
# 工具函数
# ============================================================
info()    { echo -e "  ${BLUE}ℹ${NC} $1"; }
pass()    { echo -e "  ${GREEN}✓${NC} $1"; CREATED=$((CREATED + 1)); }
skip()    { echo -e "  ${YELLOW}→${NC} $1"; SKIPPED=$((SKIPPED + 1)); }
update()  { echo -e "  ${CYAN}↻${NC} $1"; UPDATED=$((UPDATED + 1)); }
fail()    { echo -e "  ${RED}✗${NC} $1"; }
section() {
  echo ""
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# 安全写入文件（支持 --dry-run 和 --force）
safe_write() {
  local file="$1"
  local content="$2"
  local desc="$3"

  if $DRY_RUN; then
    info "[dry-run] 将创建: $file"
    return
  fi

  if [ -f "$file" ] && ! $FORCE; then
    skip "$desc 已存在 (使用 --force 覆盖): $file"
    return
  fi

  local dir
  dir="$(dirname "$file")"
  [ -d "$dir" ] || mkdir -p "$dir"

  echo "$content" > "$file"
  if [ -f "$file" ]; then
    if $FORCE && [ "${_FILE_EXISTED:-false}" = "true" ]; then
      update "$desc 已更新: $file"
    else
      pass "$desc 已创建: $file"
    fi
  else
    fail "$desc 创建失败: $file"
  fi
}

# 追加内容到已有文件（不重复追加）
safe_append() {
  local file="$1"
  local content="$2"
  local marker="$3"
  local desc="$4"

  if $DRY_RUN; then
    if [ -f "$file" ]; then
      info "[dry-run] 将追加到: $file"
    else
      info "[dry-run] 将创建: $file"
    fi
    return
  fi

  local dir
  dir="$(dirname "$file")"
  [ -d "$dir" ] || mkdir -p "$dir"

  if [ -f "$file" ]; then
    # 检查是否已有相同标记的内容
    if grep -qF "$marker" "$file" 2>/dev/null; then
      skip "$desc 已存在（检测到标记 '$marker'）"
      return
    fi
    # 追加
    echo "" >> "$file"
    echo "$content" >> "$file"
    update "$desc 已追加到: $file"
  else
    echo "$content" > "$file"
    pass "$desc 已创建: $file"
  fi
}

# --arch 向后兼容: 自动映射为对应的 profiles
if [ -n "$ARCH_TYPE" ] && [ -z "$PROFILES" ]; then
  if [ -n "${ARCH_TO_PROFILES[$ARCH_TYPE]+_}" ]; then
    PROFILES="${ARCH_TO_PROFILES[$ARCH_TYPE]}"
    info "已将 --arch $ARCH_TYPE 映射为 --profiles $PROFILES"
  else
    fail "未知的架构类型: $ARCH_TYPE"
    echo "  可选值: ddd | spring-boot-mvc | vue-frontend"
    exit 1
  fi
fi

# ============================================================
# 技术栈自动检测
# 扫描项目根目录的构建文件和目录结构，推断适用的 profiles
# ============================================================
detect_tech_stack() {
  local root="$PROJECT_ROOT"
  local detected_profiles=()
  local detected_techs=()
  local hints=()

  # ---- Java / Gradle 构建文件扫描 ----
  local build_files=()
  for f in "$root/pom.xml" "$root/build.gradle" "$root/build.gradle.kts"; do
    [ -f "$f" ] && build_files+=("$f")
  done

  if [ ${#build_files[@]} -gt 0 ]; then
    local all_content=""
    for bf in "${build_files[@]}"; do
      all_content+="$("$CAT_CMD" "$bf" 2>/dev/null)"$'\n'
    done

    # Spring Boot 基础
    if echo "$all_content" | grep -qF "spring-boot-starter-web"; then
      detected_profiles+=("spring-boot-base")
      detected_techs+=("java" "spring-boot")
    fi

    # MyBatis-Plus
    if echo "$all_content" | grep -qF "mybatis-plus"; then
      detected_profiles+=("persistence-mybatis-plus")
      detected_techs+=("mybatis-plus")
    fi

    # 原生 MyBatis（仅在未检测到 mybatis-plus 时追加）
    if echo "$all_content" | grep -qF "mybatis-spring-boot"; then
      if ! printf '%s\n' "${detected_profiles[@]}" 2>/dev/null | grep -qF "persistence-mybatis-plus"; then
        detected_profiles+=("persistence-mybatis")
        detected_techs+=("mybatis")
      fi
    fi

    # JPA / QueryDSL
    if echo "$all_content" | grep -qF "spring-boot-starter-data-jpa" || \
       echo "$all_content" | grep -qF "querydsl"; then
      detected_profiles+=("persistence-jpa")
      detected_techs+=("jpa")
    fi

    # Redis
    if echo "$all_content" | grep -qF "spring-boot-starter-data-redis"; then
      detected_profiles+=("persistence-redis")
      detected_techs+=("redis")
    fi

    # Elasticsearch
    if echo "$all_content" | grep -qF "spring-boot-starter-data-elasticsearch" || \
       echo "$all_content" | grep -qF "elasticsearch-java"; then
      detected_profiles+=("persistence-elasticsearch")
      detected_techs+=("elasticsearch")
    fi

    # COLA / DDD 提示
    if echo "$all_content" | grep -qi "cola"; then
      hints+=("cola 框架依赖 → 建议使用 arch-ddd")
    fi
  fi

  # ---- 前端 package.json 扫描 ----
  local pkg_json="$root/package.json"
  if [ -f "$pkg_json" ]; then
    local pkg_content
    pkg_content="$("$CAT_CMD" "$pkg_json" 2>/dev/null)"
    if echo "$pkg_content" | grep -qE '"vue"|"nuxt"'; then
      detected_profiles+=("frontend-vue")
      detected_techs+=("typescript" "frontend")
    fi
  fi

  # ---- 目录结构提示 ----
  local src_main
  src_main="$(find "$root" -maxdepth 5 -type d -name "java" 2>/dev/null | head -1)"
  if [ -n "$src_main" ]; then
    # DDD 目录特征: adapter/ + domain/model/
    if find "$src_main" -maxdepth 6 -type d -name "adapter" 2>/dev/null | grep -q . && \
       find "$src_main" -maxdepth 6 -type d -path "*/domain/model" 2>/dev/null | grep -q .; then
      hints+=("目录含 adapter/ + domain/model/ → 建议使用 arch-ddd")
    fi
    # MVC 目录特征: controller/ + service/impl/
    if find "$src_main" -maxdepth 6 -type d -name "controller" 2>/dev/null | grep -q . && \
       find "$src_main" -maxdepth 6 -type d -path "*/service/impl" 2>/dev/null | grep -q .; then
      hints+=("目录含 controller/ + service/impl/ → 建议使用 arch-mvc")
    fi
  fi

  # 去重
  local unique_profiles=()
  local seen_profiles=""
  for p in "${detected_profiles[@]}"; do
    if ! echo "$seen_profiles" | grep -qF "|$p|"; then
      unique_profiles+=("$p")
      seen_profiles+="|$p|"
    fi
  done
  local unique_techs=()
  local seen_techs=""
  for t in "${detected_techs[@]}"; do
    if ! echo "$seen_techs" | grep -qF "|$t|"; then
      unique_techs+=("$t")
      seen_techs+="|$t|"
    fi
  done

  # 输出结果到全局变量
  DETECTED_PROFILES="${unique_profiles[*]:-}"
  DETECTED_TECHS="${unique_techs[*]:-}"
  DETECTED_HINTS="${hints[*]:-}"
}

# 兼容无 cat 变量：直接读文件
CAT_CMD="cat"

# ============================================================
# 交互式收集项目信息
# ============================================================
collect_project_info() {
  # 项目名称
  if [ -z "$PROJECT_NAME" ]; then
    local default_name
    default_name="$(basename "$PROJECT_ROOT" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9-]/-/g')"
    echo ""
    echo -e "  ${BOLD}项目名称${NC}（回车使用默认值: $default_name）:"
    read -r -p "  > " PROJECT_NAME
    [ -z "$PROJECT_NAME" ] && PROJECT_NAME="$default_name"
  fi

  # Profiles（架构包列表）
  if [ -z "$PROFILES" ]; then
    # 1. 自动检测技术栈
    echo ""
    echo -e "  ${CYAN}正在扫描项目技术栈...${NC}"
    detect_tech_stack

    # 显示检测结果
    if [ -n "$DETECTED_PROFILES" ]; then
      echo ""
      echo -e "  ${BOLD}自动检测结果:${NC}"
      echo -e "    推荐 profiles:  ${GREEN}$DETECTED_PROFILES${NC}"
      [ -n "$DETECTED_TECHS" ] && echo -e "    识别技术栈:     $DETECTED_TECHS"
      if [ -n "$DETECTED_HINTS" ]; then
        echo -e "    ${YELLOW}提示:${NC}"
        # hints 是空格分隔的，每条以 → 为分界
        local IFS_BAK="$IFS"
        IFS=' '
        for hint in $DETECTED_HINTS; do
          echo "      - $hint"
        done
        IFS="$IFS_BAK"
      fi
    else
      echo -e "  ${YELLOW}未检测到明确的构建文件，请手动选择 profiles${NC}"
    fi

    # 2. 提供预设快捷选择
    echo ""
    echo -e "  ${BOLD}选择架构包配置方式${NC}:"
    echo "    1) 使用自动检测推荐（上方结果）"
    echo "    2) DDD 预设      → spring-boot-base, arch-ddd, persistence-mybatis-plus"
    echo "    3) MVC 预设      → spring-boot-base, arch-mvc, persistence-mybatis-plus"
    echo "    4) 前端预设      → frontend-vue"
    echo "    5) 自定义输入    → 手动输入逗号分隔的 profile ID 列表"
    echo ""
    read -r -p "  请选择 [1/2/3/4/5] (默认 1): " profile_choice
    case "${profile_choice:-1}" in
      1)
        if [ -n "$DETECTED_PROFILES" ]; then
          # 将空格分隔转为逗号分隔
          PROFILES="$(echo "$DETECTED_PROFILES" | tr ' ' ',')"
        else
          PROFILES="spring-boot-base"
          info "未检测到技术栈，默认使用 spring-boot-base"
        fi
        ;;
      2) PROFILES="spring-boot-base,arch-ddd,persistence-mybatis-plus" ;;
      3) PROFILES="spring-boot-base,arch-mvc,persistence-mybatis-plus" ;;
      4) PROFILES="frontend-vue" ;;
      5)
        echo ""
        echo "  可用 profile ID:"
        for kp in "${KNOWN_PROFILES[@]}"; do
          echo "    - $kp"
        done
        echo ""
        read -r -p "  请输入 profile ID 列表（逗号分隔）: " PROFILES
        if [ -z "$PROFILES" ]; then
          PROFILES="spring-boot-base"
          info "输入为空，默认使用 spring-boot-base"
        fi
        ;;
      *)
        if [ -n "$DETECTED_PROFILES" ]; then
          PROFILES="$(echo "$DETECTED_PROFILES" | tr ' ' ',')"
        else
          PROFILES="spring-boot-base"
          info "默认使用 spring-boot-base"
        fi
        ;;
    esac

    echo ""
    echo -e "  ${GREEN}已选择 profiles:${NC} $PROFILES"
  fi

  # HTTP 模式
  if [ -z "$HTTP_MODE" ]; then
    echo ""
    echo -e "  ${BOLD}HTTP 方法模式${NC}:"
    echo "    1) 全 RESTful — GET/POST/PUT/PATCH/DELETE（团队 REST 成熟度高）"
    echo "    2) 简化模式  — 仅 GET/POST（降低前后端联调复杂度）"
    echo ""
    read -r -p "  请选择 [1/2] (默认 1): " http_choice
    case "${http_choice:-1}" in
      1) HTTP_MODE="1" ;;
      2) HTTP_MODE="2" ;;
      *) HTTP_MODE="1" ;;
    esac
  fi
}

# ============================================================
# Step 1: 前置检查
# ============================================================
section "Step 1/5: 前置检查"

# 检查 docs 目录结构
if [ ! -f "$DOCS_DIR/skill/SKILL.md" ]; then
  fail "未找到 docs/skill/SKILL.md"
  echo "  请确保 docs/ 目录完整。参考: docs/README.md"
  exit 1
fi
pass "docs/skill/SKILL.md 引擎文件存在"

if [ ! -f "$DOCS_DIR/skill/routing-rules.yaml" ]; then
  fail "未找到 docs/skill/routing-rules.yaml"
  exit 1
fi
pass "docs/skill/routing-rules.yaml 路由配置存在"

# 检查 docs 目录相对于项目根的路径
DOCS_REL="docs"
if [ "$(basename "$PROJECT_ROOT")" = "docs" ]; then
  # 用户在 docs 目录下运行
  PROJECT_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"
  DOCS_REL="docs"
fi
info "项目根目录: $PROJECT_ROOT"
info "docs 目录:   $DOCS_DIR"

# ============================================================
# Step 2: 创建项目指纹（spec-manifest.yaml）
# ============================================================
section "Step 2/5: 项目指纹配置"

SPEC_DIR="$PROJECT_ROOT/.spec"
MANIFEST="$SPEC_DIR/spec-manifest.yaml"

if [ -f "$MANIFEST" ] && $SKIP_MANIFEST; then
  skip "spec-manifest.yaml 已存在（--skip-manifest）"
elif [ -f "$MANIFEST" ] && ! $FORCE; then
  skip "spec-manifest.yaml 已存在 (使用 --force 覆盖)"
  # 从已有 manifest 读取配置（兼容新 profiles 和旧 architecture 格式）
  if command -v python3 &>/dev/null; then
    EXISTING_PROFILES=$(python3 -c "
import yaml
m = yaml.safe_load(open('$MANIFEST'))
fp = m.get('fingerprint', {})
profiles = fp.get('profiles', [])
if profiles:
    print(','.join(profiles))
else:
    # 旧版兼容：读取 architecture 字段并映射
    arch = fp.get('architecture', '')
    mapping = {
        'ddd': 'spring-boot-base,arch-ddd,persistence-mybatis-plus',
        'spring-boot-mvc': 'spring-boot-base,arch-mvc,persistence-mybatis-plus',
        'vue-frontend': 'frontend-vue'
    }
    print(mapping.get(arch, ''))
" 2>/dev/null || echo "")
    if [ -n "$EXISTING_PROFILES" ]; then
      PROFILES="$EXISTING_PROFILES"
      info "从已有 manifest 读取 profiles: $PROFILES"
    fi
  fi
else
  collect_project_info

  # 将逗号分隔的 PROFILES 转为 YAML 列表
  PROFILES_YAML=""
  if [ -n "$PROFILES" ]; then
    _IFS_BAK="$IFS"
    IFS=','
    for p in $PROFILES; do
      p="$(echo "$p" | xargs)"  # trim whitespace
      [ -n "$p" ] && PROFILES_YAML+="    - $p"$'\n'
    done
    IFS="$_IFS_BAK"
  fi

  # 从检测到的技术栈构建 tech_stack YAML（若 collect_project_info 中有结果）
  TECH_STACK_YAML="[]"
  if [ -n "${DETECTED_TECHS:-}" ]; then
    TECH_STACK_YAML="["
    _first=true
    _IFS_BAK2="$IFS"
    IFS=' '
    for t in $DETECTED_TECHS; do
      if $_first; then
        TECH_STACK_YAML+="\"$t\""
        _first=false
      else
        TECH_STACK_YAML+=", \"$t\""
      fi
    done
    IFS="$_IFS_BAK2"
    TECH_STACK_YAML+="]"
  fi

  if $DRY_RUN; then
    info "[dry-run] 将创建 $MANIFEST"
  else
    mkdir -p "$SPEC_DIR"
    cat > "$MANIFEST" << MANIFEST_EOF
# ============================================================
# spec-manifest.yaml — 项目规范清单
# 由 bootstrap.sh 自动生成
# Skill 引擎根据本文件自动路由并加载适用的编码规范
# ============================================================

version: "2.0"

project:
  name: "$PROJECT_NAME"
  description: ""  # 请填写项目一句话描述

fingerprint:
  profiles:
${PROFILES_YAML}  tech_stack: $TECH_STACK_YAML
  domains: []              # 如需激活 SPI 插件: ["data-governance"]
  http_mode: "$HTTP_MODE"  # 1 = 全 RESTful | 2 = 简化 GET/POST

protection:
  protected_globs: []      # 示例: ["**/application-*.yml", "**/pom.xml"]
  enable_branch_protection: true
  require_ci_pass: true

overrides:
  disabled_specs: []       # 显式禁用的规范 ID
  forced_specs: []         # 强制加载的规范 ID
MANIFEST_EOF
    pass "spec-manifest.yaml 已创建"
    info "请编辑 $MANIFEST 补充 description 和 domains"
  fi
fi

# 创建 .spec 辅助文件模板
INVENTORY="$SPEC_DIR/project-inventory.yaml"
if [ ! -f "$INVENTORY" ]; then
  safe_write "$INVENTORY" "$(cat << 'INVENTORY_EOF'
# ============================================================
# project-inventory.yaml — 项目资产清单
# Skill 引擎在执行防重复检查时查阅本文件
# 在编码过程中逐步补充
# ============================================================

version: "1.0"

modules: []
  # - id: "order-service"
  #   path: "src/main/java/com/example/order/"
  #   core_classes:
  #     - "OrderService"
  #     - "OrderRepository"

utilities: []
  # - class: "StringUtils"
  #   methods: ["isEmpty()", "toCamelCase()"]
  #   purpose: "字符串工具方法"

api_endpoints: []
  # - method: "GET"
  #   path: "/api/orders/{id}"
  #   description: "查询订单详情"

shared_components: []
  # - name: "SearchBar"
  #   path: "src/components/SearchBar.tsx"
  #   description: "通用搜索栏组件"
INVENTORY_EOF
)" "项目资产清单模板"
else
  skip "project-inventory.yaml 已存在"
fi

GLOSSARY="$SPEC_DIR/glossary.yaml"
if [ ! -f "$GLOSSARY" ]; then
  safe_write "$GLOSSARY" "$(cat << 'GLOSSARY_EOF'
# ============================================================
# glossary.yaml — 项目术语字典
# Skill 引擎在命名变量/类/接口时查阅本文件
# AI MUST 使用本字典中的标准命名，不得自行发明新术语
# ============================================================

version: "1.0"

domain_terms: []
  # - zh: "订单"
  #   en: "Order"
  #   java_class: "Order"
  #   ts_interface: "IOrder"
  #   api_path_segment: "orders"

abbreviations: []
  # - abbr: "DTO"
  #   full: "Data Transfer Object"
  #   usage: "数据传输对象，用于 API 层"

naming_conventions: []
  # - context: "订单编号"
  #   standard: "orderNo"
  #   avoid: ["orderId", "orderNumber", "ordNo"]
GLOSSARY_EOF
)" "项目术语字典模板"
else
  skip "glossary.yaml 已存在"
fi

# ============================================================
# Step 3: 配置 Claude Code
# ============================================================
section "Step 3/5: 配置 Claude Code"

# 计算 docs 的相对路径
DOCS_PATH="docs"

CLAUDE_MD="$PROJECT_ROOT/CLAUDE.md"

# ═══════════════════════════════════════════════════════════════
# CLAUDE.md 规范引擎增量注入块
# ═══════════════════════════════════════════════════════════════
#
# 设计目标：
#   1. 首次初始化 → claude init 生成空 CLAUDE.md（如果 CLI 可用），
#      然后追加规范块
#   2. 已有 CLAUDE.md → 增量追加，不破坏现有内容
#   3. AI 收到后能自动执行三步：读清单 → 读指纹 → 按 profiles 加载
#
# 关键选择：
#   - 不用 `claude --print` 生成整个文件（太慢、不稳定、token 高）
#   - 用 `safe_append` + 唯一 marker 实现去重
#   - 增量块是"指令式"不是"介绍式"——AI 读完后会自动执行
# ═══════════════════════════════════════════════════════════════

ECS_MARKER="<!-- ENTERPRISE-CODING-STANDARDS -->"
ECS_BLOCK="$(cat << 'ECS_BLOCK_EOF'

<!-- ENTERPRISE-CODING-STANDARDS -->
## 企业编码规范引擎 — 指令协议

> 本块由 \`bootstrap.sh\` 自动注入，记录规范引擎的加载协议。
> 如需更新，重新运行 \`bash docs/governance/scripts/bootstrap.sh --force\`。

### AI 执行协议（MUST 按顺序）

在收到任何编码相关任务时，你必须先执行以下三步，再开始编码：

**第 1 步：加载规范索引**

阅读 \`docs/skill/SKILL.md\`，了解规范引擎的加载原理和可用的 profiles 列表。

**第 2 步：读取项目指纹**

阅读 \`.spec/spec-manifest.yaml\` 中的 \`fingerprint.profiles\` 字段。它列出了当前项目激活的架构包 ID。本项目已激活：

\`\`\`
# 如需查看当前激活列表：
# cat .spec/spec-manifest.yaml | grep -A 20 '^fingerprint:'
\`\`\`

**第 3 步：按 profiles 加载对应规范**

根据第 2 步获得的 profiles 列表，按包 ID 一一加载：

| 包 ID | 规范文件（相对于 docs/） | 何时加载 |
|-------|------------------------|---------|
| \`spring-boot-base\` | \`profiles/backend/spring-boot-base.md\` | 任何后端项目 |
| \`arch-mvc\` | \`profiles/backend/arch-mvc.md\` | MVC 三层架构 |
| \`arch-ddd\` | \`profiles/backend/ddd/*.md\` | DDD 四层架构 |
| \`persistence-mybatis-plus\` | \`profiles/backend/persistence-mybatis-plus.md\` | 使用 MyBatis-Plus |
| \`persistence-*\` | \`profiles/backend/persistence-*.md\` | 对应持久化技术 |
| \`messaging-kafka\` | \`profiles/backend/messaging-kafka.md\` | Kafka 消息队列 |
| \`messaging-rocketmq\` | \`profiles/backend/messaging-rocketmq.md\` | RocketMQ 消息队列（与 Kafka 互斥） |
| \`testing-jvm\` | \`profiles/backend/testing-jvm.md\` | Java/JVM 项目使用 JUnit 5 + Mockito + TestContainers |
| \`db-migration\` | \`profiles/backend/db-migration.md\` | 使用 Flyway/Liquibase 管理 schema 演进 |
| \`frontend-vue\` | \`profiles/frontend/vue/00-overview.md\`（先读概览），然后加载 \`common/\` + \`vue3/\` | Vue 3 前端项目 |

### L0 通用规范（始终生效，无需在 profiles 中声明）

无论 profiles 列表是什么，以下 L0 规范始终适用：

| 规范 | 文件 |
|------|------|
| 命名规范 | \`universal/naming-conventions.md\` |
| API 设计 | \`universal/api-design.md\` |
| 安全基线 | \`universal/security-baseline.md\` |
| 测试规范 | \`universal/testing-standards.md\` |
| 日志规范 | \`universal/logging-standards.md\` |
| 异常处理 | \`universal/exception-handling.md\` |
| 链路追踪 | \`universal/request-tracing.md\` |
| Git 工作流 | \`universal/git-workflow.md\` |
| 变更范围控制 | \`universal/change-scope-control.md\` |

### 异常处理

- 如果 \`.spec/spec-manifest.yaml\` 不存在 → 提示用户运行 \`bash docs/governance/scripts/bootstrap.sh\`
- 如果某个 profile 文件不存在 → 跳过，不影响其他规范加载
- 如果 profiles 列表为空 → 只加载 L0 通用规范

### 规范体系全貌

\`\`\`
cat docs/README.md        # 体系概述 + 三级引导链图解
cat docs/governance/GOVERNANCE.md  # 治理流程
cat docs/governance/QUICKSTART.md  # 快速指南
\`\`\`
ECS_BLOCK_EOF
)"

# ---------- CLAUDE.md 首次创建时的初始化 ----------
# 设计：
#   1. 有 claude CLI → 用 claude init 生成项目默认 CLAUDE.md
#   2. 无 claude CLI → 用最小模板生成
#   3. 生成后再追加规范引擎块
# ----------

_init_claude_md() {
  # 此函数在 CLAUDE.md 不存在时调用
  if $DRY_RUN; then
    info "[dry-run] 将初始化: $CLAUDE_MD"
    return
  fi

  if command -v claude &>/dev/null; then
    info "检测到 claude CLI，使用 claude init 初始化 CLAUDE.md..."
    # claude init 会在当前项目生成 CLAUDE.md
    # 注意：这里的 claude init 是 cc-sdk 的叫法，
    # 实际 Claude Code CLI 的初始化是 claude（不带参数进入交互模式）
    # 脚本中无法交互，使用 --print 让 AI 生成
    claude --print "Initialize a CLAUDE.md for this project. Keep it brief: describe the project type, primary language/framework, and any special conventions. Do NOT include a long 'Available Skills' table or gstack references — those will be injected separately. Output ONLY the CLAUDE.md content, no extra commentary." > "$CLAUDE_MD" 2>/dev/null || true

    if [ -f "$CLAUDE_MD" ] && [ -s "$CLAUDE_MD" ]; then
      # 清理 claude CLI 可能输出的无需内容（如 ```markdown 包装）
      if head -1 "$CLAUDE_MD" | grep -q '^\`\`\`'; then
        # 去掉首尾的 ```markdown 和 ``` 包装
        local tmpfile
        tmpfile="$(mktemp)"
        tail -n +2 "$CLAUDE_MD" | sed '/^\`\`\`$/,$d' > "$tmpfile"
        mv "$tmpfile" "$CLAUDE_MD"
      fi
      pass "CLAUDE.md 已通过 claude init 创建"
    else
      # claude --print 失败或输出为空，fallback 到手动模板
      fail "claude init 失败，使用手动模板创建"
      _create_fallback_claude_md
    fi
  else
    info "未检测到 claude CLI，使用手动模板创建 CLAUDE.md..."
    _create_fallback_claude_md
  fi
}

_create_fallback_claude_md() {
  # 从项目名和检测到的技术栈构建简短的 CLAUDE.md 模板
  local proj_name="${PROJECT_NAME:-$(basename "$PROJECT_ROOT" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9-]/-/g')}"
  local tech_desc=""
  [ -n "${DETECTED_TECHS:-}" ] && tech_desc="Detected tech stack: ${DETECTED_TECHS}."

  cat > "$CLAUDE_MD" << MD_EOF
# ${PROJECT_NAME}

$( [ -n "${DETECTED_TECHS:-}" ] && echo "${tech_desc}" )

This project follows enterprise coding standards. See the section below for the specification engine protocol.

> Auto-generated by bootstrap.sh. Update this file with your project-specific instructions above the separator.
MD_EOF

  if [ -f "$CLAUDE_MD" ]; then
    pass "CLAUDE.md 已通过手动模板创建"
  else
    fail "CLAUDE.md 创建失败"
  fi
}

# ---------- 主流程 ----------
if [ -f "$CLAUDE_MD" ]; then
  # CLAUDE.md 已存在 → 增量追加规范引擎引用块
  safe_append "$CLAUDE_MD" "$ECS_BLOCK" "$ECS_MARKER" "规范引擎引用块"
else
  # CLAUDE.md 不存在 → 先初始化，再追加
  _init_claude_md

  # 确认文件存在后追加规范块
  if [ -f "$CLAUDE_MD" ]; then
    if ! grep -qF "$ECS_MARKER" "$CLAUDE_MD" 2>/dev/null; then
      echo "$ECS_BLOCK" >> "$CLAUDE_MD"
      update "规范引擎引用块已写入 CLAUDE.md"
    fi
  fi
fi

# ============================================================
# Step 4: 配置 .gitignore
# ============================================================
section "Step 4/5: 检查 .gitignore"

GITIGNORE="$PROJECT_ROOT/.gitignore"
SPEC_GITIGNORE_ENTRY=".spec/"

if [ -f "$GITIGNORE" ]; then
  if grep -qF "$SPEC_GITIGNORE_ENTRY" "$GITIGNORE" 2>/dev/null; then
    skip ".gitignore 已包含 .spec/ 条目"
  else
    # .spec/ 是项目本地配置，不应提交（manifest 除外）
    # 但我们建议提交 manifest，所以改为不忽略
    info ".spec/ 目录包含项目本地配置（manifest, inventory, glossary）"
    info "建议将 .spec/spec-manifest.yaml 纳入版本控制，其余按需决定"
  fi
  # 建议忽略 .wolf/（OpenWolf 运行时产物）
  if grep -qF ".wolf/" "$GITIGNORE" 2>/dev/null; then
    skip ".gitignore 已包含 .wolf/ 条目"
  else
    if [ -d "$PROJECT_ROOT/.wolf" ] || command -v openwolf &>/dev/null; then
      info "建议将 .wolf/ 加入 .gitignore（OpenWolf 运行时产物，无需版本控制）"
    fi
  fi
else
  info "项目尚无 .gitignore，可稍后自行配置"
fi

# ============================================================
# Step 5: 输出接入报告
# ============================================================
section "Step 5/5: 接入报告"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║   Bootstrap 完成!  规范引擎已注入项目            ║${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
echo ""

echo "  项目信息:"
echo "  ─────────────────────────────────────────"
[ -n "${PROJECT_NAME:-}" ] && echo "  项目名称:   $PROJECT_NAME"
if [ -n "${PROFILES:-}" ]; then
  echo "  Profiles:   $PROFILES"
elif [ -n "${ARCH_TYPE:-}" ]; then
  echo "  架构类型:   $ARCH_TYPE（旧版，建议改用 --profiles）"
fi
[ -n "${HTTP_MODE:-}" ]    && echo "  HTTP 模式:  $HTTP_MODE"
echo ""

echo "  文件清单:"
echo "  ─────────────────────────────────────────"
echo -e "  ${GREEN}✓${NC} .spec/spec-manifest.yaml      — 项目指纹"
echo -e "  ${GREEN}✓${NC} .spec/project-inventory.yaml   — 资产清单（待填充）"
echo -e "  ${GREEN}✓${NC} .spec/glossary.yaml            — 术语字典（待填充）"
if [ -f "$PROJECT_ROOT/CLAUDE.md" ]; then
  echo -e "  ${GREEN}✓${NC} CLAUDE.md                      — Claude Code 配置"
fi
echo ""

echo "  下一步:"
echo "  ─────────────────────────────────────────"
echo "  1. 编辑 .spec/spec-manifest.yaml 补充 description 和 domains"
echo "  2. 确认 profiles 列表是否准确（可手动增减）"
echo "  3. 在编码过程中逐步填充 project-inventory.yaml 和 glossary.yaml"
echo "  4. 打开你的 AI 编程助手，开始编码 — 规范会自动生效"
echo ""
echo "  如需了解体系全貌: cat docs/README.md"
echo "  如需治理流程:     cat docs/governance/GOVERNANCE.md"
echo "  如需快速指南:     cat docs/governance/QUICKSTART.md"
echo ""

# ============================================================
# 推荐增强工具检测
# ============================================================

# OpenWolf — AI 编程助手的第二大脑
OPENWOLF_INSTALLED=false
if command -v openwolf &>/dev/null; then
  OPENWOLF_INSTALLED=true
fi

if [ -d "$PROJECT_ROOT/.wolf" ]; then
  info "检测到已有 OpenWolf 配置 (.wolf/)，与规范引擎互补运行"
elif $OPENWOLF_INSTALLED; then
  echo -e "  ${CYAN}推荐增强: OpenWolf${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  检测到已安装 OpenWolf（AI 编程助手的第二大脑）。"
  echo "  它与本规范引擎天然互补："
  echo "    规范引擎 → 告诉 AI \"该遵循什么规则\""
  echo "    OpenWolf → 给 AI \"项目记忆和学习能力\""
  echo ""
  echo "  运行以下命令在项目根目录初始化 OpenWolf:"
  echo "    openwolf init"
  echo ""
  echo "  OpenWolf 会创建 .wolf/ 目录，包含:"
  echo "    cerebrum.md    — AI 的学习笔记（跨会话记忆）"
  echo "    anatomy.md     — 项目结构地图"
  echo "    memory.md      — 持久化上下文"
  echo "    do-not-repeat  — 避免重复犯错的清单"
  echo ""
  echo "  .wolf/ 目录为运行时产物，建议加入 .gitignore。"
  echo "  此步骤完全可选，不影响规范引擎的核心功能。"
  echo ""
else
  echo -e "  ${YELLOW}可选增强: OpenWolf${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  OpenWolf 是 Claude Code 的开源中间件，为 AI 提供项目记忆、"
  echo "  token 追踪和\"不重复犯错\"能力。它与本规范引擎互补:"
  echo "    规范引擎 → 告诉 AI \"该遵循什么规则\""
  echo "    OpenWolf → 给 AI \"项目记忆和学习能力\""
  echo ""
  echo "  如需安装（完全可选）:"
  echo "    npm install -g openwolf"
  echo "    openwolf init"
  echo ""
  echo "  安装后 .wolf/ 目录为运行时产物，建议加入 .gitignore。"
  echo ""
fi

# CodeGraph — 代码结构可视化与依赖分析
CODEGRAPH_INSTALLED=false
if command -v codegraph &>/dev/null; then
  CODEGRAPH_INSTALLED=true
fi

if [ -f "$PROJECT_ROOT/.codegraph/config.yaml" ] || [ -d "$PROJECT_ROOT/.codegraph" ]; then
  info "检测到已有 CodeGraph 配置 (.codegraph/)"
elif $CODEGRAPH_INSTALLED; then
  echo -e "  ${CYAN}推荐增强: CodeGraph${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  检测到已安装 CodeGraph（代码结构可视化工具）。"
  echo "  它可以生成项目依赖图谱，帮助 AI 理解代码架构:"
  echo "    规范引擎 → 定义\"应该怎么写\""
  echo "    CodeGraph → 展示\"现在是怎么写的\""
  echo ""
  echo "  运行以下命令生成代码图谱:"
  echo "    codegraph analyze"
  echo ""
  echo "  此步骤完全可选，不影响规范引擎的核心功能。"
  echo ""
else
  echo -e "  ${YELLOW}可选增强: CodeGraph${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  CodeGraph 是代码结构可视化工具，生成依赖图谱帮助 AI"
  echo "  理解项目架构。与规范引擎互补:"
  echo "    规范引擎 → 定义\"应该怎么写\""
  echo "    CodeGraph → 展示\"现在是怎么写的\""
  echo ""
  echo "  如需安装（完全可选）:"
  echo "    npm install -g codegraph"
  echo "    codegraph analyze"
  echo ""
fi

# Superpowers — AI 编程助手的增强能力集
SUPERPOWERS_INSTALLED=false
if command -v superpowers &>/dev/null || [ -d "$HOME/.superpowers" ]; then
  SUPERPOWERS_INSTALLED=true
fi

if [ -d "$PROJECT_ROOT/.superpowers" ]; then
  info "检测到已有 Superpowers 配置 (.superpowers/)"
elif $SUPERPOWERS_INSTALLED; then
  echo -e "  ${CYAN}推荐增强: Superpowers${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  检测到已安装 Superpowers（AI 编程助手增强能力集）。"
  echo "  它提供额外的 AI 编程能力增强，与规范引擎协同工作。"
  echo ""
  echo "  运行以下命令在项目根目录初始化:"
  echo "    superpowers init"
  echo ""
  echo "  此步骤完全可选，不影响规范引擎的核心功能。"
  echo ""
else
  echo -e "  ${YELLOW}可选增强: Superpowers${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  Superpowers 是 AI 编程助手的增强能力集，提供额外的"
  echo "  编程辅助功能。与规范引擎协同工作（完全可选）。"
  echo ""
  echo "  如需安装:"
  echo "    npm install -g superpowers"
  echo "    superpowers init"
  echo ""
fi

# gstack — GStack 开发工具集
GSTACK_INSTALLED=false
if command -v gstack &>/dev/null || [ -d "$HOME/.gstack" ]; then
  GSTACK_INSTALLED=true
fi

if [ -d "$PROJECT_ROOT/.gstack" ]; then
  info "检测到已有 gstack 配置 (.gstack/)"
elif $GSTACK_INSTALLED; then
  echo -e "  ${CYAN}推荐增强: gstack${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  检测到已安装 gstack（GStack 开发工具集）。"
  echo "  它提供浏览器自动化、设计审查、性能基准等开发工具，"
  echo "  与规范引擎协同工作。"
  echo ""
  echo "  运行以下命令在项目根目录初始化:"
  echo "    gstack init"
  echo ""
  echo "  此步骤完全可选，不影响规范引擎的核心功能。"
  echo ""
else
  echo -e "  ${YELLOW}可选增强: gstack${NC}"
  echo "  ─────────────────────────────────────────"
  echo "  gstack 是 GStack 开发工具集，提供浏览器自动化、"
  echo "  设计审查、性能基准等开发工具。与规范引擎协同工作"
  echo "  （完全可选）。"
  echo ""
  echo "  如需安装:"
  echo "    npm install -g gstack"
  echo "    gstack init"
  echo ""
fi

if $DRY_RUN; then
  echo -e "  ${YELLOW}（dry-run 模式，以上文件未实际写入）${NC}"
  echo ""
fi
