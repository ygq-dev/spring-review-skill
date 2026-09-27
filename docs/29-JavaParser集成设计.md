# docs/29-JavaParser集成设计

状态：E7 设计冻结候选  
对应：E2 模块 M09  
范围：仅 M09 Java 源码解析；不修改源码、不编译、不写 `.class`、不提交、不训练模型。  
依赖冻结：E1、E2、E4、E5、E6、A2、A5、A6。  
不修改：`docs/23～28`、`SKILL.md`、`rules/`、`schema/`、A5、A6、A2、A4.2。

## 1. 设计原则

1. 只读内存解析：JavaParser 仅生成 AST 与依赖图，不回写源码、不生成字节码、不触发构建。
2. 契约优先：M09 对外接口与 E2 冻结一致，`ParsedSource` 顶层字段不新增、不改名。
3. 失败隔离：单文件解析失败记录 `ParseError` 并跳过，不致命，不影响 `parseAll` 整体产出。
4. 证据可追溯：路径统一 `/`，UTF-8，行号 1-based，列号 1-based；未知列号用 `0`。
5. MVP 去 ArchUnit：AR-01、AR-02 用 JavaParser 包/类依赖图替代，`tool=CUSTOM`，`detection_method=HYBRID`。
6. 增量优先：DIFF 模式只解析变更文件；M09 输出局部图，不擅自合并全量历史图。

## 2. JavaParser 3.26.2 集成方式

| 项         | 决策                                                         |
| ---------- | ------------------------------------------------------------ |
| 集成方式   | 库调用，非命令行；由构建依赖引入 JavaParser 3.26.2           |
| 语言级别   | 默认 `JAVA_17`；按 `SourceUnit.language` 与项目编译级别映射；不支持时降级并记录 |
| 解析配置   | 保留注释：开启；保留行号/列号：依赖节点 `begin/end`；词法保留：关闭 |
| 符号求解   | 关闭，不引入类路径、不解析依赖 jar、不执行编译               |
| 编码       | 固定 UTF-8；非法字节记录 `ENCODING` 并跳过                   |
| 解析器实例 | 线程封闭或每次调用创建；不共享可变 `ParserConfiguration`     |
| 输出       | AST、包、imports、类、方法、局部依赖图、解析错误             |
| 不启用     | ArchUnit、字节码解析、编译执行、源码修改、网络访问           |

## 3. M09 接口设计

| 接口                                                  | 输入                             | 输出              | 异常契约                                                     | 幂等性                                         | 线程安全                     |
| ----------------------------------------------------- | -------------------------------- | ----------------- | ------------------------------------------------------------ | ---------------------------------------------- | ---------------------------- |
| `parse(SourceUnit) -> ParsedSource`                   | `SourceUnit`、语言级别、解析配置 | `ParsedSource`    | 不抛致命异常；失败写入 `parseErrors`；非 Java 返回空解析并标记跳过 | 相同 `hash + language + content` 产出稳定      | 是；无共享可变状态           |
| `analyzePackageDeps(ParsedSource) -> DependencyGraph` | `ParsedSource`                   | `DependencyGraph` | 纯函数；缺失信息返回空边/空图，不抛致命异常                  | 是                                             | 是；只读输入                 |
| `parseAll(SourceBundle) -> ParsedBundle`              | `SourceBundle`                   | `ParsedBundle`    | 单文件失败记录并跳过；整体不因单文件失败而失败               | 相同文件集合与 hash 产出稳定；输出按 path 排序 | 是；内部可并行，输出稳定排序 |

退出码映射：M09 不直接决定退出码；内部不可恢复错误映射 `6`；解析失败不触发 `1`；范围收集问题由 E4 映射 `3`；报告/校验由后续模块映射 `5`。

## 4. SourceUnit 到 ParsedSource 的映射

