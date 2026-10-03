# Running and inspecting Minecraft in Codex cloud

The GitHub repository supplies source, not another chat's installed tools,
dependency cache, display server or Minecraft worlds. Each cloud environment
must be prepared separately. Compilation alone does not verify client visuals.

## Toolchain and dependencies

Read `AGENTS.md` and the available cloud environment skills first. Inspect the
actual network policy and build failure before changing configuration.

This prepared workspace uses:

```bash
cd /workspace/HobbyMod
source /workspace/.hobbymod-tools/activate.sh
java -version
./gradlew --no-daemon build
```

`activate.sh` is environment-local and is not in Git. It selects Java 21,
`GRADLE_USER_HOME=/workspace/.hobbymod-tools/gradle`, and configures Java to use
the inherited HTTP/HTTPS proxy and trusted system certificates. If that file
does not exist, install/select a Linux Java 21 JDK through the environment's
supported setup workflow and use the repository's Gradle wrapper (8.10.2).

Java/Gradle may require explicit `http.proxyHost`, `http.proxyPort`,
`https.proxyHost`, and `https.proxyPort` JVM properties even when HTTP_PROXY and
HTTPS_PROXY are present. Derive these from the configured proxy; preserve its
credentials and CA trust, and exempt localhost from proxying. Do not disable
TLS verification or bypass a denied destination.

Gradle resolves dependencies automatically when the required destinations are
reachable. Repository/build/asset hosts include:

- `maven.architectury.dev`
- `maven.fabricmc.net`
- `maven.neoforged.net`
- `plugins.gradle.org` and `plugins-artifacts.gradle.org`
- `repo.maven.apache.org`
- `services.gradle.org` and `downloads.gradle.org`
- `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`
- `resources.download.minecraft.net`

The exact failed URL and redirect targets in the build log determine any
additional hosts needed. Request policy changes through the supported cloud
configuration workflow. Repeated retries cannot fix an explicit policy denial.

A compatible, authorized Linux Gradle cache can also supply dependencies.
An arbitrary Windows cache is not a complete Linux client setup: native
libraries, transformed artifacts and absolute paths can differ. Do not upload
credentials or commit caches to Git. Use `--offline` only after confirming the
needed dependencies and Minecraft assets are already cached.

## Virtual display and client

Provide Xvfb, Mesa software OpenGL, an X11 input tool such as xdotool, and a
screenshot tool such as ImageMagick through the environment setup. Inspect
existing displays before starting a new one; reuse a working display when possible.
For a free display number 99:

```bash
Xvfb :99 -screen 0 1280x800x24 > /tmp/hobbymod-xvfb.log 2>&1 &
export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
./gradlew --no-daemon :neoforge:runClient
```

Run the client in a persistent terminal/session and wait for the title screen.
Use actual mouse/keyboard input to enter a disposable world or local development
server. If server GameTests are also running, launch both with `-Pgametest` and
keep that flag on intervening Gradle commands. See README for server setup.

Capture the display:

```bash
DISPLAY=:99 import -window root /tmp/hobbymod-client.png
```

Then **open the PNG with Codex's image viewing tool**. Saving a screenshot without
looking at it is not visual verification. Inspect the GUI, in-world models,
transparency, proportions and interaction feedback. Test the actual inventory
and gameplay operations, and repeat screenshot review after relevant fixes.
Check at multiple window sizes/GUI scales. Use screenshots of the game itself
for evidence; generated images do not prove the mod works.

Quit the client through its menu. Stop a test server with `stop` and wait for
`All dimensions are saved`; follow README's Architectury shutdown instructions
if its JVM remains alive. Do not kill unrelated displays or Java processes.

## Aquarium reference

`docs/AQUARIUMS.md` includes real client screenshots from the aquarium overhaul.
Those provide a comparison, but another chat must still play-test its own
changes. Report build, gameplay, and visual verification separately. If a client
cannot run, state the exact blocker and do not describe visuals as verified.

## Prepared cloud session

The October 2026 aquarium session successfully used Java 21, cached Gradle
dependencies and all 3,911 Minecraft asset records, Xvfb, Mesa llvmpipe,
xdotool input and ImageMagick screenshots. Required downloads succeeded in
that environment; inspect policy again if a new environment blocks them.

For the tested launch, create a private runtime directory and use:

```bash
mkdir -p /tmp/hobbymod-runtime
chmod 700 /tmp/hobbymod-runtime
export DISPLAY=:99
export XDG_RUNTIME_DIR=/tmp/hobbymod-runtime
export LIBGL_ALWAYS_SOFTWARE=1
export ALSOFT_DRIVERS=null
./gradlew --no-daemon :neoforge:runClient
```

`ALSOFT_DRIVERS=null` permits a headless session without an audio device;
this does not verify sound. Environment-local tools and caches still need to
be installed or restored in another environment. The attempt to update the
managed environment's saved start instructions was rejected because its draft
was no longer editable; these instructions preserve the verified launch steps.
