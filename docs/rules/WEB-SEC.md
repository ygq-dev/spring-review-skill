# WEB-SEC 规则元数据

## 说明
本文件涵盖 WEB、SEC 两个分类，共 10 条规则。其中 WEB 5 条，SEC 5 条。所有规则状态为 ACTIVE，规则版本 1.0.0。

## SRS-WEB-01-001 Controller 中保存请求态可变字段

```yaml
rule_id: "SRS-WEB-01-001"
name: "Controller 中保存请求态可变字段"
description: "检测 Spring MVC Controller 单例中保存请求态可变实例字段，或请求处理方法写入非线程安全字段。重点识别非 static final 的可变字段、请求处理方法内赋值、以及依赖请求数据的字段。"
category_l1: "WEB"
category_l2: "01"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "HIGH"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
  framework:
    - spring-mvc
  exclude: []
message: "Controller 中不应保存请求态可变字段，避免并发请求数据串扰。"
remediation: "将请求态数据保存在方法局部变量、方法参数或请求作用域中；Controller 保持无状态。"
positive_example: "Controller 仅使用方法参数和局部变量处理请求，不定义可变实例字段。"
negative_example: "Controller 定义可变实例字段并在请求处理方法中写入当前请求数据。"
false_positive_notes: "若字段为 static final 常量、不可变配置或由容器管理的只读依赖，可忽略。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Reference: Web MVC"
    version: "6.2.x"
coverage: "扫描 @Controller/@RestController 类中的实例字段、赋值点及请求映射方法调用链。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-WEB-02-001 请求参数缺少 @Valid/@Validated

```yaml
rule_id: "SRS-WEB-02-001"
name: "请求参数缺少 @Valid/@Validated"
description: "检测 Spring MVC 控制器请求参数、请求体或方法参数缺少 @Valid/@Validated 校验注解，或校验注解未生效的场景。"
category_l1: "WEB"
category_l2: "02"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
  framework:
    - spring-mvc
  exclude: []
message: "请求参数应启用 Bean Validation 校验。"
remediation: "在需要校验的 @RequestBody、@ModelAttribute 或方法参数上添加 @Valid 或 @Validated，并处理校验错误。"
positive_example: "控制器对请求体或模型属性使用 @Valid 或 @Validated 触发校验。"
negative_example: "控制器直接接收请求体或参数，未添加任何校验注解。"
false_positive_notes: "若参数已在其他层显式校验，或为无需校验的基础类型，可忽略。"
sources:
  - type: "OFFICIAL_SPEC"
    ref: "Jakarta Bean Validation"
    version: "3.0"
  - type: "FRAMEWORK_DOC"
    ref: "Spring MVC Validation"
    version: "6.2.x"
coverage: "扫描控制器方法参数上的 @RequestBody、@ModelAttribute、@RequestParam 等及其校验注解。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-WEB-03-001 全局异常处理缺失或返回堆栈

```yaml
rule_id: "SRS-WEB-03-001"
name: "全局异常处理缺失或返回堆栈"
description: "使用规则引擎扫描 Spring MVC 应用，检测 @ControllerAdvice/@ExceptionHandler 缺失，或异常处理器向客户端返回堆栈、内部异常信息。"
category_l1: "WEB"
category_l2: "03"
status: "ACTIVE"
severity: "MAJOR"
confidence: "MEDIUM"
detection_method: "RULE_ENGINE"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
  framework:
    - spring-mvc
  exclude: []
message: "应提供全局异常处理，且不得向客户端返回堆栈或内部异常细节。"
remediation: "添加 @ControllerAdvice 和 @ExceptionHandler，统一映射错误响应，记录内部日志但不返回堆栈。"
positive_example: "存在全局异常处理器，返回稳定错误码和脱敏消息。"
negative_example: "无全局异常处理，或异常响应包含堆栈、类名、SQL 等内部信息。"
false_positive_notes: "若框架或网关已统一处理且返回体已脱敏，可忽略。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring MVC Exceptions"
    version: "6.2.x"
  - type: "OFFICIAL_SPEC"
    ref: "RFC 9457 Problem Details"
    version: "RFC 9457"
coverage: "扫描 @ControllerAdvice/@ExceptionHandler 定义及异常响应体构造。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-WEB-04-001 跨域配置过宽

```yaml
rule_id: "SRS-WEB-04-001"
name: "跨域配置过宽"
description: "检测 Spring Web CORS 配置中 allowedOrigins 使用通配源、allowedOriginPatterns 过宽、allowCredentials 为 true 且通配源等不安全组合。"
category_l1: "WEB"
category_l2: "04"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "HIGH"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
  framework:
    - spring-web
  exclude: []
