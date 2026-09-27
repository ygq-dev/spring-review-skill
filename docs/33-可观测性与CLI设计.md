# 33-可观测性与CLI设计

状态：E11 设计冻结候选  
范围：M18 可观测性与日志、M19 退出码与 CLI 反馈、M01 CLI 入口  
约束：只读审查；不修改代码、不提交、不训练模型；日志/指标不写目标仓库；密钥、prompt 原文、response 原文不入日志/指标；退出码严格按 E2 冻结；不新增 E3 未冻结配置字段。

## 1. 设计原则

1. 只读优先：日志、指标、trace 仅用于观测；不写目标仓库源码，不改变审查对象。
2. 结构化优先：日志以稳定字段表达事件；指标以稳定名称、标签、单位表达；CLI 摘要与错误分离。
3. 脱敏默认：任何进入日志、指标、控制台、异常消息的值先经 M18.redact；密钥、prompt/response 原文、敏感代码片段禁止出现。
4. 契约唯一：M18/M19/M01 接口严格对齐 E2；退出码严格对齐 E2；CLI 参数严格对齐 E3。
5. 降级不中断：可观测性失败不阻断审查；M18 日志初始化失败降级控制台；M19 未知异常映射为 6。
6. stdout/stderr 分离：正常摘要走 stdout；日志、警告、错误、校验失败路径走 stderr；CI 可稳定解析。

## 2. M18 可观测性与日志设计

### 2.1 结构化日志规范

- 输出通道：默认 stderr；不写目标仓库。
- 格式：默认文本 key=value；字段稳定；CI 可重定向为日志文件。
- 编码：强制 UTF-8；Windows 下不依赖 GBK。
- 时间：ISO-8601 UTC，字段 `ts`。
- 必含字段：`ts`、`level`、`logger`、`trace_id`、`module`、`event`、`message`。
- 事件字段：`duration_ms`、`status`、`count`、`path`、`tool`、`rule_id`、`report_id`、`exit_code`、`error_type`。
- 事件名：点分小写；E10 冻结事件必须使用 `report.generate`、`report.validate`、`report.write`、`report.archive`、`report.update_latest`。
- 参数化：使用 SLF4J 参数化，禁止字符串拼接敏感值。
- 幂等：同一事件重复记录不改变语义。

### 2.2 日志级别策略

- ERROR：审查失败、内部异常、报告校验失败、规则加载失败、CLI 解析失败。
- WARN：降级、跳过、超时、重试、工具 FAILED/TIMEOUT、LLM 降级、strict 下升级前警告。
- INFO：启动、结束、范围摘要、规则数量、工具状态、LLM 汇总、聚合摘要、报告生成/归档、退出码。
- DEBUG：参数解析结果、配置来源、文件列表、规则 ID、工具命令脱敏后、逐指标、并发调度。
- TRACE：不设计、不启用；最细为 DEBUG。
- 默认级别：`logging.level=INFO`，与 E3 对齐；`--log-level` 覆盖。

### 2.3 traceId 生成与传递

- 生成：每次 M01 `run` 开始生成一次；UUID v4 去连字符，32 位 hex。
- 绑定：生成后写入 MDC；M18.traceId() 读取当前值。
- 传递：同步调用直接继承；异步、线程池、CompletableFuture、虚拟线程必须复制 MDC。
- 并发：同一审查 run 共享同一 traceId；LLM 并发请求不新建 traceId。
- 缺失：无 traceId 时 M18.traceId() 返回空字符串，不抛异常。

### 2.4 脱敏规则（与 E3 对齐）

- 密钥来源：环境变量、CI Secrets、本地 `.env.local`；值永不记录。
- 字段名命中即脱敏：`key`、`secret`、`token`、`password`、`passwd`、`pwd`、`authorization`、`api_key`、`apikey`、`access_key`、`private_key`、`credential`、`cookie`、`session`。
- URL 脱敏：userinfo、query 中的 token/key/secret 替换为 `[REDACTED]`。
- 路径脱敏：用户主目录替换为 `~`；Windows 用户目录同样处理。
- 代码脱敏：不记录完整文件内容、敏感代码片段；仅记录路径、行号、规则 ID、哈希或计数。
- LLM 脱敏：不记录 prompt 原文、response 原文；仅记录模型、token 数、耗时、状态、错误类型。
- redact(value)：null 安全、幂等、不抛异常；失败返回 `[REDACTED]`。

