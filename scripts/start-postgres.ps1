param([switch]$RunApplication)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

# Resolve Docker even when the IDE has not refreshed PATH after installation.
$dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
$dockerExecutable = if ($dockerCommand) { $dockerCommand.Source } else { $null }
if (-not $dockerExecutable) {
    foreach ($candidate in @(
        "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\docker.exe",
        "$env:ProgramFiles\Docker\Docker\resources\bin\docker.exe"
    )) {
        if (Test-Path -LiteralPath $candidate) {
            $dockerExecutable = $candidate
            break
        }
    }
}
if (-not $dockerExecutable) {
    throw 'Docker CLI was not found. Install Docker Desktop and reopen the terminal.'
}

Push-Location -LiteralPath $projectRoot
try {
    & $dockerExecutable info --format '{{.ServerVersion}}'
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Engine is unavailable. Start Docker Desktop; restart Windows if WSL setup requires it.'
    }

    & $dockerExecutable compose up -d --wait --wait-timeout 120 postgres
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed. Check docker compose logs postgres.' }

    & $dockerExecutable compose exec -T postgres pg_isready -U rental -d home_management
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL is not ready.' }

    Write-Host 'PostgreSQL ready: localhost:5432 / database home_management / user rental'
    if ($RunApplication) {
        & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
        if ($LASTEXITCODE -ne 0) { throw 'Spring Boot startup failed.' }
    }
} finally {
    Pop-Location
}
