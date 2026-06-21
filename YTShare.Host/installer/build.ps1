#requires -Version 5.1
$ErrorActionPreference = 'Stop'

$root    = Split-Path -Parent $MyInvocation.MyCommand.Path
$project = Join-Path $root '..\YTShare.Server\YTShare.Server.csproj'
$iss     = Join-Path $root 'YTShareHost.iss'
$msi     = Join-Path $root 'redist\Bonjour64.msi'
$iscc    = Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6\ISCC.exe'
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'

if (-not (Test-Path $msi)) {
    throw "Missing $msi. Place the Apple Bonjour64.msi (Bonjour SDK for Windows) there before building."
}
if (-not (Test-Path $iscc)) {
    throw "Inno Setup 6 not found at $iscc. Install it from https://jrsoftware.org/isdl.php"
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

Write-Host 'Compiling installer...'
& $iscc $iss
if ($LASTEXITCODE -ne 0) { throw "ISCC failed ($LASTEXITCODE)" }

Write-Host "Done: $(Join-Path $root 'Output\YTShareHostSetup.exe')"