| 步骤            | 决策                                                         |
| --------------- | ------------------------------------------------------------ |
| 读取            | 仅读 `SourceUnit.content`；不读磁盘二次、不写磁盘            |
| 解析            | 从 `content` 构造 CompilationUnit；UTF-8；1-based 行列       |
| package         | 提取包声明；无包名记为 `""`，不记为 `null`                   |
| imports         | 记录普通、静态、通配 import；含名称、是否静态、是否通配、行号 |
| classes         | 记录类/接口/枚举/记录/注解；含限定名、简单名、类型、修饰符、注解、继承、实现、起止行 |
| methods         | 记录方法/构造器；含名称、参数类型、返回类型、修饰符、注解、所属类、起止行 |
| dependencyGraph | 生成该 `SourceUnit` 局部包/类依赖边                          |
| parseErrors     | 语法、编码、语言级别、超限、内部错误均写入                   |

## 5. DependencyGraph 设计

| 图       | 节点     | 边                               | 边属性                                 |
| -------- | -------- | -------------------------------- | -------------------------------------- |
| 包依赖图 | 包名     | `fromPackage -> toPackage`       | 类型、次数、来源文件、行号、列号、证据 |
| 类依赖图 | 类限定名 | `fromClass -> toClass/toPackage` | 类型、来源文件、行号、列号、证据       |

依赖方向：`from` 依赖 `to`。  
层级映射：M09 只提供原始图；层级由 M12 按包名、注解、规则元数据映射，不在 M09 硬编码业务分层。  
边类型：`IMPORT`、`STATIC_IMPORT`、`WILDCARD_IMPORT`、`EXTENDS`、`IMPLEMENTS`、`ANNOTATION`、`TYPE_REF`、`METHOD_CALL`、`FIELD_TYPE`、`PARAM_TYPE`、`RETURN_TYPE`。  
AR-01 支持：识别 Controller 与 Repository 类/包，查询是否存在直接依赖边。  
AR-02 支持：识别 domain 包到 web、jdbc、mq 等基础设施包的依赖边。

## 6. AR 规则用 JavaParser 的替代实现

| 规则                  | JavaParser 检测方式                                          | ArchUnit 语义对应                                            | 输出                     |
| --------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------ |
| AR-01 / SRS-AR-01-001 | 类依赖图中，Controller 层类直接指向 Repository 层类；注解 `@Controller/@RestController`、`@Repository` 与包名共同作为证据 | 禁止 `controller` 直接依赖 `repository`；允许经 Service 间接依赖 | 证据边、文件、行号、列号 |
| AR-02 / SRS-AR-02-001 | 包/类依赖图中，`domain` 包指向 `web`、`jdbc`、`mq` 等包      | 禁止 `domain` 依赖 Web/基础设施                              | 证据边、文件、行号、列号 |

`detection_method=HYBRID` 语义：M09 提供 AST 与依赖图硬证据；M12 结合规则元数据、注解、包名、命名启发式做最终判定。非纯 AST，非纯正则。  
`tool=CUSTOM`：不引入 ArchUnit；`ARCH_TEST` 改为 `HYBRID`。  
M09 不产出 `IssueCandidate`；`IssueCandidate` 由 M10/M12 按 E6、A6 生成。

## 7. ParsedSource 结构（对齐 E2）

| 字段              | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| `sourceUnit`      | 原 `SourceUnit` 引用或快照                                   |
| `package`         | 包名；无包名为 `""`                                          |
| `imports`         | import 列表；含名称、静态、通配、行号                        |
| `classes`         | 类/接口/枚举/记录/注解列表；含限定名、类型、注解、继承、起止行 |
| `methods`         | 方法/构造器列表；含所属类、签名、注解、起止行                |
| `dependencyGraph` | 该文件局部依赖图                                             |
| `parseErrors`     | 该文件解析错误列表                                           |

不新增 E2 顶层字段；统计信息放 `ParsedBundle`。

## 8. ParsedBundle 结构

