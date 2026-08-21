#Requires -Version 5.1
function Write-GestPovAsciiCmd {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Content
    )
    $text = $Content -replace "`r`n", "`n" -replace "`n", "`r`n"
    if (-not $text.EndsWith("`r`n")) { $text += "`r`n" }
    $list = New-Object System.Collections.Generic.List[byte]
    foreach ($ch in $text.ToCharArray()) {
        $code = [int][char]$ch
        if ($code -gt 127) {
            throw "Write-GestPovAsciiCmd: non-ASCII char U+$($code.ToString('X4')) in $Path"
        }
        [void]$list.Add([byte]$code)
    }
    [System.IO.File]::WriteAllBytes($Path, $list.ToArray())
}