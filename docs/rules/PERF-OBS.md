# PERF-OBS 规则元数据

## 1. 说明

本文件涵盖 PERF、OBS 两个一级分类，共 5 条规则：

- SRS-PERF-01-001：循环内远程调用
- SRS-PERF-02-001：大集合全量加载/分页缺失
- SRS-OBS-01-001：日志占位符拼接字符串
- SRS-OBS-02-001：Actuator 端点未授权暴露
- SRS-OBS-03-001：日志或事务切点表达式过宽

## 2. 规则 YAML

### SRS-PERF-01-001 循环内远程调用

```yaml
rule_id: SRS-PERF-01-001
name: "循环内远程调用"
description: "在方法体 AST/文本启发式扫描中识别 for、while、do 循环体及其嵌套块内的远程调用迹象，包括 RestTemplate、WebClient、Feign、@FeignClient 方法调用和常见 HTTP 客户端调用；结合循环边界、调用深度与是否存在批量接口标记疑似循环内远程调用。边界：不跨方法追踪调用链，不解析动态代理与反射，不确认实际网络开销，仅作为性能风险提示。"
category_l1: PERF
category_l2: "01"
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: HEURISTIC
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-web"]
  exclude: []
message: "检测到循环内可能存在远程调用，可能导致延迟放大、连接耗尽与吞吐下降。"
remediation: "将远程调用移出循环，改用批量接口、缓存、并行聚合或先收集参数再批量请求。"
positive_example: "先收集循环所需参数，在循环外调用一次批量远程接口。"
negative_example: "在 for 或 while 循环体内直接调用远程 HTTP 服务或 Feign 客户端。"
false_positive_notes: "待 B 阶段评估可检测性；循环内调用可能为本地缓存、内存服务或已批量化封装，需人工确认。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "内部性能审查经验：循环内远程调用导致延迟放大"
    version: "1.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B6 初始规则元数据"
test_cases: []
coverage: "当前覆盖规则定义、检测策略与误报边界；测试用例与工具配置待后续阶段补充。"
```

### SRS-PERF-02-001 大集合全量加载/分页缺失

```yaml
rule_id: SRS-PERF-02-001
name: "大集合全量加载或分页缺失"
description: "通过 AST 识别数据访问方法返回 List、Collection、Stream 或数组且未传入分页参数（Pageable、Slice、limit、offset、分页对象）的查询调用，以及循环内逐条查询导致的全量加载迹象。边界：不推断运行时数据量，不分析 SQL 语义与索引，不覆盖原生 SQL 动态拼接和存储过程。"
category_l1: PERF
category_l2: "02"
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: AST
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-data"]
  exclude: []
message: "检测到可能的大集合全量加载或分页缺失，存在内存与数据库压力风险。"
remediation: "为查询增加分页参数或流式处理，限制返回条数，避免在循环中逐条查询。"
positive_example: "查询方法接收 Pageable 或 limit/offset 并只返回当前页数据。"
negative_example: "Repository 查询直接返回全表 List 且调用方未限制数据量。"
false_positive_notes: "若查询明确受限于小数据集、已由数据库优化或调用方保证数据量，可能误报；需结合上下文确认。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "内部性能审查经验：大集合全量加载导致内存与数据库压力"
    version: "1.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B6 初始规则元数据"
test_cases: []
coverage: "当前覆盖规则定义、检测策略与误报边界；测试用例与工具配置待后续阶段补充。"
```

### SRS-OBS-01-001 日志占位符拼接字符串

