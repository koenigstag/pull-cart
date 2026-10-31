# Versioning

The mod version carries the game version as a suffix, so one version string never
stands for two different jars:

```
<mod version>+<minecraft version>
```

| Branch | `mod_version` | Jar |
| --- | --- | --- |
| the 1.20 line | `1.0.0+1.20.1` | `<mod id>-1.0.0+1.20.1.jar` |
| the 1.21 line | `1.0.0+1.21.1` | `<mod id>-1.0.0+1.21.1.jar` |

## Rules

- `mod_version` in `gradle.properties` always ends with `+` and the Minecraft
  version that branch targets.
- The part before `+` is the mod's own version and moves on its own merits: a
  breaking change bumps the major, a feature the minor, a fix the patch. It does
  not encode the game version, and porting to a new game version does not bump it
  by itself.
- A port of the same code to another game version keeps the number and changes
  only the suffix. Two branches may therefore carry the same number, which is the
  point: `1.2.0+1.20.1` and `1.2.0+1.21.1` are the same mod for two games.
- Release tags take the same shape: `v1.0.0+1.21.1`.

## Why this and not a major version per game version

Giving each game version its own major (v1 for 1.20, v2 for 1.21) would spend the
major on the game rather than on the mod, leaving nothing to signal a breaking
change inside one line, and it does not survive the next game version.

The suffix also keeps dependency ranges working. Forge and NeoForge compare
versions by Maven rules, where the part after `+` is an unknown qualifier and
sorts above the plain release, so `[1.0.0,)` still accepts `1.0.0+1.21.1`.

This is what the ecosystem already does: Curios ships `9.5.1+1.21.1`, and
SmartMovingReborn in this same workspace ships `1.0.1+1.20.1` and `1.0.1+1.21.1`.
