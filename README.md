# Spring Review Skill

一个**只读**的 Spring 代码审查 Skill：把 Spring 学习笔记与团队规范提炼为
可版本化的规则库，结合静态分析工具、JavaParser、可选 LLM，对 `git diff`
或指定模块输出可追溯、可评测的审查报告。

- **只读**：不修改目标仓库、不提交、不推送、不训练模型
- **规则驱动**：规则 YAML 化，元数据 Schema 冻结，检测器可插拔
- **三种模式**：`DIFF`（PR 审查）、`MODULE`（模块审查）、`FILES`（文件审查）
- **默认离线**：LLM 调用可关闭；静态分析与 AST 检测完全本地
- **可扩展**：团队可基于本框架添加自己的规则与检测器

---

## 快速开始

### 前置

| 依赖  | 版本                    |
| ----- | ----------------------- |
| Java  | 17+                     |
| Maven | 3.8+                    |
| Git   | 可选（`DIFF` 模式需要） |

### 构建

```bash
git clone <this-repo>
cd spring-review-skill
mvn -q -DskipTests package
```