### 2.5 指标采集与暴露

- 采集：内存注册表；通过 `metric(name, tags, value)` 写入。
- 类型：Counter、Gauge、Timer；内部统一为数值。
- 标签：低基数；禁止文件路径、完整规则 ID 作为标签；可用 module、tool、status、severity、category、provider。
- 暴露：INFO 摘要到 stderr；DEBUG 逐指标到 stderr；最终映射进 A6 `metrics`；不单独写目标仓库。
- JMX：可选，不默认；启用需后续配置清单评审。
- 失败：采集失败丢弃该指标，不中断审查；可计数 `metric_dropped_total`。

### 2.6 与 Logback 集成

- 使用 Logback 作为 SLF4J 实现。
- 控制台 Appender 输出 stderr；编码 UTF-8。
- Pattern 含 `trace_id`、`module`、`event`。
- 日志初始化失败：降级为控制台基础输出；不阻断审查；尽力记录 `log_init_failures_total`。
- Windows：JVM 与 Console Appender 均强制 UTF-8；不依赖默认 GBK。

### 2.7 与 SLF4J 集成

- 所有模块通过 M18.logger(name) 获取 Logger，不直接散落 LoggerFactory。
- M18 依赖 SLF4J/Logback；被所有模块依赖。
- M18 不可关闭；日志初始化失败降级控制台。
- 对外接口保持：`logger(name) -> Logger`、`metric(name, tags, value) -> void`、`traceId() -> String`、`redact(value) -> String`。

## 3. 日志内容规范

### 3.1 各模块日志级别

| 模块                     | DEBUG                  | INFO                                                 | WARN                | ERROR                  |
| ------------------------ | ---------------------- | ---------------------------------------------------- | ------------------- | ---------------------- |
| M01 CLI                  | 参数解析、互斥检查     | 启动、结束、退出码                                   | 参数即将失败        | 参数错误、未知命令     |
| M02/M03 范围             | 文件列表、排除规则     | 文件数、行数、范围摘要                               | 空范围、跳过文件    | 范围解析失败           |
| M04/M05 配置/密钥        | 配置来源、字段名       | 配置加载完成                                         | 缺失可选配置        | 配置/密钥加载失败      |
| M06 规则加载             | 规则 ID、文件路径      | 加载数量、版本                                       | 跳过规则、重复规则  | 规则加载失败           |
| M07/M08 静态工具         | 命令脱敏、原始输出路径 | 工具状态、耗时、问题数                               | FAILED/TIMEOUT/跳过 | 工具不可执行           |
| M09/M10 JavaParser       | 文件、AST 节点数       | 解析摘要                                             | 解析降级            | 解析失败               |
| M11/M12/M13 LLM/规则引擎 | 规则 ID、模型、重试    | 请求数、token、匹配统计                              | 重试、降级、超时    | LLM 失败、规则引擎失败 |
| M14/M15 聚合             | 分组键                 | 聚合耗时、问题总数                                   | 降级 warning        | 聚合失败               |
| M16/M17 报告             | 报告路径、校验项       | report.generate/validate/write/archive/update_latest | 校验警告、归档跳过  | 校验失败、写入失败     |
| M18 可观测性             | 指标详情、脱敏命中     | 初始化摘要                                           | 日志降级、指标丢弃  | 不可用异常             |
| M19 退出码               | 映射依据               | 摘要输出、退出码                                     | 未知结果降级        | 未知异常映射 6         |

### 3.2 关键事件日志

