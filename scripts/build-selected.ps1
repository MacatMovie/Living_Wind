param(
    [Parameter(Mandatory=$true, Position=0)][string]$Selection,
    [switch]$Clean
)

$ErrorActionPreference = 'Continue'
$Root = Split-Path -Parent $PSScriptRoot
$BuildScript = Join-Path $PSScriptRoot 'build.ps1'
$LogsDir = Join-Path $Root 'logs'
New-Item -ItemType Directory -Force -Path $LogsDir | Out-Null

$map = [ordered]@{
    'A' = 'forge-1.20.1'
    'B' = 'neoforge-1.21.1'
    'C' = 'fabric-1.20.1'
    'D' = 'fabric-1.21.1'
}

# Accept A, A,C, A C, or AC. Each letter builds one target.
$tokens = $Selection.ToUpperInvariant() -split '[,;\s]+' | Where-Object { $_ }
$targets = New-Object System.Collections.Generic.List[string]
$invalid = New-Object System.Collections.Generic.List[string]

foreach ($token in $tokens) {
    if ($token -notmatch '^[A-D]+$') {
        $invalid.Add($token)
        continue
    }
    foreach ($letter in $token.ToCharArray()) {
        $mapped = $map[[string]$letter]
        if (-not $targets.Contains($mapped)) { $targets.Add($mapped) }
    }
}

# Refuse an invalid selection instead of silently building only part of it.
if ($targets.Count -eq 0 -or $invalid.Count -gt 0) {
    Write-Host ""
    Write-Host "Choose A, B, C, or D (multiple letters are allowed)." -ForegroundColor Red
    if ($invalid.Count -gt 0) {
        Write-Host ("Unknown selection(s): " + ($invalid -join ', ')) -ForegroundColor Red
    }
    exit 1
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$failures = New-Object System.Collections.Generic.List[string]
$built = 0

Write-Host ""
Write-Host "============================================================"
Write-Host ("Selected target(s): " + ($targets -join ', '))
if ($Clean) { Write-Host "Clean build: YES" }
Write-Host "============================================================"

foreach ($target in $targets) {
    $log = Join-Path $LogsDir "build-$target-$stamp.log"
    $latest = Join-Path $LogsDir 'build-one-latest.log'

    Write-Host ""
    Write-Host "============================================================"
    Write-Host "BUILDING: $target"
    Write-Host "============================================================"
    Write-Host "Log: $log"
    Write-Host ""

    $args = @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', $BuildScript,
        $target,
        '-LogPath', $log
    )
    if ($Clean) { $args += '-Clean' }

    & powershell.exe @args
    $code = $LASTEXITCODE

    if (Test-Path $log) {
        Copy-Item $log $latest -Force -ErrorAction SilentlyContinue
    }

    if ($code -eq 0) {
        Write-Host ""
        Write-Host "=== $target SUCCESS ===" -ForegroundColor Green
        $built++
    } else {
        Write-Host ""
        Write-Host "=== $target FAILED (exit code $code) ===" -ForegroundColor Red
        Write-Host "Log: $log" -ForegroundColor Red
        $failures.Add($target)
    }
}

Write-Host ""
Write-Host "============================================================"
if ($failures.Count -eq 0) {
    Write-Host "ALL SELECTED BUILDS FINISHED SUCCESSFULLY." -ForegroundColor Green
    Write-Host "Built $built target(s)."
    Write-Host "Finished jars are in: $(Join-Path $Root 'dist')"
    Write-Host "============================================================"
    exit 0
}

Write-Host ("SELECTED BUILDS FINISHED WITH {0} FAILURE(S)." -f $failures.Count) -ForegroundColor Red
Write-Host ("Failed: " + ($failures -join ', ')) -ForegroundColor Red
Write-Host "============================================================"
exit 1
