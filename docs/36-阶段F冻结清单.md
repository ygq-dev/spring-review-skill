# 阶段 F 冻结清单

冻结日期：2026-09-26
阶段状态：冻结（代码实现层面）

## 1. 冻结产出

| 编号  | 产出                                             | 状态 | 备注                                                         |
| ----- | ------------------------------------------------ | ---- | ------------------------------------------------------------ |
| F1    | pom.xml + src/ 目录骨架 + 包结构                 | 冻结 | dev.springreview 包名                                        |
| F1    | scripts/fix_legacy.py                            | 冻结 | 遗留项修复（实际改用 strip）                                 |
| F1    | scripts/strip_tool_rule_id_null.py               | 冻结 | 清理 tool_rule_id: null                                      |
| F2    | M01 CLI（picocli）+ M18 可观测性 + M19 退出码    | 冻结 | ReviewCommand / Logs / Metrics                               |
| F2    | M02 配置（ConfigLoader / AppConfig / SecretRef） | 冻结 | CLI > env > dotenv > yaml > 默认                             |
| F2    | M03 编排器空壳                                   | 冻结 | 8 步工作流                                                   |
| F2    | 7 个单元测试                                     | 冻结 | Redactor / Trace / Metrics / ConfigLoader / ExitCodeMapper / ReviewResult / PathFilters |
| F3    | M04/M05/M06 范围收集                             | 冻结 | JGit + 文件扫描 + 过滤器                                     |
| F3.1  | LineRange 精算                                   | 冻结 | FileHeader.toEditList 合并相邻 Edit                          |
| F3.1  | DiffSourceReader                                 | 冻结 | DIFF 模式读新侧内容                                          |
| F4    | M07/M08 规则加载与选择                           | 冻结 | index.json 唯一入口；55 条 ACTIVE                            |
| F5    | M10 静态工具适配                                 | 冻结 | Checkstyle/PMD/SpotBugs 命令行走临时目录                     |
| F6    | M09 JavaParser 解析与依赖图                      | 冻结 | JAVA_17；关闭符号求解                                        |
| F7    | M11 LLM 客户端（OpenAI 兼容）                    | 冻结 | 默认 DeepSeek；offline 降级                                  |
| F8a   | M12/M13 规则引擎（确定性）                       | 冻结 | 5 个检测器 + 聚合去重排序                                    |
| F8b   | M12 LLM 语义分支                                 | 冻结 | parameters.use_llm=true 触发                                 |
| F9    | M14/M15/M16/M17 报告生成与归档                   | 冻结 | A6 兼容；latest 原子替换                                     |
| F10   | 端到端集成测试（FILES/MODULE）                   | 冻结 | 临时仓库 + 端到端链路                                        |
| F10.1 | DIFF 模式端到端测试                              | 冻结 | JGit 临时 Git 仓库                                           |
| F11   | CI 示例（GitHub Actions + 通用脚本）             | 冻结 | ci.yml / review.yml / ci-review.sh                           |
| F11   | .gitignore                                       | 冻结 | 屏蔽 target/、reports/、密钥                                 |

## 2. 冻结决定

### 2.1 构建与依赖
- Maven 3.8.9 + Java 17（17.0.3.1）+ Spring Boot 3.5.13 parent。
- 不引入任何 starter；仅用 parent 做版本管理与打包。
- picocli 4.7.6、JGit 6.10.0、JavaParser 3.26.2、networknt json-schema-validator 1.4.0。
- Checkstyle/PMD/SpotBugs 不进入运行时依赖，M10 通过命令行调用。

### 2.2 包结构
- `dev.springreview` 顶层入口。
- `cli`、`config`、`orchestration`、`scope`、`rules`、`parser`、`tools`、`llm`、`engine`、`report`、`observability`、`exit` 子包。

### 2.3 配置与密钥
- 优先级：CLI > 环境变量 > `.env.local` > YAML > 默认值。
- `.env.local` 支持短 key 别名（`LLM_MODEL` → `SPRING_REVIEW_LLM_MODEL`）。
- 密钥不进入 AppConfig；由 LlmClientFactory 从 env / dotenv 解析。
- 脱敏：`Redactor` 覆盖私钥块、Bearer、敏感 key=value、URL userinfo。