| 字段              | 说明                                             |
| ----------------- | ------------------------------------------------ |
| `bundleId`        | 稳定标识，可由范围与 hash 生成                   |
| `sourceBundleRef` | 来源 `SourceBundle` 引用                         |
| `parsedSources`   | `ParsedSource` 列表，按 path 排序                |
| `dependencyGraph` | 合并后的局部依赖图                               |
| `parseErrors`     | 全局解析错误列表                                 |
| `stats`           | 文件数、成功数、跳过数、边数、类数、方法数、耗时 |
| `partial`         | DIFF 模式下为 `true`，表示依赖图可能不完整       |
| `mode`            | `FULL` / `DIFF`                                  |

## 9. ParseError 结构

| 字段             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| `file`           | 路径，统一 `/`                                               |
| `line`           | 1-based；未知为 `0`                                          |
| `column`         | 1-based；未知为 `0`                                          |
| `message`        | 错误信息                                                     |
| `errorType`      | `SYNTAX`、`ENCODING`、`LANGUAGE_LEVEL`、`IO`、`AST_LIMIT`、`INTERNAL`、`SKIP` |
| `severity`       | `ERROR`、`WARNING`、`INFO`                                   |
| `recoverable`    | 是否可跳过继续                                               |
| `sourceUnitPath` | 来源路径                                                     |
| `sourceHash`     | 来源 hash                                                    |

## 10. 增量解析

| 项             | 决策                                                         |
| -------------- | ------------------------------------------------------------ |
| DIFF 模式      | 只解析 `SourceBundle` 内变更文件                             |
| 依赖图合并     | M09 默认不合并变更前后全量图；只输出当前 bundle 局部图       |
| 全量合并       | 若上层已有全量 `ParsedBundle`，按 `path + hash` 替换节点/边；删除文件按全量基准移除 |
| 跨文件依赖     | DIFF 下未变更文件不在 bundle 时，跨文件边可能缺失，标记 `partial=true` |
| 规则需要全量图 | 由 M12 请求 E4 扩展范围，或使用缓存全量 `ParsedBundle`；M09 不擅自扩范围 |
| 接口不变       | `parseAll(SourceBundle)` 签名不变，不新增历史图参数          |

## 11. 只读保证

- 不修改源码文件、不写回磁盘。
- 不写 `.class`、不执行 `javac`、不执行 Maven/Gradle 编译。
- 不解析字节码，不加载业务类，不运行应用。
- 不访问网络，不训练模型，不提交代码。
- 关闭词法保留，避免产生修改语义。
- 仅内存 AST、依赖图、错误列表。

## 12. 异常与降级

| 场景            | 处理                                                     |
| --------------- | -------------------------------------------------------- |
| 单文件解析失败  | 捕获，写 `ParseError`，跳过，继续其他文件                |
| Java 版本不支持 | 降级到 `JAVA_17`；仍失败则记录 `LANGUAGE_LEVEL` 并跳过   |
| 编码问题        | 严格 UTF-8；非法字节记录 `ENCODING`，跳过，不自动猜 GBK  |
| 大文件          | 沿用 E4：单文件 1 MiB；超限不进入 M09 或记录 `AST_LIMIT` |
| 超大 AST        | 超 E4 总行/总读取上限时记录 `AST_LIMIT`，跳过该文件      |
| 非 Java 文件    | 返回空解析，记录 `SKIP`，不致命                          |
| 内部异常        | 记录 `INTERNAL`，单文件隔离；整体不可恢复映射退出码 `6`  |

## 13. 性能与规模

| 项         | 决策                                                         |
| ---------- | ------------------------------------------------------------ |
| 解析上限   | 沿用 E4：变更文件 2000；单文件 1 MiB；总行 200k；总读取 100 MiB |
| 大项目耗时 | 线性于文件数与大小；小文件毫秒级，大文件百毫秒级；2000 文件可并行 |
| 并发       | 按文件并行；并发度 MVP 阶段写死为 min(CPU/2, 4)，不可配置。如需配置化，走 E2 配置清单评审 |
| 内存控制   | 逐文件解析；不保留 tokens；关闭词法保留；关闭符号求解；注释按需保留 |
| 图内存     | 边去重并聚合计数；证据保留有限行号/列号；避免重复字符串      |
| 超限策略   | 超 E4 上限不解析；记录 `AST_LIMIT`；不导致整体失败           |

