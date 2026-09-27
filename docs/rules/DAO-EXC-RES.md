# DAO、EXC、RES 规则元数据（B4）

本文件涵盖 DAO、EXC、RES 三个分类共 7 条规则，全部为只读审查规则元数据。

## SRS-DAO-01-001

```yaml
rule_id: SRS-DAO-01-001
name: "@Transactional 不得标注在 private/final 方法"
description: >-
  使用 AST 解析方法修饰符与 @Transactional 注解，识别 private 或 final 方法上的事务声明。
  结合 Spring 代理机制判断：private/final 方法无法被 CGLIB/JDK 代理拦截，事务切面不会生效。
category_l1: DAO
category_l2: "01"
status: ACTIVE
severity: BLOCKER
confidence: HIGH
detection_method: AST
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["data"]
  framework: ["spring-tx"]
  exclude: []
message: "检测到 @Transactional 标注在 private/final 方法上，Spring 事务代理无法拦截。"
remediation: "将事务方法改为 public，并通过 Spring 代理或独立 Bean 调用；必要时将事务边界上移到 public 方法。"
positive_example: "事务方法声明为 public，并通过 Spring 代理调用；private/final 方法不标注 @Transactional。"
negative_example: "在 private 或 final 方法上标注 @Transactional，并期望 Spring 事务代理拦截。"
false_positive_notes: "若项目使用 AspectJ 编译期/加载期织入而非 Spring 代理，需人工确认；若该方法由框架特殊调用，也应复核。"
sources:
  - type: OFFICIAL_SPEC
    ref: "Spring Framework Javadoc: org.springframework.transaction.annotation.Transactional"
    version: "6.1.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本，继承 B0 候选 CAND-011 并补充完整元数据。"
test_cases: []
coverage: "覆盖 DAO 层 @Transactional 方法可见性规则的静态 AST 检测；不覆盖 AspectJ 织入配置差异。"
```

## SRS-DAO-02-001

```yaml
rule_id: SRS-DAO-02-001
name: "避免同类自调用导致事务失效"
description: >-
  使用 AST 解析同类方法调用图，识别 this 自调用事务方法或未经过代理的同类调用。
  结合 Spring 代理失效检测方法判断：自调用不会经过事务代理，@Transactional 不会开启或加入事务。
category_l1: DAO
category_l2: "02"
status: ACTIVE
severity: CRITICAL
confidence: HIGH
detection_method: AST
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["data"]
  framework: ["spring-tx"]
  exclude: []
message: "检测到同类自调用事务方法，调用未经过 Spring 代理，事务可能失效。"
remediation: "通过注入自身代理、AopContext.currentProxy() 或拆分到独立 Bean 后调用事务方法。"
positive_example: "同类方法间通过代理对象或独立 Bean 调用事务方法。"
negative_example: "在同一个 Bean 内直接调用本类事务方法，导致事务切面不生效。"
false_positive_notes: "若通过 AopContext.currentProxy()、自注入代理或 AspectJ 织入调用，则可能不失效；需结合调用链确认。"
sources:
  - type: FRAMEWORK_DOC
    ref: "Spring Framework Reference: Transaction Management - Proxy self-invocation"
    version: "6.1.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本，继承 B0 候选 CAND-012 并补充完整元数据。"
test_cases: []
coverage: "覆盖同类自调用导致事务失效的 AST 调用图检测；不覆盖运行时代理实现差异。"
```

## SRS-DAO-03-001

```yaml
rule_id: SRS-DAO-03-001
name: "只读查询应设置 readOnly=true"
description: >-
  使用 AST 识别只读查询方法及其事务声明，检查是否显式设置 readOnly=true。
  仅读查询未设置只读事务属性时，可能失去只读优化与一致性提示。
category_l1: DAO
category_l2: "03"
status: ACTIVE
severity: MINOR
confidence: MEDIUM
detection_method: AST
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["data"]
  framework: ["spring-tx"]
  exclude: []
message: "检测到只读查询未设置 readOnly=true。"
remediation: "为只读查询事务显式设置 readOnly=true，或使用只读事务模板。"
positive_example: "只读查询事务明确设置 readOnly=true，或使用只读事务模板。"
negative_example: "只读查询方法使用默认事务属性，未设置 readOnly=true。"
false_positive_notes: "若方法包含写操作、调用写服务或依赖默认事务语义，可能不应强制只读；需结合方法体确认。"
sources:
  - type: FRAMEWORK_DOC
    ref: "Spring Framework Reference: Declarative transaction management - readOnly"
    version: "6.1.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本，继承 B0 候选 CAND-013 并补充完整元数据。"
test_cases: []
coverage: "覆盖只读查询事务属性设置的 AST 检测；不覆盖数据库层只读优化效果。"
```

## SRS-DAO-04-001

