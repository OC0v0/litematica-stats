# Litematica Stats

A client-side Fabric addon that extends **Litematica's Material List** with entity and container-content statistics.

## Features

- **Three tabs in the material list screen**
  - Blocks — the original Litematica material list.
  - Entities — counts entities by type with spawn egg icons (informational only, never added to material totals).
  - Container Contents — scans block containers and container entities, shows items as slot-grid previews.
- Double chests are merged into one container; containers with identical contents can be merged with an ×N badge.
- Vanilla container GUI layouts and full capacities are respected (furnace, hopper, dispenser, brewing stand, bookshelf, campfire, beacon, chest/barrel/shulker, …).
- Hover slots for the vanilla item tooltip, compatible with Tweakeroo/Minihud shulker previews.
- Ignore individual containers or whole categories; ignored items collect in a section at the bottom.
- Unified item view, hide-empty toggle, search filtering and sortable columns.
- Smooth pixel-based scrolling.

## Version ports

One standalone Gradle project per Minecraft version:

| Directory | Minecraft | Build JVM |
|---|---|---|
| `litematica-stats` | 1.21.11 | JDK 21 |
| `litematica-stats-1.21.1` | 1.21.1 | JDK 21 |
| `litematica-stats-1.20.1` | 1.20.1 | JDK 21 |
| `litematica-stats-1.19.4` | 1.19.4 | JDK 21 |
| `litematica-stats-1.18.2` | 1.18.2 | JDK 21 |
| `litematica-stats-26.2` | 26.2 | JDK 26 |
| `litematica-stats-26.3` | 26.3 | JDK 26 |

Every project uses Gradle 9.5 and Loom 1.17.20. Third-party mod jars are **not** included in this repository; place the matching Litematica + MaLiLib jars into each project's `libs/` directory before building:

| Project | Required jars in `libs/` |
|---|---|
| `litematica-stats` | `litematica-fabric-1.21.11-0.26.16.jar`, `malilib-fabric-1.21.11-0.27.20.jar`, `conditional-mixin-fabric-0.6.4.jar` |
| `litematica-stats-1.21.1` | `litematica-fabric-1.21-0.19.61.jar`, `malilib-fabric-1.21-0.21.10.jar` |
| `litematica-stats-1.20.1` | `litematica-fabric-1.20.1-0.15.4.jar`, `malilib-fabric-1.20.1-0.16.3.jar` |
| `litematica-stats-1.19.4` | `litematica-fabric-1.19.4-0.14.7.jar`, `malilib-fabric-1.19.4-0.15.4.jar` |
| `litematica-stats-1.18.2` | `litematica-fabric-1.18.2-0.11.7.jar`, `malilib-fabric-1.18.2-0.12.1.jar` |
| `litematica-stats-26.2` | `litematica-fabric-26.2-0.28.8.jar`, `malilib-fabric-26.2-0.29.6.jar`, `conditional-mixin-fabric-0.6.4.jar` |
| `litematica-stats-26.3` | `litematica-fabric-26.3-0.29.1.jar`, `malilib-fabric-26.3-0.30.2.jar`, `conditional-mixin-fabric-0.6.4.jar` |

For the 26.x ports, patch `Fabric-Loom-Version` in the jars' `META-INF/MANIFEST.MF` to `1.17.20`.

## Building

```bash
cd litematica-stats          # or any version port
JAVA_HOME=/path/to/jdk gradle build
```

The remapped jar is copied to `../output/` automatically.

## Requirements

- Fabric Loader, Fabric API
- [Litematica](https://modrinth.com/mod/litematica)
- [MaLiLib](https://modrinth.com/mod/malilib)

## Links

- Modrinth: https://modrinth.com/mod/litematica-stats

## License

MIT — see [LICENSE](LICENSE).
