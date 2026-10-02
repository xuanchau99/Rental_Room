#Requires -RunAsAdministrator

$ErrorActionPreference = 'Stop'

# Enable WSL prerequisites without restarting Windows automatically.
Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -All -NoRestart

winget install --id Microsoft.WSL --exact --source winget --accept-source-agreements --accept-package-agreements --silent --disable-interactivity
if ($LASTEXITCODE -ne 0) { throw "WSL installation failed: $LASTEXITCODE" }

wsl --version
if ($LASTEXITCODE -ne 0) { throw 'WSL version verification failed.' }

Write-Host 'WSL installed. Restart Windows, then open Docker Desktop.'
