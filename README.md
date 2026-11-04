# Glaxotion

**Client-side identity customization for Minecraft Java 26.1.1 (Fabric).**

Glaxotion changes how nearby players are displayed on your own client. It never edits server data, sends spoofed packets, or changes another player's actual Minecraft account.

## Current features

- **Auto Target:** tracks the nearest other player currently loaded in your client world.
- **Manual target:** cycle through nearby players; manual selection takes priority. If that player leaves the loaded world or moves outside range, automatic targeting resumes.
- **Range selector:** 16, 24, 32, 48, 64, or 96 blocks.
- **Local profiles:** save multiple presets for each player UUID, switch between them, create new presets, and delete presets.
- **Skin source:** enter a Java username. Glaxotion looks up the public profile and skin texture through Mojang's profile services, then applies the texture only in your local render state.
- **Displayed IGN:** independently replace the locally rendered name tag.
- **PvP tier:** HT1, LT1, HT2, LT2, HT3, LT3, or a custom label.
- **Color:** choose white, cyan, gold, coral, mint, or violet for the custom name tag.
- **Persistent settings:** profiles are stored in `config/glaxotion.json`.

## Controls

- Press **G** to open the Glaxotion Identity Studio.
- **Next Player** cycles through nearby loaded players and selects one manually.
- **Auto Target** clears manual selection and returns to nearest-player tracking.
- **New Preset**, **Next Preset**, and **Delete Preset** manage presets for the selected player.
- **Tier**, **Color**, and **Range** buttons cycle their available options.

## Build

Requirements: Java 25 and Gradle 9.4.0.

```bash
gradle build
```

The distributable mod is the regular JAR in `build/libs/`. Do not install the `-sources.jar`.

GitHub Actions builds the project on every push to `main` and uploads the JAR as a workflow artifact: [Actions](https://github.com/nintendo-source/Glaxotion/actions).

## Scope and privacy

- Overrides affect only the local client rendering.
- Only players loaded in the current client world can be targeted.
- Mojang username lookup uses public profile endpoints. No Microsoft password, session token, or account credential is requested.
- The displayed IGN and tier are visual labels; they do not alter server identity, chat signing, tab-list server data, or permissions.

## Compatibility

Built for Minecraft Java **26.1.1**, Fabric Loader **0.18.4+**, Fabric API, and Java **25+**.
