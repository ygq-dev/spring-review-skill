---
name: spring-review
description: >-
  审查 Java 17+/Spring Boot 3.x 项目代码是否符合团队规范。
  当用户要求 review Spring 代码、检查事务、AOP、Bean 线程安全、
  Spring MVC 规范、SQL 注入、硬编码密钥、分层依赖、异常处理、
  资源泄漏、日志可观测性等问题时使用。
version: 1.0.0

---

# Spring Review Skill

## 1. 元信息

- name: `spring-review`
- description: 审查 Java 17+/Spring Boot 3.x 项目代码是否符合团队规范。触发关键词：review Spring 代码、Spring 代码审查、事务、AOP、Bean 线程安全、Spring MVC 规范、SQL 注入、硬编码密钥、分层依赖、异常处理、资源泄漏、日志可观测性。
- version: `1.0.0`

## 2. 目标

本 Skill 对 Java 17+/Spring Boot 3.x 项目执行只读代码审查：根据 `git diff`、指定模块或指定文件确定范围，加载规则库，运行静态检查与规则匹配，汇总问题并生成符合 Schema 的 JSON 报告与 Markdown 报告。本 Skill 不修改目标仓库任何文件，不自动修复代码，不执行提交、推送、分支创建、PR 合并，不训练模型。

## 3. 能力范围

本 Skill 可以做：

- 审查 `git diff`：审查工作区、暂存区或指定 base/head 之间的 Java/Spring 变更。
- 审查指定模块：审查 Maven/Gradle 模块中的 Java/Spring 代码。
- 审查指定文件：审查用户给定 Java/Spring 文件列表。

### 3.1 执行入口（如何运行）

本 Skill 的审查能力由仓库内 Java CLI 承载，agent 按以下顺序执行：

```bash
# 1. 构建（首次或代码变更后；仓库根目录执行）
mvn -q -B -DskipTests package

# 2. 查看全部参数
java -jar target/spring-review-skill-*.jar --help

# 3. 典型调用（审查 git diff，默认离线）
java -jar target/spring-review-skill-*.jar \
  --mode DIFF --repo <目标仓库路径> \
  --base <base-ref> --head <head-ref> \
  --offline \
  --output-dir reports
```

报告输出于 `reports/latest/review-report.json` 与 `review-report.md`。构建与运行的完整前置条件（Java 17+、Maven 3.8+）见 README《快速开始》。LLM 语义规则需要配置 API Key 环境变量并去掉 `--offline`，静态分析完全本地执行。

## 4. 非能力范围

本 Skill 明确禁止：

- 不修改代码。
- 不自动修复代码。
- 不执行 `git commit`、`git push`、分支创建、PR 合并。
- 不训练模型。
- 不审查非 Java / 非 Spring 代码。
- 不做安全渗透测试。
- 不做依赖漏洞扫描。
- 不处理二进制文件。
- 不审查生成代码。
- 第一版不支持 IDE 插件、SaaS。

## 5. 输入

支持三种模式，写入 `scope.mode`：

- `DIFF`
- `MODULE`
- `FILES`

输入参数说明：

| 参数                 | 必填          | 说明                                                      |
| -------------------- | ------------- | --------------------------------------------------------- |
| `mode`               | 是            | `DIFF`、`MODULE`、`FILES` 之一                            |
| `repo_root`          | 是            | 目标仓库根目录，只读访问                                  |
| `diff_base`          | `DIFF` 必填   | 对比基准，如 `HEAD~1`、`origin/main`                      |
| `diff_head`          | 否            | 对比终点，默认工作区或 `HEAD`                             |
| `module`             | `MODULE` 必填 | Maven/Gradle 模块路径或坐标                               |
| `files`              | `FILES` 必填  | 待审 Java/Spring 文件路径列表                             |
| `include`            | 否            | 包含 glob，如 `**/*.java`                                 |
| `exclude`            | 否            | 排除 glob，如 `**/generated/**`                           |
| `rules_path`         | 否            | 规则库路径，默认 `rules/`；已有规则文档位于 `docs/rules/` |
| `schema_path`        | 否            | 报告 Schema 路径，默认 `schema/review-report.schema.json` |
| `output_json`        | 否            | JSON 报告输出路径                                         |
| `output_md`          | 否            | Markdown 报告输出路径                                     |
| `severity_threshold` | 否            | 最低报告级别，默认 `INFO`                                 |
| `offline`            | 否            | 是否禁止外部模型调用，默认 `false`                        |