message: "跨域配置不应过宽，尤其禁止通配源与凭证同时启用。"
remediation: "使用明确白名单源；如需凭证，配置具体源并限制方法、头、最大年龄。"
positive_example: "CORS 仅允许受信来源，且凭证与通配源不共存。"
negative_example: "CORS 允许所有来源，或允许所有来源同时携带凭证。"
false_positive_notes: "若通配配置仅用于本地开发且通过 profile 隔离，可降低等级。"
sources:
  - type: "OFFICIAL_SPEC"
    ref: "Fetch Living Standard: CORS"
    version: "2026"
  - type: "FRAMEWORK_DOC"
    ref: "Spring Web CORS"
    version: "6.2.x"
coverage: "扫描 @CrossOrigin、CorsConfiguration、WebMvcConfigurer#addCorsMappings 等配置。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-WEB-05-001 异步接口未处理超时/异常

```yaml
rule_id: "SRS-WEB-05-001"
name: "异步接口未处理超时/异常"
description: "检测 @Async、WebAsyncTask、DeferredResult、Callable 等异步接口缺少超时、异常回调、线程池隔离或错误处理。边界包括异步返回值、超时配置、AsyncUncaughtExceptionHandler 和 MVC 异步异常处理。"
category_l1: "WEB"
category_l2: "05"
status: "ACTIVE"
severity: "MAJOR"
confidence: "MEDIUM"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
  framework:
    - spring-web
  exclude: []
message: "异步接口应显式处理超时和异常，避免请求悬挂或异常丢失。"
remediation: "为异步任务设置超时，注册异常处理器，使用有界线程池，并在 DeferredResult/WebAsyncTask 中处理超时与错误。"
positive_example: "异步接口配置超时、异常回调和独立线程池。"
negative_example: "异步接口未设置超时，也未处理异常或拒绝策略。"
false_positive_notes: "若上游网关统一超时且异步逻辑无可失败操作，可忽略。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring MVC Async"
    version: "6.2.x"
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Task Execution"
    version: "6.2.x"
coverage: "扫描 @Async、WebAsyncTask、DeferredResult、Callable 及异步配置。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-SEC-01-001 硬编码密钥/密码/token

```yaml
rule_id: "SRS-SEC-01-001"
name: "硬编码密钥/密码/token"
description: "通过正则扫描源码、配置和资源文件，检测硬编码密码、密钥、token、API key 等敏感凭据。"
category_l1: "SEC"
category_l2: "01"
status: "ACTIVE"
severity: "BLOCKER"
confidence: "HIGH"
detection_method: "REGEX"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - all
  framework:
    - spring-boot
  exclude: []
message: "禁止硬编码密钥、密码或 token。"
remediation: "改用环境变量、密钥管理服务、Spring Config 加密或外部化配置，并轮换已泄露凭据。"
positive_example: "敏感凭据从环境变量或密钥管理服务读取。"
negative_example: "源码或配置中直接写入密码、token、API key。"
false_positive_notes: "测试固定假值、示例占位符或已明确标记的非敏感常量可忽略。"
sources:
  - type: "INTERNAL_EXPERIENCE"
    ref: "内部安全编码基线"
    version: "1.0"
  - type: "OFFICIAL_SPEC"
    ref: "OWASP ASVS 5.0 V6"
    version: "5.0"
coverage: "扫描 Java、YAML、properties、XML 等文件中的凭据模式。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-SEC-02-001 日志输出敏感信息

```yaml
rule_id: "SRS-SEC-02-001"
name: "日志输出敏感信息"
description: "通过正则扫描日志语句和日志配置，检测输出密码、token、身份证、银行卡、手机号等敏感信息，或记录完整请求体/响应体。"
category_l1: "SEC"
category_l2: "02"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "HIGH"
detection_method: "REGEX"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - all
  framework:
    - spring-boot
  exclude: []
message: "日志不得输出敏感信息或完整凭据。"
remediation: "脱敏后记录，使用日志白名单字段，避免打印请求体、响应体和认证头。"
positive_example: "日志仅记录脱敏标识或非敏感摘要。"
negative_example: "日志直接打印密码、token、完整请求体或认证头。"
false_positive_notes: "若变量名含敏感词但实际为脱敏值或测试假值，可忽略。"
sources:
  - type: "INTERNAL_EXPERIENCE"
    ref: "内部日志安全规范"
    version: "1.0"
  - type: "OFFICIAL_SPEC"
    ref: "OWASP ASVS 5.0 V7"
    version: "5.0"
coverage: "扫描日志调用、占位符参数和日志配置文件中的敏感字段。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-SEC-03-001 SQL 拼接

```yaml
rule_id: "SRS-SEC-03-001"
name: "SQL 拼接"
description: "检测通过字符串拼接、格式化或动态 SQL 构造 SQL 语句的代码，重点关注用户输入进入 SQL 片段的路径。"
category_l1: "SEC"
category_l2: "03"
status: "ACTIVE"
severity: "BLOCKER"
confidence: "HIGH"
detection_method: "AST"
tool: "PMD"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - data
  framework:
    - spring-boot
  exclude: []
