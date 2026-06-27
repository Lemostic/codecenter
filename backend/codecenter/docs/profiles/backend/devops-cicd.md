# DevOps 与 CI/CD 规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | backend |
| 引入条件 | `fingerprint.profiles contains 'devops-cicd'` |
| 适用架构 | 后端服务（Spring Boot 生态为主）+ Docker/K8s 部署 |
| 依赖规范 | `universal/git-workflow.md`、`universal/security-baseline.md` |
| 互斥规范 | 无（CI/CD 是通用工程实践） |

> 本包是 L1 后端服务专属，定义 CI/CD 流水线设计、Docker 容器化、K8s 部署、监控告警的标准。
> 配套 `universal/git-workflow.md`（分支策略与提交规范）使用。

---

## 一、CI 流水线设计

### 1.1 标准流水线阶段

**CICD-001** 后端项目 CI 流水线 MUST 包含以下 6 个阶段，缺一视为不完整。 [MUST]

| 阶段 | 工具示例 | 失败处理 |
|------|----------|----------|
| 1. checkout | `actions/checkout` | 终止 |
| 2. build | `mvn package` / `gradle build` | 终止 |
| 3. unit-test | `mvn test` | 终止 |
| 4. lint | `mvn checkstyle:check` | 警告（不阻断） |
| 5. integration-test | Testcontainers + 真实 DB | 终止 |
| 6. package | `docker build` / `mvn package` | 终止 |

**CICD-002** CI 流水线任意阶段失败 MUST 阻断合并到主分支。 [MUST]

**CICD-003** 流水线总时长 SHOULD 控制在 10 分钟内，超过 MUST 拆分并行任务或缓存依赖。 [SHOULD]

### 1.2 GitHub Actions 后端示例

```yaml
# .github/workflows/backend-ci.yml
name: Backend CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports: [5432:5432]
        options: --health-cmd pg_isready --health-interval 10s
      redis:
        image: redis:7-alpine
        ports: [6379:6379]
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17', cache: 'maven' }
      - name: Build & Unit Test
        run: mvn -B verify -DskipITs
      - name: Integration Test
        run: mvn -B verify -Dtest=**/*IT
      - name: Package
        run: mvn -B package -DskipTests
      - name: Build Docker image
        run: docker build -t myapp:${{ github.sha }} .
      - name: Push to Registry
        if: github.ref == 'refs/heads/main'
        env:
          DOCKER_USERNAME: ${{ secrets.DOCKER_USERNAME }}
          DOCKER_PASSWORD: ${{ secrets.DOCKER_PASSWORD }}
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push myapp:${{ github.sha }}
```

---

## 二、Docker 容器化

### 2.1 后端 Dockerfile 标准模板

**CICD-010** 后端服务 MUST 使用多阶段构建（builder + runtime），最终镜像 MUST 基于 `eclipse-temurin:17-jre-alpine`（或同等 JRE 基础镜像），禁止包含 JDK。 [MUST]

**CICD-011** 镜像层 MUST 优先复制依赖描述文件（`pom.xml` / `package.json`）并执行依赖下载，再复制源码层。利用 Docker 缓存。 [MUST]

**CICD-012** 运行时镜像 MUST 以非 root 用户启动（`USER appuser` 或 `USER 1001`）。 [MUST]

```dockerfile
# Dockerfile.backend
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appuser && adduser -S appuser -G appuser
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
COPY application.yml /app/config/

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 2.2 Docker Compose

**CICD-020** Docker Compose MUST 仅用于本地开发与测试环境，禁止用于生产部署。 [MUST]

**CICD-021** Compose 文件 MUST 显式声明所有依赖服务（DB / Cache / MQ），禁止依赖隐式网络发现。 [MUST]

**CICD-022** 数据库与缓存 MUST 配置数据卷（`volumes: postgres_data:/var/lib/postgresql/data`），禁止容器内存储生产数据。 [MUST]

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DATABASE_URL=${DATABASE_URL}
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

  db:
    image: postgres:15
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

---

## 三、K8s 部署

**CICD-030** K8s Deployment MUST 配置 `livenessProbe` 与 `readinessProbe`，并指向 `/actuator/health`（或等价健康检查端点）。 [MUST]

**CICD-031** K8s 资源请求与限制 MUST 显式声明（`resources.requests` 与 `resources.limits`），禁止依赖默认值。 [MUST]

**CICD-032** 镜像标签 MUST 包含 Git SHA 或等价不可变标识，禁止使用 `latest` 标签部署到生产。 [MUST]

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
        - name: myapp
          image: myapp:${{ github.sha }}  # 必须包含不可变标签
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
```

