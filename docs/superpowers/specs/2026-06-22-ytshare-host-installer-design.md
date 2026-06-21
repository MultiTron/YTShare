# YTShare.Host Installer — Design

**Date:** 2026-06-22
**Status:** Approved (design); pending implementation plan
**Components:**
- `YTShare.Host/YTShare.Server` (ASP.NET Core minimal API, .NET 8) — the installer
- `frontend/` (Angular static app on Render) — the landing-page download link

## Goal

Produce a single double-clickable installer that sets up YTShare.Host on a
Windows machine with zero manual steps: it installs every redistributable the
app needs, runs the app with no visible terminal window, and makes it start
automatically. Uninstalling cleanly reverses everything the installer added.

Additionally, make the installer downloadable from the Angular landing page's
existing "Windows Host" download button.

## Context

`YTShare.Server` is an ASP.NET Core minimal API (.NET 8) that:

- Listens on `http://0.0.0.0:7296`.
- Exposes `GET /Share?link=...`, which calls `Process.Start` to open the default
  browser at the supplied link.
- Registers an mDNS/Bonjour service (`_http._tcp.`, name `YTShareService`,
  port 7296) via the Apple `DNSSD` COM component so other LAN devices can
  discover it.
- Already calls `builder.Host.UseWindowsService()`.

The browser-opening behavior is the deciding constraint: Windows Services run in
session 0, isolated from the logged-in user's desktop, so a browser launched
from a service would not appear for the user. The app must therefore run inside
the user's own session.

## Decisions (resolved during brainstorming)

1. **.NET runtime:** publish **self-contained** (bundle the runtime) — no .NET
   install step, no version-mismatch failures.
2. **Bonjour:** **bundle `Bonjour64.msi`** and silently install it if absent.
3. **Run model:** **per-user startup app, no window** — launched at user logon
   in the user's session (not a session-0 Windows Service), so the browser opens
   correctly.
4. **Installer technology:** **Inno Setup** — single self-contained `.exe`.

### Out of scope / assumptions

- **Code signing** is out of scope (no certificate available); SmartScreen may
  warn on first run.
- The user **supplies `Bonjour64.msi`** (from Apple's Bonjour SDK for Windows /
  Bonjour Print Services) and places it in `installer/redist/`. It cannot be
  downloaded as part of this work.
- Target architecture is **win-x64**.

## Architecture

A single Inno Setup `.exe` runs **elevated** (admin needed to install Bonjour,
add a firewall rule, and write to Program Files). The installed app itself runs
**per-user, non-elevated** inside the logged-in user's session.

### Repository layout (additions only — nothing existing is moved)

```
YTShare.Host/
  installer/
    YTShareHost.iss          # Inno Setup script
    redist/Bonjour64.msi     # Apple redistributable (user-supplied, gitignored)
    build.ps1                # publishes app + compiles installer
```

### Components

| Component | Purpose | Depends on |
| --- | --- | --- |
| `YTShareHost.iss` | Declares files, install/uninstall steps, elevation, UI; generates uninstaller | Inno Setup compiler (`ISCC.exe`); published app output; `Bonjour64.msi` |
| `build.ps1` | One command: `dotnet publish` (self-contained single-file win-x64) then run `ISCC.exe` | .NET SDK, Inno Setup |
| Published app | Self-contained single-file `YTShare.Server.exe` + `appsettings.json` | none at runtime (runtime bundled); Bonjour service for discovery |

## Code change to the host

One change, to make the terminal invisible:

- `YTShare.Server.csproj`: add `<OutputType>WinExe</OutputType>` so no console
  window is allocated. Kestrel runs normally without a console. Console logging
  output is no longer visible (acceptable; the app is meant to run unattended).
- `UseWindowsService()` is **left unchanged** — it no-ops when the process is not
  started by the Service Control Manager, so it is harmless when run as a
  per-user app. No further code change is required.

## Install flow

