# Spring Review Skill 规则元数据：AR、CF、BUILD、CLOUD、MIG

## 1. 说明

本文件涵盖 AR、CF、BUILD、CLOUD、MIG 五个一级分类，共 9 条规则。规则元数据用于 B 规则提炼阶段，状态默认为 ACTIVE，测试用例留空至 C 阶段补充。

## 2. 规则元数据

### SRS-AR-01-001

```yaml
rule_id: SRS-AR-01-001
name: 分层依赖倒置
description: |
  检测 Controller 层直接依赖 Mapper/Repository 实现的违规。
  Spring 分层架构要求 Controller → Service → Repository 单向依赖。
  检测方式：JavaParser 解析包/类依赖图，结合命名与注解做 HYBRID 判定。
  边界：测试、生成代码、配置类可豁免。
category_l1: AR
category_l2: "01"
tags:
  - architecture
  - layering
status: ACTIVE
severity: CRITICAL
confidence: HIGH
detection_method: HYBRID
tool: CUSTOM
parameters:
  layers:
    - Controller
    - Service
    - Repository
  forbidden_targets:
    - Mapper
    - Repository
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
  framework:
    - spring-mvc
    - spring-data
  exclude:
    - "**/test/**"
    - "**/generated/**"
message: "Controller {className} 直接依赖 {repositoryName}，违反分层依赖。"
remediation: "将 Repository 调用下沉到 Service 层；Controller 仅依赖 Service 接口。"
positive_example: |
  Controller 只注入 Service，通过 Service 访问数据层。
negative_example: |
  Controller 直接注入 Repository 并调用查询方法。
false_positive_notes: "测试或代码生成模块可豁免；若为显式 CQRS 读模型直连需人工确认。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "团队分层架构规范"
    version: "1.0.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "architecture-team"
created_at: "2026-09-23"
updated_at: "2026-09-25"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "JavaParser 依赖图 + 规则元数据 HYBRID 判定覆盖"
```

### SRS-AR-02-001

```yaml
rule_id: SRS-AR-02-001
name: 领域层依赖 Web/基础设施
description: |
  检测领域层（domain/model）依赖 Web 或基础设施包。
  领域层应保持隔离，仅依赖领域模型和抽象端口。
  检测方式：JavaParser 解析包/类依赖图，结合命名与注解做 HYBRID 判定。
  边界：应用服务、适配器不在领域层范围内。
category_l1: AR
category_l2: "02"
tags:
  - architecture
  - domain-isolation
status: ACTIVE
severity: MAJOR
confidence: HIGH
detection_method: HYBRID
tool: CUSTOM
parameters:
  layers:
    - Domain
    - Web
    - Infrastructure
  forbidden_dependencies:
    - web
    - infrastructure
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
    - spring
    - spring-boot
  exclude:
    - "**/test/**"
message: "领域层 {className} 依赖 {targetPackage}，违反领域隔离。"
remediation: "通过接口/端口反转依赖，将 Web 与基础设施实现放在外层。"
positive_example: |
  领域类仅依赖领域模型和抽象端口。
negative_example: |
  领域类导入 Web 注解或基础设施实现。
false_positive_notes: "框架必需的注解若无法避免，可限定到特定包；需人工复核。"
sources:
  - type: FRAMEWORK_DOC
    ref: "Spring Modulith / Hexagonal Architecture 文档"
    version: "3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "architecture-team"
created_at: "2026-09-23"
updated_at: "2026-09-25"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "JavaParser 依赖图 + 规则元数据 HYBRID 判定覆盖"
```

### SRS-CF-01-001

```yaml
rule_id: SRS-CF-01-001
name: 生产配置包含 dev/test 或调试开关
description: |
  检测生产配置文件包含 dev/test profile、调试开关或开发专用参数。
  检测方式：规则引擎扫描 application-prod/生产 profile 配置。
  边界：本地开发配置和测试资源不报。
category_l1: CF
category_l2: "01"
tags:
  - config
  - production
  - security
status: ACTIVE
severity: CRITICAL
confidence: HIGH
detection_method: RULE_ENGINE
tool: CUSTOM
parameters:
  profiles:
    - prod
    - production
  forbidden_keys:
    - debug
    - devtools
    - h2-console
    - show-sql
  forbidden_values:
    - "true"
    - dev
    - test
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
    - batch
    - cloud
  framework:
    - spring-boot
  exclude:
    - "**/src/test/**"
    - "**/application-dev.*"
    - "**/application-test.*"
message: "生产配置 {file} 包含开发/调试项 {key}。"
remediation: "移除生产配置中的调试开关，使用 profile 隔离开发配置。"
positive_example: |
  生产配置仅包含生产连接与安全参数。
negative_example: |
  application-prod.yml 中启用 show-sql 或 devtools。
false_positive_notes: "若调试开关由环境变量强制关闭且默认关闭，可人工豁免。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "生产配置基线"
    version: "1.0.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "config-team"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "规则引擎扫描生产 profile 配置文件"
```

