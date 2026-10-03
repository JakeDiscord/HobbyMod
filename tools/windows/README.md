# Jacob's Windows checkout

The cloud task cannot directly write to your computer. This helper creates and
updates the local checkout when you run it on that computer. It checks the real
Windows account and profile first and skips all local work for other users.

Install Git for Windows. Download `Sync-HobbyMod.ps1`, open PowerShell in the
folder containing that downloaded script, and run:

```powershell
.\Sync-HobbyMod.ps1 -Mode Setup
```

If your PowerShell policy blocks the downloaded script, review its contents
before allowing it under your computer's script policy. A private repository
also requires signing into GitHub through Git Credential Manager.

The checkout goes to `C:\Users\Jacob\Downloads\HobbyMod`. Existing unrelated
files, redirected directories, local changes, other branches, and conflicting
history cause the helper to stop rather than overwrite work.

Open this folder in the local Codex app and install a Java 21 **JDK** to build.
From the project root, use `./gradlew.bat build` or
`./gradlew.bat :neoforge:runClient`.

Sync cloud changes down to the PC:

```powershell
.\tools\windows\Sync-HobbyMod.ps1 -Mode Pull
```

Publish local changes for the cloud to pick up:

```powershell
.\tools\windows\Sync-HobbyMod.ps1 -Mode Push -Message "Describe your changes"
```

Push commits all current local project edits, so inspect `git status` and test
them first. Git author name and email must already be configured. Cloud tasks
pick up published changes through the same repository. Synchronization happens
when these commands run; there is no background file watcher. Keep each copy
synced before editing to minimize conflicts. Worlds, caches, processes, and
local runtime settings stay on their own machines.
