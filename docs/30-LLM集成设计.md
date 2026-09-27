# docs/30-LLM集成设计.md

阶段：E8；模块：M11；状态：设计候选。  
约束：只读审查；不修改代码、不提交、不训练模型；不引入 Spring AI、LangChain4j、向量库；不新增 E3 未冻结配置字段；不改 A2/A4.2/A5/A6 结构。

## 1. 设计原则

1. 只读与最小暴露：M11 只发起模型调用，不写目标仓库、不落盘、不训练、不缓存明文 prompt/response。
2. 契约稳定：严格对齐 E2 的 M11 接口、LlmRequest、LlmResponse、退出码；不扩展 A5/A6 Schema。
3. 多厂商 OpenAI 兼容：默认 DeepSeek，可切 OpenAI / Qwen / 自定义兼容端点；协议差异收敛在适配层。
4. 可关闭与可降级：`llm.enabled=false` 或无密钥时进入 offline，语义规则跳过，静态/AST/规则引擎继续。
5. 安全与可观测：密钥只走环境变量 / CI Secrets / `.env.local`；日志、指标、trace、报告全链路脱敏。
6. 配置冻结：仅使用 E3 已冻结字段；重试、队列、成本单价等作为内部常量或后续 E2 配置清单评审项。

## 2. LLM 提供方与协议

协议：OpenAI 兼容 `POST {baseUrl}/chat/completions`，JSON 请求/响应，鉴权 `Authorization: Bearer <secret>`。  
请求核心：`model`、`messages`、`temperature`、`max_tokens`、`response_format`。  
响应核心：`choices[0].message.content`、`choices[0].finish_reason`、`usage`、`error`。