```yaml
rule_id: SRS-OBS-01-001
name: "日志占位符拼接字符串"
description: "通过 AST 识别 SLF4J 等日志门面调用中使用字符串拼接而非参数化占位符的日志语句，重点检测 debug、info、warn、error 等方法的 message 参数由加号拼接或 String.format 构造的情况。边界：不处理非 SLF4J 门面、自定义日志封装和条件日志的运行时求值差异。"
category_l1: OBS
category_l2: "01"
status: ACTIVE
severity: MINOR
confidence: HIGH
detection_method: AST
tool: PMD
tool_rule_id: "InvalidLogMessageFormat"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-boot"]
  exclude: []
message: "检测到日志消息使用字符串拼接，可能造成不必要的字符串构造与性能损耗。"
remediation: "使用日志门面占位符，例如将拼接参数改为 {} 占位符并按顺序传入参数。"
positive_example: "日志调用使用 {} 占位符并按顺序传入变量。"
negative_example: "日志调用使用加号拼接消息字符串或 String.format 构造消息。"
false_positive_notes: "日志参数本身已是格式化字符串或非 SLF4J 门面时可能误报；自定义日志封装需人工确认。"
sources:
  - type: TOOL_RULE
    ref: "PMD InvalidLogMessageFormat"
    version: "PMD 7.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B6 初始规则元数据"
test_cases: []
coverage: "当前覆盖规则定义、检测策略与误报边界；测试用例与工具配置待后续阶段补充。"
```

### SRS-OBS-02-001 Actuator 端点未授权暴露

```yaml
rule_id: SRS-OBS-02-001
name: "Actuator 端点未授权暴露"
description: "通过规则引擎扫描配置项 management.endpoints.web.exposure.include，识别包含星号通配或敏感端点且未配套授权限制的暴露配置。边界：不验证实际认证授权链，不解析环境变量与配置中心间接引用，不覆盖仅管理端口暴露的场景。"
category_l1: OBS
category_l2: "02"
status: ACTIVE
severity: CRITICAL
confidence: HIGH
detection_method: RULE_ENGINE
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-boot", "spring-boot-actuator"]
  exclude: []
message: "检测到 Actuator 端点可能未授权暴露，存在敏感信息泄露与运维接口滥用风险。"
remediation: "最小化 exposure.include 列表，排除敏感端点，并为 Actuator 配置认证授权或仅绑定管理端口。"
positive_example: "仅暴露 health、info 等非敏感端点，并配置安全访问控制。"
negative_example: "management.endpoints.web.exposure.include 配置为星号或包含 env、beans、heapdump 等敏感端点且无授权限制。"
false_positive_notes: "若端点仅在管理端口暴露、已由网络策略或 Spring Security 限制，可能误报；需结合运行时配置确认。"
sources:
  - type: OFFICIAL_SPEC
    ref: "Spring Boot Actuator Endpoints: Exposing Endpoints"
    version: "Spring Boot 3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B6 初始规则元数据"
test_cases: []
coverage: "当前覆盖规则定义、检测策略与误报边界；测试用例与工具配置待后续阶段补充。"
```

### SRS-OBS-03-001 日志或事务切点表达式过宽

```yaml
rule_id: SRS-OBS-03-001
name: "日志或事务切点表达式过宽"
description: "通过 AST 识别 @Pointcut 与 @Around、@Before、@After、@Transactional 等注解中的 execution 表达式过宽情况，例如匹配所有方法、所有包或仅用通配符覆盖大范围类型。边界：不评估运行时织入影响，不分析 XML AOP 配置，不判断业务是否有意使用宽切点。"
category_l1: OBS
category_l2: "03"
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: AST
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-aop"]
  exclude: []
message: "检测到切点表达式过宽，可能导致日志、事务或性能切面意外覆盖大量方法。"
remediation: "收窄切点表达式到明确包、类或注解，避免使用 execution(* *(..)) 等全匹配模式。"
positive_example: "切点表达式限定到特定包、特定注解或明确的方法签名。"
negative_example: "切点表达式匹配所有方法或所有包，导致日志或事务切面覆盖范围过大。"
false_positive_notes: "若宽切点有意用于审计、度量或事务传播且已评估影响，可能误报；需结合切面目的确认。"
sources:
  - type: FRAMEWORK_DOC
    ref: "Spring Framework AOP @Pointcut"
    version: "Spring Framework 6.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B6 初始规则元数据"
test_cases: []
coverage: "当前覆盖规则定义、检测策略与误报边界；测试用例与工具配置待后续阶段补充。"
```