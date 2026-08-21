#Requires -Version 5.1
#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Installation serveur Gest POV Windows (package offline).
  Idempotent : ne recree pas DB, server.id, licence ni secrets existants.
#>
[CmdletBinding()]
param(
    [string]$PackageRoot,
    [switch]$ResetSecrets
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $PackageRoot) {
    if ($PSScriptRoot -like '*\scripts\server') {
        $PackageRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
        $buildGuess = Join-Path $PackageRoot 'server-package\build\GestPOV-Server-Offline'
        if (Test-Path $buildGuess) { $PackageRoot = $buildGuess }
        elseif (Test-Path (Join-Path (Split-Path $PSScriptRoot -Parent) '..\server-package\backend')) {
            $PackageRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..\server-package')
        }
    } else {
        $PackageRoot = $PSScriptRoot
    }
}

. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
. (Join-Path $PSScriptRoot 'lib\GestPovSecrets.ps1')

Assert-GestPovAdministrator
$P = Get-GestPovPaths

function Resolve-PackageLayout {
    param([string]$Root)
    $root = (Resolve-Path $Root).Path
    $candidates = @(
        $root,
        (Join-Path $root 'GestPOV-Server-Offline'),
        (Join-Path $root 'server-package\build\GestPOV-Server-Offline'),
        (Join-Path $root 'build\GestPOV-Server-Offline')
    )
    foreach ($c in $candidates) {
        $jar = Join-Path $c 'backend\gest-pov-server.jar'
        if (Test-Path $jar) {
            return [pscustomobject]@{
                Root     = $c
                Jar      = $jar
                Runtime  = Join-Path $c 'runtime'
                Postgres = Join-Path $c 'postgres'
                WinSw    = Join-Path $c 'winsw\WinSW.exe'
                JavaExe  = Join-Path $c 'runtime\bin\java.exe'
                PgBin    = Join-Path $c 'postgres\bin'
            }
        }
    }
    throw "Package offline introuvable (gest-pov-server.jar manquant). Construisez-le avec desktop/scripts/build/build-offline-package.ps1"
}

$pkg = Resolve-PackageLayout -Root $PackageRoot

# Logs early: ProgramData may not exist yet
New-Item -ItemType Directory -Path $P.LogsDir -Force | Out-Null
$log = $P.InstallerLog
Write-GestPovLog -LogFile $log -Message "=== install-server.ps1 Package=$($pkg.Root) ==="

foreach ($req in @($pkg.Jar, $pkg.JavaExe)) {
    if (-not (Test-Path $req)) {
        throw "Fichier requis manquant dans le package : $req"
    }
}
if (-not (Test-Path (Join-Path $pkg.PgBin 'initdb.exe'))) {
    throw "PostgreSQL embarque manquant (initdb.exe). Relancez le build editeur."
}
if (-not (Test-Path $pkg.WinSw)) {
    throw "WinSW.exe manquant dans le package (winsw\WinSW.exe)."
}

$free = Get-GestPovDiskFreeGb
if ($null -ne $free -and $free -lt 1.5) {
    throw "Espace disque insuffisant sur C: (${free} Go). Minimum recommande : 2 Go."
}

# --- Repertoires persistants ---
foreach ($d in @($P.ConfigDir, $P.PgDataDir, $P.LicenseDir, $P.UploadsDir, $P.LogsDir, $P.BackupsDir,
                 $P.ServerDir, $P.RuntimeDir, $P.PostgresDir)) {
    if (-not (Test-Path $d)) {
        New-Item -ItemType Directory -Path $d -Force | Out-Null
        Write-GestPovLog -LogFile $log -Message "Repertoire cree : $d"
    }
}

# --- Copie binaires (idempotent) ---
Write-GestPovLog -LogFile $log -Message "Copie runtime Java..."
robocopy $pkg.Runtime $P.RuntimeDir /E /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
if ($LASTEXITCODE -ge 8) { throw "Echec copie runtime Java (robocopy $LASTEXITCODE)" }