| provider | 默认 baseUrl                                        | 默认 model      | 密钥变量约定                          | 说明                                             |
| -------- | --------------------------------------------------- | --------------- | ------------------------------------- | ------------------------------------------------ |
| deepseek | `https://api.deepseek.com`                          | `deepseek-chat` | `DEEPSEEK_API_KEY`                    | E1/E3 冻结默认                                   |
| openai   | `https://api.openai.com/v1`                         | `gpt-4o-mini`   | `OPENAI_API_KEY`                      | 切换时使用；默认值需 E2 评审确认                 |
| qwen     | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus`     | `DASHSCOPE_API_KEY` 或 `QWEN_API_KEY` | 兼容模式；默认值需 E2 评审确认                   |
| custom   | 必填                                                | 必填            | `<PROVIDER>_API_KEY`                  | 自定义 OpenAI 兼容；密钥映射如需自定义走 E2 评审 |

路径拼接：`baseUrl` 去尾斜杠后追加 `/chat/completions`。  
差异：OpenAI / DeepSeek / Qwen 最小请求集一致；Qwen 兼容模式可能忽略部分 OpenAI 扩展字段；DeepSeek 默认 `baseUrl` 不含 `/v1`，按 `/chat/completions` 调用。

## 3. M11 接口设计

| 接口                                  | 输入       | 输出        | 异常契约                                                     | 幂等性                                               | 线程安全                                                     |
| ------------------------------------- | ---------- | ----------- | ------------------------------------------------------------ | ---------------------------------------------------- | ------------------------------------------------------------ |
| `complete(LlmRequest) -> LlmResponse` | LlmRequest | LlmResponse | 不抛网络/超时/解析异常，封装为 `LlmResponse.error`；编程错误可抛内部异常 | 无外部副作用；重试可能产生不同 content，调用方需容忍 | 线程安全；单例无状态；HttpClient 线程安全；并发受 `maxConcurrency` 控制 |
| `health() -> LlmHealth`               | 无         | LlmHealth   | 不抛；返回状态                                               | 幂等                                                 | 线程安全；可短 TTL 缓存，内部常量                            |
| `enabled() -> boolean`                | 无         | boolean     | 不抛                                                         | 幂等                                                 | 线程安全；只读配置与密钥可解析性                             |
| `provider() -> String`                | 无         | provider 名 | 不抛                                                         | 幂等                                                 | 线程安全                                                     |

LlmHealth：`status`、`provider`、`model`、`latencyMs`、`error`、`checkedAt`、`traceId`。  
status 枚举：`UP`、`DOWN`、`DEGRADED`、`OFFLINE`。  
`health()` 默认只做配置/密钥可解析检查，不发起模型调用；网络探测走内部常量，不默认开启。

## 4. LlmRequest 结构（对齐 E2）

| 字段           | 类型          | 必填 | 默认                        | 说明                                          |
| -------------- | ------------- | ---- | --------------------------- | --------------------------------------------- |
| provider       | String        | 否   | AppConfig `llm.provider`    | 空则用配置；默认 deepseek                     |
| model          | String        | 否   | AppConfig `llm.model`       | 空则用配置；默认 deepseek-chat                |
| messages       | List<Message> | 是   | 无                          | role/content；role 支持 system/user/assistant |
| temperature    | Double        | 否   | AppConfig `llm.temperature` | 默认 0.1                                      |
| maxTokens      | Integer       | 否   | AppConfig `llm.maxTokens`   | 默认 4096                                     |
| responseFormat | Enum          | 否   | `TEXT`                      | `TEXT` / `JSON_OBJECT`；不新增配置字段        |
| timeout        | Long          | 否   | AppConfig `llm.timeoutMs`   | 毫秒；默认 60000                              |
| traceId        | String        | 否   | M18 生成                    | 透传可观测链路                                |

## 5. LlmResponse 结构（对齐 E2）

| 字段         | 类型     | 说明                                                         |
| ------------ | -------- | ------------------------------------------------------------ |
| content      | String   | 成功为模型文本；失败为 null                                  |
| usage        | Usage    | inputTokens / outputTokens / totalTokens                     |
| finishReason | Enum     | `stop` / `length` / `content_filter` / `tool_calls` / `error` / null |
| latencyMs    | Long     | 端到端耗时，含重试                                           |
| error        | LlmError | 失败时非空；成功为 null                                      |

tool_calls 仅作协议兼容占位；MVP 不产生此值。

LlmError：`code`、`message`、`retryable`、`providerStatus`、`traceId`。  
code 枚举建议：`NO_SECRET`、`OFFLINE`、`HTTP_ERROR`、`TIMEOUT`、`RATE_LIMITED`、`PARSE_ERROR`、`INVALID_REQUEST`、`INTERNAL`。

## 6. 调用流程

1. 请求构造：M12 构造 LlmRequest；M11 校验 messages、合并 AppConfig 默认值。
2. 密钥获取：M11 调 M02 `resolveSecret(name)`；M02 按 provider 映射密钥名；失败则 offline/error。
3. HTTP 调用：JDK 17 HttpClient；`POST {baseUrl}/chat/completions`；Bearer 鉴权；超时取 `timeout` 或 `llm.timeoutMs`。
4. 响应解析：Jackson 解析；取 `choices[0].message.content`、`finish_reason`、`usage`、`error`。
5. usage 记录：映射 `prompt_tokens`、`completion_tokens`、`total_tokens` 到 input/output/total。
6. 错误处理：HTTP、超时、限流、解析失败封装为 LlmResponse.error；按重试策略处理可重试错误。
7. 可观测：M18 记录 provider、model、traceId、latency、usage、status、retryCount；不记录 prompt/response 原文和密钥。

## 7. 密钥与安全

来源：环境变量、CI Secrets 注入的环境变量、本地 `.env.local`。  
解析：M11 不直接读文件；统一经 M02 `resolveSecret(name) -> SecretRef`。  
优先级：进程环境变量 / CI Secrets > `.env.local`；CI Secrets 视为环境变量。  
脱敏规则：`*_API_KEY`、`*_TOKEN`、`*_SECRET`、`Authorization`、`Cookie`、私钥 → `***`。  
安全硬约束：密钥绝不落盘、不入日志、不入报告、不入 trace、不入异常堆栈。  
与 M02 协作：M11 只拿 SecretRef 构造请求头；不缓存明文；AppConfig 不含密钥。

## 8. offline 模式

触发条件：
- `llm.enabled=false`。
- M02 无法解析密钥。
- provider/baseUrl/model 缺失或非法。
- 上层显式要求 offline。

与 M02 缺密钥降级：`resolveSecret` 失败 → M11 `enabled=false` → 语义规则跳过，不使整体失败。  
与 strict 模式协作：M11 不定义 strict 配置；若上层 strict 启用，则无密钥/LLM 不可用不被静默忽略，由 M19/M12 按退出码 2 或 failOn 策略处理；非 strict 则降级 offline。  
offline 时 `complete` 返回 `error.code=OFFLINE`，不发起 HTTP。

## 9. 多厂商适配

| provider | 鉴权   | 请求路径                               | 请求差异                         | 响应差异                |
| -------- | ------ | -------------------------------------- | -------------------------------- | ----------------------- |
| OpenAI   | Bearer | `/v1/chat/completions`                 | 标准 OpenAI                      | 标准 choices/usage      |
| DeepSeek | Bearer | `/chat/completions`                    | 兼容 OpenAI；默认 baseUrl 无 /v1 | 兼容 choices/usage      |
| Qwen     | Bearer | `/compatible-mode/v1/chat/completions` | 兼容模式；部分扩展字段可能忽略   | 兼容 choices/usage      |
| 自定义   | Bearer | `{baseUrl}/chat/completions`           | 以 OpenAI 最小集为准             | 缺失 usage 时置零并记录 |

适配策略：provider 只影响 baseUrl、model、密钥名、少量请求字段映射；业务语义不进入 M11。  
不新增配置字段：新增 provider 默认值走 E2 配置清单评审；自定义密钥名映射也走评审。

## 10. 结构化输出与工具调用

JSON 模式：可选。`LlmRequest.responseFormat=JSON_OBJECT` 时映射为 OpenAI 兼容 `response_format`。厂商不支持则降级 `TEXT`，由 M12 解析。  
Function Calling：默认不需要。M11 不实现工具调用循环、不注册工具、不执行函数。若后续需要，走 E2 评审。  
与 A6 issue 结构关系：M11 不生成、不校验 issue；只返回 content。M12 负责将 content 解析/校验为 A6 issue 结构，并设置 `detection_method`、`confidence`、`evidence` 等。  
A2 相关：需要 LLM 的规则通常是 `HYBRID`、`HEURISTIC`、`RULE_ENGINE`；confidence 仍由规则元数据指定。

## 11. 重试与限流

可重试：连接失败、连接超时、读超时、HTTP 408、429、500、502、503、504。  
不可重试：401、403、400、404、422、非法请求、密钥缺失、响应结构不可解析。  
重试次数：内部常量默认最多 2 次重试，总尝试 3 次；不新增配置字段。  
退避：指数退避，初始 500ms，倍增，加抖动，上限 5s；429 尊重 `Retry-After`。  
限流处理：429 优先退避；连续限流则返回 `RATE_LIMITED`。  
与 maxConcurrency 关系：`llm.maxConcurrency` 控制全局并发信号量；重试不突破并发上限；重试占用原请求配额。

## 12. 超时与并发

单请求超时：`LlmRequest.timeout` 或 `llm.timeoutMs`，默认 60000ms。  
连接超时：内部常量，建议 `min(10000, timeoutMs)`；不新增配置字段。  
并发上限：`llm.maxConcurrency`，默认 4；M11 内部信号量控制。  
队列与背压：有界队列，内部常量建议 `maxConcurrency * 2`；满时阻塞提交至超时，避免 OOM。  
线程安全：M11 单例；HttpClient 线程安全；请求级状态不共享；`enabled`/`provider` 只读。

## 13. 成本与 Token 统计

Token：从响应 `usage.prompt_tokens` / `completion_tokens` / `total_tokens` 映射 input/output/total。  
成本计算：内置单价表作为代码常量，不新增配置字段；未知单价只记录 token，不估算金额。  
与 metrics 关系：M18 记录 `llm_requests_total`、`llm_latency_ms`、`llm_tokens_total`、`llm_errors_total`、`llm_retries_total`。  
与 A6 token_usage 关系：不改 A6；由 M12/M14 从 `LlmResponse.usage` 透传到 `summary.token_usage`；该字段是 A6 冻结必填项。

## 14. 只读保证

不修改目标仓库：M11 不写文件、不 git add/commit、不改配置。  
不训练模型：不微调、不上传训练数据、不建向量库。  
不上传敏感代码：M11 不主动扫描仓库；只发送 M12 构造的 prompt。M12 负责脱敏/最小化；M11 不持久化 prompt/response。  
报告安全：Markdown + JSON 输出经 M18 脱敏；不含密钥、Authorization、Cookie、私钥。

## 15. 异常与降级

| 场景         | M11 行为                                       | M12 协作                                |
| ------------ | ---------------------------------------------- | --------------------------------------- |
| 无密钥       | offline，`enabled=false`，error.code=NO_SECRET | 语义规则跳过；非 strict 不失败          |
| 网络错误     | 可重试则重试；最终 error.code=HTTP_ERROR       | 跳过或降 confidence；不产出无证据 issue |
| 超时         | 重试后 error.code=TIMEOUT                      | 跳过该规则或标记低置信                  |
| 限流         | 退避重试；最终 error.code=RATE_LIMITED         | 跳过/延后；不直接退出 6                 |
| 响应解析失败 | error.code=PARSE_ERROR，content=null           | 不产出语义 issue；记录可观测            |
| 参数非法     | error.code=INVALID_REQUEST                     | 规则引擎侧修正或跳过                    |

## 16. 性能与规模

大项目、大 diff：M12 负责分片、摘要、按规则筛选；M11 不拼超大 prompt。  
单次调用时间上限：默认 60000ms；超时进入重试/错误。  
并发：默认 4；大项目通过队列背压，避免瞬时打满。  
调用规模控制：只对需要 LLM 的规则调用；相同输入不默认缓存，避免误用非幂等结果。  
单次输出限制：`llm.maxTokens` 默认 4096；超长由 M12 分片或摘要。

## 17. 模块接口（对齐 E2 M11 契约）

| 项          | 内容                                                         |
| ----------- | ------------------------------------------------------------ |
| 职责        | 抽象 LLM 调用，支持 OpenAI 兼容多厂商，默认 DeepSeek         |
| 输入        | LlmRequest、AppConfig、SecretRef                             |
| 输出        | LlmResponse、usage、延迟、错误                               |
| 对外接口    | `complete(LlmRequest) -> LlmResponse`；`health() -> LlmHealth`；`enabled() -> boolean`；`provider() -> String` |
| 依赖方向    | 依赖 HTTP Client、Jackson、M02、M18；被 M12 依赖             |
| 可关闭/降级 | 可关闭；offline 时 enabled=false，语义规则跳过               |
| 配置        | 仅 E3 冻结：llm.enabled/provider/baseUrl/model/timeoutMs/maxConcurrency/temperature/maxTokens |
| 密钥        | 环境变量 + CI Secrets + 本地 .env.local；经 M02 resolveSecret |

## 18. 测试策略

单元：LlmRequest 默认值合并、provider 路由、路径拼接、脱敏、错误映射。  
契约：OpenAI / DeepSeek / Qwen / 自定义兼容端点请求/响应最小集；不调真实模型。  
集成：Mock HTTP Server 覆盖 200、400、401、429、500、超时、畸形 JSON。  
并发：maxConcurrency 信号量、队列背压、重试不突破并发。  
安全：日志/指标/trace/报告无密钥、无 Authorization、无 Cookie、无私钥。  
offline：无密钥、enabled=false、strict/非 strict 行为。  
性能：大 prompt 由 M12 分片；M11 只验证超时与并发。  
真实模型测试：可选、默认关闭、需显式环境变量。

## 19. 未决问题

1. OpenAI / Qwen 默认 baseUrl、model 是否冻结，或仅作为内置建议。
2. 自定义 provider 密钥变量映射是否需配置化；如需要走 E2 配置清单评审。
3. 重试次数、退避、队列容量是否需配置化；当前为内部常量。
4. JSON 模式在各厂商的支持差异与降级策略。
5. 成本单价表维护方式；是否只记 token 不记金额。
6. strict 模式归属与退出码映射细节。
7. `health()` 是否允许网络探测；当前默认仅配置/密钥检查。
8. `summary.token_usage` 的聚合范围（是否含所有 LLM 调用）由 M14 决定。

## 20. 对 E9～E11 的影响

### E9 规则引擎（M12/M13）

M12 依赖 M11：构造 LlmRequest、传 traceId、responseFormat、temperature、maxTokens；消费 LlmResponse。  
M12 负责语义规则解析、A6 issue 校验、confidence 赋值；M11 不产出 issue。  
offline / 无密钥 / 调用失败时，M12 跳过需要 LLM 的 HYBRID、HEURISTIC、RULE_ENGINE 规则；静态、AST、ARCH_TEST 等不受影响。  
M13 规则元数据不变：detection_method、confidence 仍按 A2/A5 冻结结构。

### E10 报告（M14/M15/M16/M17）

报告模块不直接依赖 M11；通过 M12 产出的 issue 消费。  
消费 `LlmResponse.usage`，聚合到 `summary.token_usage`。  
Markdown + JSON 输出保持脱敏；不出现密钥、Authorization、Cookie、私钥。  
LLM 失败不应导致报告结构变化；仅影响语义 issue 是否出现及其 confidence。

### E11 可观测/CLI/退出码（M18/M19）

M18：记录 LLM 请求数、延迟、token、错误、重试、provider、model、traceId；全链路脱敏；不记录 prompt/response 原文。  
M19：处理 `llm.enabled`、offline、strict、failOn；退出码遵循 E2 冻结：0 成功；1 达到 failOn；2 CLI/配置；3 范围/代码收集；4 规则加载；5 报告/输出/校验；6 内部错误。  
LLM 调用失败不直接映射为 6；配置/密钥缺失可按 2；offline 不导致 6；是否因语义规则缺失触发 1 由 failOn 与 M12 协作决定。