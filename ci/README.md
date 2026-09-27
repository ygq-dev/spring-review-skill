# CI 集成

本目录说明如何把 Spring Review Skill 接入 CI。默认示例使用 GitHub Actions。

## 设计原则

1. **只读**：CI 只运行审查，不修改目标仓库、不提交、不推送。
2. **默认 offline**：CI 不向外部模型发送代码；如需 LLM，通过 `SPRING_REVIEW_LLM_API_KEY` 等 Secrets 显式开启。
3. **工具缺失不阻塞**：Checkstyle/PMD/SpotBugs 未安装时标记 `SKIPPED`，不失败。
4. **报告归档**：JSON + Markdown 作为 CI artifact 保留。
5. **退出码驱动**：
   - `0` 成功且未达到 failOn
   - `1` 达到 failOn（默认 CRITICAL）→ CI 失败
   - `2`～`6` CLI/配置/范围/规则/报告/内部错误 → CI 失败

## 工作流

### `ci.yml`

每次 push / PR 触发：构建、单元与集成测试。失败时上传 surefire 报告。

### `review.yml`

PR 触发：只读审查本次 diff，输出 Markdown 摘要到 PR Step Summary，JSON/Markdown 作为 artifact。

## 密钥与安全

- LLM 密钥通过 GitHub Secrets 提供，名称建议：
  - `DEEPSEEK_API_KEY`（默认 provider）
  - `OPENAI_API_KEY`
  - `SPRING_REVIEW_LLM_API_KEY`（通用）
- 未提供密钥时，`LlmClientFactory` 返回 `NoopLlmClient`，语义规则跳过。
- **不要**把密钥写入代码、配置文件或提交记录。

## 自定义

### 使用本地工具（Checkstyle / PMD / SpotBugs）

在 CI 中预置：
- Checkstyle jar：`tools/checkstyle-10.17.0-all.jar`
- PMD：`tools/pmd-bin-7.4.0/bin/pmd`
- SpotBugs：`tools/spotbugs-4.8.6/bin/spotbugs`

或将环境变量指向安装位置：
- `CHECKSTYLE_JAR`
- `PMD_HOME`
- `SPOTBUGS_HOME`

### 切换审查模式

在 `review.yml` 中修改 `--mode`：
- `DIFF`：审查 base→head 之间的变更（PR 推荐）
- `MODULE`：审查指定模块，需 `--module`
- `FILES`：审查指定文件，需 `--files`

### failOn 阈值

`--fail-on BLOCKER|CRITICAL|MAJOR|MINOR|INFO|NEVER`

默认 `CRITICAL`。设为 `NEVER` 时，审查结果不会让 CI 失败（只输出报告）。

## 其他平台

- GitLab CI：参见 `ci/gitlab-ci/`（预留）
- Jenkins：参见 `ci/jenkins/`（预留）

将 `review.yml` 中的 `mvn`、`java -jar` 步骤翻译为目标平台的 DSL 即可；退出码语义保持一致。