Write-GestPovLog -LogFile $log -Message "Copie PostgreSQL..."
robocopy $pkg.Postgres $P.PostgresDir /E /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
if ($LASTEXITCODE -ge 8) { throw "Echec copie PostgreSQL (robocopy $LASTEXITCODE)" }

Copy-Item $pkg.Jar (Join-Path $P.ServerDir 'gest-pov-server.jar') -Force
Copy-Item $pkg.WinSw (Join-Path $P.ServerDir 'GestPOV-Server.exe') -Force
Write-GestPovLog -LogFile $log -Message "JAR et WinSW copies."

$java = Join-Path $P.RuntimeDir 'bin\java.exe'
$pgBin = Join-Path $P.PostgresDir 'bin'
$env:Path = "$pgBin;" + $env:Path

# --- server.id ---
if (-not (Test-Path $P.ServerIdFile) -or [string]::IsNullOrWhiteSpace((Get-Content $P.ServerIdFile -Raw))) {
    [guid]::NewGuid().ToString() | Set-Content -Path $P.ServerIdFile -Encoding ASCII -NoNewline
    Write-GestPovLog -LogFile $log -Message "serverId genere."
} else {
    Write-GestPovLog -LogFile $log -Message "serverId existant conserve."
}
Set-GestPovAclAdminOnly -Path $P.ServerIdFile

# --- Secrets ---
$needSecrets = $ResetSecrets -or -not (Test-Path $P.SecretsFile)
if ($needSecrets) {
    Write-GestPovLog -LogFile $log -Message "Generation des secrets (DPAPI LocalMachine)."
    $adminEmail = 'admin@gestpov.local'
    $secrets = @{
        dbAdminUser           = $P.DbAdminUser
        dbAdminPassword       = New-GestPovPassword 28
        dbAppUser             = $P.DbAppUser
        dbAppPassword         = New-GestPovPassword 28
        dbName                = $P.DbName
        jwtSecret             = New-GestPovPassword 48
        bootstrapAdminEmail   = $adminEmail
        bootstrapAdminPassword= New-GestPovPassword 20
        bootstrapAdminName    = 'Administrateur'
        bootstrapAdminLastName= 'Local'
    }
    Protect-GestPovSecrets -Secrets $secrets -Path $P.SecretsFile -Scope LocalMachine
    @"
Gest POV — identifiants administrateur INITIAL (a changer apres premiere connexion)
Email    : $adminEmail
Mot de passe : $($secrets.bootstrapAdminPassword)

Ce fichier est restreint Administrateurs. Conservez-le hors du PC (coffre) puis supprimez-le.
"@ | Set-Content -Path $P.InitialAdminFile -Encoding UTF8
    Set-GestPovAclAdminOnly -Path $P.InitialAdminFile
    Write-GestPovLog -LogFile $log -Message "Fichier INITIAL_ADMIN.txt ecrit (ACL admin). Mot de passe non journalise."
} else {
    Write-GestPovLog -LogFile $log -Message "Secrets existants conserves."
}

$sec = Unprotect-GestPovSecrets -Path $P.SecretsFile -Scope LocalMachine