### 2.4 范围收集
- DIFF：JGit 只读；两点 diff；`FileHeader.toEditList` 精算新侧行号。
- MODULE/FILES：递归扫描；单文件 1 MiB 上限；UTF-8；SHA-256 hash。
- 默认排除 target/build/generated/vendor/node_modules/.git + 二进制/图片/压缩包扩展名。

### 2.5 规则加载
- 唯一入口 `rules/index.json`；多文档 YAML → JsonNode → A5 Schema 校验。
- 索引一致性：category/subcategory/severity/status/rule_version。
- 默认仅加载 ACTIVE；55 条全 ACTIVE。

### 2.6 静态工具
- 工具 jar/可执行路径不进入配置字段；用约定目录 + 环境变量。
- 工具缺失 → SKIPPED，不报错。
- 未映射的工具问题默认丢弃。
- 串行执行，总预算 600s。

### 2.7 规则引擎
- 5 个检测器：AR 专用、Build 文件、配置 key、通用正则、AST 回退。
- LLM 语义检测器：仅 `parameters.use_llm=true` 触发。
- 去重：rule+file+line+msg，保留最高 confidence。
- 排序 6 级：severity/confidence/file/line/column/rule_id。
- issue_id = SHA-256(fingerprint) 前 8 位大写。

### 2.8 报告
- A6 Schema 单一事实源；顶层 8 必填字段。
- report_id：RPT-YYYY-NNNN；同日按历史归档数 + 1。
- latest 原子替换；history/YYYY/MM/DD/<report_id>/。
- JSON 是主产物；Markdown 可降级。

### 2.9 可观测性与退出码
- 日志默认 stderr；trace_id 走 MDC；UTF-8 强制。
- 退出码 0~6 冻结：0 成功；1 failOn；2 CLI/配置；3 范围；4 规则加载；5 报告/输出/校验；6 内部。

### 2.10 CI
- GitHub Actions：`ci.yml`（构建 + 测试）、`review.yml`（PR 只读审查，默认 offline）。
- 通用脚本：`scripts/ci-review.sh` / `.ps1`。
- 报告作为 artifact 保留 14 天。

## 3. 遗留问题（不阻塞 MVP）

### 3.1 规则库内容
- 55 条规则的 `parameters.patterns` / `forbidden_keys` 等**未填**；
  实际运行时检测器返回空集合，导致审查报告 0 issues。
- 需要在 F.1 / G 阶段逐条补全 `parameters`，让每个规则可真实检出。

### 3.2 静态工具配置
- `config/checkstyle/checkstyle.xml` 与 `config/pmd/pmd.xml` 未落地；
  Runner 目前 `SKIPPED`（`CONFIG_NOT_FOUND`）。
- 需 F5.1 补工具配置；同时需工具 jar/executable。

### 3.3 LLM Token 统计
- LLM 调用记录与 token usage 未汇总到 `summary.token_usage` 和 `metrics.llm_calls`。
- 需 F8b.1 补。

### 3.4 报告时间戳 / 命名
- 多个并行的临时测试都在自己的临时目录下产生 `RPT-2026-0001`；
  真实项目连续跑才会 N+1。当前无冲突。

### 3.5 DIFF 边界
- rename/copy 类型的端到端测试未做（F10.2）。
- 空 diff（HEAD == HEAD~1）的降级未做（F10.2）。

### 3.6 规则选择精度
- M08 首发版过滤逻辑为简化版；`applies_to` 版本表达式、文件类型、
  tool/detection_method 与 AppConfig 的联动、module_type 精确匹配均未做。
- 需 F4.1 补齐。

### 3.7 stderr 编码
- Windows Git Bash 下日志中文可能显示为 GBK 乱码；
  跑 java 时加 `-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8`。

## 4. 下一步

阶段 F 完成后，MVP 的"跑通只读审查闭环"目标达成：
- 3 种模式（DIFF/MODULE/FILES）可运行
- 55 条规则可加载、可分发到检测器
- 报告符合 A6
- 有单元测试 + 端到端测试 + CI

建议后续：
- **G 阶段（MVP 收口）**：
  - G1：补全 55 条规则的 parameters，让规则真实检出；
  - G2：补 `config/checkstyle/checkstyle.xml` 与 `config/pmd/pmd.xml`；
  - G3：真实项目验证（A1 要求 ≥ 3 个 Spring Boot 项目）；
  - G4：基准评测集落地（A1 要求 ≥ 30 案例）；
  - G5：性能验证（1000 行 diff P95 ≤ 60s）。