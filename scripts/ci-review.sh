#!/usr/bin/env bash
# 通用 CI 审查入口。
# 环境变量：
#   REVIEW_MODE        DIFF | MODULE | FILES（默认 DIFF）
#   REVIEW_BASE        DIFF 模式的 base ref（默认 origin/main）
#   REVIEW_HEAD        DIFF 模式的 head ref（默认 HEAD）
#   REVIEW_MODULES     逗号分隔的模块路径（MODULE 模式）
#   REVIEW_FILES       逗号分隔的文件路径（FILES 模式）
#   REVIEW_OUTPUT_DIR  输出目录（默认 reports）
#   REVIEW_OFFLINE     true | false（默认 true）
#   REVIEW_FAIL_ON     BLOCKER | CRITICAL | MAJOR | MINOR | INFO | NEVER（默认 CRITICAL）
#   REVIEW_STRICT      true | false（默认 false）
#   REVIEW_LOG_LEVEL   INFO | DEBUG（默认 INFO）
#
# 退出码：与 CLI 一致（0 成功，1 达到 failOn，2~6 各阶段失败）。

set -euo pipefail

REVIEW_MODE="${REVIEW_MODE:-DIFF}"
REVIEW_BASE="${REVIEW_BASE:-origin/main}"
REVIEW_HEAD="${REVIEW_HEAD:-HEAD}"
REVIEW_MODULES="${REVIEW_MODULES:-}"
REVIEW_FILES="${REVIEW_FILES:-}"
REVIEW_OUTPUT_DIR="${REVIEW_OUTPUT_DIR:-reports}"
REVIEW_OFFLINE="${REVIEW_OFFLINE:-true}"
REVIEW_FAIL_ON="${REVIEW_FAIL_ON:-CRITICAL}"
REVIEW_STRICT="${REVIEW_STRICT:-false}"
REVIEW_LOG_LEVEL="${REVIEW_LOG_LEVEL:-INFO}"

JAR=$(ls -t target/spring-review-skill-*.jar 2>/dev/null | grep -v sources | grep -v javadoc | head -n1 || true)
if [[ -z "${JAR}" ]]; then
    echo "未找到 target/spring-review-skill-*.jar；请先执行 mvn -q -DskipTests package" >&2
    exit 2
fi

ARGS=( -jar "$JAR" \
       --mode "$REVIEW_MODE" \
       --repo . \
       --output-dir "$REVIEW_OUTPUT_DIR" \
       --fail-on "$REVIEW_FAIL_ON" \
       --log-level "$REVIEW_LOG_LEVEL" )

case "$REVIEW_MODE" in
    DIFF)
        ARGS+=( --base "$REVIEW_BASE" --head "$REVIEW_HEAD" )
        ;;
    MODULE)
        IFS=',' read -ra MODS <<< "$REVIEW_MODULES"
        for m in "${MODS[@]}"; do
            [[ -n "$m" ]] && ARGS+=( --module "$m" )
        done
        ;;
    FILES)
        IFS=',' read -ra FLS <<< "$REVIEW_FILES"
        for f in "${FLS[@]}"; do
            [[ -n "$f" ]] && ARGS+=( --files "$f" )
        done
        ;;
    *)
        echo "unknown REVIEW_MODE: $REVIEW_MODE" >&2
        exit 2
        ;;
esac

[[ "$REVIEW_OFFLINE" == "true" ]] && ARGS+=( --offline )
[[ "$REVIEW_STRICT"  == "true" ]] && ARGS+=( --strict )

echo "Running: java ${ARGS[*]}"
java "${ARGS[@]}"