# ConvertFrom-Json -> PSCustomObject ; garantir les champs requis (install partielle / ancien fichier)
# StrictMode: ([string]$null).Trim() PLANTE — toujours passer par ConvertTo-GestPovText
function ConvertTo-GestPovText {
    param($Value)
    if ($null -eq $Value) { return '' }
    return ("$Value").Trim()
}
function Get-GestPovSecretText {
    param($Secrets, [string]$Name)
    $prop = $Secrets.PSObject.Properties[$Name]
    if (-not $prop) { return $null }
    $v = $prop.Value
    if ($null -eq $v) { return $null }
    $text = ConvertTo-GestPovText $v
    if ($text -eq '') { return $null }
    return $text
}
function Set-GestPovSecretText {
    param($Secrets, [string]$Name, [string]$Value)
    if ($Secrets.PSObject.Properties[$Name]) {
        $Secrets.$Name = $Value
    } else {
        Add-Member -InputObject $Secrets -NotePropertyName $Name -NotePropertyValue $Value -Force
    }
}
$secretRepaired = $false
foreach ($pair in @(
        @{ Name = 'dbAdminUser'; Default = $P.DbAdminUser },
        @{ Name = 'dbAppUser'; Default = $P.DbAppUser },
        @{ Name = 'dbName'; Default = $P.DbName }
    )) {
    if ([string]::IsNullOrWhiteSpace((Get-GestPovSecretText $sec $pair.Name))) {
        Set-GestPovSecretText $sec $pair.Name $pair.Default
        $secretRepaired = $true
    }
}
foreach ($pwName in @('dbAdminPassword', 'dbAppPassword', 'jwtSecret', 'bootstrapAdminPassword')) {
    if ([string]::IsNullOrWhiteSpace((Get-GestPovSecretText $sec $pwName))) {
        $len = if ($pwName -eq 'jwtSecret') { 48 } elseif ($pwName -eq 'bootstrapAdminPassword') { 20 } else { 28 }
        Set-GestPovSecretText $sec $pwName (New-GestPovPassword $len)
        $secretRepaired = $true
        Write-GestPovLog -LogFile $log -Message "Secret manquant regenere: $pwName (valeur non journalisee)."
    }
}
if ($secretRepaired) {
    $hash = @{}
    foreach ($p in $sec.PSObject.Properties) { $hash[$p.Name] = $p.Value }
    Protect-GestPovSecrets -Secrets $hash -Path $P.SecretsFile -Scope LocalMachine
    Write-GestPovLog -LogFile $log -Message "Fichier secrets.dpapi mis a jour (champs manquants)."
}

# --- PostgreSQL cluster ---
$pgData = $P.PgDataDir
$pgMarker = Join-Path $pgData 'PG_VERSION'
if (-not (Test-Path $pgMarker)) {
    Write-GestPovLog -LogFile $log -Message "Initialisation cluster PostgreSQL..."
    $pwFile = Join-Path $env:TEMP 'gestpov-init-pw.txt'
    try {
        Set-Content -Path $pwFile -Value (Get-GestPovSecretText $sec 'dbAdminPassword') -Encoding ASCII -NoNewline
        $initdb = Join-Path $pgBin 'initdb.exe'
        $arg = @(
            '-D', $pgData,
            '-U', (Get-GestPovSecretText $sec 'dbAdminUser'),
            '-A', 'scram-sha-256',
            '--locale=C',
            '--encoding=UTF8',
            '--pwfile', $pwFile
        )
        & $initdb @arg
        if ($LASTEXITCODE -ne 0) { throw "initdb a echoue ($LASTEXITCODE)." }
    } finally {
        if (Test-Path $pwFile) { Remove-Item $pwFile -Force }
    }
    $conf = Join-Path $pgData 'postgresql.conf'
    $hba = Join-Path $pgData 'pg_hba.conf'
    (Get-Content $conf) `
        -replace "^#?listen_addresses\s*=.*", "listen_addresses = '127.0.0.1'" `
        -replace "^#?port\s*=.*", "port = 5432" `
        | Set-Content $conf -Encoding ASCII
    if (-not ((Get-Content $conf) -match "listen_addresses = '127.0.0.1'")) {
        Add-Content $conf "`nlisten_addresses = '127.0.0.1'`nport = 5432`n"
    }
    @"
# Gest POV — localhost only
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
"@ | Set-Content $hba -Encoding ASCII
    Write-GestPovLog -LogFile $log -Message "Cluster initialise (listen 127.0.0.1)."
} else {
    Write-GestPovLog -LogFile $log -Message "Cluster PostgreSQL existant conserve."
}

