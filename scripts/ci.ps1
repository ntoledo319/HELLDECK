# PowerShell script for Windows users
# Run full CI pipeline (check + test)

Write-Host "🚀 Running CI pipeline..." -ForegroundColor Cyan

# Run checks
& .\scripts\check.ps1
if ($LASTEXITCODE -ne 0) {
    exit 1
}

# Run tests
& .\scripts\test.ps1
if ($LASTEXITCODE -ne 0) {
    exit 1
}

Write-Host "✅ CI pipeline completed successfully" -ForegroundColor Green