| 事件        | 级别       | 必含字段                                                     |
| ----------- | ---------- | ------------------------------------------------------------ |
| 启动        | INFO       | trace_id、mode、repo、rules_dir、output_dir                  |
| 范围解析    | INFO       | trace_id、files、lines、duration_ms、status                  |
| 规则加载    | INFO       | trace_id、rules_loaded、rules_skipped、duration_ms           |
| 工具运行    | INFO/WARN  | trace_id、tool、status、duration_ms、issue_count、exit_code  |
| LLM 调用    | INFO/WARN  | trace_id、provider、model、requests、tokens、latency_ms、retries、status |
| 聚合        | INFO       | trace_id、total_issues、by_severity、by_category、blocking、duration_ms |
| 报告生成    | INFO       | trace_id、report_id、report.generate、duration_ms            |
| 报告校验    | INFO/ERROR | trace_id、report_id、report.validate、status、error_path     |
| 报告写入    | INFO       | trace_id、report_id、report.write、latest_json、latest_md、history |
| 报告归档    | INFO/WARN  | trace_id、report_id、report.archive、status                  |
| 更新 latest | INFO/WARN  | trace_id、report_id、report.update_latest、status            |
| 结束        | INFO       | trace_id、exit_code、duration_ms、report_id                  |

### 3.3 禁止记录的内容

- 密钥、token、password、authorization、cookie、session、私钥。
- `.env.local`、CI Secrets、环境变量值。
- LLM prompt 原文、response 原文。
- 完整源码、敏感代码片段、完整 diff。
- 未脱敏的用户主目录、URL userinfo、query 密钥。
- 原始工具输出全文；仅记录路径、计数、状态、错误类型。

## 4. 指标采集设计

### 4.1 指标命名规范

- 小写 snake_case。
- 单位后缀：`_ms`、`_bytes`、`_total`、`_ratio`。
- Counter 用 `_total`；Gauge 用当前值；Timer 用 `_duration_ms` 或 `_latency_ms`。
- 标签低基数；禁止路径、完整规则 ID、文件名作为标签。
- E8 冻结字段保持原样：`llm_requests_total`、`llm_latency_ms`、`llm_tokens_total`、`llm_errors_total`、`llm_retries_total`。

### 4.2 关键指标清单

| 模块    | 指标                         | 类型    | 说明                 |
| ------- | ---------------------------- | ------- | -------------------- |
| M01     | run_total                    | Counter | 运行次数             |
| M01     | run_duration_ms              | Timer   | 总耗时               |
| M01     | exit_code_total              | Counter | 按 exit_code 标签    |
| M02/M03 | files_scanned_total          | Counter | 扫描文件数           |
| M02/M03 | lines_scanned_total          | Counter | 扫描行数             |
| M02/M03 | scope_errors_total           | Counter | 范围失败             |
| M04/M05 | config_load_errors_total     | Counter | 配置失败             |
| M04/M05 | secret_redactions_total      | Counter | 脱敏次数             |
| M06     | rules_loaded_total           | Counter | 加载规则数           |
| M06     | rules_load_errors_total      | Counter | 规则失败             |
| M07/M08 | tool_runs_total              | Counter | 按 tool、status      |
| M07/M08 | tool_duration_ms             | Timer   | 按 tool              |
| M07/M08 | tool_errors_total            | Counter | FAILED/TIMEOUT       |
| M07/M08 | tool_success_rate            | Gauge   | 成功/总运行          |
| M09/M10 | parse_files_total            | Counter | 解析文件数           |
| M09/M10 | parse_errors_total           | Counter | 解析失败             |
| M11     | llm_requests_total           | Counter | E8 冻结              |
| M11     | llm_latency_ms               | Timer   | E8 冻结              |
| M11     | llm_tokens_total             | Counter | E8 冻结              |
| M11     | llm_errors_total             | Counter | E8 冻结              |
| M11     | llm_retries_total            | Counter | E8 冻结              |
| M12/M13 | rule_matches_total           | Counter | 按 severity/category |
| M12/M13 | rule_match_duration_ms       | Timer   | 规则匹配耗时         |
| M14/M15 | aggregate_duration_ms        | Timer   | 聚合耗时             |
| M16/M17 | report_generate_total        | Counter | report.generate      |
| M16/M17 | report_validate_errors_total | Counter | report.validate      |
| M16/M17 | report_write_bytes           | Counter | report.write         |
| M16/M17 | report_archive_total         | Counter | report.archive       |
| M16/M17 | report_update_latest_total   | Counter | report.update_latest |
| M18     | log_init_failures_total      | Counter | 日志降级             |
| M18     | metric_dropped_total         | Counter | 指标丢弃             |
| M18     | redact_total                 | Counter | 脱敏次数             |

### 4.3 指标暴露方式