# --- Service PostgreSQL ---
$pgSvc = Get-GestPovService $P.PgServiceName
if (-not $pgSvc) {
    Write-GestPovLog -LogFile $log -Message "Enregistrement service $($P.PgServiceName)..."
    $pgctl = Join-Path $pgBin 'pg_ctl.exe'
    & $pgctl register -N $P.PgServiceName -D $pgData -S auto
    if ($LASTEXITCODE -ne 0) {
        Write-GestPovLog -Level WARN -LogFile $log -Message "pg_ctl register code $LASTEXITCODE - tentative sc.exe"
        $pgSvcCmd = "`"$pgctl`" runservice -N `"$($P.PgServiceName)`" -D `"$pgData`" -s"
        sc.exe create $P.PgServiceName binPath= $pgSvcCmd start= auto | Out-Null
    }
} else {
    Write-GestPovLog -LogFile $log -Message "Service PostgreSQL deja present."
}

$pgSvc = Get-GestPovService $P.PgServiceName
if ($pgSvc -and $pgSvc.Status -ne 'Running') {
    Start-Service $P.PgServiceName
}
Write-GestPovLog -LogFile $log -Message "Attente PostgreSQL (pg_isready)..."
$ready = $false
$deadline = (Get-Date).AddSeconds(120)
$pgIsReady = Join-Path $pgBin 'pg_isready.exe'
while ((Get-Date) -lt $deadline) {
    & $pgIsReady -h 127.0.0.1 -p 5432 | Out-Null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw "PostgreSQL n'est pas pret (pg_isready timeout)." }
Write-GestPovLog -LogFile $log -Message "PostgreSQL pret."

# --- Role + DB ---
$env:PGPASSWORD = Get-GestPovSecretText $sec 'dbAdminPassword'
$psql = Join-Path $pgBin 'psql.exe'
$dbAdminUser = Get-GestPovSecretText $sec 'dbAdminUser'
$dbAppUser = Get-GestPovSecretText $sec 'dbAppUser'
$dbName = Get-GestPovSecretText $sec 'dbName'
$dbAppPassword = Get-GestPovSecretText $sec 'dbAppPassword'
if ([string]::IsNullOrWhiteSpace($dbAppPassword)) {
    throw "dbAppPassword introuvable dans secrets.dpapi apres reparation."
}

function Invoke-PsqlAdmin([string]$Sql) {
    & $psql -h 127.0.0.1 -p 5432 -U $dbAdminUser -d postgres -v ON_ERROR_STOP=1 -c $Sql
    if ($LASTEXITCODE -ne 0) { throw "psql a echoue." }
}
# Exact match only (never -match '1': error text like 127.0.0.1 would false-positive).
$dataDirCheck = & $psql -h 127.0.0.1 -p 5432 -U $dbAdminUser -d postgres -tAc "SHOW data_directory"
if ($LASTEXITCODE -ne 0) { throw "psql admin login failed before role setup." }
$dataDirText = ConvertTo-GestPovText $dataDirCheck
Write-GestPovLog -LogFile $log -Message "PostgreSQL data_directory=$dataDirText"

