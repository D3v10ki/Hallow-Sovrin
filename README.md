# Hollow Sovereign

A co-op, anime-style boss-rush mod for **Minecraft 1.20.1 (Fabric)**. Pick one of four
sorcerer classes, level a skill tree, and fight cinematic bosses with your friends.

> Built one phase at a time from the design doc. This is an early, playable foundation —
> not the finished mod. See **Roadmap** below for what's in and what's next.

## Requirements
- Minecraft **1.20.1**
- **Fabric Loader** and **Fabric API**
- (Optional) **Mod Menu** — adds a settings button for the visual options
- (Optional) **Satin API** — enables the bloom shader (glow bleed). Without it the mod runs fine, just no bloom.

## Build & run (dev)
From this folder:

```bash
./gradlew runClient
```

Other useful tasks:
- `./gradlew build` — produce the mod jar in `build/libs/`
- `./gradlew runServer` — start a dev dedicated server (test multiplayer with a 2nd client)

The built mod jar is `build/libs/hollowsovereign-<version>.jar` (ignore the `-sources` jar).

## Playing with shaders (use a real launcher, NOT the dev client)
The dev launcher (`./gradlew runClient`) is only for testing mod code — it uses a patched
LWJGL that **crashes Sodium/Iris**. To actually see shaders + this mod together (this is also
how your private modpack will be set up):

1. Build the mod: `./gradlew build` → the jar is `build/libs/hollowsovereign-0.1.0.jar`.
2. In the **Modrinth App** (or Prism Launcher), make a new **Minecraft 1.20.1 / Fabric** instance.
3. Add these to the instance (one click each in the Modrinth App): **Fabric API**, **Sodium**,
   **Iris**, and a shaderpack (**Complementary Reimagined**). Optionally **Mod Menu**.
4. Drop `hollowsovereign-0.1.0.jar` into the instance's `mods` folder.
5. Launch → **Options → Video Settings → Shader Packs** → pick the pack → Done. Your bright
   ability particles will now bloom.

Your RTX 5070 will run Complementary at high FPS with Sodium.

## What works right now (Phases 1–2)
- Mod loads on Fabric 1.20.1; shows up in Mod Menu.
- **Class select** screen on first join. Choice is saved and survives death/logout.
- Four classes with two working starter abilities each (real effects + particles + sound).
- **Keybinds** (rebindable in Controls): abilities `R V G Z X C`, ultimate `B`, skill tree `K`.
- **Ability bar** (bottom-left) with a custom icon per ability, cooldown wipe + seconds, a "ready" glow, and locked-slot markers. No keybind letters — check Controls to rebind.
- **Leveling**: XP from kills, level-up popup, skill points. Max level 30.
- Impact **flash** feedback with intensity + flash-safe options.
- **Admin commands** (`/hollow …`) — see below.
- Client + server **config files** in `config/`.

## Controls
| Key | Action |
|-----|--------|
| `R V G Z X C` | Abilities 1–6 (only 1–2 are implemented so far) |
| `B` | Ultimate (unlocks at level 15 — stub for now) |
| `K` | Skill tree screen (view-only placeholder) |
| Right-click w/ class weapon | Block / parry (combat phase — not yet) |

## Admin commands (need op / permission level 2)
```
/hollow class set <player> <void_weaver|crimson_warden|hex_binder|ashen_blade>
/hollow class reset <player>
/hollow level set <player> <1-30>
/hollow xp add <player> <amount>
/hollow skillpoints add <player> <amount>
/hollow cooldowns reset <player>
```

## Roadmap (from the design doc build order)
See `ROADMAP.md` for the full phase list and current status.

## License
All rights reserved (for now). Change the `license` field in `fabric.mod.json` and this
section before publishing on Modrinth if you want a different license.
