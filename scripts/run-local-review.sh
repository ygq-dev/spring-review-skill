#!/usr/bin/env bash
# Spring Review Skill 本地手工验证脚本（Linux/macOS）
# 用法：
#   ./scripts/run-local-review.sh -m FILES -f src/main/java/com/example/Demo.java --offline
#   ./scripts/run-local-review.sh -m DIFF -b HEAD~1 -h HEAD

set -euo pipefail

MODE="FILES"
REPO="."
BASE="HEAD~1"
HEAD="HEAD"
OUTPUT_DIR="reports"
OFFLINE=""
NO_LLM=""
STRICT=""
LOG_LEVEL="INFO"
MODULE_ARGS=()
FILE_ARGS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        -m|--mode)      MODE="$2"; shift 2 ;;
        -r|--repo)      REPO="$2"; shift 2 ;;
        -b|--base)      BASE="$2"; shift 2 ;;
        -h|--head)      HEAD="$2"; shift 2 ;;
        -o|--output)    OUTPUT_DIR="$2"; shift 2 ;;
        --module)       MODULE_ARGS+=("--module" "$2"); shift 2 ;;
        -f|--files)     FILE_ARGS+=("--files" "$2"); shift 2 ;;
        --offline)      OFFLINE="--offline"; shift ;;
        --no-llm)       NO_LLM="--no-llm"; shift ;;
        --strict)       STRICT="--strict"; shift ;;
        --log-level)    LOG_LEVEL="$2"; shift 2 ;;
        *) echo "unknown: $1" >&2; exit 2 ;;
    esac
done

JAR=$(ls -t target/spring-review-skill-*.jar 2>/dev/null | grep -v sources | grep -v javadoc | head -n1 || true)
if [[ -z "${JAR}" ]]; then
    echo "未找到 target/spring-review-skill-*.jar；请先执行 mvn -q -DskipTests package" >&2
    exit 2
fi

ARGS=( -jar "$JAR" --mode "$MODE" --repo "$REPO" --output-dir "$OUTPUT_DIR" --log-level "$LOG_LEVEL" )

if [[ "$MODE" == "DIFF" ]]; then
    ARGS+=( --base "$BASE" --head "$HEAD" )
fi
if [[ "$MODE" == "MODULE" && ${#MODULE_ARGS[@]} -gt 0 ]]; then
    ARGS+=( "${MODULE_ARGS[@]}" )
fi
if [[ "$MODE" == "FILES" && ${#FILE_ARGS[@]} -gt 0 ]]; then
    ARGS+=( "${FILE_ARGS[@]}" )
fi
[[ -n "$OFFLINE" ]] && ARGS+=( --offline )
[[ -n "$NO_LLM"  ]] && ARGS+=( --no-llm )
[[ -n "$STRICT"  ]] && ARGS+=( --strict )

echo "Running: java ${ARGS[*]}"
set +e
java "${ARGS[@]}"
CODE=$?
set -e

echo ""
echo "exit_code=$CODE"
echo "latest_json=$OUTPUT_DIR/latest/review-report.json"
echo "latest_md=$OUTPUT_DIR/latest/review-report.md"
exit "$CODE"