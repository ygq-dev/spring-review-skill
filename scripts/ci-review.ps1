# 通用 CI 审查入口（Windows PowerShell）。
# 用法参见 scripts/ci-review.sh 的环境变量说明。
#
# 退出码：与 CLI 一致（0 成功，1 达到 failOn，2~6 各阶段失败）。

param()

$ErrorActionPreference = "Stop"

$Mode        = if ($env:REVIEW_MODE)        { $env:REVIEW_MODE }        else { "DIFF" }
$Base        = if ($env:REVIEW_BASE)        { $env:REVIEW_BASE }        else { "origin/main" }
$Head        = if ($env:REVIEW_HEAD)        { $env:REVIEW_HEAD }        else { "HEAD" }
$Modules     = if ($env:REVIEW_MODULES)     { $env:REVIEW_MODULES }     else { "" }
$Files       = if ($env:REVIEW_FILES)       { $env:REVIEW_FILES }       else { "" }
$OutputDir   = if ($env:REVIEW_OUTPUT_DIR)  { $env:REVIEW_OUTPUT_DIR }  else { "reports" }
$Offline     = if ($env:REVIEW_OFFLINE)     { $env:REVIEW_OFFLINE }     else { "true" }
$FailOn      = if ($env:REVIEW_FAIL_ON)     { $env:REVIEW_FAIL_ON }     else { "CRITICAL" }
$Strict      = if ($env:REVIEW_STRICT)      { $env:REVIEW_STRICT }      else { "false" }
$LogLevel    = if ($env:REVIEW_LOG_LEVEL)   { $env:REVIEW_LOG_LEVEL }   else { "INFO" }

$jar = Get-ChildItem -Path "target" -Filter "spring-review-skill-*.jar" -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
       Sort-Object LastWriteTime -Descending |
       Select-Object -First 1

if (-not $jar) {
    Write-Error "未找到 target/spring-review-skill-*.jar；请先执行 mvn -q -DskipTests package"
}

$args = @(
    "-jar", $jar.FullName,
    "--mode", $Mode,
    "--repo", ".",
    "--output-dir", $OutputDir,
    "--fail-on", $FailOn,
    "--log-level", $LogLevel
)

switch ($Mode) {
    "DIFF"   { $args += @("--base", $Base, "--head", $Head) }
    "MODULE" { foreach ($m in $Modules.Split(',')) { if ($m) { $args += @("--module", $m) } } }
    "FILES"  { foreach ($f in $Files.Split(','))   { if ($f) { $args += @("--files", $f) } } }
    default  { Write-Error "unknown REVIEW_MODE: $Mode" }
}

if ($Offline -eq "true") { $args += "--offline" }
if ($Strict  -eq "true") { $args += "--strict" }

Write-Host "Running: java $($args -join ' ')"
java @args
exit $LASTEXITCODE