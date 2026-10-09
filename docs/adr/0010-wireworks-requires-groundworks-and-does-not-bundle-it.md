---
status: accepted
---

# Wireworks requires Groundworks and does not bundle it

Wireworks nested Groundworks in its jar (`jarJar`), so it ran with nothing else installed. Beltworks
and the Pack also hold Groundworks, and a nested copy per mod leaves the loader choosing among them.
FactoryWorks #607 moves every Library to a required dependency.

## Decision

**Groundworks is a required dependency, installed beside Wireworks.** `build.gradle` compiles and
runs against it without `jarJar`, `neoforge.mods.toml` requires `groundworksRange`
(`[0.5.4,0.6)`), and the Modrinth and CurseForge uploads list Groundworks as a required dependency
(`modrinth_dependencies`, `curseforge_dependencies`).

The range keeps ADR 0001's shape: the lower bound is the version built against, and the upper bound
is the next minor, so a Groundworks patch needs no Wireworks release.

## Consequences

- A player installing Wireworks alone must also install Groundworks. The changelog says so.
- Wireworks' jar no longer carries Groundworks' licensing or a `META-INF/jarjar` entry.
