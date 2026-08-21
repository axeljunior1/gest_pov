#Requires -Version 5.1
<#
.SYNOPSIS
  Secrets Gest POV — DPAPI LocalMachine + ACL NTFS.
  Le mot de passe n'est jamais ecrit dans les logs.
#>

Add-Type -AssemblyName System.Security

function Protect-GestPovSecrets {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][hashtable]$Secrets,
        [Parameter(Mandatory)][string]$Path,
        [ValidateSet('LocalMachine','CurrentUser')]
        [string]$Scope = 'LocalMachine'
    )
    # PS 5.1: (hashtable | ConvertTo-Json) peut sortir un tableau Key/Value inutilisable.
    # Forcer un objet plat pour que ConvertFrom-Json expose .dbAppPassword etc.
    $obj = New-Object PSCustomObject
    foreach ($key in $Secrets.Keys) {
        Add-Member -InputObject $obj -NotePropertyName ([string]$key) -NotePropertyValue $Secrets[$key] -Force
    }
    $json = $obj | ConvertTo-Json -Compress -Depth 5
    $plain = [System.Text.Encoding]::UTF8.GetBytes($json)
    $dpScope = if ($Scope -eq 'LocalMachine') {
        [System.Security.Cryptography.DataProtectionScope]::LocalMachine
    } else {
        [System.Security.Cryptography.DataProtectionScope]::CurrentUser
    }
    $protected = [System.Security.Cryptography.ProtectedData]::Protect($plain, $null, $dpScope)
    $dir = Split-Path -Parent $Path
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    [System.IO.File]::WriteAllBytes($Path, $protected)
    if ($Scope -eq 'LocalMachine' -and (Get-Command Set-GestPovAclAdminOnly -ErrorAction SilentlyContinue)) {
        Set-GestPovAclAdminOnly -Path $Path
    }
}

function Unprotect-GestPovSecrets {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Path,
        [ValidateSet('LocalMachine','CurrentUser')]
        [string]$Scope = 'LocalMachine'
    )
    if (-not (Test-Path $Path)) {
        throw "Fichier secrets introuvable."
    }
    $blob = [System.IO.File]::ReadAllBytes($Path)
    $dpScope = if ($Scope -eq 'LocalMachine') {
        [System.Security.Cryptography.DataProtectionScope]::LocalMachine
    } else {
        [System.Security.Cryptography.DataProtectionScope]::CurrentUser
    }
    $plain = [System.Security.Cryptography.ProtectedData]::Unprotect($blob, $null, $dpScope)
    $json = [System.Text.Encoding]::UTF8.GetString($plain)
    $parsed = $json | ConvertFrom-Json

    # Ancien format (bug hashtable|ConvertTo-Json) : tableau {Key,Value}
    if ($parsed -is [System.Array] -or ($parsed -and $parsed.PSObject.Properties['Key'] -and $parsed.PSObject.Properties['Value'] -and -not $parsed.PSObject.Properties['dbAppPassword'])) {
        $hash = @{}
        foreach ($row in @($parsed)) {
            if ($null -ne $row.Key) { $hash[[string]$row.Key] = $row.Value }
        }
        $obj = New-Object PSCustomObject
        foreach ($key in $hash.Keys) {
            Add-Member -InputObject $obj -NotePropertyName $key -NotePropertyValue $hash[$key] -Force
        }
        return $obj
    }
    return $parsed
}

function Test-GestPovSecretsFile {
    param([string]$Path)
    return (Test-Path -LiteralPath $Path)
}
