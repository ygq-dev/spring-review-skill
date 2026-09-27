# docs/rules/DI-CON.md

# DI/CON 规则元数据

## 1. 说明

本文件涵盖 DI、CON 两个一级分类，共 12 条规则：DI 7 条，CON 5 条。规则状态默认 ACTIVE；`module_type` 使用 A2 枚举；适用于 Java 17+/Spring Boot 3.x。`test_cases` 为空数组，待 C 阶段补充。未决问题：SRS-DI-03-001、SRS-DI-05-001、SRS-CON-05-001 均标注“待 B 阶段评估可检测性”。

## 2. 规则

### SRS-DI-01-001

```yaml
rule_id: "SRS-DI-01-001"
name: "禁止字段注入"
description: "检测 Spring Bean 中通过字段直接注入依赖的方式。检测对象为 Spring 管理 Bean 的字段注入点；边界为不检测构造器注入、setter 注入、方法注入，不覆盖非 Spring 管理对象。"
category_l1: "DI"
category_l2: "01"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "检测到字段注入，请改用构造器注入。"
remediation: "将必需依赖改为构造器注入，并在构造器中完成赋值；可选依赖可使用 setter 注入或 ObjectProvider。"
positive_example: "依赖通过构造器参数传入，字段保持不可变。"
negative_example: "在字段上直接使用注入注解，由容器反射写入依赖。"
false_positive_notes: "自定义组合注解或元注解可能造成漏报或误报，需结合 AST 注解解析。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Documentation - Dependency Injection"
    version: "6.x / Spring Boot 3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-DI-02-001

```yaml
rule_id: "SRS-DI-02-001"
name: "必需依赖应为构造器注入且 final"
description: "检测必需依赖未使用构造器注入或未声明为 final 的情况。检测对象为 Spring Bean 的必需依赖字段与构造器；边界为不强制可选依赖、不覆盖配置属性绑定。"
category_l1: "DI"
category_l2: "02"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "必需依赖应通过构造器注入并声明为 final。"
remediation: "将必需依赖放入唯一构造器参数，字段声明为 final，避免运行期替换。"
positive_example: "必需依赖通过构造器注入，字段为 final。"
negative_example: "必需依赖使用字段注入，字段非 final。"
false_positive_notes: "Lombok 生成构造器或编译期生成代码可能影响 AST 判断。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Documentation - Constructor-based Dependency Injection"
    version: "6.x / Spring Boot 3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-DI-03-001

```yaml
rule_id: "SRS-DI-03-001"
name: "检测构造器/字段循环依赖"
description: "检测 Spring Bean 之间通过构造器或字段形成的循环依赖。检测对象为 Bean 依赖图；边界为不分析运行期动态代理产生的隐式依赖，不覆盖非 Spring 管理对象。"
category_l1: "DI"
category_l2: "03"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "HIGH"
detection_method: "HYBRID"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "检测到循环依赖，可能导致启动失败或运行期异常。"
remediation: "重构依赖关系，提取公共协作对象，或使用事件、接口隔离、延迟注入打破循环。"
positive_example: "Bean 依赖关系为无环图，构造器依赖不形成闭环。"
negative_example: "两个单例 Bean 通过构造器互相注入，形成循环依赖。"
false_positive_notes: "待 B 阶段评估可检测性；动态条件装配和代理可能造成误报。"
sources:
  - type: "INTERNAL_EXPERIENCE"
    ref: "Spring 项目循环依赖审查经验"
    version: "1.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-DI-04-001

```yaml
rule_id: "SRS-DI-04-001"
name: "单例 Bean 中非线程安全可变状态"
description: "检测单例作用域 Bean 中保存非线程安全可变状态的情况。检测对象为单例 Bean 的可变实例字段；边界为不检测方法局部变量、不检测线程安全容器。"
category_l1: "DI"
category_l2: "04"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "HIGH"
detection_method: "AST"
tool: "SPOTBUGS"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "单例 Bean 中存在非线程安全可变状态。"
remediation: "移除可变实例状态，或改为线程安全结构、不可变对象、ThreadLocal 并明确清理。"
positive_example: "单例 Bean 无可变实例字段，或状态通过线程安全方式管理。"
negative_example: "单例 Bean 中保存非线程安全的可变集合或可变对象字段。"
false_positive_notes: "字段实际不可变或仅初始化后不再修改时可能误报。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Documentation - Bean Scopes"
    version: "6.x / Spring Boot 3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-DI-05-001

