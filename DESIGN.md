# jlt_ores — Design

Balanced automatic ore-vein mining, the pickaxe sibling of `jlt_trees`
(`../trees`). All balance mechanics come from `mining-engine`
(`../mining-engine/DESIGN.md`); this document specifies only what is
ore-specific. Execution checklist: `../mining-engine/TODO.md` Phase 6.
When in doubt about structure or style, mirror the trees repo — it is the
template (same MC 26.2 / loader 0.19.3 / fabric-api 0.154.2+26.2 / Java 25 /
Gradle 9.5.1 toolchain).

## 1. Identity

- Repo: `minecraft-mod-mining-ores`; `rootProject.name`/`archivesName`
  `mod-mining-ores` (matches repo name, like trees' `mod-trees`).
- Mod id **`jlt_ores`** (deliberately short, matching `jlt_trees`; not
  `jlt_mining_ores`). Config `config/jlt_ores.json`, command `/jlt_ores reload`,
  keys `message.jlt_ores.*`.
- Package `media.jlt.minecraft.mods.ores`, entrypoint `OresMod`,
  `maven_group media.jlt.minecraft.mods`, `mod_version 0.1.0`.
- Depends on `media.jlt.minecraft:mining-engine:0.2.0` (`implementation` +
  `include`, `mavenLocal()`).

## 2. Gameplay summary

Break one ore block (sneaking, with a pickaxe of sufficient tier) → the whole
connected vein is mined for you, at a cost: tool durability (with a multiplier),
hunger/XP tax per block, efficiency-gated pacing, durability-floor protection,
optional XP-for-durability substitution. Identical knobs and failure modes to
trees — pause on hunger, resume on eating, cancel on death/distance/tool loss.
Fortune/Silk Touch and ore XP drops work untouched because breaking goes
through vanilla `destroyBlock` with the bound tool (same slot-swap technique
as trees).

Differences from trees, by design:
- Single phase: no leaf analog, no natural-tree check, no `AdjacentBlockCollector`.
- Vein scan has **no depth limit** (`ScanLimits.maxDepthBelowOrigin` empty) —
  veins legitimately run downward; the horizontal radius and block cap bound it.
- Tier gate is per ore family (not per shape classification).

## 3. OreFamily

```java
public enum OreFamily {
    COAL, COPPER, IRON, GOLD, REDSTONE, LAPIS, DIAMOND, EMERALD,
    QUARTZ, NETHER_GOLD, ANCIENT_DEBRIS;
    public String configKey() { return name().toLowerCase(Locale.ROOT); }
}
```

Classification of a `BlockState`, **specific blocks before tags** — ordering
matters because vanilla's `#minecraft:gold_ores` tag includes
`nether_gold_ore`:

1. `Blocks.NETHER_GOLD_ORE` → `NETHER_GOLD`
2. `Blocks.NETHER_QUARTZ_ORE` → `QUARTZ`
3. `Blocks.ANCIENT_DEBRIS` → `ANCIENT_DEBRIS`
4. `BlockTags.COAL_ORES / COPPER_ORES / IRON_ORES / GOLD_ORES / REDSTONE_ORES /
   LAPIS_ORES / DIAMOND_ORES / EMERALD_ORES` → the matching family

The tag families automatically unify stone/deepslate variants, so a mixed
`iron_ore`/`deepslate_iron_ore` vein scans as one vein. The vein matcher passed
to `ConnectedBlockScanner` is "same `OreFamily` as the trigger block".
`matchDeepslateVariants=false` narrows the matcher to the exact trigger
`Block` instead of the family.

Structure this as a small mod-side class (`OreFamilies`) with two pure,
testable parts where possible; the tag lookups themselves need MC classes and
are exercised via gametests or left to the smoke test (mirror how trees splits
`TreeScanner` vs `LeafCollectionPolicy`).

## 4. Entry flow (`OresMod.handleBlockBroken` → `runAutoMine`)

Mirrors `TreesMod` at trees commit `4f18707`, minus the tree-only steps:

1. Server level + server player only; state classifies to an `OreFamily`;
   `config.enabled`.
2. `requireSneakForAutoMine` gate; `requirePickaxe` gate (`PickaxeItem`,
   hint `requires_pickaxe`); already-active guard + re-entrancy set
   (same `ACTIVE_PLAYERS` pattern).
3. Vein scan: `ConnectedBlockScanner.scan(origin, familyMatcher,
   new ScanLimits(maxVeinBlocks, maxHorizontalVeinRadius, OptionalInt.empty()))`,
   excluding the trigger position (already broken). Empty → hint `no_vein`.
4. Tier gate: `minimumTierByOreFamily.get(family)` vs
   `ToolTier.fromMaxDamage(tool.getMaxDamage())`; failure → hint
   `tier_required(family, tier)`.
5. `HarvestPlanner.plan(...)` — identical call shape to trees' stem phase
   (protection-active from settings + tool, `availableXpAboveFloor`, etc.).
   Map refusals/trim/substitution to hints (§6).
6. Schedule all planned blocks on the `HarvestScheduler<ServerLevel, ItemStack>`
   with `matchKind = 0` ("still classifies to the same family/block"),
   `exhaustionPerBlock = config.exhaustionPerBlock`,
   `xpCostPerBlock = config.xpCostPerBlock`, delay from
   `settings.delayTicksForEfficiencyLevel(efficiency)`.

Port implementations are copies of trees' adapters with `AxeItem → PickaxeItem`
in tool validation; factor nothing across the two mods beyond what the engine
already shares (the adapters are small and version-coupled by nature).

## 5. Config (`config/jlt_ores.json`, flat, with `_docs`, via engine `JsonConfigStore`)

Shared-balance fields — same names, defaults, and sanitization rules as trees
unless noted: `enabled`, `showGateFeedback`, `enableTimePenalty`,
`enableHungerTax`, `xpTaxMode` ("hunger_only"), `durabilityProtectionMode`
("all"), `durabilityMultiplier` (**3** — a vein block is cheaper than a log;
trees uses 4), `blockBreakDelayTicksByEfficiencyLevel` ({16,8,4,2,1,0}),
`maxInstantBlocksPerTick` (8), `exhaustionPerBlock` (0.4), `xpCostPerBlock` (1),
`enableDurabilityXpSubstitution` (false), `durabilityXpSubstitutionWindow` (10),
`xpPerSubstitutedDurabilityPoint` (3), `durabilityProtectionFloor` (1),
`hungerTaxFloor` (1), `xpTaxFloor` (0), `hungerResumeTimeoutTicks` (400).

Ore-specific fields:

| Field | Default | Notes |
|---|---|---|
| `requirePickaxe` | `true` | analog of `requireAxe` |
| `requireSneakForAutoMine` | `true` | analog of `requireSneakForAutoChop` |
| `maxVeinBlocks` | `64` | veins ≪ trees; cap guards modded mega-veins |
| `maxHorizontalVeinRadius` | `8` | XZ radius from trigger block |
| `maxMineDistance` | `64` | → `BalanceSettings.maxHarvestDistance` |
| `matchDeepslateVariants` | `true` | family-wide vs exact-block matching (§3) |
| `minimumTierByOreFamily` | table below | string map, keys = `OreFamily.configKey()` |

Default minimum tiers — one step above vanilla's harvest requirement
(the mod's philosophy, same as trees gating above vanilla), players can lower:

| Family | Vanilla needs | Default gate |
|---|---|---|
| coal | wood | stone |
| quartz | wood | stone |
| nether_gold | wood | stone |
| copper | stone | iron |
| lapis | stone | iron |
| iron | stone | iron |
| gold | iron | diamond |
| redstone | iron | diamond |
| emerald | iron | diamond |
| diamond | iron | diamond |
| ancient_debris | diamond | netherite |

Sanitization mirrors trees' `ModConfig` (invalid tier strings → default,
numeric floors/clamps, unknown-key tolerance) with equivalent tests.

## 6. Translation keys (`assets/jlt_ores/lang/en_us.json`)

Statuses — same set as trees §5 of `../trees/MIGRATION.md`, under
`message.jlt_ores.status.*` (death, distance, tool_missing, hunger_timeout,
hunger_paused, hunger_resumed, no_substitution_xp, no_xp, tool_broke,
durability_floor).

Hints under `message.jlt_ores.hint.*`: `requires_pickaxe`, `already_active`,
`no_vein`, `tier_required` (args: ore family, tier), `durability_floor_blocked`,
`durability_trimmed`, `substitution_planned`, `vein_durability`
(analog of `stem_durability`, args: required, remaining).

Names: `ore_family.jlt_ores.<configKey>` for all 11 families,
`tool_tier.jlt_ores.<tier>` for the 6 tiers. Wording style: copy trees'
`en_us.json` phrasing with ore vocabulary.

## 7. Tests

- Pure JUnit (no MC): config sanitization/reload/tier-map validation (mirror
  trees' `ModConfig*Test` suite), any pure classification helpers.
- The balance machinery itself is already covered by engine tests — do not
  re-test scheduler behavior here.
- Stretch (only if asked): Fabric gametests copied from `../reseed`'s wiring
  (`fabric-gametest` entrypoint + loom `runGametest`; use
  `helper.makeMockServerPlayer(GameType.SURVIVAL)` — the default mock is
  creative and bypasses all balance costs).

## 8. Deferred ideas (not in 0.1.0)

- Large-vein tier bump (`largeVeinBlockThreshold` raising the required tier by
  one step) — the analog of tree-shape classification.
- Per-family exhaustion/XP cost overrides.
- Raise-block-drops-to-player convenience (out of scope: balance-neutral QoL).