1. Request elevation (UAC prompt). Steps 3–5 require admin.
2. Copy the self-contained app to `C:\Program Files\YTShare Host\`.
3. **Bonjour:** detect existing install (presence of the `Bonjour Service`
   Windows service / its registry key). If absent, run
   `msiexec /i "Bonjour64.msi" /qn /norestart` silently.
4. **Firewall:** add an inbound allow rule for TCP port **7296**
   (`netsh advfirewall firewall add rule name="YTShare Host" dir=in
   action=allow protocol=TCP localport=7296`). Remove any pre-existing rule of
   the same name first to stay idempotent.
5. **Startup:** register a **logon Scheduled Task** named `YTShare Host` from a
   generated **XML task definition** (`schtasks /Create /XML ... /F`). The task
   triggers at user logon and runs `YTShare.Server.exe` in the interactive
   user's own session with **restart-on-failure** (retry a few times at a short
   interval). To run as whoever logs on — and to avoid the ambiguity of an
   elevated installer's `{username}` resolving to a different admin account —
   the task principal targets the built-in **Users** group
   (SID `S-1-5-32-545`) with least-privilege run level. (Chosen over a `Run`
   registry key because it survives crashes via restart-on-failure. The app is
   already a windowless `WinExe`, so no separate "hidden window" task setting is
   needed.) If task registration fails, the installer surfaces a warning rather
   than failing silently.
6. Offer a **"Launch now"** finish-page checkbox so the app starts immediately
   without requiring a logoff/logon cycle.

## Uninstall flow

1. Stop the running app (end the task and any running `YTShare.Server.exe`).
2. Delete the `YTShare Host` scheduled task.
3. Remove the `YTShare Host` firewall rule.
4. Delete `C:\Program Files\YTShare Host\`.
5. **Leave Bonjour installed** (other apps may depend on it), with an optional
   checkbox to remove it via `msiexec /x`.

## Error handling

- Bonjour MSI install failure: warn but continue — the HTTP endpoint still works;
  only LAN discovery is degraded.
- Firewall or scheduled-task failure: surface a clear message rather than failing
  silently; do not roll back the whole install for these.
- Re-running the installer is **idempotent**: it upgrades the app in place and
  replaces (does not duplicate) the firewall rule and scheduled task.
- All steps write to the Inno Setup log for diagnosis.

## Build

`build.ps1`:

1. Publish via **Visual Studio MSBuild** (located through `vswhere`), NOT the
   `dotnet` CLI: the project's Bonjour `COMReference` cannot be resolved by
   `dotnet build`/`dotnet publish` (error MSB4803 — `ResolveComReference`
   requires .NET Framework MSBuild). Command:
   `MSBuild.exe YTShare.Server.csproj /t:Restore,Publish /p:Configuration=Release
   /p:RuntimeIdentifier=win-x64 /p:SelfContained=true /p:PublishSingleFile=true`
   → produces the windowless self-contained single-file app (~89 MB exe).
2. Invoke `ISCC.exe YTShareHost.iss` → produces `YTShareHostSetup.exe`.

> **Build prerequisite:** Visual Studio with MSBuild + the .NET 8 targeting
> pack must be installed on the build machine (the `dotnet` SDK alone is
> insufficient because of the COM reference).

## Landing-page download link

The Angular landing page (`frontend/`, a static app served by nginx in a Docker
container on Render) already renders a "Windows Host" download card, but its
button points at a dead relative path (`/downloads/YTShare.Host.zip`). Wire it to
the installer.

### Hosting: GitHub Releases

The ~80MB self-contained installer must **not** enter the git repo or the Render
Docker image. It is published as a **GitHub Release asset** on the public repo
`MultiTron/YTShare` (confirmed public, so anonymous download links work — no
auth required).

### Download URL

Use GitHub's **"latest" permalink** rather than a version-pinned URL:

```
https://github.com/MultiTron/YTShare/releases/latest/download/YTShareHostSetup.exe
```

GitHub redirects this to the most recent published (non-prerelease) release that
has an asset named `YTShareHostSetup.exe`, and serves it with
`Content-Disposition: attachment` (browser downloads rather than navigates).
Benefit: the landing page is edited once and never again — each release just
needs an asset with that exact filename.

### Code change (one file)

`frontend/src/app/features/landing/landing.component.html` — the Windows Host
card's anchor (currently the `href="/downloads/YTShare.Host.zip"` link):

- `href` → the GitHub latest-download URL above.
- Add `rel="noopener"`.
- Button label stays "Download for Windows".

No nginx, Dockerfile, backend, or Angular asset-pipeline changes. The Android
card is left untouched (out of scope).

### Release process (manual, documented)

Per version:

1. Build `YTShareHostSetup.exe` via the installer's `build.ps1`.
2. Create a GitHub Release (e.g. tag `host-v1.0.0`) on `MultiTron/YTShare`.
3. Upload the `.exe` as an asset named **exactly** `YTShareHostSetup.exe`.

Automating this with a GitHub Actions workflow is out of scope for now (YAGNI).

### Edge cases

- **No release yet:** the latest-download URL 404s until the first release is
  published. Acceptable — the button works the moment a release exists.
- **Filename drift:** an asset not named exactly `YTShareHostSetup.exe` breaks
  the link — hence the explicit naming rule above.

## Success criteria

- Double-clicking the installer on a clean win-x64 machine (no .NET, no Bonjour)
  results in: the app running with no visible window, reachable at
  `http://<host>:7296/Share?link=...`, discoverable via mDNS, and auto-starting
  at the next logon.
- No console window is ever shown.
- Uninstall removes the app, task, and firewall rule (Bonjour optional).
- Re-running the installer over an existing install does not create duplicate
  firewall rules or tasks.
- The landing page's "Download for Windows" button links to the GitHub
  latest-download URL and downloads the installer once a release is published.
- `npm run build` succeeds with the updated landing page.
