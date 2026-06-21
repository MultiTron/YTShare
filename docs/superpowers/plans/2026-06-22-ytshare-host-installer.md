# YTShare.Host Installer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Inno Setup installer that deploys YTShare.Host as a silent, windowless, auto-starting per-user app with Bonjour and a firewall rule, and wire the Angular landing page's "Download for Windows" button to the released installer.

**Architecture:** Publish `YTShare.Server` self-contained (single-file, win-x64) so no .NET install is needed. An Inno Setup script bundles that output plus Apple's `Bonjour64.msi`, runs elevated to install Bonjour (if absent), add a firewall rule for TCP 7296, and register a per-user logon Scheduled Task that launches the windowless app. The installer is published as a GitHub Release asset; the landing page links to GitHub's "latest" permalink.

**Tech Stack:** .NET 8 (ASP.NET Core), Inno Setup 6 (Pascal scripting), PowerShell (build script), Angular 20 (static landing page), GitHub Releases.

## Global Constraints

- Target architecture: **win-x64** only.
- App must run **windowless** (no console) — `OutputType` = `WinExe`.
- App runs **per-user, non-elevated** in the logged-in user's session; the installer runs **elevated** (admin).
- App listens on `http://0.0.0.0:7296`; firewall rule and Bonjour service name must use port/name verbatim.
- Scheduled Task name: exactly `YTShare Host`. Firewall rule name: exactly `YTShare Host`.
- The logon task is registered from an XML definition (`schtasks /Create /XML`), runs at any interactive logon as the Users group (SID `S-1-5-32-545`, least privilege) with restart-on-failure. No "hidden" task setting (the app is a windowless `WinExe`).
- Real failure of the firewall *add* or the task *create* surfaces a warning (not silent); the firewall *delete* and uninstall steps stay fail-open for idempotency.
- Installer output filename: exactly `YTShareHostSetup.exe` (the landing-page URL depends on this name).
- Bonjour is **left installed** on uninstall unless the user opts to remove it.
- Repo: `MultiTron/YTShare` (public). Landing-page download URL: `https://github.com/MultiTron/YTShare/releases/latest/download/YTShareHostSetup.exe`.
- `Bonjour64.msi` is **user-supplied** (Apple Bonjour SDK for Windows) and must **never** be committed to git.
- Code signing is out of scope.
- **Build with Visual Studio MSBuild, NOT the `dotnet` CLI.** The project has a `COMReference` to Bonjour, which `dotnet build`/`dotnet publish` cannot resolve (error MSB4803 — `ResolveComReference` requires .NET Framework MSBuild). Locate MSBuild via vswhere: `"${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe" -latest -requires Microsoft.Component.MSBuild -find "MSBuild\**\Bin\MSBuild.exe"`. Restore must run with the RID (`/t:Restore,Publish` with `/p:RuntimeIdentifier=win-x64`), or restore errors with NETSDK1047.

## File Structure

- `YTShare.Host/YTShare.Server/YTShare.Server.csproj` — **modify**: add `WinExe`.
- `YTShare.Host/installer/YTShareHost.iss` — **create**: Inno Setup script (files, install/uninstall logic).
- `YTShare.Host/installer/build.ps1` — **create**: publish app + compile installer.
- `YTShare.Host/installer/.gitignore` — **create**: exclude `redist/Bonjour64.msi` and `Output/`.
- `YTShare.Host/installer/redist/` — directory for the user-supplied `Bonjour64.msi` (only a `.gitkeep` is committed).
- `frontend/src/app/features/landing/landing.component.html` — **modify**: Windows Host download anchor.

---

### Task 1: Make the host windowless (WinExe)

**Files:**
- Modify: `YTShare.Host/YTShare.Server/YTShare.Server.csproj`

**Interfaces:**
- Consumes: nothing.
- Produces: a `YTShare.Server` project that, when published, yields a windowless `YTShare.Server.exe` (no console window). Consumed by Task 2's publish output path `bin/Release/net8.0/win-x64/publish/`.

- [ ] **Step 1: Add the WinExe output type**

In `YTShare.Host/YTShare.Server/YTShare.Server.csproj`, edit the first `<PropertyGroup>` so it reads:

```xml
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <OutputType>WinExe</OutputType>
  </PropertyGroup>
```

- [ ] **Step 2: Verify it still builds (with VS MSBuild — see Global Constraints)**