### SRS-CF-02-001

```yaml
rule_id: SRS-CF-02-001
name: "@Value 缺少默认值或配置校验"
description: |
  检测 @Value 注入缺少默认值或未使用 @ConfigurationProperties 校验。
  检测方式：AST 分析注解与参数。
  边界：必填配置显式失败可接受；非 Spring 管理类不报。
category_l1: CF
category_l2: "02"
tags:
  - config
  - configuration-properties
  - validation
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: AST
tool: CUSTOM
parameters:
  annotations:
    - "@Value"
  require_default: false
  require_validation: true
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
    - batch
    - cloud
  framework:
    - spring-boot
    - spring-context
  exclude:
    - "**/test/**"
message: "@Value 注入 {field} 缺少默认值或配置校验。"
remediation: "使用 @ConfigurationProperties + @Validated，或为 @Value 提供默认值并校验。"
positive_example: |
  配置属性类使用 @ConfigurationProperties 和校验注解。
negative_example: |
  @Value("${app.timeout}") 无默认值且无校验。
false_positive_notes: "启动时快速失败是合理设计；仅当缺少校验导致运行时错误时告警。"
sources:
  - type: FRAMEWORK_DOC
    ref: "Spring Boot Externalized Configuration"
    version: "3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "config-team"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "AST 扫描 @Value 与配置属性类"
```

### SRS-BUILD-01-001

```yaml
rule_id: SRS-BUILD-01-001
name: 依赖版本冲突/重复声明
description: |
  检测构建文件中同一依赖重复声明或版本冲突。
  检测方式：规则引擎解析 Maven/Gradle 依赖树与声明。
  边界：传递依赖冲突由依赖管理解决；排除测试依赖。
category_l1: BUILD
category_l2: "01"
tags:
  - build
  - dependency-management
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: RULE_ENGINE
tool: CUSTOM
parameters:
  dependency_manager:
    - maven
    - gradle
  check_duplicates: true
  check_version_conflicts: true
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
    - batch
    - cloud
    - all
  framework:
    - spring-boot
  exclude:
    - "**/test/**"
message: "依赖 {dependency} 存在重复声明或版本冲突：{versions}。"
remediation: "统一在 dependencyManagement/BOM 或版本目录中声明版本，移除重复声明。"
positive_example: |
  依赖版本由 Spring Boot BOM 统一管理。
negative_example: |
  同一模块多次声明同一依赖且版本不同。
false_positive_notes: "多模块不同技术栈可有意覆盖版本；需人工确认。"
sources:
  - type: TOOL_RULE
    ref: "Maven Enforcer / Gradle dependencyInsight 规则"
    version: "1.0.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "build-team"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "解析构建文件与依赖树"
```

### SRS-BUILD-02-001

```yaml
rule_id: SRS-BUILD-02-001
name: SNAPSHOT 依赖进入生产构建
description: |
  检测生产构建依赖中包含 SNAPSHOT 版本。
  检测方式：正则扫描 Maven/Gradle 构建文件与锁文件。
  边界：开发分支和测试构建可豁免。
category_l1: BUILD
category_l2: "02"
tags:
  - build
  - release
  - dependency-version
status: ACTIVE
severity: CRITICAL
confidence: HIGH
detection_method: REGEX
tool: CUSTOM
parameters:
  pattern: "SNAPSHOT"
  scope:
    - dependencies
    - dependencyManagement
    - plugins
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
    - batch
    - cloud
    - all
  framework:
    - spring-boot
  exclude:
    - "**/test/**"
    - "**/build/dev/**"
message: "生产构建声明了 SNAPSHOT 依赖 {dependency}。"
remediation: "替换为固定 release 版本，或通过发布流程冻结快照。"
positive_example: |
  生产构建仅使用已发布版本。
negative_example: |
  pom.xml 中依赖版本以 -SNAPSHOT 结尾。
false_positive_notes: "仅开发 profile 或内部快照仓库可人工豁免。"
sources:
  - type: INTERNAL_EXPERIENCE
    ref: "发布构建规范"
    version: "1.0.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "build-team"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "正则扫描构建文件中的 SNAPSHOT"
```

### SRS-CLOUD-01-001

