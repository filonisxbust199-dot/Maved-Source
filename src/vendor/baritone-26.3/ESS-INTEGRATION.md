# ESS integration

Source: the supplied `baritone-26.3.zip`, Baritone 1.20.0 for Minecraft 26.3.
The upstream source and license are retained in this directory.

The root Gradle project uses `ess-integration.gradle` instead of the upstream
multi-loader build. It builds the Fabric mod and embeds it, including
nether-pathfinder, in the ESS jar. Schematica API compile stubs are excluded
from the binary. `:baritone:sourcesJar` produces the corresponding sources.

Local lifecycle patch: `MixinMinecraft.close` stops Baritone's background pool,
waits for cache workers to exit, and saves cached worlds. Interrupted workers
preserve the interrupt flag. This prevents Minecraft 26.3's shutdown watchdog
from crashing the client because the upstream persistent workers keep the JVM alive.

Validation: upstream JUnit tests and the opt-in `ess.autoshot.blockesp` check,
which exercises commands, block scanning, rendering, and an actual Baritone path
in the separate `run-shots` world.