- 日志：INFO 输出摘要；DEBUG 输出逐指标。
- 文件：不单独写指标文件；报告内 `metrics` 即持久化。
- JMX：可选，不默认。
- 目标仓库：不写入。

### 4.4 与 A6 metrics 的关系

- A6 必填：`total_files_scanned`、`total_lines_scanned`、`duration_ms`、`tool_success_rate`。
- 映射见第 11 节。
- 其他指标仅内部观测，不写入 A6，除非 A6 允许扩展且经 Schema 评审。

## 5. traceId 设计

### 5.1 生成规则

- 每次 M01 `run` 生成一个 traceId。
- 格式：UUID v4 去连字符，32 位 hex。
- 不依赖环境变量；不新增配置字段。
- 同一 run 内所有模块共享。

### 5.2 传递机制

- M01 生成后写入 MDC。
- M18.traceId() 读取 MDC。
- 异步线程、线程池、LLM 并发、工具并发必须复制 MDC。
- 子任务不新建 traceId；如需 span，由后续设计评审，不新增 M18 接口。

### 5.3 在日志、报告、异常中的呈现

- 日志：字段 `trace_id`。
- 报告：不修改 A6；报告生成日志、控制台摘要携带 `report_id` 与 `trace_id`；报告本体不新增必填字段。
- 异常：异常消息或包装前缀 `[traceId=...]`；stderr 输出。
- CLI：摘要中显示 traceId，便于 CI 关联日志。

## 6. M19 退出码与 CLI 反馈设计

### 6.1 接口契约

| 接口                               | 输入         | 输出        | 异常契约                   | 幂等性 | 线程安全 |
| ---------------------------------- | ------------ | ----------- | -------------------------- | ------ | -------- |
| map(ReviewResult) -> int           | ReviewResult | 退出码      | 不抛异常；异常按未知映射 6 | 是     | 是       |
| mapException(Exception) -> int     | Exception    | 退出码      | 不抛异常；未知 6           | 是     | 是       |
| printSummary(ReviewResult) -> void | ReviewResult | stdout 摘要 | 不抛异常；失败降级 stderr  | 是     | 是       |

### 6.2 map(ReviewResult) -> int

- 正常结果：若达到 failOn，返回 1；否则返回 0。
- failOn 来源：M19 实例持有的 AppConfig；不新增 CLI 字段。
- 判定依据：A6 `summary.blocking`、`summary.by_severity`、`summary.by_category`。
- 空结果或非法结果：返回 6。
- 报告校验失败不由该接口返回 5；由异常路径或结果状态决定。

### 6.3 mapException(Exception) -> int

- CLI/配置异常：2。
- 范围/代码收集异常：3。
- 规则加载异常：4。
- 报告/输出/校验异常：5。
- 未知异常：6。
- 不返回 0、1；1 仅由 ReviewResult 判定。

### 6.4 printSummary(ReviewResult) -> void

- 输出到 stdout。
- 内容：report_id、latest JSON/Markdown、history、total_issues、by_severity、blocking、duration_ms、token_usage、tool_success_rate、trace_id。
- 校验失败：错误路径输出到 stderr。
- 大报告：摘要只输出关键计数，不输出 issue 明细。
- 失败降级：stdout 失败时 stderr 输出简化摘要。

## 7. 退出码映射表

| 退出码 | 场景                | 典型模块               |
| ------ | ------------------- | ---------------------- |
| 0      | 成功且未达到 failOn | 全流程                 |
| 1      | 达到 failOn         | M19 根据 ReviewResult  |
| 2      | CLI/配置错误        | M01 参数、M04/M05 配置 |
| 3      | 范围/代码收集失败   | M02/M03                |
| 4      | 规则加载失败        | M06                    |
| 5      | 报告/输出/校验失败  | M16/M17                |
| 6      | 内部错误/未知异常   | 任意模块               |

各模块失败映射：

- M01 参数错误、未知参数、互斥冲突：2，fail-fast。
- M04/M05 配置或密钥加载失败：2。
- M02/M03 范围解析失败、git diff 失败、代码收集失败：3。
- M06 规则目录不可读、规则解析失败：4。
- M07/M08 工具失败：默认 WARN，不直接退出；strict 下可计入 failOn 导致 1。
- M11 LLM 失败：默认降级 WARN；strict 下可计入 failOn 导致 1。
- M16/M17 报告生成、写入、校验失败：5；校验失败时 stderr 输出错误路径。
- 未知异常：6。

