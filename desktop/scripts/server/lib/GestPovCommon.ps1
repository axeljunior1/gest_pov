#Requires -Version 5.1
<#
.SYNOPSIS
  Constantes et helpers communs Gest POV Server (Windows).
  Ne jamais journaliser de mot de passe, JWT ou secret.
#>

$script:GestPovInstallRoot = 'C:\Program Files\GestPOV'
$script:GestPovDataRoot    = 'C:\ProgramData\GestPOV'

function Get-GestPovPaths {
    [CmdletBinding()]
    param()
    $data = $script:GestPovDataRoot
    $install = $script:GestPovInstallRoot
    [pscustomobject]@{
        InstallRoot     = $install
        ServerDir       = Join-Path $install 'server'
        RuntimeDir      = Join-Path $install 'runtime'
        PostgresDir     = Join-Path $install 'postgres'
        WinSwDir        = Join-Path $install 'server'
        DataRoot        = $data
        ConfigDir       = Join-Path $data 'config'
        SecretsFile     = Join-Path $data 'config\secrets.dpapi'
        ServerIdFile    = Join-Path $data 'config\server.id'
        AppYaml         = Join-Path $data 'config\application.yml'
        PgDataDir       = Join-Path $data 'postgres\data'
        LicenseDir      = Join-Path $data 'license'
        UploadsDir      = Join-Path $data 'uploads'
        LogsDir         = Join-Path $data 'logs'
        BackupsDir      = Join-Path $data 'backups'
        InstallerLog    = Join-Path $data 'logs\installer.log'
        ServerLog       = Join-Path $data 'logs\server.log'
        PostgresLog     = Join-Path $data 'logs\postgres.log'
        BackupLog       = Join-Path $data 'logs\backup.log'
        InitialAdminFile= Join-Path $data 'config\INITIAL_ADMIN.txt'
        PgServiceName   = 'GestPOV-PostgreSQL'
        AppServiceName  = 'GestPOV-Server'
        ApiPort         = 8080
        UdpPort         = 38471
        DbName          = 'gest_pov'
        DbAppUser       = 'gest_pov_app'
        DbAdminUser     = 'gest_pov_admin'
        FirewallTcpName = 'GestPOV-API-TCP'
        FirewallUdpName = 'GestPOV-Discovery-UDP'
        WinSwId         = 'GestPOV-Server'
    }
}

function Test-GestPovAdministrator {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($id)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Assert-GestPovAdministrator {
    if (-not (Test-GestPovAdministrator)) {
        throw "Droits administrateur requis. Relancez PowerShell en tant qu'administrateur."
    }
}

function Initialize-GestPovLogDir {
    param([string]$LogFile)
    $dir = Split-Path -Parent $LogFile
    if ($dir -and -not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

function Write-GestPovLog {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Message,
        [ValidateSet('INFO','WARN','ERROR')][string]$Level = 'INFO',
        [string]$LogFile
    )
    $line = '{0:yyyy-MM-dd HH:mm:ss} [{1}] {2}' -f (Get-Date), $Level, $Message
    Write-Host $line
    if ($LogFile) {
        Initialize-GestPovLogDir -LogFile $LogFile
        Add-Content -Path $LogFile -Value $line -Encoding UTF8
    }
}

function Test-GestPovSecretLeak {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return }
    $lower = $Text.ToLowerInvariant()
    foreach ($word in @('password=','jwt=','secret=','-----begin')) {
        if ($lower.Contains($word)) {
            throw "Refus d'ecrire une valeur sensible dans les logs."
        }
    }
}

function New-GestPovPassword {
    param([int]$Length = 24)
    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_'
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
        $sb = New-Object System.Text.StringBuilder
        foreach ($b in $bytes) {
            [void]$sb.Append($chars[$b % $chars.Length])
        }
        return $sb.ToString()
    } finally {
        $rng.Dispose()
    }
}

function Get-GestPovService {
    param([string]$Name)
    return Get-Service -Name $Name -ErrorAction SilentlyContinue
}

function Wait-GestPovTcpPort {
    param(
        [string]$TargetHost = '127.0.0.1',
        [int]$Port,
        [int]$TimeoutSec = 120,
        [string]$LogFile
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $client = New-Object System.Net.Sockets.TcpClient
            $iar = $client.BeginConnect($TargetHost, $Port, $null, $null)
            $ok = $iar.AsyncWaitHandle.WaitOne(1000, $false)
            if ($ok -and $client.Connected) {
                $client.EndConnect($iar)
                $client.Close()
                return $true
            }
            $client.Close()
        } catch {
            # retry
        }
        Start-Sleep -Seconds 1
    }
    Write-GestPovLog -Level ERROR -LogFile $LogFile -Message "Timeout attente ${TargetHost}:${Port}"
    return $false
}

function Wait-GestPovHttpOk {
    param(
        [Parameter(Mandatory)][string]$Url,
        [int]$TimeoutSec = 180,
        [string]$LogFile
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300) {
                return $true
            }
        } catch {
            # retry
        }
        Start-Sleep -Seconds 2
    }
    Write-GestPovLog -Level ERROR -LogFile $LogFile -Message "Timeout HTTP $Url"
    return $false
}

function Get-GestPovDiskFreeGb {
    $drive = Get-PSDrive -Name C -ErrorAction SilentlyContinue
    if (-not $drive) { return $null }
    return [math]::Round($drive.Free / 1GB, 2)
}

function Set-GestPovAclAdminOnly {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path $Path)) { return }
    try {
        $acl = Get-Acl -LiteralPath $Path
        $acl.SetAccessRuleProtection($true, $false)
        $system = [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18')
        $admins = [System.Security.Principal.SecurityIdentifier]::new('S-1-5-32-544')
        $ruleSys = [System.Security.AccessControl.FileSystemAccessRule]::new($system, 'FullControl', 'Allow')
        $ruleAdm = [System.Security.AccessControl.FileSystemAccessRule]::new($admins, 'FullControl', 'Allow')
        $acl.SetAccessRule($ruleSys)
        $acl.SetAccessRule($ruleAdm)
        Set-Acl -LiteralPath $Path -AclObject $acl
    } catch {
        Write-GestPovLog -Level WARN -Message "ACL restreinte non appliquee sur $Path"
    }
}
