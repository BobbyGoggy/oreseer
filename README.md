# OreSeer — Fabric mod for Minecraft 26.3

Ore and Ancient Debris finder with exotic through-the-wall highlights, radar HUD and a multi-page menu.
Client-side only; works in singleplayer and on servers.

## Versions (latest for 26.3)
- Minecraft 26.3, Java 25
- Fabric Loader 0.19.5, Fabric API 0.162.0+26.3
- Fabric Loom 1.18-SNAPSHOT (plugin id `net.fabricmc.fabric-loom`), Gradle 9.5.1

## Build
1. Install JDK 25.
2. In this folder run `gradle wrapper --gradle-version 9.5.1` once (or open the folder in IntelliJ IDEA, it will import the Gradle project).
3. `./gradlew build` (Windows: `gradlew.bat build`)
4. Take `build/libs/oreseer-1.0.0.jar` and put it into `.minecraft/mods` together with Fabric API 0.162.0+26.3.

If `./gradlew build` shows red/unknown names, press Alt+Enter in IDEA on the import, or send the compiler output for a quick fix.
The two most likely spots: imports in `OreRenderer.java` (rendering API changes often) and `KeyMappingHelper` in `OreSeerClient.java`.

## Keys (rebindable in Controls → OreSeer)
- `O` open the settings menu
- `K` toggle highlights
- `H` Ancient Debris only mode
- `J` cycle the Ancient Debris style

## Menu pages
General (radius, scan speed, limits, hide visible ores) · Ancient Debris (style, colour, size, pillar, tracer, vein strobe)
· Ores (per-ore on/off, style, colour, size) · Effects (opacity, speed, distance, fade, tracers) · HUD (panel, radar)

## Highlight styles
Heartbeat, Spinning Crystal, Aurora Layers, Sky Beacon, Sonar Ripples, Glitch, Atom Orbit, Prism Frame.
Colours: 9 presets plus a time-cycling Rainbow.

Config file: `.minecraft/config/oreseer.json`