Do NOT use `dotnet build` (fails with MSB4803 on the Bonjour COMReference). Locate and run VS MSBuild:
```powershell
$msbuild = & "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe" -latest -requires Microsoft.Component.MSBuild -find "MSBuild\**\Bin\MSBuild.exe" | Select-Object -First 1
& $msbuild "C:\Users\iliev\Desktop\YTShare\YTShare.Host\YTShare.Server\YTShare.Server.csproj" /t:Restore,Build /p:Configuration=Release /v:minimal /nologo
"EXITCODE=$LASTEXITCODE"
```
Expected: `EXITCODE=0` and a line `YTShare.Server -> ...\bin\Release\net8.0\YTShare.Server.dll`. The Bonjour `COMReference` must remain intact in `Program.cs` and the `.csproj` — do not remove it.

- [ ] **Step 3: Commit**

```powershell
git add YTShare.Host/YTShare.Server/YTShare.Server.csproj
git commit -m "feat(host): build YTShare.Server as windowless WinExe"
```

---

### Task 2: Inno Setup script

**Files:**
- Create: `YTShare.Host/installer/YTShareHost.iss`
- Create: `YTShare.Host/installer/redist/.gitkeep`

**Interfaces:**
- Consumes: the published app at `..\YTShare.Server\bin\Release\net8.0\win-x64\publish\*` (produced by Task 1's project via Task 3's publish step); `redist\Bonjour64.msi` (user-supplied).
- Produces: when compiled by `ISCC.exe`, an installer `Output\YTShareHostSetup.exe`. Consumed by Task 3's build script.

- [ ] **Step 1: Create the redist placeholder so the folder is tracked**

Create `YTShare.Host/installer/redist/.gitkeep` with a single comment line:

```
# Place the user-supplied Apple Bonjour64.msi here. Do NOT commit the .msi.
```

- [ ] **Step 2: Write the Inno Setup script**

Create `YTShare.Host/installer/YTShareHost.iss` with exactly this content:

```pascal
; YTShare Host installer
; Build with: ISCC.exe YTShareHost.iss  (see build.ps1)

#define AppName "YTShare Host"
#define AppVersion "1.0.0"
#define AppPublisher "YTShare"
#define ExeName "YTShare.Server.exe"

[Setup]
AppId={{8F2C7A14-4E63-4B9E-9D2A-2C7B1E0A9F31}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\YTShare Host
DisableProgramGroupPage=yes
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=Output
OutputBaseFilename=YTShareHostSetup
Compression=lzma2
SolidCompression=yes
UninstallDisplayName={#AppName}
WizardStyle=modern

[Files]
Source: "..\YTShare.Server\bin\Release\net8.0\win-x64\publish\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion
Source: "redist\Bonjour64.msi"; DestDir: "{app}\redist"; Flags: ignoreversion

[Run]
Filename: "{app}\{#ExeName}"; Description: "Launch YTShare Host now"; Flags: postinstall skipifsilent nowait runasoriginaluser

[Code]
const
  TaskName = 'YTShare Host';
  FirewallRule = 'YTShare Host';
  Port = '7296';

function BonjourInstalled(): Boolean;
begin
  Result := RegKeyExists(HKLM, 'SYSTEM\CurrentControlSet\Services\Bonjour Service');
end;

{ Register the logon task from an XML definition: runs at any interactive logon,
  in the user's own session, least-privilege, with restart-on-failure.
  Principal targets the built-in Users group (SID S-1-5-32-545) so it runs as
  whoever logs on — avoiding the elevated-installer {username} ambiguity.
  No "hidden" setting is needed because the app is a windowless WinExe. }
function CreateLogonTask(): Boolean;
var
  XmlPath, Xml, ExePath: String;
  ResultCode: Integer;
begin
  ExePath := ExpandConstant('{app}\{#ExeName}');
  XmlPath := ExpandConstant('{tmp}\YTShareHostTask.xml');
  Xml :=
    '<?xml version="1.0" encoding="UTF-8"?>' + #13#10 +
    '<Task version="1.2" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task">' + #13#10 +
    '  <RegistrationInfo><Description>YTShare Host</Description></RegistrationInfo>' + #13#10 +
    '  <Triggers><LogonTrigger><Enabled>true</Enabled></LogonTrigger></Triggers>' + #13#10 +
    '  <Principals><Principal id="Author">' + #13#10 +
    '    <GroupId>S-1-5-32-545</GroupId>' + #13#10 +
    '    <RunLevel>LeastPrivilege</RunLevel>' + #13#10 +
    '  </Principal></Principals>' + #13#10 +
    '  <Settings>' + #13#10 +
    '    <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>' + #13#10 +
    '    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>' + #13#10 +
    '    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>' + #13#10 +
    '    <StartWhenAvailable>true</StartWhenAvailable>' + #13#10 +
    '    <ExecutionTimeLimit>PT0S</ExecutionTimeLimit>' + #13#10 +
    '    <Enabled>true</Enabled>' + #13#10 +
    '    <RestartOnFailure><Interval>PT1M</Interval><Count>3</Count></RestartOnFailure>' + #13#10 +
    '  </Settings>' + #13#10 +
    '  <Actions Context="Author">' + #13#10 +
    '    <Exec><Command>' + ExePath + '</Command></Exec>' + #13#10 +
    '  </Actions>' + #13#10 +
    '</Task>';
  { Write UTF-8 with BOM so schtasks parses the XML correctly }
  SaveStringToFile(XmlPath, #$EF#$BB#$BF + Xml, False);
  Result := Exec(ExpandConstant('{sys}\schtasks.exe'),
       '/Create /TN "' + TaskName + '" /XML "' + XmlPath + '" /F',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0);
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ResultCode: Integer;
begin
  if CurStep = ssPostInstall then
  begin
    { Install Bonjour silently only if it is not already present }
    if not BonjourInstalled() then
      Exec('msiexec.exe',
           '/i "' + ExpandConstant('{app}\redist\Bonjour64.msi') + '" /qn /norestart',
           '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

    Exec(ExpandConstant('{sys}\netsh.exe'),
         'advfirewall firewall delete rule name="' + FirewallRule + '"',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
         'advfirewall firewall add rule name="' + FirewallRule + '" dir=in action=allow protocol=TCP localport=' + Port,

  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
begin
  if CurUninstallStep = usUninstall then
  begin
    { Stop the running app }
    Exec(ExpandConstant('{sys}\taskkill.exe'), '/IM {#ExeName} /F',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    { Remove the scheduled task }
    Exec(ExpandConstant('{sys}\schtasks.exe'), '/Delete /TN "' + TaskName + '" /F',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    { Remove the firewall rule }
    Exec(ExpandConstant('{sys}\netsh.exe'),
         'advfirewall firewall delete rule name="' + FirewallRule + '"',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    { Optionally remove Bonjour (other apps may depend on it) }
    if MsgBox('Also remove Apple Bonjour? Other applications may depend on it.',
              mbConfirmation, MB_YESNO) = IDYES then
      Exec('msiexec.exe',
           '/x "' + ExpandConstant('{app}\redist\Bonjour64.msi') + '" /qn /norestart',
           '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  end;
end;
```

