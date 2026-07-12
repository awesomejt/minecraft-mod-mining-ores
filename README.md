# JLT Ores

Balanced automatic ore-vein mining for Minecraft Java Edition (Fabric), built
on the shared `mining-engine` library.

Implementation is in progress; see `DESIGN.md` and the master checklist in
`../mining-engine/TODO.md`.

- Mod id: `jlt_ores`
- Entrypoint: `media.jlt.minecraft.mods.ores.OresMod`
- Minecraft/Fabric target: Minecraft 26.2, Fabric Loader 0.19.3

## Build

Publish the sibling engine to Maven Local before building from a fresh checkout:

```bash
../mining-engine/gradlew -p ../mining-engine publishToMavenLocal
./gradlew build
```
