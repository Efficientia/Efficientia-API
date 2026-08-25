[CmdletBinding()]
param()

Set-StrictMode -Version Latest

$projectRoot = $PSScriptRoot
$dotenvPath = Join-Path $projectRoot ".env"

if (-not (Test-Path -LiteralPath $dotenvPath)) {
    Write-Warning "Arquivo .env não encontrado. A aplicação usará variáveis definidas no sistema ou no ambiente de execução."
}

Push-Location $projectRoot
try {
    & "$projectRoot\mvnw.cmd" spring-boot:run
    $mavenExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $mavenExitCode
