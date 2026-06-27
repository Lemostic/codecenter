# 安全实践规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | backend |
| 引入条件 | `fingerprint.profiles contains 'security-practices'` |
| 适用架构 | 全部后端服务（Web API 安全基线） |
| 依赖规范 | `universal/security-baseline.md`、`profiles/backend/api-design.md` |
| 互斥规范 | 无 |

> 本包是 L1 后端服务通用基线，定义 XSS/CSRF/SQL 注入防护、JWT 认证、敏感数据处理、安全审计日志等具体落地实践。
> 配套 `universal/security-baseline.md`（最低安全要求）使用，本规范提供详细实现。

---

## 一、注入攻击防护

### 1.1 SQL 注入防护

**SEC-001** 所有 SQL MUST 使用参数化查询（PreparedStatement / Repository 方法名查询），禁止字符串拼接。 [MUST]

```java
// ✅ 正确：参数化查询
@Query("SELECT u FROM User u WHERE u.username = :username AND u.status = :status")
List<User> findByUsernameAndStatus(@Param("username") String username,
                                    @Param("status") String status);

// ❌ 错误：字符串拼接
@Query("SELECT u FROM User u WHERE u.username = '" + username + "'")
List<User> findByUsername(String username);
```

**SEC-002** 动态 SQL 拼接场景（如多条件查询）MUST 使用白名单字段映射，禁止直接将外部输入拼接到 SQL。 [MUST]

```java
// ✅ 正确：使用 CriteriaBuilder / QueryDSL
public List<User> search(UserQuery query) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<User> cq = cb.createQuery(User.class);
    Root<User> root = cq.from(User.class);

    List<Predicate> predicates = new ArrayList<>();
    if (query.getUsername() != null) {
        predicates.add(cb.equal(root.get("username"), query.getUsername()));
    }
    if (query.getStatus() != null) {
        predicates.add(cb.equal(root.get("status"), query.getStatus()));
    }
    cq.where(predicates.toArray(new Predicate[0]));
    return em.createQuery(cq).getResultList();
}
```

**SEC-003** 动态表名 / 排序字段场景 MUST 使用白名单校验（`{allowedSortFields.contains(input)}`），禁止直接拼接到 ORDER BY。 [MUST]

### 1.2 XSS 防护

**SEC-010** 所有用户输入在渲染到 HTML 前 MUST 经过转义或净化。 [MUST]

```java
// 使用 OWASP Java Encoder
import org.owasp.encoder.Encode;

String safe = Encode.forHtml(userInput);
String safeAttr = Encode.forHtmlAttribute(userInput);
String safeJs = Encode.forJavaScript(userInput);
```

**SEC-011** 前端 MUST 设置响应头 `Content-Security-Policy: default-src 'self'`，禁止 inline script / eval。 [MUST]

**SEC-012** 前后端 MUST 对所有用户输入字段实施 XSS 净化（前端：DOMPurify；后端：OWASP Encoder）。 [MUST]

### 1.3 CSRF 防护

