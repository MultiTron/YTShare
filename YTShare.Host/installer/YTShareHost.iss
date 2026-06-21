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

    { Firewall: delete any stale rule, then add (idempotent). Ignore delete failures. }
    Exec(ExpandConstant('{sys}\netsh.exe'),
         'advfirewall firewall delete rule name="' + FirewallRule + '"',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec(ExpandConstant('{sys}\netsh.exe'),
         'advfirewall firewall add rule name="' + FirewallRule + '" dir=in action=allow protocol=TCP localport=' + Port,
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

    { Logon Scheduled Task for the original (non-elevated) user, least privilege, overwrite if present }
    Exec(ExpandConstant('{sys}\schtasks.exe'),
         '/Create /TN "' + TaskName + '" /TR "\"' + ExpandConstant('{app}\{#ExeName}') + '\"" /SC ONLOGON /RU "' + ExpandConstant('{username}') + '" /RL LIMITED /F',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
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
