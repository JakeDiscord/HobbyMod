# HobbyMod

A Minecraft **1.21.1 / NeoForge** mod combining marble sculpture, astronomy,
vineries, painting, cameras, woodworking, and other creative hobbies.

## Marble sculpting

- Craft four marble blocks from two calcite and two quartz in a checkerboard.
- Craft a chisel with an iron ingot directly above a stick.
- Place marble and right-click it with the chisel to carve a small human statue.
  The statue faces you. Each sculpture uses one of the chisel's 128 durability;
  creative mode preserves the tool.
- Alternatively, turn a marble block into a statue with a stonecutter.
- Mine marble and statues with a pickaxe to recover them. Both appear alongside
  the chisel in the HobbyMod creative tab.

The first models reuse vanilla calcite, quartz, iron, and wood textures.
Marble is crafted for now; natural deposits and additional statue designs are
future work. Astronomy (a telescope and sky observation) is the next planned hobby.

## Development

For Jacob's local Windows checkout and GitHub synchronization, see
[Windows setup](tools/windows/README.md). Other Windows accounts must skip local
setup; cloud work remains available. This restriction is also recorded in
`AGENTS.md` for future Codex tasks.

Install a **Java 21 JDK** (a runtime alone is insufficient), then use the included
Gradle wrapper from the repository root:

In this prepared Codex cloud environment, first run
`source /workspace/.hobbymod-tools/activate.sh` to select the JDK and configure
Java's network proxy and trusted system certificates.

```sh
./gradlew build
./gradlew :neoforge:runClient
./gradlew :neoforge:runServer
```

The NeoForge development launcher starts a headless server in development mode.
A client requires a graphical display. Headless cloud tasks should build first
and use the development server for functional validation. A standalone production
server requires accepting Minecraft's EULA; read https://aka.ms/MinecraftEULA
before setting `eula=true` in that server's local file.
Look for `HobbyMod initialized on NeoForge` and the server's `Done` message.
Generated worlds, logs, and EULA files stay in ignored run directories.
The development server uses `neoforge/run/server.properties`; this cloud instance
binds it to `127.0.0.1`. Use an interactive terminal and enter `stop` to shut down.
Architectury's development transformer currently leaves worker threads alive
after Minecraft stops. Once the log reports `All dimensions are saved`, terminate
the remaining Java process running `dev.architectury.transformer.TransformerRuntime`
that belongs to that launch (SIGTERM), then exit Gradle if needed. Ctrl+C alone
can leave that child process behind. This affects the development launcher,
not the mod's initialization or release jar.

Distribute `neoforge/build/libs/hobbymod-neoforge-0.1.0.jar`, together with the
Minecraft 1.21.1 NeoForge edition of **Architectury API 13.0.8**. The `dev` and
`dev-shadow` jars are development artifacts, not installable releases.
No credentials are required to resolve public dependencies.

## Portable design

- `common`: gameplay, assets, data, and Architectury API usage.
- `neoforge`: NeoForge entry point and any platform-specific integrations.

Prefer Architectury's registry, event, and networking abstractions in shared
code. Keep client-only rendering separate from dedicated-server initialization.
Add feature packages under the common namespace as each hobby is developed;
keep stable IDs under `hobbymod` and group assets/data by feature.

A future Fabric port should add a Fabric module and entry point calling
`HobbyMod.init()`, a Fabric Architectury runtime dependency, Fabric metadata,
and `fabric` to the common platform list. Cross-version ports still need updates
to Minecraft APIs, mappings, dependencies, and data formats. Architectury does
not make those changes automatic.

Minecraft, NeoForge, Architectury API, and Gradle versions are centralized in
`gradle.properties` and the wrapper. The Architectury and Loom build plugins
use concrete versions rather than snapshot versions.

## Integration tests

Run `./gradlew --no-daemon -Pgametest :neoforge:runServer` in an interactive
terminal. Once the server is ready, enter `test runall`. The five GameTests cover
survival carving and facing, creative durability, unrelated stone, the chisel's
last use, and already-carved statues. In a headless run, check for five distinct
`SCULPTING_TEST_PASS` entries in the server log (the vanilla summary goes to
in-game players). Then shut down as described above. Tests use the ignored development world;
use a disposable world when running them.

Tests and their structure template live in `neoforge/src/gametest` and are
included only with `-Pgametest`. Run a normal `./gradlew build` to produce the
release jar without test classes or test structures. Gradle's ordinary `test`
task does not run these integration tests. Visual client play-testing remains
separate from the dedicated-server tests.
