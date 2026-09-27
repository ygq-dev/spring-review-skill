# TEST、STYLE、NULL、I18N 规则元数据

## 1. 说明

本文件涵盖 TEST、STYLE、NULL、I18N 四个分类，共 7 条规则：

- TEST：2 条
- STYLE：2 条
- NULL：2 条
- I18N：1 条

## 2. 规则 YAML

### SRS-TEST-01-001

```yaml
rule_id: "SRS-TEST-01-001"
name: "测试依赖真实外部服务或数据库"
description: "检测使用 @SpringBootTest、@DataJpaTest、@Testcontainers 等测试入口时，是否直接连接真实外部服务或数据库；边界为静态可识别的真实连接配置、真实主机/端口、生产数据源引用和未替换的远程客户端，不覆盖运行期动态发现、反射调用和外部配置中心最终解析结果。"
category_l1: "TEST"
category_l2: "01"
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
  module_type: ["test"]
  framework: ["spring-boot", "spring-test"]
  exclude: []
message: "测试不应依赖真实外部服务或数据库，应使用 Mock、内存替身或隔离的 Testcontainers 配置。"
remediation: "将外部依赖替换为 Mock、Stub、内存数据库或受控容器，并确保测试数据与外部环境隔离。"
positive_example: "测试使用 Mock 或内存替身隔离外部服务，不直接连接真实数据库。"
negative_example: "测试直接使用真实数据库连接或外部 HTTP 服务。"
false_positive_notes: "集成测试有意验证真实外部服务、或 Testcontainers 仅声明未实际连接时可能误报。"
sources:
  - type: "INTERNAL_EXPERIENCE"
    ref: "项目内部测试隔离经验"
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
    description: "初始规则元数据"
test_cases: []
coverage: "覆盖 @SpringBootTest、@DataJpaTest、@Testcontainers 等测试中直接连接真实外部服务或数据库的静态可识别模式；不覆盖动态反射、运行期条件化连接和外部配置解析后的实际连接。"
```

### SRS-TEST-02-001

```yaml
rule_id: "SRS-TEST-02-001"
name: "@SpringBootTest 全量上下文滥用"
description: "检测测试中过度使用 @SpringBootTest 加载全量应用上下文的模式；检测策略为统计测试类或测试套件中 @SpringBootTest 的使用范围、是否缺少 webEnvironment 限定、是否可被切片测试替代、是否重复加载全量上下文；边界为不覆盖运行期上下文缓存命中、条件化配置和动态测试生成。"
category_l1: "TEST"
category_l2: "02"
status: "ACTIVE"
severity: "MINOR"
confidence: "MEDIUM"
detection_method: "AST"
tool: "CUSTOM"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["test"]
  framework: ["spring-boot", "spring-test"]
  exclude: []
message: "@SpringBootTest 应谨慎使用，优先选择切片测试或轻量上下文。"
remediation: "将可切片验证的测试改为 @WebMvcTest、@DataJpaTest 等；合并重复全量上下文；明确 webEnvironment 需求。"
positive_example: "仅对确实需要完整启动流程的测试使用 @SpringBootTest，其余使用切片测试。"
negative_example: "大量单元级测试无差别使用 @SpringBootTest 加载全量上下文。"
false_positive_notes: "需要验证完整启动配置、自动配置或跨层集成流程时，全量上下文可能合理。"
sources:
  - type: "COMMUNITY"
    ref: "Spring 测试上下文缓存与 @SpringBootTest 使用实践"
    version: "3.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始规则元数据"
test_cases: []
coverage: "覆盖测试类级别 @SpringBootTest 使用频率、webEnvironment 缺失和可切片替代的静态模式；不覆盖运行期上下文缓存复用和条件化测试执行。"
```

### SRS-STYLE-01-001

