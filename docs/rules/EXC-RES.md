# EXC-RES 规则元数据

## 说明

本文件涵盖 EXC、RES 两个一级分类，共 5 条规则元数据：SRS-EXC-01-001、SRS-EXC-02-001、SRS-EXC-04-001、SRS-EXC-05-001、SRS-RES-01-001。

## 规则

### SRS-EXC-01-001

```yaml
rule_id: "SRS-EXC-01-001"
name: "禁止捕获 Exception 后吞没异常"
description: "通过 AST 检查 catch 块，若捕获 Exception、Throwable 或 RuntimeException 后未记录日志、未重新抛出、未转换为业务异常且无有效处理逻辑，则判定为异常吞没。"
category_l1: "EXC"
category_l2: "01"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "AST"
tool: "PMD"
tool_rule_id: "EmptyCatchBlock"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-boot"]
  exclude: []
message: "捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。"
remediation: "在 catch 块中至少执行一种处理：记录带上下文的日志、重新抛出、包装为业务异常、或触发明确降级逻辑；禁止空 catch 或仅注释。"
positive_example: "捕获异常后记录错误日志，并抛出或转换为业务异常，保留原始异常作为 cause。"
negative_example: "捕获 Exception 后 catch 块为空或仅保留注释，调用方无法感知失败。"
false_positive_notes: "若 catch 块通过统一异常处理器、AOP 或框架机制处理，且静态分析无法解析调用链时可能误报；需结合项目统一异常处理约定复核。"
sources:
  - type: "TOOL_RULE"
    ref: "PMD Java Error Prone: EmptyCatchBlock"
    version: "7.0.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill-team"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B5 初始规则元数据。"
test_cases: []
coverage: "覆盖 Java 17+/Spring Boot 3.x、Maven/Gradle 项目中 catch 块吞异常检测；不覆盖 Lombok 生成代码、测试代码及编译期生成代码。"
```

### SRS-EXC-02-001

```yaml
rule_id: "SRS-EXC-02-001"
name: "禁止 catch 后仅 printStackTrace"
description: "通过 REGEX 检测 catch 块中仅调用 printStackTrace 且未使用日志框架、未重新抛出、未转换为业务异常或未执行其他有效处理的情况。"
category_l1: "EXC"
category_l2: "02"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "REGEX"
tool: "PMD"
tool_rule_id: "AvoidPrintStackTrace"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-boot"]
  exclude: []
message: "catch 后不得仅使用 printStackTrace，应使用日志框架记录并妥善处理异常。"
remediation: "替换 printStackTrace 为项目统一日志 API，记录异常对象与业务上下文；根据场景重新抛出、包装为业务异常或执行降级处理。"
positive_example: "catch 块中使用日志框架记录 error 级别日志，并携带异常对象和关键业务标识。"
negative_example: "catch 块中仅调用 printStackTrace，生产环境无法集中采集、检索和告警。"
false_positive_notes: "若 printStackTrace 位于临时调试代码、测试代码或命令行工具入口，可能属于可接受场景；需结合模块类型与代码路径复核。"
sources:
  - type: "TOOL_RULE"
    ref: "PMD Java Error Prone: AvoidPrintStackTrace"
    version: "7.0.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill-team"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B5 初始规则元数据。"
test_cases: []
coverage: "覆盖 Java 17+/Spring Boot 3.x、Maven/Gradle 项目中 catch 块仅 printStackTrace 的检测；不覆盖日志框架内部实现与测试代码。"
```

### SRS-EXC-04-001

