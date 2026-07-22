# PowerShell script for Windows users
# Run unit and integration tests

Write-Host "🧪 Running tests..." -ForegroundColor Cyan
& .\gradlew.bat :app:testProductionDebugUnitTest
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Tests completed" -ForegroundColor Green
} else {
    Write-Host "❌ Tests failed" -ForegroundColor Red
    exit 1
}