```yaml
rule_id: "SRS-STYLE-01-001"
name: "命名不符合 Java/Spring 约定"
description: "覆盖 Checkstyle 命名规则族，包括 TypeName、MethodName、MemberName、ConstantName、LocalVariableName、PackageName 等；因无单一 Checkstyle 规则可完整对应，tool_rule_id 置为 null，检测范围涵盖 Java 与 Spring 项目常见命名约定。"
category_l1: "STYLE"
category_l2: "01"
status: "ACTIVE"
severity: "MINOR"
confidence: "HIGH"
detection_method: "STATIC_ANALYSIS"
tool: "CHECKSTYLE"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-boot"]
  exclude: []
message: "类型、方法、变量、常量和包命名应符合 Java/Spring 约定。"
remediation: "按 Java 命名规范重命名，并保持 Spring 组件、配置和测试命名一致。"
positive_example: "类名使用 UpperCamelCase，方法和变量使用 lowerCamelCase，常量使用 UPPER_SNAKE_CASE。"
negative_example: "类名使用小写或下划线，方法名使用 UpperCamelCase。"
false_positive_notes: "生成代码、外部协议 DTO、遗留接口适配层可能有意保留非标准命名。"
sources:
  - type: "TOOL_RULE"
    ref: "Checkstyle Naming Rules"
    version: "10.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始规则元数据"
test_cases: []
coverage: "覆盖 Java 包、类型、方法、成员、常量、局部变量和参数的命名约定；不覆盖架构分层命名和业务语义命名质量。"
```

### SRS-STYLE-02-001

```yaml
rule_id: "SRS-STYLE-02-001"
name: "方法过长/复杂度过高"
description: "以 PMD CyclomaticComplexity 为代表规则，同时覆盖 ExcessiveMethodLength、NPathComplexity 等方法长度与复杂度问题；检测方法体行数、分支数量和嵌套深度是否超过约定阈值。"
category_l1: "STYLE"
category_l2: "02"
status: "ACTIVE"
severity: "MINOR"
confidence: "HIGH"
detection_method: "STATIC_ANALYSIS"
tool: "PMD"
tool_rule_id: "CyclomaticComplexity"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-boot"]
  exclude: []
message: "方法应保持可读长度和可控复杂度，避免过长方法与深层分支。"
remediation: "拆分长方法、提取私有方法、减少嵌套分支，并优先使用早返回和策略化。"
positive_example: "方法职责单一，分支数量少，长度在团队约定阈值内。"
negative_example: "单个方法包含大量 if/else、循环和嵌套逻辑，长度明显过长。"
false_positive_notes: "生成代码、解析器、映射器和算法密集代码可能因领域需要而复杂度较高。"
sources:
  - type: "TOOL_RULE"
    ref: "PMD Java Rules: CyclomaticComplexity / ExcessiveMethodLength / NPathComplexity"
    version: "7.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始规则元数据"
test_cases: []
coverage: "覆盖方法长度、圈复杂度和 NPath 复杂度的静态分析模式；不覆盖认知复杂度的主观可读性评价。"
```

### SRS-NULL-01-001

```yaml
rule_id: "SRS-NULL-01-001"
name: "Optional 作为字段/参数"
description: "检测 Optional 被用作实体字段、DTO 字段、方法参数或构造器参数的模式；对应 PMD OptionalUsedAsFieldOrParameterType，关注 Optional 应用于返回值而非字段和参数。"
category_l1: "NULL"
category_l2: "01"
status: "ACTIVE"
severity: "MINOR"
confidence: "MEDIUM"
detection_method: "AST"
tool: "PMD"
tool_rule_id: "OptionalUsedAsFieldOrParameterType"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "Optional 不宜作为字段或参数类型，应优先用于返回值表达可缺失结果。"
remediation: "将字段或参数改为可空类型并在边界处理，或使用方法返回值 Optional 表达缺失。"
positive_example: "Optional 仅用于方法返回值，字段和参数使用普通类型。"
negative_example: "实体字段或方法参数声明为 Optional。"
false_positive_notes: "某些框架扩展或函数式 API 可能要求 Optional 参数，需结合具体契约判断。"
sources:
  - type: "TOOL_RULE"
    ref: "PMD Best Practices: OptionalUsedAsFieldOrParameterType"
    version: "7.x"
rule_version: "1.0.0"
metadata_version: "1.0.0"
schema_version: "1.0.0"
owner: "spring-review-skill"
created_at: "2026-09-24"
updated_at: "2026-09-24"
change_log:
  - version: "1.0.0"
    date: "2026-09-24"
    description: "初始规则元数据"
test_cases: []
coverage: "覆盖 Optional 作为字段、方法参数和构造器参数的静态 AST 模式；不覆盖运行期空值传播和外部 API 契约要求。"
```