- [ ] **Step 3: Provide a placeholder MSI so the script can be compile-verified**

The real `Bonjour64.msi` is user-supplied. To verify the script *compiles* without it, create a throwaway placeholder (ISCC only needs a file to exist at that path; it packs raw bytes):

```powershell
Set-Content -Path YTShare.Host/installer/redist/Bonjour64.msi -Value "placeholder" -Encoding ascii
```

Note in your own records: **replace this placeholder with the real Apple `Bonjour64.msi` before producing a distributable installer.**

- [ ] **Step 4: Verify the script compiles**

```powershell
```

Then compile the installer (from `YTShare.Host/installer`):
```powershell
& "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe" YTShareHost.iss
```
Expected: `Successful compile` and `Output\YTShareHostSetup.exe` is produced. (If Inno Setup 6 is not installed, install it from https://jrsoftware.org/isdl.php first.)

- [ ] **Step 5: Commit**

```powershell
git add YTShare.Host/installer/YTShareHost.iss YTShare.Host/installer/redist/.gitkeep
git commit -m "feat(installer): add Inno Setup script for YTShare Host"
```

---

### Task 3: Build script and gitignore

**Files:**
- Create: `YTShare.Host/installer/build.ps1`
- Create: `YTShare.Host/installer/.gitignore`

**Interfaces:**
- Consumes: `YTShare.Server.csproj` (Task 1); `YTShareHost.iss` (Task 2).
- Produces: a one-command build that outputs `installer/Output/YTShareHostSetup.exe`.

- [ ] **Step 1: Write the gitignore**

Create `YTShare.Host/installer/.gitignore` with exactly:

```gitignore
# User-supplied Apple redistributable — never commit
redist/Bonjour64.msi
# Inno Setup compiler output
Output/
```

- [ ] **Step 2: Write the build script**

Create `YTShare.Host/installer/build.ps1` with exactly:

```powershell
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


Write-Host 'Compiling installer...'
& $iscc $iss
if ($LASTEXITCODE -ne 0) { throw "ISCC failed ($LASTEXITCODE)" }

Write-Host "Done: $(Join-Path $root 'Output\YTShareHostSetup.exe')"
```

- [ ] **Step 3: Verify the build script end-to-end**

Ensure a `Bonjour64.msi` exists in `redist/` (placeholder from Task 2 is fine for this verification). Run (from `YTShare.Host/installer`):
```powershell
.\build.ps1
```
Expected: prints the publish and compile steps and ends with `Done: ...Output\YTShareHostSetup.exe`; the file exists.

- [ ] **Step 4: Confirm the placeholder MSI is not staged**

Run:
```powershell
git status --short
```
Expected: `redist/Bonjour64.msi` and `Output/` do **not** appear (excluded by `.gitignore`). Only `build.ps1` and `.gitignore` are new.

- [ ] **Step 5: Commit**

```powershell
git add YTShare.Host/installer/build.ps1 YTShare.Host/installer/.gitignore
git commit -m "feat(installer): add build script and gitignore"
```

---

### Task 4: Landing-page download link

**Files:**
- Modify: `frontend/src/app/features/landing/landing.component.html`

**Interfaces:**
- Consumes: the installer published to GitHub Releases as `YTShareHostSetup.exe`.
- Produces: a working "Download for Windows" button. No code consumes this.

- [ ] **Step 1: Update the Windows Host download anchor**

In `frontend/src/app/features/landing/landing.component.html`, find the Windows Host card's anchor:

```html
                <a href="/downloads/YTShare.Host.zip" class="btn btn-primary btn-download">
```

Replace that opening tag with:

```html
                <a href="https://github.com/MultiTron/YTShare/releases/latest/download/YTShareHostSetup.exe" rel="noopener" class="btn btn-primary btn-download">
```

(Leave the SVG, the "Download for Windows" text, and the closing `</a>` unchanged.)

- [ ] **Step 2: Verify the production build embeds the new URL**

Run (from `frontend`):
```powershell
npm run build
```
Expected: build succeeds.

Then confirm the URL is present in the built output:
```powershell
Select-String -Path dist\ytshare-frontend\browser\*.html -Pattern 'releases/latest/download/YTShareHostSetup.exe'
```
Expected: at least one match. (The landing template is inlined into the build output.) If no match appears in `*.html`, also check the JS chunks: `Select-String -Path dist\ytshare-frontend\browser\*.js -Pattern 'YTShareHostSetup.exe'`.

- [ ] **Step 3: Confirm the dead link is gone**

Run (from `frontend`):
```powershell
Select-String -Path src\app\features\landing\landing.component.html -Pattern 'YTShare.Host.zip'
```
Expected: **no** matches.

- [ ] **Step 4: Commit**

```powershell
git add frontend/src/app/features/landing/landing.component.html
git commit -m "feat(frontend): link Windows Host download to GitHub release"
```

---

## Release process (manual, performed after implementation)

This is operational, not a code task — documented here for completeness:

1. Place the real Apple `Bonjour64.msi` in `YTShare.Host/installer/redist/`.
2. Run `YTShare.Host/installer/build.ps1` to produce `Output/YTShareHostSetup.exe`.
3. Create a GitHub Release on `MultiTron/YTShare` (e.g. tag `host-v1.0.0`).
4. Upload the `.exe` as a release asset named **exactly** `YTShareHostSetup.exe`.

The landing page's "latest" permalink resolves the moment that release is published.

## Manual smoke test (after first real release)

On a clean win-x64 machine (no .NET, no Bonjour), with the real-MSI installer:
1. Run `YTShareHostSetup.exe`, accept the UAC prompt, finish the wizard.
2. Confirm **no console window** appears and `YTShare.Server.exe` is running (Task Manager).
3. From another LAN device, hit `http://<host>:7296/Share?link=youtube.com/watch?v=...` and confirm the default browser opens on the host's desktop.
4. Confirm an `_http._tcp.` service named `YTShareService` is discoverable (e.g. a Bonjour browser).
5. Log off and back on; confirm the app auto-starts (Scheduled Task `YTShare Host`).
6. Re-run the installer; confirm no duplicate firewall rule (`netsh advfirewall firewall show rule name="YTShare Host"`) or task.
7. Uninstall; confirm app, task, and firewall rule are removed; choose "No" at the Bonjour prompt and confirm Bonjour remains.
