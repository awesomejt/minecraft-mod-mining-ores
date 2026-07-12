# JLT Ores

Balanced automatic ore-vein mining for Minecraft Java Edition (Fabric), built
on the shared `mining-engine` library.

Implementation is in progress; see `DESIGN.md` and the master checklist in
`../mining-engine/TODO.md`.

## Build

Publish the sibling engine to Maven Local before building from a fresh checkout:

```bash
../mining-engine/gradlew -p ../mining-engine publishToMavenLocal
./gradlew build
```
