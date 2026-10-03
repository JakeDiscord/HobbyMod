# Run from a downloaded copy or from tools/windows inside the checkout.
[CmdletBinding()]
param(
    [ValidateSet('Setup', 'Pull', 'Push')]
    [string]$Mode = 'Setup',
    [string]$Message
)

$ErrorActionPreference = 'Stop'

# Check the real local process identity before touching directories or Git.
if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    Write-Host 'Local setup skipped: this script only runs on Windows. The cloud project is unaffected.'
    exit 0
}
$account = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name.Split('\')[-1]
$profile = [Environment]::GetFolderPath([Environment+SpecialFolder]::UserProfile)
if ($account -ine 'Jacob' -or [Environment]::UserName -ine 'Jacob' -or
    $profile.TrimEnd('\') -ine 'C:\Users\Jacob') {
    Write-Host 'Local setup skipped: the Windows account and profile must belong to Jacob. The cloud project is unaffected.'
    exit 0
}

$target = 'C:\Users\Jacob\Downloads\HobbyMod'
$repository = 'https://github.com/JakeDiscord/HobbyMod.git'
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw 'Install Git for Windows, then run this script again.'
}

function Invoke-ProjectGit {
    param([string[]]$GitArguments)
    & git -C $target @GitArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Git operation failed (exit $LASTEXITCODE). Existing files were preserved; resolve the reported error before retrying."
    }
}

# Never follow an existing directory junction or symlink to another location.
foreach ($path in @('C:\Users', 'C:\Users\Jacob', 'C:\Users\Jacob\Downloads', $target)) {
    if (Test-Path -LiteralPath $path) {
        $entry = Get-Item -LiteralPath $path -Force
        if (-not $entry.PSIsContainer -or
            ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "Refusing to use a non-directory or redirected path: $path"
        }
    }
}

if (-not (Test-Path -LiteralPath (Join-Path $target '.git'))) {
    if ($Mode -ne 'Setup') {
        throw 'Run this script with -Mode Setup first.'
    }
    if ((Test-Path -LiteralPath $target) -and
        @(Get-ChildItem -LiteralPath $target -Force).Count -gt 0) {
        throw "The target folder contains existing files and is not a Git checkout. Nothing was overwritten: $target"
    }
    if (-not (Test-Path -LiteralPath 'C:\Users\Jacob\Downloads')) {
        New-Item -ItemType Directory -Path 'C:\Users\Jacob\Downloads' | Out-Null
    }
    & git clone --branch main -- $repository $target
    if ($LASTEXITCODE -ne 0) {
        throw 'Clone failed. If the repository is private, sign in through Git Credential Manager and retry. No token belongs in this script.'
    }
}

# Confirm this is the intended root and remote before pulling or publishing.
$root = (Invoke-ProjectGit -GitArguments @('rev-parse', '--show-toplevel')).Trim().Replace('/', '\').TrimEnd('\')
if ($root -ine $target) { throw 'The target is not the expected repository root.' }
$origin = (Invoke-ProjectGit -GitArguments @('remote', 'get-url', 'origin')).Trim()
if ($origin -notmatch '^(https://github\.com/JakeDiscord/HobbyMod(?:\.git)?/?|git@github\.com:JakeDiscord/HobbyMod(?:\.git)?)$') {
    throw 'The checkout has a different origin. Its configuration was left unchanged.'
}
$branch = (Invoke-ProjectGit -GitArguments @('branch', '--show-current')).Trim()
if ($branch -ne 'main') { throw 'Switch to main before syncing. The current branch was preserved.' }
$changes = @(Invoke-ProjectGit -GitArguments @('status', '--porcelain=v1', '--untracked-files=all'))

if ($Mode -eq 'Push') {
    if ($changes.Count -gt 0) {
        if ([string]::IsNullOrWhiteSpace($Message)) {
            throw 'To publish local edits, provide a commit message: -Mode Push -Message "Describe your changes"'
        }
        foreach ($field in @('user.name', 'user.email')) {
            & git -C $target config --get $field | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "Configure your Git $field before committing local edits." }
        }
        Invoke-ProjectGit -GitArguments @('add', '--all')
        Invoke-ProjectGit -GitArguments @('commit', '-m', $Message)
    }
    # A normal push rejects conflicting cloud changes; never force or reset.
    Invoke-ProjectGit -GitArguments @('push', 'origin', 'main')
} else {
    if ($changes.Count -gt 0) {
        throw 'Local edits are present. Commit/push them or resolve them before pulling. Nothing was overwritten.'
    }
    Invoke-ProjectGit -GitArguments @('pull', '--ff-only', 'origin', 'main')
}

Write-Host "HobbyMod is available at $target"
Write-Host 'Use -Mode Pull after cloud changes; use -Mode Push -Message "..." to publish local changes.'
Write-Host 'Open that folder in your local Codex app. Building locally requires a Java 21 JDK.'