$appPw = (ConvertTo-GestPovText $dbAppPassword).Replace("'", "''")
$roleExists = & $psql -h 127.0.0.1 -p 5432 -U $dbAdminUser -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$dbAppUser'"
if ((ConvertTo-GestPovText $roleExists) -ne '1') {
    $sqlCreateRole = "CREATE ROLE {0} LOGIN PASSWORD '{1}';" -f $dbAppUser, $appPw
    Invoke-PsqlAdmin $sqlCreateRole
    Write-GestPovLog -LogFile $log -Message "Role applicatif cree."
} else {
    $sqlAlterRole = "ALTER ROLE {0} WITH LOGIN PASSWORD '{1}';" -f $dbAppUser, $appPw
    Invoke-PsqlAdmin $sqlAlterRole
    Write-GestPovLog -LogFile $log -Message "Role applicatif existant: mot de passe resynchronise avec secrets.dpapi."
}
$dbExists = & $psql -h 127.0.0.1 -p 5432 -U $dbAdminUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$dbName'"
if ((ConvertTo-GestPovText $dbExists) -ne '1') {
    $sqlCreateDb = "CREATE DATABASE {0} OWNER {1};" -f $dbName, $dbAppUser
    Invoke-PsqlAdmin $sqlCreateDb
    Write-GestPovLog -LogFile $log -Message "Base $dbName creee."
} else {
    Invoke-PsqlAdmin ("ALTER DATABASE {0} OWNER TO {1};" -f $dbName, $dbAppUser)
    Write-GestPovLog -LogFile $log -Message "Base existante conservee (owner aligne)."
}
Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue

# --- application.yml overlay ---
$yaml = @"
spring:
  profiles:
    active: prod,desktop
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/$dbName
    username: $dbAppUser
    password: `${GEST_POV_DB_PASSWORD}

server:
  port: $($P.ApiPort)
  address: 0.0.0.0

logging:
  file:
    name: $($P.ServerLog.Replace('\','/'))

app:
  upload:
    dir: $($P.UploadsDir.Replace('\','/'))
  license:
    data-dir: $($P.LicenseDir.Replace('\','/'))
    enforcement-enabled: true
  desktop:
    server-id-file: $($P.ServerIdFile.Replace('\','/'))
    discovery:
      enabled: true
      udp-port: $($P.UdpPort)
  jwt:
    secret: `${APP_JWT_SECRET}
  bootstrap:
    admin:
      email: `${APP_BOOTSTRAP_ADMIN_EMAIL}
      password: `${APP_BOOTSTRAP_ADMIN_PASSWORD}
      first-name: `${APP_BOOTSTRAP_ADMIN_NAME:Administrateur}
      last-name: `${APP_BOOTSTRAP_ADMIN_LAST_NAME:Local}
"@
Set-Content -Path $P.AppYaml -Value $yaml -Encoding UTF8
Write-GestPovLog -LogFile $log -Message "application.yml genere (mots de passe via variables d'environnement service)."

