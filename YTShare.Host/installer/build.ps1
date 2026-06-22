#requires -Version 5.1
$ErrorActionPreference = 'Stop'

$root    = Split-Path -Parent $MyInvocation.MyCommand.Path
$project = Join-Path $root '..\YTShare.Server\YTShare.Server.csproj'
$iss     = Join-Path $root 'YTShareHost.iss'
$msi     = Join-Path $root 'redist\Bonjour64.msi'
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'

# Locate ISCC.exe — Inno Setup can be a per-machine OR per-user install.
function Find-Iscc {
    $candidates = @(
        (Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6\ISCC.exe'),
        (Join-Path $env:ProgramFiles        'Inno Setup 6\ISCC.exe'),
        (Join-Path $env:LOCALAPPDATA        'Programs\Inno Setup 6\ISCC.exe')
    )
    # Registry-recorded install location (per-machine and per-user)
    $uninstall = @(
        'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*',
        'HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*',
        'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*'
    )
    foreach ($k in $uninstall) {
        Get-ItemProperty $k -ErrorAction SilentlyContinue |
            Where-Object { $_.DisplayName -match 'Inno Setup' -and $_.InstallLocation } |
            ForEach-Object { $candidates += (Join-Path $_.InstallLocation 'ISCC.exe') }
    }
    foreach ($c in $candidates) { if ($c -and (Test-Path $c)) { return $c } }
    # Last resort: on PATH
    $cmd = Get-Command iscc.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

if (-not (Test-Path $msi)) {
    throw "Missing $msi. Place the Apple Bonjour64.msi (Bonjour SDK for Windows) there before building."
}
$iscc = Find-Iscc
if (-not $iscc) {
    throw "ISCC.exe (Inno Setup command-line compiler) not found. Install Inno Setup 6 from https://jrsoftware.org/isdl.php"
}
if (-not (Test-Path $vswhere)) {
    throw "vswhere not found at $vswhere. Install Visual Studio (with MSBuild) — the dotnet CLI cannot build the Bonjour COMReference."
}

# The project's Bonjour COMReference requires full VS MSBuild; dotnet build/publish fails with MSB4803.
$msbuild = & $vswhere -latest -requires Microsoft.Component.MSBuild -find 'MSBuild\**\Bin\MSBuild.exe' | Select-Object -First 1
if (-not $msbuild) { throw 'MSBuild.exe not found via vswhere. Ensure the MSBuild component is installed with Visual Studio.' }

Write-Host "Publishing YTShare.Server (self-contained, single-file, win-x64) via $msbuild ..."
& $msbuild $project /t:Restore,Publish /p:Configuration=Release /p:RuntimeIdentifier=win-x64 /p:SelfContained=true /p:PublishSingleFile=true /v:minimal /nologo
if ($LASTEXITCODE -ne 0) { throw "MSBuild publish failed ($LASTEXITCODE)" }

Write-Host "Compiling installer via $iscc ..."
& $iscc $iss
if ($LASTEXITCODE -ne 0) { throw "ISCC failed ($LASTEXITCODE)" }

Write-Host "Done: $(Join-Path $root 'Output\YTShareHostSetup.exe')"
