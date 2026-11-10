# Glaxotion build notes

## Target
- Minecraft Java 26.1.1
- Fabric Loader 0.18.4 or newer
- Java 25

## Build
The GitHub Actions workflow in `.github/workflows/build.yml` runs `gradle build` on every push to `main` and uploads the resulting JAR as a workflow artifact.

## Install
1. Download the `glaxotion-...` artifact ZIP from a successful GitHub Actions run.
2. Extract the JAR from the ZIP.
3. Put the JAR in the Minecraft instance's `mods` folder alongside Fabric API.
4. Launch Minecraft 26.1.1 with Java 25.
5. Join a world with another player nearby and press `G` to open Glaxotion.

## Important verification note
A successful CI build confirms compilation and packaging only. It does not by itself confirm that the in-game mixin, player name-tag override, or remote skin download works in a live client. Those behaviors still require an in-game test.