```yaml
rule_id: SRS-CLOUD-01-001
name: 缺少优雅停机配置
description: |
  检测 Spring Boot 应用缺少优雅停机配置。
  检测方式：规则引擎检查 server.shutdown=graceful 与超时配置。
  边界：非 Web 应用或短生命周期任务可豁免。
category_l1: CLOUD
category_l2: "01"
tags:
  - cloud
  - graceful-shutdown
  - lifecycle
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: RULE_ENGINE
tool: CUSTOM
parameters:
  required_keys:
    - server.shutdown
  expected_value: graceful
  timeout_key: spring.lifecycle.timeout-per-shutdown-phase
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
    - cloud
  framework:
    - spring-boot
  exclude:
    - "**/test/**"
    - "**/batch/**"
message: "应用缺少优雅停机配置 server.shutdown=graceful。"
remediation: "设置 server.shutdown=graceful 并配置合理的停机超时。"
positive_example: |
  配置 server.shutdown=graceful 与超时。
negative_example: |
  未配置优雅停机，直接终止。
false_positive_notes: "非 HTTP 服务或无状态短任务可豁免。"
sources:
  - type: OFFICIAL_SPEC
    ref: "Spring Boot Graceful Shutdown 文档"
    version: "3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "cloud-team"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "规则引擎检查应用配置"
```

### SRS-CLOUD-02-001

```yaml
rule_id: SRS-CLOUD-02-001
name: 健康检查依赖未隔离
description: |
  检测健康检查端点依赖未隔离，可能因外部依赖故障导致健康检查失败。
  检测方式：规则引擎/AST 检查 HealthIndicator 与 readiness/liveness 分组。
category_l1: CLOUD
category_l2: "02"
tags:
  - cloud
  - health-check
  - readiness
status: ACTIVE
severity: MAJOR
confidence: MEDIUM
detection_method: HYBRID
tool: CUSTOM
parameters:
  health_groups:
    - liveness
    - readiness
  external_dependencies:
    - db
    - redis
    - mq
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
    - cloud
  framework:
    - spring-boot
    - spring-boot-actuator
  exclude:
    - "**/test/**"
message: "健康检查 {healthIndicator} 未隔离外部依赖，可能影响就绪状态。"
remediation: "使用健康分组隔离 liveness 与 readiness，外部依赖纳入 readiness。"
positive_example: |
  liveness 不依赖外部服务，readiness 包含必要依赖。
negative_example: |
  liveness 与数据库健康检查强绑定。
false_positive_notes: "待 B 阶段评估可检测性；启发式规则可能误报，需人工复核。"
sources:
  - type: FRAMEWORK_DOC
    ref: "Spring Boot Actuator Health Groups"
    version: "3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "cloud-team"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "规则引擎/AST 检查健康检查与分组配置"
```

### SRS-MIG-01-001

```yaml
rule_id: SRS-MIG-01-001
name: 使用 javax.* 未迁移 jakarta.*
description: |
  检测源码或依赖中仍使用 javax.* 包，未迁移到 jakarta.*。
  检测方式：正则扫描导入、包声明和配置。
  边界：javax.sql、javax.crypto 等 JDK 保留包不报。
category_l1: MIG
category_l2: "01"
tags:
  - migration
  - jakarta
  - javax
status: ACTIVE
severity: CRITICAL
confidence: HIGH
detection_method: REGEX
tool: CUSTOM
parameters:
  forbidden_import_prefixes:
    - javax.servlet
    - javax.persistence
    - javax.validation
    - javax.annotation
    - javax.transaction
  allowed_prefixes:
    - javax.sql
    - javax.crypto
    - javax.naming
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool:
    - maven
    - gradle
  module_type:
    - web
    - api
    - batch
    - cloud
    - all
  framework:
    - spring-boot
    - spring-mvc
    - spring-data
    - spring-security
  exclude:
    - "**/test/**"
    - "**/generated/**"
message: "检测到 javax.* 导入 {importName}，Spring Boot 3.x 需迁移到 jakarta.*。"
remediation: "替换 javax.* 为 jakarta.*，并更新依赖到 Jakarta EE 9+ 兼容版本。"
positive_example: |
  使用 jakarta.servlet 与 jakarta.persistence。
negative_example: |
  仍导入 javax.servlet.http.HttpServletRequest。
false_positive_notes: "JDK 自带 javax.sql、javax.crypto 等不报；第三方旧依赖需人工确认。"
sources:
  - type: FRAMEWORK_DOC
    ref: "Spring Boot 3.0 Migration Guide"
    version: "3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "migration-team"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "初始版本"
test_cases: []
coverage: "正则扫描源码导入与依赖坐标"
```