```yaml
rule_id: "SRS-DI-05-001"
name: "@Scope 与注入点生命周期不匹配"
description: "检测 Bean 作用域与注入点生命周期不匹配的情况。检测对象为 @Scope 声明与注入点；边界为不推断运行期代理行为，不覆盖 XML 配置。"
category_l1: "DI"
category_l2: "05"
status: "ACTIVE"
severity: "MAJOR"
confidence: "MEDIUM"
detection_method: "HYBRID"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "注入点生命周期与 Bean 作用域不匹配。"
remediation: "使用作用域代理、ObjectProvider、Provider 或按需查找，确保短生命周期 Bean 不被长生命周期 Bean 捕获。"
positive_example: "原型 Bean 注入到单例时使用代理或提供者，生命周期匹配。"
negative_example: "将请求作用域 Bean 直接注入单例 Bean，未使用作用域代理。"
false_positive_notes: "待 B 阶段评估可检测性；代理模式与自定义作用域可能造成误报。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Documentation - Bean Scopes"
    version: "6.x / Spring Boot 3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-DI-06-001

```yaml
rule_id: "SRS-DI-06-001"
name: "多个 @Aspect 未指定 @Order"
description: "检测 Spring AOP 中多个 @Aspect 切面未使用 @Order 或 Ordered 指定顺序的情况。检测对象为被 @Aspect 标注的 Bean；边界为不分析 XML 配置、不推断无注解切面的隐式顺序、不覆盖非 Spring AOP 代理。"
category_l1: "DI"
category_l2: "06"
status: "ACTIVE"
severity: "MAJOR"
confidence: "MEDIUM"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-aop"]
  exclude: []
message: "存在多个切面但未显式指定执行顺序。"
remediation: "为每个切面添加 @Order 或实现 Ordered，明确优先级。"
positive_example: "多个切面均通过 @Order 或 Ordered 明确顺序。"
negative_example: "多个切面未指定顺序，执行顺序依赖类名或扫描顺序。"
false_positive_notes: "仅一个切面或通过其他机制排序时可能误报。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Documentation - Aspect Oriented Programming"
    version: "6.x / Spring Boot 3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-DI-07-001

```yaml
rule_id: "SRS-DI-07-001"
name: "@Around 未调用 proceed()"
description: "检测 Spring AOP @Around 通知方法体内未调用 ProceedingJoinPoint.proceed() 的情况。检测对象为 @Around 方法；边界为不分析方法体经反射或动态调用 proceed 的场景，不覆盖 AspectJ 编译期织入。"
category_l1: "DI"
category_l2: "07"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-aop"]
  exclude: []
message: "@Around 通知未调用 proceed()，目标方法可能不会执行。"
remediation: "在 @Around 通知中调用 proceed() 并返回其结果；如需短路，明确设计并记录。"
positive_example: "@Around 通知在适当时机调用 proceed() 并返回其结果。"
negative_example: "@Around 通知未调用 proceed()，直接返回其他值或抛出异常，导致目标方法不执行。"
false_positive_notes: "若通过辅助方法间接调用 proceed()，静态分析可能误报。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Spring Framework Documentation - Around Advice"
    version: "6.x / Spring Boot 3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-CON-01-001

```yaml
rule_id: "SRS-CON-01-001"
name: "SimpleDateFormat 静态共享"
description: "检测静态共享的 SimpleDateFormat 实例。检测对象为静态字段中的 SimpleDateFormat；边界为不检测局部变量、不检测 DateTimeFormatter。"
category_l1: "CON"
category_l2: "01"
status: "ACTIVE"
severity: "BLOCKER"
confidence: "HIGH"
detection_method: "AST"
tool: "SPOTBUGS"
tool_rule_id: "STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "SimpleDateFormat 被静态共享，存在线程安全问题。"
remediation: "改为局部变量创建，或使用 DateTimeFormatter 等线程安全 API。"
positive_example: "每次使用创建局部 SimpleDateFormat，或使用 DateTimeFormatter。"
negative_example: "静态 final SimpleDateFormat 被多个线程共享。"
false_positive_notes: "仅在单线程初始化阶段使用且不再并发访问时可能误报。"
sources:
  - type: "TOOL_RULE"
    ref: "SpotBugs Bug Descriptions - STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE"
    version: "4.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-CON-02-001