### SRS-NULL-02-001

```yaml
rule_id: "SRS-NULL-02-001"
name: "可能返回 null 的集合/数组"
description: "检测方法可能返回 null 集合或 null 数组的模式；主要对应 SpotBugs PZLA_PREFER_ZERO_LENGTH_ARRAYS，数组部分优先建议返回零长度数组，集合部分需结合 AST 返回路径和空值传播识别。"
category_l1: "NULL"
category_l2: "02"
status: "ACTIVE"
severity: "MAJOR"
confidence: "MEDIUM"
detection_method: "AST"
tool: "SPOTBUGS"
tool_rule_id: "PZLA_PREFER_ZERO_LENGTH_ARRAYS"
parameters: {}
enabled: true
applies_to:
  java_version: "17+"
  spring_boot_version: "3.x"
  build_tool: ["maven", "gradle"]
  module_type: ["all"]
  framework: ["spring-context"]
  exclude: []
message: "集合或数组返回值不应使用 null 表示空结果，应返回空集合或零长度数组。"
remediation: "返回 Collections.emptyList/emptySet/emptyMap 或零长度数组，避免调用方额外空值判断。"
positive_example: "无结果时返回空集合或零长度数组。"
negative_example: "无结果时返回 null 集合或 null 数组。"
false_positive_notes: "遗留 API 契约明确以 null 表示缺失、或外部序列化协议要求 null 时可能合理。"
sources:
  - type: "TOOL_RULE"
    ref: "SpotBugs Bug Descriptions: PZLA_PREFER_ZERO_LENGTH_ARRAYS"
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
    description: "初始规则元数据"
test_cases: []
coverage: "覆盖数组返回 null 的 SpotBugs 可识别模式，以及集合返回 null 的静态 AST 返回路径模式；不覆盖运行期动态代理和反射生成返回值。"
```

### SRS-I18N-01-001

```yaml
rule_id: "SRS-I18N-01-001"
name: "硬编码文案未外部化"
description: "通过正则检测用户可见硬编码文案未外部化到消息资源文件的模式；覆盖中文字符串字面量、面向用户的提示/错误/标签文本，边界为不解析字符串拼接、常量引用和注解值中的最终文案。"
category_l1: "I18N"
category_l2: "01"
status: "ACTIVE"
severity: "MINOR"
confidence: "MEDIUM"
detection_method: "REGEX"
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
message: "面向用户的文案应外部化到消息资源，不应硬编码在代码中。"
remediation: "将提示、错误和标签文案迁移到 MessageSource 资源文件，并通过消息键引用。"
positive_example: "用户可见文案通过消息键从资源文件读取。"
negative_example: "代码中直接写入面向用户的中文提示或错误文案。"
false_positive_notes: "日志、内部异常消息、协议键、测试断言和第三方接口固定字段可能不需要国际化。"
sources:
  - type: "INTERNAL_EXPERIENCE"
    ref: "项目内部国际化文案外部化经验"
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
    description: "初始规则元数据"
test_cases: []
coverage: "覆盖正则可识别的硬编码用户可见文案；不覆盖字符串拼接、常量间接引用、注解属性和资源文件内部质量。"
```