failOn 决定退出码 1：

- failOn 由 E3 冻结的 `--fail-on` 控制。
- 若 `summary.blocking` 或指定 severity/category 计数达到 failOn，map 返回 1。
- 未达到则 0。
- failOn 不改变 2-6 的映射。

strict 模式影响：

- strict 不改变退出码定义。
- strict 将非致命降级、工具 FAILED/TIMEOUT、LLM 降级、规则跳过、报告校验警告纳入 failOn 判定。
- 若因此达到 failOn，返回 1。
- strict 与 `--no-validate` 冲突时 fail-fast，返回 2。

## 8. CLI 输出设计

### 8.1 stdout

- 正常摘要。
- `--help`、`--version`。
- 报告路径：latest JSON、latest Markdown、history。
- 不输出日志、警告、错误堆栈。

### 8.2 stderr

- 所有日志。
- 警告、降级、重试。
- 错误、异常消息、traceId。
- 校验失败时的错误路径。
- 参数错误时的 usage。

### 8.3 控制台摘要格式

- 固定键值，顺序稳定：
  - `report_id`
  - `trace_id`
  - `total_issues`
  - `by_severity`
  - `by_category`
  - `blocking`
  - `fail_on`
  - `duration_ms`
  - `token_usage`
  - `tool_success_rate`
  - `latest_json`
  - `latest_md`
  - `history`
  - `exit_code`

### 8.4 CI 环境输出

- 检测 `GITHUB_ACTIONS=true`。
- 可选输出 GitHub Actions annotations：`::error`、`::warning`、`::notice`。
- annotation 消息必须脱敏；不输出源码片段。
- 默认不改变退出码；退出码仍由 M19 决定。

## 9. M01 CLI 入口设计

### 9.1 run(args, env) -> int

- 生成 traceId。
- 初始化 M18。
- 调用 parse(args)。
- 参数错误：stderr + usage，返回 2。
- 执行只读审查。
- 捕获异常，调用 M19.mapException。
- 调用 M19.printSummary。
- 返回退出码。
- 不修改代码、不提交、不训练模型。

### 9.2 parse(args) -> ReviewRequest

- 使用 picocli 4.7.6。
- 参数严格对齐 E3。
- 未知参数、缺值、非法枚举、互斥冲突：fail-fast，退出 2。
- `--help`、`--version`：stdout，退出 0。
- 不新增 E3 未冻结字段。

### 9.3 printUsage() -> void

- 正常 help：stdout。
- 参数错误：stderr。
- 内容包含所有 E3 冻结参数、默认值、互斥说明。

### 9.4 参数校验与失败行为

- 参数错误：stderr + usage，退出 2。
- 配置错误：M04/M05 映射 2。
- 范围错误：M02/M03 映射 3。
- 规则错误：M06 映射 4。
- 报告错误：M16/M17 映射 5。
- 未知异常：6。

### 9.5 picocli 集成方式

- 使用 picocli 注解定义命令与选项。
- 使用自定义 IExecutionExceptionHandler 捕获异常并交 M19。
- 使用 IExitCodeExceptionMapper 或等价机制映射退出码。
- 不输出 Java 代码；F 阶段实现。

## 10. CLI 参数总览

与 E3 冻结一致；默认值未显式处标注“E3 冻结默认/待 E3 配置清单确认”。

