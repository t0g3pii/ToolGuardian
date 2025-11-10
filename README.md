# ToolGuardian

Warn players when their tools or armor are about to break — configurable thresholds, localized messages, MiniMessage formatting, and per-player toggles with persistence.

## Features
- Monitor active equipment: main hand, off hand, helmet, chestplate, leggings, boots (toggle per slot).
- Configurable thresholds in percent (add as many as you like, each with its own message key and optional sound).
- Localized messages (`lang/de.yml`, `lang/en.yml`) using MiniMessage formatting.
- Placeholder support in messages: `{item}`, `{percent}`, `{remaining}`, `{max}`.
- Anti-spam: warn once per threshold per item until the next threshold is reached.
- Gamemode filter: by default only Survival/Adventure.
- Per-player toggle command with persistent storage (`toggles.yml`).

## Requirements
- Paper (Minecraft 1.21.10)
- Java 21

## Installation
1. Download the release JAR (or build from source, see below).
2. Put the JAR into your server's `plugins` folder.
3. Start or restart the server.
4. Optional: adjust `plugins/ToolGuardian/config.yml` and localization files under `plugins/ToolGuardian/lang/`.

## Configuration
Default `config.yml` (you can add or remove thresholds freely):

```yaml
language: de

enabled-gamemodes:
  - SURVIVAL
  - ADVENTURE

monitor:
  main-hand: true
  off-hand: true
  head: true
  chest: true
  legs: true
  feet: true

thresholds:
  - percent: 20
    message: warn.medium
  - percent: 10
    message: warn.low
  - percent: 5
    message: warn.critical
    sound: BLOCK_NOTE_BLOCK_BELL
    sound-volume: 1.0
    sound-pitch: 1.0

check-interval-ticks: 20
```

Notes:
- Thresholds should be listed in ascending order by `percent`.
- A sound plays only if configured on that threshold.
- The plugin checks at a fixed interval (20 ticks = 1s by default).

## Localization
Two language files are provided: `lang/de.yml` and `lang/en.yml`. Example:

```yaml
warn:
  medium: "<yellow><bold>Heads up:</bold></yellow> {item} at <yellow>{percent}%</yellow> durability (<gray>{remaining}/{max}</gray>)."
  low: "<gold><bold>Warning:</bold></gold> {item} at <gold>{percent}%</gold> durability (<gray>{remaining}/{max}</gray>)."
  critical: "<red><bold>CRITICAL:</bold></red> {item} only <red>{percent}%</red> durability left (<gray>{remaining}/{max}</gray>)!"
toggle:
  now-enabled: "<green>ToolGuardian warnings: <bold>ON</bold></green>"
  now-disabled: "<red>ToolGuardian warnings: <bold>OFF</bold></red>"
  usage: "<gray>Usage:</gray> <yellow>/toolguardian toggle</yellow>"
  no-permission: "<red>You don't have permission.</red>"
```

Placeholders are written as `{item}`, `{percent}`, `{remaining}`, `{max}` and can be styled with MiniMessage tags around them. The plugin converts these placeholders and resolves them at runtime.

## Commands
- `/toolguardian toggle` (alias `/tg`): Toggle warnings for yourself.

## Permissions
- `toolguardian.toggle` — default: `true`

## How it works
- The plugin periodically scans configured slots. If an item has durability and enters a configured threshold (e.g., 20% → 10% → 5%), the player receives exactly one message per threshold step for that item. When the item changes or durability goes above the highest threshold again, the internal state resets, allowing future warnings as appropriate.

## Build from source
Prereqs: Java 21, Gradle.

```bash
cd plugin
gradle build
```

The JAR will be in `plugin/build/libs/`. To copy the JAR directly to your test server, you can use:

```bash
gradle copyToServer
```

This will place the JAR into `../server/plugins`.

## Troubleshooting
- No messages?
  - Ensure your gamemode is enabled in `enabled-gamemodes`.
  - Check that the slot (e.g., `main-hand`, `head`) is enabled under `monitor`.
  - Verify the item actually has durability.
- Placeholders appear literally?
  - Make sure you kept the curly-brace placeholders (`{item}`, `{percent}`, `{remaining}`, `{max}`) in your locale file.
  - Avoid conflicting chat formatters that strip MiniMessage formatting.
- No sound at critical?
  - Ensure the `sound` field exists on the desired threshold and uses a valid Bukkit sound name.

## License
Add a license of your choice (e.g., MIT) to this repository (not included by default).


