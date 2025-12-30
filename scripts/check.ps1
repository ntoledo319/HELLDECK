# PowerShell script for Windows users
# Verify formatting/lint without making changes (CI safe)

Write-Host "🔍 Running checks..." -ForegroundColor Cyan

# Check Kotlin
Write-Host "🔍 Checking Kotlin code..." -ForegroundColor Yellow
& .\gradlew.bat ktlintCheck detekt spotlessCheck
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Kotlin checks failed" -ForegroundColor Red
    exit 1
}

# Check Python
Write-Host "🔍 Checking Python code..." -ForegroundColor Yellow
$ruffInstalled = Get-Command ruff -ErrorAction SilentlyContinue
if (-not $ruffInstalled) {
    Write-Host "⚠️  ruff not found, installing..." -ForegroundColor Yellow
    pip install ruff
}
& ruff check loader/ tools/ --exclude third_party
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Python lint checks failed" -ForegroundColor Red
    exit 1
}
& ruff format --check loader/ tools/ --exclude third_party
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Python format checks failed" -ForegroundColor Red
    exit 1
}

Write-Host "✅ All checks passed" -ForegroundColor Green