| 参数                | 用途            | 默认值     | 互斥/约束                      |
| ------------------- | --------------- | ---------- | ------------------------------ |
| --mode              | 审查模式        | diff       | 与 --base/--head 关系按 E4     |
| --repo              | 仓库路径        | .          | 只读                           |
| --base              | diff 基准       | main       | 仅 diff 模式有效               |
| --head              | diff 终点       | HEAD       | 仅 diff 模式有效               |
| --module            | 指定模块        | 空         | 与 --files 互斥                |
| --files             | 指定文件        | 空         | 与 --module 互斥               |
| --rules-dir         | 规则目录        | rules      | 只读                           |
| --output-dir        | 输出目录        | reports    | 不写目标仓库源码               |
| --config            | 配置文件        | 空         | 不覆盖 CLI 显式参数            |
| --offline           | 离线模式        | false      | 禁止网络；与 LLM 在线调用冲突  |
| --no-llm            | 禁用 LLM        | false      | 与 --llm-* 同用则忽略 LLM 参数 |
| --enable-checkstyle | 启用 Checkstyle | false      | 可组合                         |
| --enable-pmd        | 启用 PMD        | false      | 可组合                         |
| --enable-spotbugs   | 启用 SpotBugs   | false      | 可组合                         |
| --fail-on           | 失败阈值        | blocking   | 决定退出码 1                   |
| --format            | 输出格式        | both       | 与 --no-markdown 冲突          |
| --log-level         | 日志级别        | INFO       | 与 E3 logging.level 对齐       |
| --strict            | 严格模式        | false      | 与 --no-validate 冲突          |
| --no-markdown       | 不输出 Markdown | false      | 与 --format=md 冲突            |
| --no-validate       | 跳过报告校验    | false      | 与 --strict 冲突               |
| --llm-provider      | LLM 提供方      | E3/E8 默认 | --no-llm/--offline 下忽略      |
| --llm-model         | LLM 模型        | E3/E8 默认 | --no-llm/--offline 下忽略      |
| --llm-base-url      | LLM 地址        | E3/E8 默认 | --no-llm/--offline 下忽略      |
| --llm-timeout       | LLM 超时        | E3/E8 默认 | --no-llm/--offline 下忽略      |
| --max-concurrency   | 最大并发        | E3/E8 默认 | 与工具/LLM 并发共享            |

help 与 version：

- `--help`：stdout，退出 0。
- `--version`：stdout，退出 0。
- 版本信息含项目名、版本、Java 17+、Spring Boot 3.x 兼容说明。

## 11. 与 A6 metrics 的关系

| A6 metrics          | M18 来源            | 写入报告 | 说明                        |
| ------------------- | ------------------- | -------- | --------------------------- |
| total_files_scanned | files_scanned_total | 是       | 范围收集汇总                |
| total_lines_scanned | lines_scanned_total | 是       | 范围收集汇总                |
| duration_ms         | run_duration_ms     | 是       | 与 summary.duration_ms 一致 |
| tool_success_rate   | tool_success_rate   | 是       | 成功工具数/总工具数         |
| summary.duration_ms | run_duration_ms     | 是       | 总审查耗时                  |
| summary.token_usage | llm_tokens_total    | 是       | LLM token 汇总              |

仅内部使用，不写入 A6：

- exit_code_total、tool_errors_total、parse_errors_total、rule_match_duration_ms、report_write_bytes、log_init_failures_total、metric_dropped_total、redact_total。
- 如需写入报告，走 A6 Schema 评审；E11 不修改 A6。

## 12. 只读保证

- 日志不写目标仓库：默认 stderr；不创建仓库内日志文件。
- 指标不写目标仓库：仅内存与报告输出目录。
- 报告仅写 `--output-dir`；若该目录位于目标仓库，视为用户显式输出，不修改源码。
- 不记录敏感信息：密钥、prompt 原文、response 原文、敏感代码片段禁止。
- 不提交、不训练模型。

## 13. 异常与降级

| 场景                | 行为                                | 退出码 |
| ------------------- | ----------------------------------- | ------ |
| 日志初始化失败      | 降级控制台；不中断                  | 继续   |
| 指标采集失败        | 丢弃指标；计数 metric_dropped_total | 继续   |
| CLI 解析失败        | stderr + usage；fail-fast           | 2      |
| 配置加载失败        | mapException                        | 2      |
| 范围失败            | mapException                        | 3      |
| 规则加载失败        | mapException                        | 4      |
| 报告/校验失败       | stderr 输出错误路径；mapException   | 5      |
| 未知异常            | 记录 traceId；mapException          | 6      |
| LLM 降级            | WARN；strict 下计入 failOn          | 0 或 1 |
| 工具 FAILED/TIMEOUT | WARN；strict 下计入 failOn          | 0 或 1 |

## 14. 性能与规模

