# Spring Review Skill 本地手工验证脚本（Windows）
# 用法：
#   .\scripts\run-local-review.ps1 -Mode FILES -Files "src/main/java/..." -Offline
#   .\scripts\run-local-review.ps1 -Mode DIFF -Base HEAD~1 -Head HEAD
# 说明：只读审查；报告落盘 reports/；不改目标仓库。

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("DIFF","MODULE","FILES")]
    [string]$Mode = "FILES",

    [string]$Repo = ".",

    [string]$Base = "HEAD~1",
    [string]$Head = "HEAD",

    [string[]]$Module = @(),
    [string[]]$Files = @(),

    [string]$OutputDir = "reports",

    [switch]$Offline,
    [switch]$NoLlm,
    [switch]$Strict,
    [string]$LogLevel = "INFO"
)

$ErrorActionPreference = "Stop"

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
    "--repo", $Repo,
    "--output-dir", $OutputDir,
    "--log-level", $LogLevel
)

if ($Mode -eq "DIFF") {
    $args += @("--base", $Base, "--head", $Head)
}
if ($Mode -eq "MODULE" -and $Module.Count -gt 0) {
    foreach ($m in $Module) { $args += @("--module", $m) }
}
if ($Mode -eq "FILES" -and $Files.Count -gt 0) {
    foreach ($f in $Files) { $args += @("--files", $f) }
}
if ($Offline)  { $args += "--offline" }
if ($NoLlm)    { $args += "--no-llm" }
if ($Strict)   { $args += "--strict" }

Write-Host "Running: java $($args -join ' ')"
java @args
$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "exit_code=$exitCode"
Write-Host "latest_json=$OutputDir/latest/review-report.json"
Write-Host "latest_md=$OutputDir/latest/review-report.md"
if (Test-Path "$OutputDir/latest") {
    Get-ChildItem "$OutputDir/latest" | Format-Table Name, Length, LastWriteTime
}

exit $exitCode