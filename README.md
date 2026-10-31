# Glaxotion

A client-side Fabric mod for Minecraft Java 26.1.1.

Glaxotion is being developed to let you locally customize how other nearby, loaded players appear on your own client. It does not change anyone else's client or the server.

## Planned features

- Automatically track the nearest loaded player, with a configurable range.
- Give a manually selected player priority over automatic targeting.
- Fall back to automatic targeting when the manual target is unavailable.
- Save multiple local identity profiles per player.
- Resolve a skin from a Minecraft Java username while customizing the displayed name independently.
- Add configurable PvP tier labels: HT1, LT1, HT2, LT2, HT3, LT3, or custom.

## Limitations

- Client-side visual changes only: no server-side changes, packet spoofing, or changes to other players' accounts.
- Overrides can only apply to players currently loaded and tracked by the local client.
- A username used as a skin source is only a lookup key; it does not grant access to or modify that account.

## Development status

Early development. The feature list above describes the project direction, not features already implemented.