## 6. 工作流程

步骤 1：解析输入，确定审查范围  
校验 `mode` 与参数，确定 `scope.mode`。若输入非法，生成降级报告并返回失败码。

步骤 2：加载相关规则  
从 `rules/` 按 A2 一级分类加载：AR、CF、DI、WEB、DAO、SEC、CON、PERF、OBS、EXC、RES、TEST、BUILD、STYLE、NULL、I18N、MIG、CLOUD。规则元数据以 `schema/rule-metadata.schema.json` 为准。若 `rules/index.json` 存在，优先使用；若不存在，只读使用 `docs/rules/` 与 `docs/03-索引文件最小字段.md` 构建内存索引，不写盘。

步骤 3：收集待审代码  
`DIFF`：只读执行 `git diff` 与 `git diff --name-only`。  
`MODULE`：只读扫描指定模块源码。  
`FILES`：只读读取指定文件。  
跳过二进制、生成代码、第三方代码与显式排除路径。

步骤 4：运行静态检查  
按需运行 Checkstyle、PMD、ArchUnit、SpotBugs。所有工具运行记录写入 `tool_runs`。工具只读执行，不修改目标仓库。

步骤 5：规则匹配  
静态工具结果优先。模型仅补充语义规则，如事务边界、AOP 误用、Bean 线程安全、Spring MVC 规范、SQL 注入、硬编码密钥等。模型不得修改代码，不得生成补丁。

步骤 6：汇总问题，去重，按严重级别排序  
按 `severity` 从 `BLOCKER` 到 `INFO` 排序。相同 `rule_id`、`file`、`line`、`evidence` 的问题去重。保留最高 `confidence` 与最完整证据。

步骤 7：生成 JSON 报告  
输出必须符合 `schema/review-report.schema.json`。顶层必填字段：`report_id`、`schema_version`、`generated_at`、`scope`、`summary`、`issues`、`tool_runs`、`metrics`。`issue` 必填字段：`issue_id`、`rule_id`、`severity`、`confidence`、`detection_method`、`file`、`line`、`column`、`evidence`、`message`、`remediation`。

步骤 8：生成 Markdown 报告  
生成人读版本，包含范围、摘要、问题统计、按严重级别分组的问题、证据、修复建议、工具运行、豁免说明、人工确认项。

## 7. 输出

JSON 报告：  
必须符合 `schema/review-report.schema.json`。字段说明以该 Schema 为准，不复制完整 Schema。关键枚举以 A6 为准：`severity`、`confidence`、`detection_method`、`tool`、`issue_status`、`scope.mode`。

Markdown 报告：  
面向人阅读，包含：

- 报告元信息
- 审查范围
- 摘要与统计
- 按严重级别分组的问题
- 每条问题的 `rule_id`、文件、行号、证据、建议
- 工具运行结果
- 豁免与跳过说明
- 人工确认项

退出码约定（与 `ExitCodes.java`、README 严格一致）：

| 码   | 常量        | 含义                                          |
| ---- | ----------- | --------------------------------------------- |
| 0    | SUCCESS     | 报告生成成功，且未达到 `--fail-on` 阈值       |
| 1    | FAIL_ON_HIT | 存在达到 `--fail-on`（默认 `CRITICAL`）的问题 |
| 2    | CLI_CONFIG  | CLI 参数 / 配置错误                           |
| 3    | SCOPE       | 审查范围 / 代码收集失败                       |
| 4    | RULE_LOAD   | 规则库加载失败                                |
| 5    | REPORT      | 报告生成 / 输出 / Schema 校验失败             |
| 6    | INTERNAL    | 未分类内部错误                                |

