# 安全基线规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-LS(日志规范)、UNI-EH(异常处理规范) |
| 互斥规范 | 无 |

---

## 规范 UNI-SB-001: 输入验证

**规则:**

1. 所有外部输入(请求参数、请求头、文件上传)MUST 在服务端进行验证,禁止信任客户端。
2. 验证策略 MUST 优先使用白名单(允许的字符/格式),而非黑名单(禁止的字符/格式)。
3. 字符串输入 MUST 限制最大长度,数值输入 MUST 限制范围。
4. 文件上传 MUST 校验 MIME 类型、文件大小和文件扩展名。
5. 验证失败 MUST 返回明确的错误信息,但 MUST 不泄露内部实现细节。

**示例:**

```java
// Java Bean Validation
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度须在 2-32 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅允许字母、数字和下划线")
    private String userName;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128)
    private String email;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄须大于 0")
    @Max(value = 200, message = "年龄须小于 200")
    private Integer age;
}
```

```typescript
// TypeScript: zod validation
import { z } from 'zod';

const createUserSchema = z.object({
  userName: z.string()
    .min(2, '用户名至少 2 个字符')
    .max(32, '用户名最多 32 个字符')
    .regex(/^[a-zA-Z0-9_]+$/, '用户名仅允许字母、数字和下划线'),
  email: z.string().email('邮箱格式不正确').max(128),
  age: z.number().int().min(1).max(200),
});
```

---

## 规范 UNI-SB-002: SQL 注入防护

**规则:**

1. 数据库操作 MUST 使用参数化查询(Prepared Statement)或 ORM 框架,禁止字符串拼接 SQL。
2. 动态查询条件 SHOULD 使用 ORM 提供的查询构造器(Query Builder)。
3. 批量操作 MUST 使用 ORM 提供的批量方法,禁止手动拼接 `IN` 子句。
4. 存储过程调用 MUST 使用参数绑定。
5. 数据库账户 MUST 遵循最小权限原则,应用账户 SHOULD 仅拥有 DML 权限,禁止 DDL 权限。

**示例:**

```java
// BAD: 字符串拼接 — SQL 注入风险!
String sql = "SELECT * FROM users WHERE user_name = '" + userName + "'";
statement.executeQuery(sql);

// GOOD: 参数化查询
String sql = "SELECT * FROM users WHERE user_name = ?";
PreparedStatement ps = connection.prepareStatement(sql);
ps.setString(1, userName);
ResultSet rs = ps.executeQuery();

// GOOD: ORM/Query Builder (MyBatis)
@Select("SELECT * FROM users WHERE user_name = #{userName}")
User findByUserName(@Param("userName") String userName);
```

```typescript
// BAD: 字符串拼接
const sql = `SELECT * FROM users WHERE user_name = '${userName}'`;

// GOOD: 参数化查询 (Node.js pg)
const result = await pool.query(
  'SELECT * FROM users WHERE user_name = $1',
  [userName]
);
```

---

## 规范 UNI-SB-003: XSS 防护

**规则:**

1. 所有动态内容输出到 HTML 时 MUST 进行编码(HTML Entity Encoding)。
2. 前端框架(React/Vue)默认转义输出 MUST 保留,禁止使用 `dangerouslySetInnerHTML` 或 `v-html` 渲染未经净化的内容。
3. 富文本输入 MUST 使用服务端白名单净化库(如 DOMPurify、OWASP Java HTML Sanitizer)。
4. HTTP 响应 MUST 设置 Content Security Policy(CSP)头。
5. Cookie MUST 设置 `HttpOnly` 标志(前端无需读取的 Cookie)。

**示例:**

```java
// Java: OWASP HTML Sanitizer
PolicyFactory policy = new PolicyFactory()
    .with(SanitizerPolicy.BLOCKS)
    .with(SanitizerPolicy.FORMATTING)
    .with(SanitizerPolicy.LINKS);

String safeHtml = policy.sanitize(userInput);
```

```typescript
// React: 默认安全
function UserComment({ text }: { text: string }) {
  // GOOD: React 自动转义
  return <p>{text}</p>;

  // BAD: 直接渲染未净化的 HTML
  // return <div dangerouslySetInnerHTML={{ __html: text }} />;
}

// 如需渲染富文本,使用 DOMPurify
import DOMPurify from 'dompurify';
function RichContent({ html }: { html: string }) {
  const safeHtml = DOMPurify.sanitize(html);
  return <div dangerouslySetInnerHTML={{ __html: safeHtml }} />;
}
```

