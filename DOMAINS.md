# Domains — design notes

Domains are the heart of the mod's JJK flavor. This captures what's built, the JJK
concepts worth adapting (as our own originals), and per-class ideas.

## What's built now — Void Weaver: "Starless Expanse"
- **Domain-first class.** Void Weaver starts with *only* the domain (unlocks at level 1);
  his other abilities are the toolkit he fills in and uses inside it (unlock 5/8/11/14/17/20).
- **Massive sealed dome, radius 100 blocks.** Everyone inside at cast is trapped in; nobody
  outside can enter. Barrier shimmers on contact. Lasts 30s.
- Inside: enemies **slowed + weakened**; allies get **speed + regen + night vision**.
- Visual is "best effort at scale": ambient starfield around each player + a wall shimmer that
  appears when you get near the 100-block edge. (A solid 100-block dome *model* isn't renderable
  as one object — entities cull past render distance — so the wall is drawn where you can see it.)

### Known rough edges (to tune with you)
- Barrier clamps by teleport → can feel slightly rubber-bandy right at the edge.
- The 200-block **Void Step** teleport can currently cross the barrier (escape your own dome).
  Easy fix: clamp Void Step to stay inside an active domain.
- No opening cinematic yet (just a burst + sound).

## JJK concepts worth adapting (our own versions)
1. **Sure Hit (the domain's whole point):** inside your domain, your techniques *cannot miss*.
   → Make Void Weaver's abilities auto-target / guaranteed-hit while he's in his own domain.
2. **Domain Barrier:** the sealed shell (built). Could add a "incomplete/open" domain that's
   weaker but doesn't fully seal.
3. **Simple Domain:** a small defensive counter-shell that auto-parries anything entering it —
   a great *defensive* option for melee classes (Ashen Blade) instead of a full domain.
4. **Domain Amplification:** wrap yourself in domain energy to neutralize an enemy domain's
   sure-hit — a counter/counterplay layer for later.
5. **Domain Clash:** two domains overlap → tug-of-war. We stubbed the idea; real version =
   both casters mash/time prompts, teammates hitting the boss add to your side, loser's domain
   shatters and they're stunned.
6. **Binding Vows:** opt-in tradeoffs — e.g., shrink your domain radius for much bigger damage,
   or drop the barrier (let people leave) to make it recharge faster.
7. **Falling-blossom style:** a no-domain counter that briefly makes you immune to an enemy
   domain — an emergency button.

## Per-class domain / ultimate ideas
- **Void Weaver — Starless Expanse** (built): sealed void arena; sure-hit for his kit inside.
- **Hex Binder — "Hollow Menagerie":** summon-domain. All his summons turn giant + empowered,
  and the domain keeps spawning shades. Support-flavored: allies inside also get warded.
- **Crimson Warden — "Sanguine Sanctum":** a *defensive* domain / fortress. Smaller dome that
  massively heals allies and taunts every enemy inside onto the Warden. Tank fantasy.
- **Ashen Blade — "Simple Domain" + "Thousand Ash Cuts":** not a big dome guy. Gets a small
  auto-counter **Simple Domain** (parries the next hit + ripostes) and keeps the screen-filler
  ultimate as a burst finisher.
- **The Hollow Sovereign (final boss) — "Throne of Nothing":** the ultimate sure-hit domain the
  players must survive by countering with their own domain (Domain Clash), or by hiding behind a
  Simple Domain / Domain Amplification.

## Build order suggestion for domains (when you're back)
1. **Sure-hit inside your domain** (auto-hit for the owner's abilities). Big, and pure gameplay.
2. **Void Step stays inside the domain** + clamp fix.
3. **Opening cinematic** (letterbox + title card "STARLESS EXPANSE" + Tier-4 burst).
4. **Binding vow toggle** (shrink for power).
5. **Domain Clash** (needs a second domain user — ties into bosses).
6. Roll domains onto Hex Binder / Crimson Warden.