调用方（agent 或 CI）判定规则：`0` 视为通过；`1` 视为"审查完成且有达到阈值的问题"，按报告内容决定是否阻塞；`2~6` 一律视为工具本身失败，不得当作审查结论使用。

## 8. 边界与豁免

测试代码：默认豁免 `STYLE`、`PERF` 等非安全类规则；安全、资源泄漏、事务正确性问题不自动豁免。路径如 `src/test/**`、`**/test/**`、`**/*Test.java`。

生成代码：默认跳过。路径如 `target/generated-sources/**`、`build/generated/**`、`**/*Generated.java`、`**/*.g.java`。

第三方代码：默认跳过。路径如 `vendor/**`、`third_party/**`、外部依赖 jar。

`false_positive_notes` 使用：  
A6 issue 无 false_positive_notes 字段。
误报说明写入 message 或 evidence，并将 confidence 设为 LOW，
必要时将 issue_status 设为 FALSE_POSITIVE，等待人工复核。

不确定时：  
无法确认的问题必须标记 `confidence=LOW`，不得伪造 `HIGH`。

## 9. 工具调用约定

| 工具       | 关注点                 | 约定                                                      |
| ---------- | ---------------------- | --------------------------------------------------------- |
| Checkstyle | 命名与格式             | 只读运行，输出问题映射到对应规则                          |
| PMD        | 复杂度、异常、日志     | 只读运行，异常与日志问题优先映射到 EXC、OBS               |
| ArchUnit   | 分层依赖               | 只读运行，分层问题映射到 AR                               |
| SpotBugs   | 并发、空指针、资源泄漏 | 只读运行，映射到 CON、NULL、RES                           |
| CUSTOM     | 其他自定义规则         | 通过 RULE_ENGINE、REGEX、HEURISTIC、HYBRID 或 MANUAL 检测 |

工具运行失败必须记录到 `tool_runs`，不得修改目标仓库。

## 10. 报告的可追溯性

每条 `issue` 必须包含：

- `rule_id`
- `file`
- `line`
- `evidence`

`issue.rule_id` 必须能反查 `rules/index.json`。  
`issue.test_case_id` 若存在，必须能反查 `tests/cases/index.json`。  
规则元数据 Schema：`schema/rule-metadata.schema.json`。  
报告 Schema：`schema/review-report.schema.json`。  
规则索引：`rules/index.json`。  
测试用例索引：`tests/cases/index.json`。  
示例索引：`examples/index.json`。

## 11. 人工确认节点

以下问题必须人工确认：

- `BLOCKER` 问题。
- `CRITICAL` 问题。
- `confidence=LOW` 的问题。
- `issue_status` 为 `FALSE_POSITIVE` 的问题需复核。

人工确认只影响报告标记，不得自动修改代码、不得自动关闭问题、不得自动标记 `FIXED`。

## 12. 失败与降级

工具运行失败：  
记录 `tool_runs.status=FAILED`，继续运行其他工具。若全部工具失败，生成降级报告，`issues` 可为空，`summary` 说明失败原因。

模型调用失败：  
跳过语义规则，仅输出静态工具结果。不得伪造语义问题。将模型状态记录到报告顶层 metadata 字段，或写入 metrics.llm_calls（若模型确实被调用）。

输出要求：  
即使降级，输出仍必须符合 `schema/review-report.schema.json`。若无法符合 Schema，返回退出码 `2`，并输出最小合规降级报告。

## 13. 版本与变更

`SKILL.md` 版本使用语义化版本：

- `MAJOR`：不兼容变更。
- `MINOR`：新增能力或规则范围。
- `PATCH`：修正描述、错别字、非行为变更。

规则库版本引用：  
从 rules/index.json 读取 version、generated_at、generated_by，写入报告 metrics。 或 `tool_runs`。若缺失，记录为 `unknown`，并降低相关规则匹配的 `confidence`。