Copy-Item (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1') (Join-Path $P.ServerDir 'GestPovCommon.ps1') -Force
Copy-Item (Join-Path $PSScriptRoot 'lib\GestPovSecrets.ps1') (Join-Path $P.ServerDir 'GestPovSecrets.ps1') -Force

$launcherPath = Join-Path $P.ServerDir 'start-backend.ps1'
@(
    '# Generated — secrets via DPAPI, never Write-Host'
    '$ErrorActionPreference = ''Stop'''
    ". `"$($P.ServerDir.Replace('\','\\'))\GestPovCommon.ps1`""
    ". `"$($P.ServerDir.Replace('\','\\'))\GestPovSecrets.ps1`""
    "`$P = Get-GestPovPaths"
    "`$sec = Unprotect-GestPovSecrets -Path `$P.SecretsFile -Scope LocalMachine"
    "`$env:SPRING_PROFILES_ACTIVE = 'prod,desktop'"
    "`$env:GEST_POV_DB_PASSWORD = `$sec.dbAppPassword"
    "`$env:APP_JWT_SECRET = `$sec.jwtSecret"
    "`$env:APP_BOOTSTRAP_ADMIN_EMAIL = `$sec.bootstrapAdminEmail"
    "`$env:APP_BOOTSTRAP_ADMIN_PASSWORD = `$sec.bootstrapAdminPassword"
    "`$env:APP_BOOTSTRAP_ADMIN_NAME = `$sec.bootstrapAdminName"
    "`$env:APP_BOOTSTRAP_ADMIN_LAST_NAME = `$sec.bootstrapAdminLastName"
    "`$java = Join-Path `$P.RuntimeDir 'bin\java.exe'"
    "`$jar = Join-Path `$P.ServerDir 'gest-pov-server.jar'"
    "`$cfg = 'file:' + (`$P.ConfigDir -replace '\\','/') + '/'"
    '& $java -Xms256m -Xmx1024m -jar $jar "--spring.config.additional-location=$cfg"'
) | Set-Content -Path $launcherPath -Encoding UTF8

# WinSW xml - powershell wrapper, no secrets in XML
$xmlPath = Join-Path $P.ServerDir 'GestPOV-Server.xml'
$psExe = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
@"
<service>
  <id>$($P.WinSwId)</id>
  <name>Gest POV Server</name>
  <description>API Gest POV (Spring Boot) — localhost PostgreSQL</description>
  <executable>$psExe</executable>
  <arguments>-NoProfile -ExecutionPolicy Bypass -File "$launcherPath"</arguments>
  <logpath>$($P.LogsDir)</logpath>
  <log mode="roll-by-size">
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>8</keepFiles>
  </log>
  <onfailure action="restart" delay="10 sec"/>
  <startmode>Automatic</startmode>
  <depend>$($P.PgServiceName)</depend>
</service>
"@ | Set-Content -Path $xmlPath -Encoding UTF8

$winExe = Join-Path $P.ServerDir 'GestPOV-Server.exe'
$appSvc = Get-GestPovService $P.AppServiceName
if (-not $appSvc) {
    Write-GestPovLog -LogFile $log -Message "Installation service $($P.AppServiceName) (WinSW)..."
    & $winExe install
    if ($LASTEXITCODE -ne 0) {
        Write-GestPovLog -Level WARN -LogFile $log -Message "WinSW install code $LASTEXITCODE"
    }
} else {
    Write-GestPovLog -LogFile $log -Message "Service application deja present."
}

# --- Firewall (profil prive) ---
function Ensure-FirewallRule([string]$Name, [string]$Protocol, [int]$Port) {
    $exists = netsh advfirewall firewall show rule name="$Name" 2>$null | Select-String $Name
    if (-not $exists) {
        netsh advfirewall firewall add rule name="$Name" dir=in action=allow protocol=$Protocol localport=$Port profile=private | Out-Null
        Write-GestPovLog -LogFile $log -Message "Regle firewall $Name ($Protocol $Port, prive)."
    } else {
        Write-GestPovLog -LogFile $log -Message "Regle firewall $Name deja presente."
    }
}
Ensure-FirewallRule -Name $P.FirewallTcpName -Protocol TCP -Port $P.ApiPort
Ensure-FirewallRule -Name $P.FirewallUdpName -Protocol UDP -Port $P.UdpPort

# --- Demarrage application ---
$appSvc = Get-GestPovService $P.AppServiceName
if ($appSvc -and $appSvc.Status -ne 'Running') {
    Start-Service $P.AppServiceName
}
Write-GestPovLog -LogFile $log -Message "Attente API liveness..."
if (-not (Wait-GestPovHttpOk -Url "http://127.0.0.1:$($P.ApiPort)/actuator/health/liveness" -TimeoutSec 180 -LogFile $log)) {
    throw "Gest POV Server n'a pas repondu (liveness). Consultez $($P.LogsDir)"
}
Write-GestPovLog -LogFile $log -Message "API prete. Flyway s'applique au premier demarrage Spring."

Write-Host ""
Write-Host "Installation Gest POV Server terminee."
Write-Host "  PostgreSQL : $($P.PgServiceName) (127.0.0.1:5432, DB $($P.DbName))"
Write-Host "  API        : http://127.0.0.1:$($P.ApiPort)"
Write-Host "  Admin init : $($P.InitialAdminFile) (si nouvellement cree)"
Write-Host "  Health     : .\health-check.ps1"
Write-Host ""
exit 0
