# Hollow Sovereign — Build Roadmap

Phases follow section 19 of the design doc. Build one phase at a time and test in-game
before moving on.

| # | Phase | Status |
|---|-------|--------|
| 1 | Setup: Fabric 1.20.1 project, libraries, mod loads | ✅ Done |
| 2 | Player data, class select, keybinds, cooldowns, cooldown HUD | ✅ Done (core) |
| 3 | Combat core: combos, heavy attack, block, guard break, parry, parry chains, Hollow Flash | ⬜ Next |
| 4 | Impact tier system: shake, hit-stop, impact frames, shaders, intensity, flash-safe | 🟡 Partial (flash + intensity + flash-safe done; **bloom shader done** via Satin; shake/hit-stop/domain-desaturation pending) |
| 5 | Ashen Blade — full class with animations + effects | 🟡 Partial (2 abilities, vanilla fx) |
| 6 | Classes — full kits + custom-particle animation | 🟡 **Void Weaver reworked** (see below); Crimson/Hex/Ashen still 2 abilities w/ vanilla fx |

**FX tech added:** custom particle types (`void_glow`/`void_star`/`void_shard` — generated PNGs), `HSGlowParticle` (full-bright billboard), `HSEffects` per-world scheduler, `VoidFx` choreography helpers, **GeckoLib** animated 3D models (`GravityOrbEntity`).

**Void Weaver rework (domain-first):**
- **Starless Expanse** = level-1 signature: massive **100-block sealed dome** (traps everyone in, keeps others out), enemies debuffed / allies buffed, 30s. Other abilities unlock 5/8/11/14/17/20.
- **Gravity Well → Black Hole**: animated 3D GeckoLib model (dark core + spinning accretion disks) with matter spiralling in.
- **Blink → Void Step**: teleport to the block you're looking at, up to 200 blocks, look-direction only.
- See `DOMAINS.md` for domain design + JJK concepts to build next.
- ⚠️ **Skill tree flagged for a full rework** by user ("mediocre at best") — next session.
| 7 | Skill trees & leveling | 🟡 Partial (leveling + XP done; tree is a placeholder screen) |
| 8 | First boss: Hollow Choir (intro, phases, transformation, drops, scaling) | ⬜ |
| 9 | Downed & revive system | ⬜ |
| 10 | Regular Hollows, ores, materials, gear | ⬜ |
| 11 | Remaining bosses 2–6 | ⬜ |
| 12 | Domains & Domain Clash | ⬜ |
| 13 | Final boss: Hollow Sovereign | ⬜ |
| 14 | Structures & world gen, Lore Tablets | ⬜ |
| 15 | Full character-select screen with animated previews | ⬜ |
| 16 | Polish: config screen, commands, balance, sounds, bug fixes | 🟡 Partial (commands + basic config done) |

## Libraries to add as we hit the phases that need them
All are on Modrinth, so listing them as dependencies keeps the mod publishable.

- **GeckoLib** — animated 3D bosses/mobs/summons (Phase 5+, needed for bosses in Phase 8)
- **playerAnimator (KosmX)** — custom player attack/parry/ability animations (Phase 5)
- **Satin API (Ladysnake)** — full-screen shaders: B&W flash, color shift, vignette (Phase 4 polish)
- **Cardinal Components** — optional; we currently persist data with a lightweight mixin instead

## Notes / decisions
- Mappings: **Yarn** (`1.20.1+build.10`).
- Player data persists via a `PlayerEntity` mixin (save/load NBT) + respawn copy — no extra dep.
- Server is authoritative for damage, cooldowns, XP. Client only renders HUD/effects.
- "Slow-mo" must be client-side hit-stop only (never slow the server tick) — see doc §2.

## Still to decide (from doc §20)
- Exact damage/HP/cooldown numbers (balance in testing)
- Full skill-tree node lists
- Boss drop stats, texture palette, sound/model sourcing
- Downed state outside boss fights? Boss music?
