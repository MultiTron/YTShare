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

    { Firewall: delete any stale rule (ignore failure), then add. Warn if the add fails. }
    Exec(ExpandConstant('{sys}\netsh.exe'),
         'advfirewall firewall delete rule name="' + FirewallRule + '"',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    if not (Exec(ExpandConstant('{sys}\netsh.exe'),
         'advfirewall firewall add rule name="' + FirewallRule + '" dir=in action=allow protocol=TCP localport=' + Port,
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0)) then
      SuppressibleMsgBox('Warning: could not add the firewall rule for port ' + Port +
        '. Other devices may not be able to reach YTShare Host until you allow it manually.',
        mbError, MB_OK, IDOK);

    { Register the logon Scheduled Task. Warn if registration fails. }
    if not CreateLogonTask() then
      SuppressibleMsgBox('Warning: could not register the "' + TaskName +
        '" startup task. YTShare Host will not auto-start at logon until you add it manually.',
        mbError, MB_OK, IDOK);
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