---

## 四、监控与告警

**CICD-040** 服务 MUST 暴露 `/actuator/health` 端点，K8s liveness/readiness 探针 MUST 引用此端点。 [MUST]

**CICD-041** 服务 MUST 输出结构化日志（JSON 格式），便于日志聚合系统（Loki/ELK）解析。详见 `universal/logging-standards.md`。 [MUST]

**CICD-042** 关键告警 MUST 配置 4 个黄金信号：请求速率、错误率、P99 延迟、饱和度（CPU/内存/队列）。缺一视为不完整。 [MUST]

### 4.1 监控指标（Prometheus 导出）

```yaml
# 关键告警（PromQL 示例）
groups:
  - name: app-cicd
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_requests_total{status=~"5.."}[5m])) /
          sum(rate(http_requests_total[5m])) > 0.01
        for: 5m
        labels: { severity: critical }
        annotations:
          summary: "错误率超过 1% 持续 5 分钟"

      - alert: HighP99Latency
        expr: |
          histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket[5m])) by (le)) > 1
        for: 5m
        labels: { severity: warning }
        annotations:
          summary: "P99 延迟超过 1 秒"

      - alert: HighCPUSaturation
        expr: |
          1 - avg by (pod) (rate(node_cpu_seconds_total{mode="idle"}[5m])) > 0.8
        for: 10m
        labels: { severity: warning }
```

---

## 五、检查清单

### 5.1 提交前 CI 配置检查

- [ ] CI 配置文件（`.github/workflows/*.yml` 或 `.gitlab-ci.yml`）已提交
- [ ] CI 流水线覆盖构建 + 单测 + 集成测试 + 打包
- [ ] CI 失败 MUST 阻断合并
- [ ] 流水线缓存依赖（Maven `~/.m2` / npm `node_modules`）
- [ ] 流水线触发条件正确（push、PR、tag）

### 5.2 镜像构建检查

- [ ] Dockerfile 使用多阶段构建
- [ ] 运行时镜像不包含构建工具（JDK、Maven、Node）
- [ ] 镜像以非 root 用户启动
- [ ] 镜像标签不包含 `latest`
- [ ] 镜像包含健康检查端点

### 5.3 K8s 部署检查

- [ ] Deployment 配置 livenessProbe + readinessProbe
- [ ] 资源 requests + limits 显式声明
- [ ] 镜像标签包含 Git SHA
- [ ] 副本数 ≥ 2（高可用）
- [ ] 配置（`ConfigMap` / `Secret`）通过 K8s 资源管理，不硬编码

### 5.4 监控告警检查

- [ ] 4 个黄金信号（速率/错误率/P99/饱和度）已配置
- [ ] 告警有明确的 `for` 持续时间（避免抖动误报）
- [ ] 关键告警设置了 oncall 通知（PagerDuty / 飞书 / 钉钉）
- [ ] 日志结构化输出（JSON + traceId）

---

## 六、与现有规范的关系

| 规范 | 关系 |
|------|------|
| `universal/git-workflow.md` | 互补——后者是分支策略与提交规范，本包是 CI/CD 流水线与部署实现 |
| `universal/security-baseline.md` | 互补——后者是安全基线，本包第 4 节镜像非 root 用户是落地 |
| `universal/logging-standards.md` | 互补——后者是日志规范，本包第 4 节要求结构化日志是落地 |
| `profiles/backend/spring-boot-base.md` | 互补——本包第 2-3 节的 Docker 模板专用于 Spring Boot 服务 |
| `profiles/backend/testing-jvm.md` | 互补——CI 阶段 5 集成测试与本包 testing-jvm 规范配合 |

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-19 | 初版：从 V2 backend/09-DevOps与CI-CD §2-§5 抽取（14 条规则） |

---

*本包只覆盖 CI/CD 流水线 + 镜像 + 部署 + 监控。具体业务监控指标（业务 QPS、SLA 错误率等）见各业务模块的 L1 包。*