```
# CSP 响应头
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;
```

---

## 规范 UNI-SB-004: CSRF 防护

**规则:**

1. 有状态 Web 应用 MUST 实施 CSRF 防护(CSRF Token 或 SameSite Cookie)。
2. CSRF Token MUST 在每次会话开始时生成,MUST 不可预测。
3. Cookie MUST 设置 `SameSite` 属性为 `Strict` 或 `Lax`。
4. 纯 API 服务(使用 Bearer Token 认证)MAY 不实施 CSRF 防护,但 MUST 确保 Token 不通过 Cookie 传递。

**示例:**

```java
// Spring Security CSRF 配置
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 有状态 Web 应用:启用 CSRF
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            // 纯 API 服务:禁用 CSRF,使用 Bearer Token
            // .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }
}
```

```
# Cookie 安全属性
Set-Cookie: sessionId=xxx; Path=/; HttpOnly; Secure; SameSite=Strict
```

---

## 规范 UNI-SB-005: 认证与授权

**规则:**

1. 认证令牌 SHOULD 使用 JWT(JSON Web Token),MUST 设置合理的过期时间。
2. JWT MUST 使用非对称加密(RS256)或高强度对称加密(HS256 + 密钥 ≥ 256 位)。
3. JWT MUST 包含标准声明:`sub`(用户标识)、`iat`(签发时间)、`exp`(过期时间)、`iss`(签发方)。
4. 权限控制 MUST 采用 RBAC(基于角色的访问控制)或 ABAC(基于属性的访问控制)。
5. 每个 API 端点 MUST 标注所需权限,无权限注解的端点 MUST 默认拒绝访问。
6. 密码存储 MUST 使用 bcrypt、scrypt 或 Argon2 算法,禁止 MD5/SHA 等可逆哈希。

**示例:**

```java
// Spring Security 权限控制
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) { }

    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping
    public ApiResponse<PageResult<UserResponse>> listUsers() { }

    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) { }
}
```

```java
// 密码加密
@Service
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
```

```typescript
// Node.js: JWT 签发与验证
import jwt from 'jsonwebtoken';

const PRIVATE_KEY = process.env.JWT_PRIVATE_KEY!;

function generateToken(userId: string, roles: string[]): string {
  return jwt.sign(
    { sub: userId, roles, iss: '{{service_name}}' },
    PRIVATE_KEY,
    { algorithm: 'RS256', expiresIn: '2h' }
  );
}

function verifyToken(token: string): jwt.JwtPayload {
  return jwt.verify(token, PUBLIC_KEY, { algorithms: ['RS256'] }) as jwt.JwtPayload;
}
```

---

## 规范 UNI-SB-006: 敏感数据处理

**规则:**

1. 敏感数据(密码、Token、身份证号、银行卡号)MUST 在存储时加密,禁止明文存储。
2. 配置项中的敏感信息(数据库密码、API Key)MUST 通过环境变量或密钥管理服务注入,禁止硬编码。
3. 日志输出 MUST 对敏感字段进行脱敏处理(详见 UNI-LS-004)。
4. 敏感接口响应 MUST 对部分字段进行掩码处理(如邮箱显示为 `a***@example.com`)。
5. 敏感数据传输 MUST 使用 TLS 加密,禁止明文 HTTP 传输。
6. `.gitignore` MUST 包含所有配置文件、环境文件和密钥文件,禁止将敏感文件提交到代码仓库。

**示例:**

```java
// 配置脱敏 — 使用环境变量
@Value("${database.password}")
private String dbPassword;

// 响应掩码
public class UserResponse {
    @JsonSerialize(using = EmailMaskSerializer.class)
    private String email;    // 输出: a***@example.com

    @JsonSerialize(using = PhoneMaskSerializer.class)
    private String phone;    // 输出: 138****1234
}
```

```yaml
# application.yml — 通过环境变量注入
spring:
  datasource:
    password: ${DB_PASSWORD}
  redis:
    password: ${REDIS_PASSWORD}

jwt:
  private-key: ${JWT_PRIVATE_KEY}
```

```bash
# .env — 禁止提交到 Git
DB_PASSWORD=my_secure_password
REDIS_PASSWORD=my_redis_password
JWT_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----...
```
