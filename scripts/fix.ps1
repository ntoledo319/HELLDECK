# PowerShell script for Windows users
# Apply all autofixes (format + lint fixes)

Write-Host "🔧 Applying autofixes..." -ForegroundColor Cyan

# Format Kotlin
Write-Host "📝 Formatting Kotlin code..." -ForegroundColor Yellow
& .\gradlew.bat ktlintFormat spotlessApply
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Kotlin formatting failed" -ForegroundColor Yellow
}

# Fix Kotlin lint issues (detekt autoCorrect enabled in config)
Write-Host "🔍 Fixing Kotlin lint issues..." -ForegroundColor Yellow
& .\gradlew.bat detekt
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Kotlin lint fixing failed" -ForegroundColor Yellow
}

# Format Python
Write-Host "📝 Formatting Python code..." -ForegroundColor Yellow
$ruffInstalled = Get-Command ruff -ErrorAction SilentlyContinue
if (-not $ruffInstalled) {
    Write-Host "⚠️  ruff not found, installing..." -ForegroundColor Yellow
    pip install ruff
}
& ruff format loader/ tools/ --exclude third_party
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Python formatting failed" -ForegroundColor Yellow
}

# Fix Python lint issues
Write-Host "🔍 Fixing Python lint issues..." -ForegroundColor Yellow
& ruff check --fix loader/ tools/ --exclude third_party
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Python linting failed" -ForegroundColor Yellow
}

Write-Host "✅ All autofixes applied" -ForegroundColor Green