```yaml
rule_id: SRS-DAO-04-001
name: "避免事务内执行远程/IO/消息调用"
description: >-
  使用 HYBRID 组合检测：AST 识别事务边界与调用点，REGEX/AST 识别远程调用、文件 IO、消息发送等模式，
  再结合调用链判定事务内是否包含长耗时外部操作，避免长事务占用连接与锁资源。
category_l1: DAO
category_l2: "04"
status: ACTIVE
severity: CRITICAL
confidence: MEDIUM
detection_method: HYBRID
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-tx"]
  exclude: []
message: "检测到事务内执行远程/IO/消息调用，可能形成长事务。"
remediation: "将远程/IO/消息操作移出事务边界，或在事务提交后通过事件/异步机制执行。"
positive_example: "事务边界内只执行数据库短操作，远程/IO/消息在事务提交后执行。"
negative_example: "在 @Transactional 方法内直接调用远程服务、文件 IO 或发送消息。"
false_positive_notes: "若外部调用被 REQUIRES_NEW 隔离、使用本地缓存或已异步化，可能不构成长事务；需结合传播行为与调用链确认。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "内部经验：长事务导致连接占用与超时"
    version: "2026-09-24"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本，继承 B0 候选 CAND-014 并补充完整元数据。"
test_cases: []
coverage: "覆盖事务边界内外调用组合的 HYBRID 检测；不覆盖运行时实际耗时与远程调用超时配置。"
```

## SRS-DAO-05-001

```yaml
rule_id: SRS-DAO-05-001
name: "避免循环内查询导致 N+1"
description: >-
  使用 HEURISTIC 启发式策略：识别循环结构内的查询调用或懒加载访问，结合方法名、仓储调用与循环边界估计 N+1 风险。
  边界包括循环次数不可静态确定、动态条件查询、批量接口与缓存命中场景，需人工复核。
category_l1: DAO
category_l2: "05"
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: HEURISTIC
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["data"]
  framework: ["spring-data"]
  exclude: []
message: "检测到循环内查询调用，可能存在 N+1 查询风险。"
remediation: "改为循环外批量查询，或使用 fetch join、批量加载、二级缓存等方式减少查询次数。"
positive_example: "循环外批量查询，或使用 fetch join、批量加载避免逐条查询。"
negative_example: "在循环体内逐条调用查询方法，形成 N+1 查询。"
false_positive_notes: "若循环数据量很小、已批量加载、使用缓存或 fetch join，可能为误报；待 B 阶段评估可检测性。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "内部经验：循环内查询导致 N+1"
    version: "2026-09-24"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本，继承 B0 候选 CAND-015 并补充完整元数据。"
test_cases: []
coverage: "覆盖循环内查询的启发式识别；不保证可检测全部 N+1，待 B 阶段评估可检测性。"
```

## SRS-EXC-03-001

```yaml
rule_id: SRS-EXC-03-001
name: "事务回滚异常类型配置应正确"
description: >-
  使用 AST 解析 @Transactional 的 rollbackFor、noRollbackFor 配置及方法抛出异常类型，
  检查回滚规则是否与业务异常体系一致，避免受检异常未回滚或非预期回滚。
category_l1: EXC
category_l2: "03"
status: ACTIVE
severity: CRITICAL
confidence: HIGH
detection_method: AST
tool: CUSTOM
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-tx"]
  exclude: []
message: "检测到事务回滚异常类型配置可能错误。"
remediation: "按业务异常类型显式配置 rollbackFor 或 noRollbackFor，并确保异常未被包装后绕过回滚规则。"
positive_example: "按业务异常类型显式配置 rollbackFor 或 noRollbackFor，与异常体系一致。"
negative_example: "配置 rollbackFor 为不匹配的异常类型，导致受检异常不回滚或非预期回滚。"
false_positive_notes: "若使用默认 RuntimeException 回滚规则且业务异常均继承 RuntimeException，可能无需显式配置；异常包装场景需人工确认。"
sources:
  - type: OFFICIAL_SPEC
    ref: "Spring Framework Reference: Rolling back a declarative transaction"
    version: "6.1.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本，继承 B0 候选 CAND-028 并补充完整元数据。"
test_cases: []
coverage: "覆盖事务回滚异常类型配置的 AST 检测；不覆盖运行期异常传播与包装逻辑。"
```

## SRS-RES-02-001

```yaml
rule_id: SRS-RES-02-001
name: "事务中手动连接必须释放"
description: >-
  使用 AST 识别事务方法内手动获取数据库连接、语句或结果集的位置，
  检查是否在正常路径与异常路径均释放资源，避免连接泄漏。
category_l1: RES
category_l2: "02"
status: ACTIVE
severity: CRITICAL
confidence: MEDIUM
detection_method: AST
tool: SPOTBUGS
tool_rule_id: "ODR_OPEN_DATABASE_RESOURCE"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["data"]
  framework: ["spring-jdbc"]
  exclude: []
message: "检测到事务中手动连接可能未释放。"
remediation: "优先使用 Spring 管理连接；手动连接必须使用 try-with-resources 或 finally 确保释放。"
positive_example: "事务中由 Spring 管理连接，手动连接使用 try-with-resources 或 finally 释放。"
negative_example: "在事务中手动获取连接后未在异常路径和正常路径释放。"
false_positive_notes: "若连接由框架模板管理、使用 try-with-resources 或由外层统一释放，可能为误报；需结合资源作用域确认。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "内部经验：事务中手动获取连接未释放"
    version: "2026-09-24"
  - type: TOOL_RULE
    ref: "SpotBugs ODR_OPEN_DATABASE_RESOURCE"
    version: "4.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始版本，继承 B0 候选 CAND-040 并补充完整元数据。"
test_cases: []
coverage: "覆盖事务中手动连接资源释放的 AST 检测；不覆盖运行期连接池监控与泄漏追踪。"
```