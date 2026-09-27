# Spring Review Skill

一个**只读**的 Spring 代码审查 Skill：把 Spring 学习笔记与团队规范提炼为
可版本化的规则库，结合静态分析工具、JavaParser、可选 LLM，对 `git diff`
或指定模块输出可追溯、可评测的审查报告。

- **只读**：不修改目标仓库、不提交、不推送、不训练模型
- **规则驱动**：规则 YAML 化，元数据 Schema 冻结，检测器可插拔
- **三种模式**：`DIFF`（PR 审查）、`MODULE`（模块审查）、`FILES`（文件审查）
- **默认离线**：LLM 调用可关闭；静态分析与 AST 检测完全本地
- **可扩展**：团队可基于本框架添加自己的规则与检测器

## 快速开始

### 前置

| 依赖  | 版本                    |
| ----- | ----------------------- |
| Java  | 17+                     |
| Maven | 3.8+                    |
| Git   | 可选（`DIFF` 模式需要） |

### 构建

```bash
git clone https://github.com/ygq-dev/spring-review-skill.git
cd spring-review-skill
mvn -q -DskipTests package
```

生成 `target/spring-review-skill-<version>.jar`。

### 运行

**审查 git diff：**

```bash
java -jar target/spring-review-skill-<version>.jar \
  --mode DIFF \
  --repo /path/to/target-project \
  --base HEAD~1 --head HEAD \
  --offline
```

**审查指定模块：**

```bash
java -jar target/spring-review-skill-<version>.jar \
  --mode MODULE \
  --repo /path/to/target-project \
  --module src/main/java \
  --offline
```

**审查指定文件：**

```bash
java -jar target/spring-review-skill-<version>.jar \
  --mode FILES \
  --repo /path/to/target-project \
  --files src/main/java/com/example/Foo.java \
  --offline
```

报告输出到 `reports/latest/`：

- `review-report.json`（符合 [A6 Schema](docs/05-输出报告Schema.md)）
- `review-report.md`（人读版本）

### 退出码

| 码   | 含义                         |
| ---- | ---------------------------- |
| 0    | 成功，未达到 failOn          |
| 1    | 达到 failOn（默认 CRITICAL） |
| 2    | CLI / 配置错误               |
| 3    | 范围 / 代码收集失败          |
| 4    | 规则加载失败                 |
| 5    | 报告 / 输出 / 校验失败       |
| 6    | 内部错误                     |

### 常用参数

| 参数                  | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| `--mode`              | `DIFF` / `MODULE` / `FILES`                                  |
| `--offline`           | 禁止 LLM 调用（默认离线）                                    |
| `--fail-on`           | `BLOCKER` / `CRITICAL` / `MAJOR` / `MINOR` / `INFO` / `NEVER` |
| `--enable-checkstyle` | 启用 Checkstyle                                              |
| `--enable-pmd`        | 启用 PMD                                                     |
| `--enable-spotbugs`   | 启用 SpotBugs                                                |

完整参数：`java -jar <jar> --help`

## 规则库

内置 **55 条规则**，覆盖 **18 个一级分类**：

| 分类  | 说明               |
| ----- | ------------------ |
| AR    | 架构与分层         |
| CF    | 配置               |
| DI    | 依赖注入           |
| WEB   | Web 与 API         |
| DAO   | 数据访问与事务     |
| SEC   | 安全               |
| CON   | 并发               |
| PERF  | 性能               |
| OBS   | 可观测性与日志     |
| EXC   | 异常处理           |
| RES   | 资源管理           |
| TEST  | 测试               |
| BUILD | 构建与依赖         |
| STYLE | 代码风格与可维护性 |
| NULL  | 空安全             |
| I18N  | 国际化             |
| MIG   | 迁移与兼容性       |
| CLOUD | 云原生             |

规则文件在 `rules/<分类码>.yaml`；索引 `rules/index.json`。

### 添加自定义规则

核心步骤：

1. 在 `rules/<分类码>.yaml` 追加规则元数据（遵循 [A5 Schema](docs/04-规则元数据Schema.md)）
2. 重建 `rules/index.json`
3. 提供正例、反例、测试用例
4. 补检测器（可选：正则 / AST / 工具映射）

## 静态工具集成（可选）

默认不启用外部工具。启用方式：

1. 下载 Checkstyle 10.17.0 / PMD 7.4.0 到 `tools/`
2. 使用 `--enable-checkstyle` / `--enable-pmd`

详见 [config/README.md](config/README.md)。

## LLM 集成（可选）

默认 **offline**。启用需：

1. 在 `spring-review.yaml` 中设置 `llm.enabled: true`
2. 通过环境变量提供密钥：`DEEPSEEK_API_KEY` / `OPENAI_API_KEY` 等

仅当规则元数据显式声明 `parameters.use_llm: true` 时才会触发 LLM 调用。

## 评测

内置基准评测集（20 缺陷 + 10 干净）与性能测试：

```bash
python scripts/run-evaluation.py        # S5 检出率 / S6 误报率
python scripts/run-perf-test.py         # S7 P95 耗时
python scripts/run-metrics-summary.py   # 汇总 S1～S18
```

最新结果见 [docs/35-MVP验收报告.md](docs/35-MVP验收报告.md)。

## 文档

| 主题               | 文档                                                         |
| ------------------ | ------------------------------------------------------------ |
| 项目目标           | [docs/00-项目目标.md](docs/00-项目目标.md)                   |
| 规则分类与模板     | [docs/01-规则分类与元数据模板.md](docs/01-规则分类与元数据模板.md) |
| 目录结构           | [docs/02-目录结构.md](docs/02-目录结构.md)                   |
| 规则元数据 Schema  | [docs/04-规则元数据Schema.md](docs/04-规则元数据Schema.md)   |
| 输出报告 Schema    | [docs/05-输出报告Schema.md](docs/05-输出报告Schema.md)       |
| 技术选型           | [docs/23-技术选型.md](docs/23-技术选型.md)                   |
| 模块划分与接口契约 | [docs/24-模块划分与接口契约.md](docs/24-模块划分与接口契约.md) |
| MVP 验收报告       | [docs/35-MVP验收报告.md](docs/35-MVP验收报告.md)             |

## 目录结构

```
spring-review-skill/
├── docs/          设计文档
├── handoff/       阶段交接摘要
├── rules/         规则库（按分类）
├── examples/      正例与反例
├── tests/         测试用例与评测集
├── schema/        JSON Schema
├── scripts/       审查、评测、性能测试脚本
├── config/        Checkstyle / PMD 配置
├── src/           实现代码
├── reports/       输出目录（不入库）
└── pom.xml
```

## 设计约束

- **只读**：不写目标仓库、不 commit、不 push、不训练模型
- **A5/A6 Schema 冻结**：规则元数据与报告结构有严格 Schema
- **规则来源可追溯**：每条规则必须标注来源，禁止编造规范
- **默认可降级**：工具缺失、LLM 关闭时仍能完成审查

## 许可

[MIT License](LICENSE)

## 致谢

本项目基于 Spring Boot 3.x、picocli、JGit、JavaParser、Checkstyle、PMD、SpotBugs、
networknt json-schema-validator 等开源项目构建。