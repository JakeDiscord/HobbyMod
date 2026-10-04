# HobbyMod workspace instructions

## Local Windows account restriction

Jacob requested that local development use only
`C:\Users\Jacob\Downloads\HobbyMod`, and only when the actual Windows user is
`Jacob` with profile `C:\Users\Jacob`.

Before cloning, editing, building, or syncing this project on Windows, check the
real current Windows identity and profile. If either differs, do not create or
modify the local project or produce local build outputs. Report that local work
was skipped. Cloud development may continue in `/workspace/HobbyMod` regardless
of the cloud machine's account name. Do not create a Git worktree unless asked.

## Publication

Keep changes local. Do not commit, push, merge, or publish to GitHub unless the user explicitly asks.

## Development and synchronization

Use `JakeDiscord/HobbyMod` on GitHub as the shared transport between the cloud
and Jacob's PC. The guarded Windows helper is
`tools/windows/Sync-HobbyMod.ps1`; its Setup/Pull/Push modes preserve local work
and reject conflicting history. Do not force-push, reset, or overwrite existing
files to make synchronization succeed. Check Git status before syncing.
Pull clean checkouts with fast-forward-only updates; surface conflicts for
resolution. Sync transfers committed source and assets, not running processes,
ignored Minecraft worlds, Gradle caches, or machine-specific configuration.

Keep portable gameplay in `common`, using Architectury APIs where appropriate.
Keep NeoForge-specific integrations in `neoforge`. Use Java 21 and the Gradle
wrapper. In cloud tasks, source `/workspace/.hobbymod-tools/activate.sh` first.
Read README.md, docs/POTTERY.md and docs/PAINTING.md for build commands, fourteen
sculpting, nine pottery, two blueprint, nine aquarium, nine terrarium, six DJ, ten Winery, four astronomy and six painting
GameTests, shared geometry/raster tests, and safe test-server shutdown. Preserve existing changes and keep development tests out of release
jars. Do not describe client visuals as tested without a graphical play-test.