```yaml
rule_id: "SRS-CON-02-001"
name: "非线程安全集合作为单例字段"
description: "检测单例 Bean 中非线程安全集合作为可变字段的情况。检测对象为单例 Bean 的集合字段；边界为不检测局部集合、不检测线程安全集合。"
category_l1: "CON"
category_l2: "02"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "HIGH"
detection_method: "AST"
tool: "SPOTBUGS"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "单例 Bean 中存在非线程安全集合字段。"
remediation: "使用线程安全集合、不可变集合，或将状态限制在方法局部。"
positive_example: "单例字段使用线程安全集合或不可变集合。"
negative_example: "单例 Bean 中直接使用 HashMap、ArrayList 等非线程安全集合作为可变字段。"
false_positive_notes: "集合仅初始化后只读，或通过外部同步保护时可能误报。"
sources:
  - type: "INTERNAL_EXPERIENCE"
    ref: "Spring 单例并发审查经验"
    version: "1.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-CON-03-001

```yaml
rule_id: "SRS-CON-03-001"
name: "未同步的懒初始化单例"
description: "检测未同步或双重检查锁缺少 volatile 的懒初始化单例。检测对象为懒初始化单例代码；边界为不检测静态初始化、枚举单例。"
category_l1: "CON"
category_l2: "03"
status: "ACTIVE"
severity: "MAJOR"
confidence: "MEDIUM"
detection_method: "AST"
tool: "SPOTBUGS"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "懒初始化单例存在同步缺陷。"
remediation: "使用静态初始化、枚举单例，或正确实现双重检查锁并配合 volatile。"
positive_example: "使用静态初始化、枚举或同步/双重检查锁并配合 volatile。"
negative_example: "双重检查锁未使用 volatile 或未同步，导致其他线程看到部分构造对象。"
false_positive_notes: "使用其他线程安全发布机制时可能误报。"
sources:
  - type: "TOOL_RULE"
    ref: "SpotBugs Bug Descriptions - 懒初始化与双重检查锁"
    version: "4.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-CON-04-001

```yaml
rule_id: "SRS-CON-04-001"
name: "线程池未设置拒绝策略/队列无界"
description: "检测线程池使用无界队列或未显式设置拒绝策略的情况。检测对象为线程池构造；边界为不检测容器托管线程池、不覆盖响应式调度器。"
category_l1: "CON"
category_l2: "04"
status: "ACTIVE"
severity: "MAJOR"
confidence: "HIGH"
detection_method: "AST"
tool: "CUSTOM"
parameters:
  requireBoundedQueue: true
  requireRejectedExecutionHandler: true
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "线程池未设置拒绝策略或使用无界队列。"
remediation: "使用有界队列并显式设置拒绝策略，结合业务定义降级、告警或阻塞策略。"
positive_example: "线程池使用有界队列并显式设置拒绝策略。"
negative_example: "线程池使用无界队列且未设置拒绝策略。"
false_positive_notes: "通过框架统一配置线程池时可能误报。"
sources:
  - type: "FRAMEWORK_DOC"
    ref: "Java Concurrency - ThreadPoolExecutor"
    version: "Java 17"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

### SRS-CON-05-001

```yaml
rule_id: "SRS-CON-05-001"
name: "@Transactional 跨线程失效未处理"
description: "检测在 @Transactional 方法内启动新线程并访问数据库，但未显式处理事务传播的情况。检测对象为事务方法内的跨线程调用；边界为不检测响应式事务、不覆盖容器特定事务管理器。"
category_l1: "CON"
category_l2: "05"
status: "ACTIVE"
severity: "CRITICAL"
confidence: "MEDIUM"
detection_method: "HYBRID"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: ">=17"
  spring_boot_version: ">=3.0.0"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "@Transactional 跨线程后事务上下文不会自动传播。"
remediation: "在目标线程内显式开启事务，或使用 TransactionTemplate 管理边界，避免假设事务自动传播。"
positive_example: "跨线程操作显式管理事务边界，或在事务模板中执行。"
negative_example: "在 @Transactional 方法内启动新线程访问数据库，假设事务会传播。"
false_positive_notes: "待 B 阶段评估可检测性；线程池、异步注解和动态代理可能造成误报。"
sources:
  - type: "INTERNAL_EXPERIENCE"
    ref: "Spring 事务跨线程审查经验"
    version: "1.0"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "Spring Review Skill 规则组"
created_at: "2026-09-23"
updated_at: "2026-09-23"
change_log:
  - version: "1.0.0"
    date: "2026-09-23"
    description: "B2 初始元数据"
test_cases: []
coverage: "B 阶段元数据；C 阶段补充测试用例"
```