## 14. 模块接口（对齐 E2 M09 契约）

| E2 项       | 本设计                                                       |
| ----------- | ------------------------------------------------------------ |
| 职责        | JavaParser 解析 Java 17+，产出 AST、包依赖、类/方法、行号    |
| 输入        | `SourceUnit`、语言级别、解析配置                             |
| 输出        | `ParsedSource`、`DependencyGraph`、`ParseError`              |
| 对外接口    | `parse(SourceUnit) -> ParsedSource`；`analyzePackageDeps(ParsedSource) -> DependencyGraph`；`parseAll(SourceBundle) -> ParsedBundle` |
| 依赖方向    | 依赖 JavaParser、M18；被 M12 依赖                            |
| 可关闭/降级 | 非 Java 跳过；单文件失败记录并跳过，不致命                   |

## 15. 测试策略

- 单元：package、imports、类、方法、行号、列号提取。
- 语法：合法 Java 17、记录、密封类、模式匹配、文本块。
- 异常：语法错误、空文件、非 Java、非法 UTF-8、超限文件。
- 版本：Java 17 基线、Java 21 源、未知语言级别降级。
- AR：Controller 直接依赖 Repository、domain 依赖 web/jdbc/mq、允许经 Service。
- 增量：DIFF 只解析变更文件、`partial=true`、跨文件边缺失提示。
- 幂等：相同输入多次解析结果稳定。
- 并发：多线程 `parseAll` 输出稳定排序。
- 只读：解析前后源码 hash、mtime 不变；无 `.class` 生成。
- Schema：`ParsedSource`、`ParsedBundle`、`ParseError` JSON 可校验；不触碰 A5/A6。

## 16. 未决问题

1. Java 21 语言级别是否在 MVP 开放，或仅 `JAVA_17`。
2. 注释保留是否可配置；如需，走 E2 配置清单评审。
3. DIFF 全量图合并归属：M09 缓存还是 M12/M13 编排。
4. 列号未知时 `0` 是否被 A6 下游接受。
5. 内部 AST 节点上限是否需独立配置；当前沿用 E4。
6. 并发度是否纳入配置；如需，走 E2 配置清单评审。
7. `partial=true` 时 AR 规则是否允许命中，或仅告警。
8. 方法调用边深度：仅类型引用，还是包含链式调用。

## 17. 对 E8～E11 的影响

| 阶段                             | M09 给该阶段的输入                                           |
| -------------------------------- | ------------------------------------------------------------ |
| E8 LLM（M11）                    | 不直接依赖 M09；两者输出均供 E9 使用                         |
| E9 规则引擎（M12/M13）           | 消费 `ParsedSource`、`DependencyGraph`；AR 规则用图查询 + HYBRID 判定；需处理 `partial`；M12 消费图，M13 消费证据与告警 |
| E10 报告（M14/M15/M16/M17）      | `ParseError` 可作为告警输出；证据保留 `file/line/column`；不改 A6 issue 必填字段 |
| E11 可观测/CLI/退出码（M18/M19） | 解析耗时、跳过文件数、AST 超限、语言降级事件；整体不可恢复映射退出 `6` |

其他影响：

| 项           | 影响                                                         |
| ------------ | ------------------------------------------------------------ |
| M10 静态工具 | 可复用 M09 依赖图，避免重复解析；不重复产出 AST              |
| E4 范围收集  | DIFF 模式可能需扩展范围或提供缓存全量图；路径、编码、行号契约保持一致 |
| E3 配置      | 不新增解析配置键；语言级别、注释保留若需可配置，走 E2 配置清单评审 |