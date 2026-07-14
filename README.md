# JLT Ores

Balanced automatic ore-vein mining for Minecraft Java Edition (Fabric), built
on the shared `mining-engine` library.

The core entrypoint, vein matching, balance planning, and scheduled bound-tool
harvesting are implemented. See `DESIGN.md` and the master checklist in
`../mining-engine/TODO.md` for the remaining release work.

- Mod id: `jlt_ores`
- Entrypoint: `media.jlt.minecraft.mods.ores.OresMod`
- Minecraft/Fabric target: Minecraft 26.2, Fabric Loader 0.19.3

Supported families: coal, copper, iron, gold, redstone, lapis, diamond,
emerald, Nether quartz, Nether gold, and ancient debris. Stone and deepslate
variants can be treated as one family through vanilla ore tags.

The server configuration is stored at `config/jlt_ores.yaml`, grouped by theme
(`gates`, `timing`, `economy.hunger`, `economy.xp`, `economy.durability`,
`economy.substitution`, `scanBounds`, `toolTiers`) with each setting's
description as a `#` comment above it. A complete default is checked in at
`config/jlt_ores.example.yaml`. A pre-existing `config/jlt_ores.json` from an
older version is migrated automatically on first launch — its values carry
over unchanged, and it's kept alongside as `jlt_ores.json.bak`
(`config/jlt_ores.example.json` is kept for reference as that legacy shape).

## Build

Publish the sibling engine to Maven Local before building from a fresh checkout:

```bash
../mining-engine/gradlew -p ../mining-engine publishToMavenLocal
./gradlew build
```