**SEC-020** 状态变更接口（POST/PUT/PATCH/DELETE）MUST 启用 CSRF Token 校验。 [MUST]

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/v1/auth/**")  // 登录接口豁免
            );
        return http.build();
    }
}
```

**SEC-021** CSRF Token MUST 通过 HttpOnly Cookie + 请求头 `X-CSRF-Token` 双重校验。 [MUST]

---

## 二、认证与授权

### 2.1 JWT Token 设计

```java
@Component
public class JwtTokenProvider {
    private final String secret = env.getProperty("jwt.secret");  // 从环境变量读取
    private final long expirationMs = 3600000;  // 1 小时

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .claim("userId", userDetails.getUserId())
            .claim("roles", userDetails.getAuthorities())
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }
}
```

**SEC-030** JWT 签名密钥 MUST 从环境变量或密钥管理服务读取，禁止硬编码或提交到代码仓库。 [MUST]

**SEC-031** Access Token 有效期 SHOULD ≤ 1 小时，Refresh Token 有效期 ≤ 7 天，过期后 MUST 重新登录或 refresh。 [SHOULD]

**SEC-032** JWT Payload MUST 包含最小必要信息（userId、roles、exp），禁止存放敏感数据（密码、身份证等）。 [MUST]

### 2.2 Token 刷新策略

| Token 类型 | 有效期 | 用途 | 存储位置 |
|------------|--------|------|----------|
| Access Token | 1 小时 | API 调用 | 内存 / SessionStorage |
| Refresh Token | 7 天 | 刷新 Access Token | HttpOnly Cookie |
| 长期 Token | ≤ 30 天 | 设备记住登录 | Secure Cookie + 设备绑定 |

**SEC-033** Refresh Token MUST 走 HttpOnly + Secure + SameSite=Strict Cookie，禁止通过 localStorage 存储。 [MUST]

### 2.3 RBAC 权限模型

```java
@PreAuthorize("hasRole('ADMIN') or @permissionService.hasPermission(authentication, 'user:create')")
@PostMapping("/users")
public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest request) {
    // ...
}

@PreAuthorize("@permissionService.hasPermission(authentication, 'order:cancel', #orderId)")
@PostMapping("/orders/{orderId}/cancel")
public ApiResponse<Void> cancelOrder(@PathVariable String orderId) {
    // ...
}
```

**SEC-040** 接口级权限 MUST 通过 `@PreAuthorize` 注解声明，禁止仅在 Controller 内 if 判断后抛异常。 [MUST]

**SEC-041** 权限字符串 MUST 采用 `{资源}:{操作}` 格式（如 `user:create`、`order:cancel`），禁止直接用角色名。 [MUST]

---

## 三、密码与敏感数据处理

### 3.1 密码存储

**SEC-050** 用户密码 MUST 使用 BCrypt / Argon2 哈希存储，禁止使用 MD5 / SHA1 / 简单加盐哈希。 [MUST]

```java
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // strength=12, ~250ms/hash
    }
}

@Service
public class UserService {
    public void register(CreateUserRequest request) {
        String hashed = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(hashed);
        userRepository.save(user);
    }
}
```

**SEC-051** BCrypt strength SHOULD ≥ 12，单次哈希耗时控制在 200-500ms。 [SHOULD]

**SEC-052** 密码校验 MUST 使用 `passwordEncoder.matches(raw, hashed)`，禁止自己实现比较逻辑。 [MUST]

### 3.2 敏感字段加密

| 字段类型 | 加密方式 | 算法 |
|----------|----------|------|
| 身份证号 | SM4 / AES-256 | 对称加密 |
| 手机号 | SM4 / AES-256 | 对称加密 |
| 银行卡 | SM4 / AES-256 | 对称加密 |
| 邮箱 | 哈希 + 加盐 | SHA-256 |
| 密码 | BCrypt | 单向哈希 |

**SEC-060** 敏感字段加密 MUST 走统一加密服务（`mdm-common-sdk-security`），禁止业务代码自行加解密。 [MUST]

**SEC-061** 加密密钥 MUST 走密钥管理服务（KMS / Vault），禁止硬编码或写在配置文件中明文。 [MUST]

---

## 四、请求安全

### 4.1 HTTPS 强制

**SEC-070** 生产环境 MUST 强制 HTTPS，所有 HTTP 请求 MUST 301 跳转到 HTTPS。 [MUST]

```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
```

### 4.2 请求大小限制

**SEC-071** 单次请求体大小 MUST ≤ 5MB（默认），文件上传接口单独配置且 ≤ 100MB。 [MUST]

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
  codec:
    max-in-memory-size: 5MB
```

### 4.3 限流与防刷

**SEC-080** 高频接口（登录、注册、验证码）MUST 配置限流（按 IP + 用户 ID 滑动窗口）。 [MUST]

```java
@RateLimit(key = "login", limit = 5, period = 60, unit = TimeUnit.SECONDS)
@PostMapping("/auth/login")
public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
    // ...
}
```

**SEC-081** 验证码错误次数超过 5 次 MUST 锁定账号 30 分钟。 [MUST]

---

## 五、安全响应头

**SEC-090** 所有 HTTP 响应 MUST 设置以下安全头：

```yaml
# application.yml
server:
  servlet:
    session:
      cookie:
        http-only: true
        secure: true
        same-site: strict

# Spring Security 配置
http.headers()
    .contentSecurityPolicy("default-src 'self'")
    .httpStrictTransportSecurity("max-age=31536000; includeSubDomains")
    .frameOptions().deny()
    .xssProtection().block(true)
    .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
```

| 响应头 | 值 | 作用 |
|--------|-----|------|
| Content-Security-Policy | `default-src 'self'` | 防止 XSS |
| Strict-Transport-Security | `max-age=31536000` | 强制 HTTPS |
| X-Frame-Options | `DENY` | 防止点击劫持 |
| X-Content-Type-Options | `nosniff` | 防止 MIME 类型嗅探 |
| Referrer-Policy | `strict-origin-when-cross-origin` | 控制 Referer |

---

## 六、安全审计日志

### 6.1 必记录事件

| 事件类型 | 必记录字段 | 示例 |
|----------|------------|------|
| 登录成功 | userId, ip, userAgent, timestamp | "用户 10001 从 192.168.1.1 登录成功" |
| 登录失败 | username, ip, reason | "用户 admin 从 192.168.1.1 登录失败：密码错误" |
| 权限拒绝 | userId, resource, action, ip | "用户 10001 在 192.168.1.1 尝试访问 /users 被拒" |
| 敏感操作 | userId, operation, targetId | "用户 10001 删除订单 30001" |
| Token 签发 | userId, tokenType, expireAt | "用户 10001 签发 Access Token，过期 14:30" |

**SEC-100** 上述事件 MUST 写入审计日志表（`audit_log`），保留期 ≥ 180 天。 [MUST]

**SEC-101** 审计日志 MUST 独立于业务日志，存放在只读或限制写入权限的存储中。 [MUST]

### 6.2 审计日志结构

```sql
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    target_id VARCHAR(64),
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    request_id VARCHAR(64),
    detail JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_event_time (event_type, created_at)
) COMMENT '安全审计日志';
```

---

## 七、依赖安全

### 7.1 依赖漏洞扫描

**SEC-110** CI 流水线 MUST 集成 OWASP Dependency-Check / Snyk / Trivy 等依赖扫描工具。 [MUST]

```yaml
# GitHub Actions
- name: OWASP Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  with:
    project: 'myproject'
    format: 'HTML'
    args: >
      --failOnCVSS 7
      --enableRetired
```

**SEC-111** 高危漏洞（CVSS ≥ 7.0）MUST 在 7 天内修复或升级，中危漏洞 30 天内修复。 [MUST]

### 7.2 镜像安全扫描

**SEC-120** Docker 镜像 MUST 通过 Trivy 扫描，CRITICAL/HIGH 漏洞 MUST 在合并前修复。 [MUST]

---

## 八、检查清单

### 8.1 注入防护检查

- [ ] SQL 是否使用参数化查询
- [ ] 动态 SQL 是否走白名单
- [ ] 用户输入是否 XSS 转义
- [ ] 状态变更接口是否 CSRF 保护

### 8.2 认证授权检查

- [ ] JWT 密钥是否从环境变量读取
- [ ] Token 有效期是否合理
- [ ] 接口权限是否用 `@PreAuthorize`
- [ ] 权限字符串是否符合 `{资源}:{操作}`

### 8.3 敏感数据检查

- [ ] 密码是否 BCrypt 哈希
- [ ] 敏感字段是否走统一加密服务
- [ ] 加密密钥是否从 KMS 读取
- [ ] 敏感字段日志是否脱敏

### 8.4 请求安全检查

- [ ] 是否强制 HTTPS
- [ ] 请求体大小是否限制
- [ ] 高频接口是否限流
- [ ] 是否设置安全响应头

### 8.5 审计与合规检查

- [ ] 是否记录登录/权限/敏感操作
- [ ] 审计日志保留期是否 ≥ 180 天
- [ ] 依赖漏洞扫描是否接入 CI
- [ ] Docker 镜像扫描是否接入 CI

---

*本规范与 `universal/security-baseline.md`（最低安全基线）+ `profiles/backend/error-handling.md`（异常脱敏）+ `profiles/backend/devops-cicd.md`（CI 安全扫描）协同使用。*