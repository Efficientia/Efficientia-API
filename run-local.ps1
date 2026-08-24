[CmdletBinding()]
param()

Set-StrictMode -Version Latest

$projectRoot = $PSScriptRoot
$passwordDefinedByScript = $false

if ([string]::IsNullOrWhiteSpace($env:SUPABASE_DB_URL)) {
    $env:SUPABASE_DB_URL = "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require"
}

if ([string]::IsNullOrWhiteSpace($env:SUPABASE_DB_USERNAME)) {
    $env:SUPABASE_DB_USERNAME = "postgres.qngvyhvkssdphbmxmbra"
}

if ([string]::IsNullOrWhiteSpace($env:DB_SCHEMA)) {
    $env:DB_SCHEMA = "public"
}

if ([string]::IsNullOrWhiteSpace($env:SUPABASE_DB_PASSWORD)) {
    $securePassword = Read-Host "Senha do banco Supabase" -AsSecureString
    $credential = [System.Management.Automation.PSCredential]::new("supabase", $securePassword)
    $env:SUPABASE_DB_PASSWORD = $credential.GetNetworkCredential().Password
    $passwordDefinedByScript = $true
}

Push-Location $projectRoot
try {
    & "$projectRoot\mvnw.cmd" spring-boot:run
    $mavenExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
    if ($passwordDefinedByScript) {
        Remove-Item Env:SUPABASE_DB_PASSWORD -ErrorAction SilentlyContinue
    }
}

exit $mavenExitCode