```yaml
rule_id: "SRS-EXC-04-001"
name: "禁止对外异常响应暴露堆栈或内部类"
description: "通过 AST 检查控制器、异常处理器与 API 响应构造逻辑，识别将异常堆栈、异常类名、内部实现类名或框架默认错误细节直接返回给客户端的行为。"
category_l1: "EXC"
category_l2: "04"
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
  build_tool: ["maven", "gradle"]
  module_type: ["api", "web"]
  framework: ["spring-mvc", "spring-web"]
  exclude: []
message: "对外异常响应不得暴露堆栈、内部类名或实现细节，应返回稳定、脱敏的错误信息。"
remediation: "使用统一异常处理与错误响应模型，仅返回错误码、用户可读消息和必要追踪标识；堆栈与内部异常细节仅记录在服务端日志。"
positive_example: "API 异常响应返回统一错误码和脱敏消息，服务端日志保留完整堆栈与上下文。"
negative_example: "将异常堆栈、异常类名或内部实现类名直接写入 HTTP 响应体返回给客户端。"
false_positive_notes: "若项目仅内部调用且已明确允许返回调试信息，可能属于可接受场景；需结合 API 暴露范围与安全基线复核。"
sources:
  - type: "OFFICIAL_SPEC"
    ref: "OWASP Top 10 2021: A05 Security Misconfiguration"
    version: "2021"
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Reference: @ExceptionHandler and ResponseEntityExceptionHandler"
    version: "6.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill-team"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B5 初始规则元数据。"
test_cases: []
coverage: "覆盖 Java 17+/Spring Boot 3.x、Maven/Gradle 项目中 Spring MVC/Web API 异常响应暴露检测；不覆盖非 HTTP 对外协议与测试代码。"
```

### SRS-EXC-05-001

```yaml
rule_id: "SRS-EXC-05-001"
name: "异步或定时任务异常必须兜底处理"
description: "通过 AST 检测标注 @Async 或 @Scheduled 的方法，检查方法体及直接调用是否包含 try-catch、异常处理器或统一异步异常处理；边界为不解析动态代理、不跨方法深度数据流、不评估运行时线程池异常策略。"
category_l1: "EXC"
category_l2: "05"
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
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "异步或定时任务必须兜底处理异常，避免异常被线程池吞没或导致任务静默失败。"
remediation: "在 @Async 或 @Scheduled 方法内使用 try-catch 兜底，或配置统一 AsyncUncaughtExceptionHandler；记录异常日志并触发告警或补偿逻辑。"
positive_example: "异步或定时任务方法内捕获异常并记录日志，或通过统一异步异常处理器记录并告警。"
negative_example: "@Async 或 @Scheduled 方法未捕获异常，异常仅被线程池或调度器吞没，调用方无感知。"
false_positive_notes: "待 B 阶段评估可检测性；动态代理、跨方法调用链、线程池统一异常处理器可能导致误报或漏报。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Reference: Task Execution and Scheduling; @Async; @Scheduled"
    version: "6.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill-team"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B5 初始规则元数据。"
test_cases: []
coverage: "覆盖 Java 17+/Spring Boot 3.x、Maven/Gradle 项目中 @Async 与 @Scheduled 方法异常兜底检测；不覆盖动态代理展开、跨方法调用链与运行时线程池配置。"
```

### SRS-RES-01-001

```yaml
rule_id: "SRS-RES-01-001"
name: "禁止未关闭 InputStream 或 Connection"
description: "通过 AST 检测创建或获取 InputStream、OutputStream、Connection 等资源后，未在 try-with-resources 或 finally 中关闭，且未移交框架托管的情况。"
category_l1: "RES"
category_l2: "01"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "AST"
tool: "SPOTBUGS"
tool_rule_id: "OBL_UNSATISFIED_OBLIGATION"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-boot", "spring-jdbc"]
  exclude: []
message: "InputStream、Connection 等资源必须确保关闭，避免资源泄漏和连接耗尽。"
remediation: "优先使用 try-with-resources；无法使用时在 finally 中显式关闭，并处理关闭异常；框架托管资源应确认生命周期由框架负责。"
positive_example: "使用 try-with-resources 管理 InputStream 或 Connection，方法退出时自动关闭。"
negative_example: "创建 InputStream 或获取 Connection 后未关闭，异常路径下资源持续占用。"
false_positive_notes: "若资源由 Spring 事务、JdbcTemplate、连接池或框架托管，可能误报；需结合资源所有权与生命周期复核。"
sources:
  - type: "TOOL_RULE"
    ref: "SpotBugs: OBL_UNSATISFIED_OBLIGATION"
    version: "4.8.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill-team"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "B5 初始规则元数据。"
test_cases: []
coverage: "覆盖 Java 17+/Spring Boot 3.x、Maven/Gradle 项目中 InputStream/Connection 未关闭检测；不覆盖框架托管资源、连接池内部实现与测试代码。"
```