message: "禁止拼接 SQL，应使用参数化查询或预编译语句。"
remediation: "使用 JdbcTemplate/MyBatis/JPQL 参数绑定，禁止将用户输入拼入 SQL 结构。"
positive_example: "SQL 使用占位符和参数绑定传入用户输入。"
negative_example: "使用字符串拼接将用户输入拼入 SELECT/WHERE 等 SQL 片段。"
false_positive_notes: "拼接常量表名/列名且值来自白名单枚举时需人工确认。"
sources:
  - type: "TOOL_RULE"
    ref: "PMD Best Practices"
    version: "7.x"
  - type: "OFFICIAL_SPEC"
    ref: "OWASP Top 10 2021 A03"
    version: "2021"
coverage: "扫描 SQL 字符串构造、Statement 执行和 MyBatis/JPA 动态查询。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-SEC-04-001 关闭 CSRF 且无豁免说明

```yaml
rule_id: "SRS-SEC-04-001"
name: "关闭 CSRF 且无豁免说明"
description: "检测 Spring Security 中 csrf().disable() 或等效关闭 CSRF 保护且缺少豁免说明、路径限制或无状态 API 依据的配置。"
category_l1: "SEC"
category_l2: "04"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "MEDIUM"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - security
  framework:
    - spring-security
  exclude: []
message: "关闭 CSRF 保护需有明确豁免依据和范围。"
remediation: "保持 CSRF 启用；确需关闭时仅限无状态 API，并添加注释、配置边界和评审记录。"
positive_example: "CSRF 保持启用，或仅对明确无状态接口关闭并记录理由。"
negative_example: "全局关闭 CSRF，且无注释、路径限制或风险说明。"
false_positive_notes: "纯无状态 token API 且不依赖 Cookie 认证时，关闭 CSRF 可能合理。"
sources:
  - type: "OFFICIAL_SPEC"
    ref: "OWASP ASVS 5.0 V4"
    version: "5.0"
  - type: "FRAMEWORK_DOC"
    ref: "Spring Security CSRF"
    version: "6.2.x"
coverage: "扫描 SecurityFilterChain、WebSecurityConfigurerAdapter 及 csrf 配置。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```

## SRS-SEC-05-001 不安全反序列化/SpEL 执行

```yaml
rule_id: "SRS-SEC-05-001"
name: "不安全反序列化/SpEL 执行"
description: "采用 HYBRID 检测：静态分析识别 ObjectInputStream、readObject、XStream、SnakeYAML 等反序列化入口，AST/规则引擎跟踪用户输入到 SpEL 表达式解析或反序列化调用，并结合 SpotBugs 规则告警。"
category_l1: "SEC"
category_l2: "05"
status: "ACTIVE"
severity: "BLOCKER"
confidence: "MEDIUM"
detection_method: "HYBRID"
tool: "SPOTBUGS"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - all
  framework:
    - spring-context
  exclude: []
message: "避免对不可信数据反序列化或执行 SpEL 表达式。"
remediation: "使用白名单类型、安全序列化格式；禁止将用户输入传入 SpEL 解析器，必要时启用 SimpleEvaluationContext。"
positive_example: "反序列化使用类型白名单，SpEL 不使用用户输入或使用受限上下文。"
negative_example: "对用户输入直接 ObjectInputStream.readObject，或将用户输入拼入 SpEL 表达式。"
false_positive_notes: "若数据来源完全可信且类型固定，仍需评审；测试代码可忽略。"
sources:
  - type: "CVE"
    ref: "CVE-2015-4852"
    version: "2015"
  - type: "OFFICIAL_SPEC"
    ref: "OWASP Deserialization Cheat Sheet"
    version: "2026"
  - type: "TOOL_RULE"
    ref: "SpotBugs OBJECT_DESERIALIZATION"
    version: "4.x"
coverage: "扫描反序列化 API、SpEL 解析调用及 SpotBugs 告警。"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本。"
test_cases: []
```