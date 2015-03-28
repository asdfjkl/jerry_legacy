; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{E7E68CDA-C809-4440-AD45-51CB6CF7F7E7}
AppName=Jerry
AppVersion=1.01
;AppVerName=Jerry 1.0 (beta)
AppPublisher=Dominik Klein
AppPublisherURL=https://github.com/asdfjkl/jerry
AppSupportURL=https://github.com/asdfjkl/jerry
AppUpdatesURL=https://github.com/asdfjkl/jerry
DefaultDirName={pf}\Jerry
DefaultGroupName=Jerry
OutputBaseFilename=setup
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "C:\Users\user\Documents\jerry\dist\jerry.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\_bz2.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\_ctypes.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\_hashlib.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\_lzma.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\_socket.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\_ssl.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\_win32sysloader.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\jerry.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\library.zip"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\pyexpat.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\PyQt4.QtCore.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\PyQt4.QtGui.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\PyQt4.QtSvg.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\python34.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\pythoncom34.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\pywintypes34.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\QtCore4.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\QtGui4.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\QtSvg4.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\select.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\sip.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\unicodedata.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\win32api.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\win32com.shell.shell.pyd"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\Documents\jerry\dist\engine\*"; DestDir: "{app}\engine"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\Users\user\Documents\jerry\dist\res\*"; DestDir: "{app}\res"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\Users\user\Documents\jerry\dist\books\*"; DestDir: "{app}\books"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\Users\user\Documents\jerry\dist\Microsoft.VC90.CRT\*"; DestDir: "{app}\Microsoft.VC90.CRT"; Flags: ignoreversion recursesubdirs createallsubdirs

; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\Jerry"; Filename: "{app}\jerry.exe"
Name: "{commondesktop}\Jerry"; Filename: "{app}\jerry.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\jerry.exe"; Description: "{cm:LaunchProgram,Jerry}"; Flags: nowait postinstall skipifsilent

