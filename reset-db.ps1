# PowerShell script to reset the database
# Usage: .\reset-db.ps1

$env:PGPASSWORD = "flipspace_password"
$psqlPath = "psql"  # Adjust if psql is not in PATH

Write-Host "Resetting database schema..." -ForegroundColor Yellow

# Try to find psql in common locations
$possiblePaths = @(
    "psql",
    "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files\PostgreSQL\15\bin\psql.exe",
    "C:\Program Files\PostgreSQL\14\bin\psql.exe"
)

$psqlPath = $null
foreach ($path in $possiblePaths) {
    if (Get-Command $path -ErrorAction SilentlyContinue) {
        $psqlPath = $path
        break
    }
}

if ($null -eq $psqlPath) {
    Write-Host "ERROR: psql not found. Please install PostgreSQL client or add it to PATH" -ForegroundColor Red
    Write-Host "Common locations:" -ForegroundColor Yellow
    Write-Host "  - C:\Program Files\PostgreSQL\16\bin\psql.exe" -ForegroundColor Gray
    Write-Host "  - C:\Program Files\PostgreSQL\15\bin\psql.exe" -ForegroundColor Gray
    exit 1
}

Write-Host "Found psql at: $psqlPath" -ForegroundColor Green

# Execute the init.sql script
& $psqlPath -U flipspace_user -d flipspace -f init.sql

if ($LASTEXITCODE -eq 0) {
    Write-Host "Database reset successfully!" -ForegroundColor Green
} else {
    Write-Host "Database reset failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
