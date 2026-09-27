# 工具配置

## 1. 概述

本目录承载 Checkstyle、PMD、SpotBugs 的配置文件。工具的 jar / 可执行文件
不进入 Git（`.gitignore` 已屏蔽 `tools/`），需要本地或 CI 显式放置。

## 2. 工具版本

| 工具       | 版本    | MVP 默认 |
| ---------- | ------- | -------- |
| Checkstyle | 10.17.0 | 启用     |
| PMD        | 7.4.0   | 启用     |
| SpotBugs   | 4.8.6   | 关闭     |

## 3. 放置约定

### 3.1 Skill 包根

工具与配置文件默认相对于 **skill 包根** 查找。
未设置 `SPRING_REVIEW_HOME` 时，skill 包根 = 当前工作目录。

```bash
export SPRING_REVIEW_HOME=/path/to/spring-review-skill   # 可选
```