- 日志开销：INFO 只输出摘要；DEBUG 才输出文件/规则级细节；禁止大循环内 INFO。
- 指标开销：内存计数；低基数标签；避免每文件 Timer 高基数。
- 大项目：范围与解析流式处理；CLI 最终摘要只输出关键计数。
- 大报告：报告写文件；stdout 不输出 issue 明细。
- 并发：MDC 复制；指标注册表线程安全；退出码映射无共享可变状态。
- Windows：强制 UTF-8；避免 GBK 乱码导致日志/报告解析失败。

## 15. 模块接口（对齐 E2 M18/M19/M01 契约）

| 模块 | 接口                               | 说明               |
| ---- | ---------------------------------- | ------------------ |
| M18  | logger(name) -> Logger             | 包装 SLF4J         |
| M18  | metric(name, tags, value) -> void  | 低基数标签         |
| M18  | traceId() -> String                | 当前 run traceId   |
| M18  | redact(value) -> String            | 幂等、null 安全    |
| M19  | map(ReviewResult) -> int           | 正常结果映射 0/1   |
| M19  | mapException(Exception) -> int     | 异常映射 2/3/4/5/6 |
| M19  | printSummary(ReviewResult) -> void | stdout 摘要        |
| M01  | run(args, env) -> int              | 只读审查入口       |
| M01  | parse(args) -> ReviewRequest       | picocli 解析       |
| M01  | printUsage() -> void               | help/错误用法      |

依赖方向：

- M18 依赖 SLF4J/Logback；被所有模块依赖。
- M19 依赖 M18；被 M01、M03 依赖。
- M01 依赖 M02、M03、M19、M18；不被其他模块依赖。

## 16. 测试策略

- M18 单元测试：结构化字段、级别过滤、traceId、redact 幂等、null 安全、指标注册。
- 脱敏测试：密钥、URL、路径、LLM prompt/response 禁止出现。
- 日志初始化失败测试：降级控制台，不中断。
- M19 契约测试：map/mapException/printSummary 幂等、线程安全、不抛异常。
- 退出码矩阵：0-6 全覆盖；各模块失败映射；failOn；strict。
- CLI 集成测试：picocli 解析、help/version、互斥、未知参数、退出码 2。
- traceId 并发测试：线程池、LLM 并发、MDC 复制。
- A6 metrics 映射测试：必填字段存在且一致。
- 只读测试：日志/指标不写目标仓库。
- UTF-8 测试：Windows 控制台、报告文件、日志。
- 快照测试：控制台摘要格式稳定。

## 17. 未决问题

- `--fail-on` 的默认值与取值枚举需以 E3 配置清单为准。
- strict 下工具 FAILED/TIMEOUT、LLM 降级是否统一计入 failOn，需 E2/E3 确认。
- report_id 与 traceId 是否需要在 A6 中建立显式关联；当前不修改 A6。
- JMX 是否默认启用；当前不默认，不新增配置。
- GitHub Actions annotations 是否默认开启；当前检测环境可选输出。
- 日志默认文本还是 JSON Lines；当前文本 key=value，JSON 需后续评审。
- 大日志文件重定向策略由 CI 负责；不新增日志文件配置字段。
- 并发 span 是否需要；当前不新增 M18 接口。

## 18. 对 F 阶段的影响

- F 阶段按本设计实现 M18、M19、M01；不得修改 E1-E10、A5、A6、B1-B7、SKILL.md、docs/23-32。
- 实现顺序建议：M18 日志/脱敏/指标/traceId → M19 退出码/摘要 → M01 picocli 入口 → 集成测试。
- 必须落地：Logback UTF-8 配置、picocli 命令、异常分类、退出码矩阵、指标注册表、脱敏工具、A6 metrics 映射。
- 验收标准：退出码 0-6 全覆盖；日志/指标不写目标仓库；密钥/prompt/response 不入日志；Windows UTF-8 正常；M18/M19/M01 接口与 E2 完全一致。
- 风险：failOn 与 strict 交互需在 F 前确认；A6 metrics 扩展需走 Schema 评审；CI annotations 默认行为需在 F 阶段验证。
- E11 作为 E 阶段最后一个任务，冻结本设计后，F 阶段不得再新增 E3 未冻结配置字段；如需，走 E2 配置清单评审。