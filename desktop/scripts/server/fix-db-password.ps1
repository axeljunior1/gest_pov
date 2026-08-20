#Requires -Version 5.1
#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Resynchronise le mot de passe PostgreSQL gest_pov_app avec secrets.dpapi,
  puis redemarre GestPOV-Server. N'affiche aucun secret.
#>
$ErrorActionPreference = 'Stop'

$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
. (Join-Path $scriptRoot 'lib\GestPovCommon.ps1')
. (Join-Path $scriptRoot 'lib\GestPovSecrets.ps1')

function Get-SafeText {
    param($Value)
    if ($null -eq $Value) { return '' }
    return ("$Value").Trim()
}

$P = Get-GestPovPaths
if (-not (Test-Path $P.LogsDir)) {
    New-Item -ItemType Directory -Path $P.LogsDir -Force | Out-Null
}
$report = Join-Path $P.LogsDir 'fix-db-password.log'

function Log([string]$m) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $m
    Add-Content -Path $report -Value $line -Encoding UTF8
    Write-Host $m
}

Log '=== fix-db-password start ==='

if (-not (Test-Path $P.SecretsFile)) { throw 'secrets.dpapi introuvable' }
$sec = Unprotect-GestPovSecrets -Path $P.SecretsFile -Scope LocalMachine
$appPwLen = (Get-SafeText $sec.dbAppPassword).Length
$adminPwLen = (Get-SafeText $sec.dbAdminPassword).Length
Log ("Secrets OK db=$($sec.dbName) user=$($sec.dbAppUser) appPwLen=$appPwLen adminPwLen=$adminPwLen")

$psql = Join-Path $P.PostgresDir 'bin\psql.exe'
if (-not (Test-Path $psql)) { throw "psql introuvable: $psql" }

function Invoke-Psql {
    param(
        [string]$User,
        [string]$Database,
        [string]$Sql,
        [switch]$Command
    )
    if ($Command) {
        $out = & $psql -h 127.0.0.1 -p 5432 -U $User -d $Database -v ON_ERROR_STOP=1 -c $Sql 2>&1
    } else {
        $out = & $psql -h 127.0.0.1 -p 5432 -U $User -d $Database -tAc $Sql 2>&1
    }
    return @{
        ExitCode = $LASTEXITCODE
        Text     = (Get-SafeText $out)
        Raw      = $out
    }
}

$env:PGPASSWORD = [string]$sec.dbAdminPassword
try {
    $dataDirRes = Invoke-Psql -User $sec.dbAdminUser -Database postgres -Sql 'SHOW data_directory'
    Log ("admin login exit=$($dataDirRes.ExitCode) data_directory=$($dataDirRes.Text)")
    if ($dataDirRes.ExitCode -ne 0) {
        throw 'Login admin PostgreSQL echoue. secrets.dpapi desynchronise avec le cluster.'
    }
    $dd = $dataDirRes.Text -replace '/', '\'
    if ($dd -notmatch 'GestPOV') {
        Log "WARN: port 5432 n'est peut-etre pas le cluster GestPOV ($dd)"
    }

    $roleRes = Invoke-Psql -User $sec.dbAdminUser -Database postgres -Sql "SELECT 1 FROM pg_roles WHERE rolname='$($sec.dbAppUser)'"
    $roleOk = ($roleRes.Text -eq '1')
    Log ("role $($sec.dbAppUser) exists=$roleOk")

    $appPw = (Get-SafeText $sec.dbAppPassword).Replace("'", "''")
    if (-not $roleOk) {
        $r = Invoke-Psql -User $sec.dbAdminUser -Database postgres -Sql "CREATE ROLE $($sec.dbAppUser) LOGIN PASSWORD '$appPw';" -Command
        if ($r.ExitCode -ne 0) { throw "CREATE ROLE failed: $($r.Text)" }
        Log 'CREATE ROLE done'
    } else {
        $r = Invoke-Psql -User $sec.dbAdminUser -Database postgres -Sql "ALTER ROLE $($sec.dbAppUser) WITH LOGIN PASSWORD '$appPw';" -Command
        if ($r.ExitCode -ne 0) { throw "ALTER ROLE failed: $($r.Text)" }
        Log 'ALTER ROLE password synced'
    }

    $dbRes = Invoke-Psql -User $sec.dbAdminUser -Database postgres -Sql "SELECT 1 FROM pg_database WHERE datname='$($sec.dbName)'"
    if ($dbRes.Text -ne '1') {
        $r = Invoke-Psql -User $sec.dbAdminUser -Database postgres -Sql "CREATE DATABASE $($sec.dbName) OWNER $($sec.dbAppUser);" -Command
        if ($r.ExitCode -ne 0) { throw "CREATE DATABASE failed: $($r.Text)" }
        Log "CREATE DATABASE $($sec.dbName)"
    } else {
        $r = Invoke-Psql -User $sec.dbAdminUser -Database postgres -Sql "ALTER DATABASE $($sec.dbName) OWNER TO $($sec.dbAppUser);" -Command
        if ($r.ExitCode -ne 0) { throw "ALTER DATABASE owner failed: $($r.Text)" }
        Log "DATABASE $($sec.dbName) owner ensured"
    }
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

$env:PGPASSWORD = [string]$sec.dbAppPassword
try {
    $who = Invoke-Psql -User $sec.dbAppUser -Database $sec.dbName -Sql 'SELECT current_user'
    if ($who.ExitCode -ne 0) { throw "App login still failing: $($who.Text)" }
    Log ("app login OK as $($who.Text)")
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Log 'Restart GestPOV-Server...'
Stop-Service $P.AppServiceName -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Start-Service $P.AppServiceName
Start-Sleep -Seconds 8
$svc = Get-Service $P.AppServiceName
Log ("service status=$($svc.Status)")

$ok = $false
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    try {
        $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$($P.ApiPort)/api/discovery" -UseBasicParsing -TimeoutSec 5
        if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300) {
            $ok = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 3
    }
}

if (-not $ok) {
    Log 'Discovery still failing - see GestPOV-Server.out.log'
    exit 1
}

Log 'Discovery OK. Fix complete.'
exit 0
