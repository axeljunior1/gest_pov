# Commit une feature sur une branche dédiée (workflow local)
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Feature,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$Message,

    [switch]$MergeToMain
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".git")) {
    Write-Error "Dépôt Git absent. Lancez d'abord : git init"
}

$branchName = "feature/" + ($Feature.Trim() -replace '\s+', '-' -replace '[^a-zA-Z0-9\-/]', '-').ToLower()
$current = git branch --show-current 2>$null

if ($current -ne $branchName) {
    $exists = git branch --list $branchName
    if ($exists) {
        git checkout $branchName | Out-Null
    } else {
        git checkout -b $branchName | Out-Null
    }
}

git add -A
$status = git status --porcelain
if (-not $status) {
    Write-Host "Rien à committer." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
git diff --cached --stat
Write-Host ""

git commit -m "$Message"
Write-Host ""
Write-Host "Commit sur $branchName" -ForegroundColor Green
Write-Host $Message -ForegroundColor DarkGray

if ($MergeToMain) {
    git checkout main | Out-Null
    git merge $branchName --no-edit
    Write-Host "Fusionné dans main." -ForegroundColor Green
}
