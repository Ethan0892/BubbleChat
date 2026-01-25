# BubbleChat

BubbleChat is a modern Paper 1.21+ chat plugin built to cover the practical “EssentialsChat + ChatControl-style” feature set, without requiring packet libraries.

## Highlights

- Two chat systems: **channels** (global/local/staff) or **radius** (Essentials-style proximity with shout/question)
- Simple, legacy `&` formatting by default (server-dev friendly)
- Optional MiniMessage in player messages (permission-gated)
- Private messages + reply + SocialSpy
- Moderation: mute/unmute, ignore, global chat mute, clear chat
- Mentions: `@name` highlight + sound
- **Automated responses / automation**: regex rules engine (reply/cancel/replace/cooldowns/commands/staff alerts/logging)
- QoL: `/me`, join MOTD/announcements, per-player toggles
- Extras: `[item]` hover item display, click-to-remove messages for moderators (`[x]`)

## Requirements

- Server: Paper/Purpur 1.21+
- Java: 21

Optional integrations (auto-detected):

- PlaceholderAPI (expand `%placeholders%` in formats and optionally in player-typed messages)
- Vault (via PlaceholderAPI for prefix/suffix placeholders like `%vault_prefix%`)
- DiscordSRV (send rule/staff alerts to Discord)
- ChatColor2 (applies players' `/chatcolor` colour to their messages when MiniMessage is not being used)

## Install / Update

1. Drop the jar into `plugins/`
2. Restart the server
3. Edit `plugins/BubbleChat/config.yml` and `plugins/BubbleChat/rules.yml`
4. Reload: `/bchat reload`

## Quick Start (recommended defaults)

Open `plugins/BubbleChat/config.yml`:

### Channels mode (default)

Channels mode uses one simple format line plus per-channel prefixes:

```yml
chat:
	mode: channels
	format: '&f{DISPLAYNAME} &7 » &r{MESSAGE}'

	channel-prefixes:
		global: "&7[G]&r "
		local: "&7[L]&r "
		staff: "&c[Staff]&r "
```

This produces:

`[G] DisplayName » message`

No surprise colon/extra characters unless you put them in `chat.format`.

### Radius mode (Essentials-style)

```yml
chat:
	mode: radius
	radius: 100
	format: '&f{DISPLAYNAME} &7 » &r{MESSAGE}'
```

Players can:

- Prefix `!` to shout globally (permission-gated)
- Prefix `?` to use question mode (permission-gated)

## Formatting & Placeholders

### Format placeholders (legacy `&`)

In `chat.format` (and radius mode overrides), you can use:

- `{MESSAGE}`
- `{USERNAME}`
- `{DISPLAYNAME}`
- `{WORLD}` `{WORLDNAME}` `{SHORTWORLDNAME}`
- `{GROUP}` (best-effort via PlaceholderAPI)
- `{PREFIX}` `{SUFFIX}` (best-effort via PlaceholderAPI + Vault placeholders)

### MiniMessage in player messages

BubbleChat can allow players to use MiniMessage in what they type:

```yml
chat:
	message:
		allow-player-minimessage: true
		escape-without-permission: true
```

- Permission: `bubblechat.format`
- If `escape-without-permission` is true, tags are escaped for non-permitted players.

### PlaceholderAPI expansion inside messages

If enabled, BubbleChat can expand `%placeholders%` that players type:

```yml
placeholders:
	enabled-in-player-messages: true
	permission: "bubblechat.placeholders"
```

## Features

### Channels

- `/ch <global|local|staff>`
- `/local` and `/global` shortcuts
- Local channel uses `chat.local-radius-blocks`
- Staff channel visibility requires `bubblechat.channel.staff`

### Private Messages

- `/msg <player> <message>`
- `/reply <message>`
- `/socialspy` toggles spy (permission-gated)

Config:

```yml
private-messages:
	enabled: true
	format-to-sender: "<gray>[to <white><target></white>]</gray> <message>"
	format-to-target: "<gray>[from <white><sender></white>]</gray> <message>"
```

### Moderation

- Mute system: `/mute`, `/unmute` (duration + reason supported)
- Ignore system: `/ignore`, `/ignorelist`
- Global chat mute: `/chatmute [on|off]`
- Clear chat: `/clearchat [-s]`

#### Click-to-remove messages (`[x]`)

BubbleChat can append a clickable `[x]` to chat lines for moderators and attempt to delete the message (Paper signed-message deletion).

```yml
chat:
	remove:
		enabled: true
		permission: "bubblechat.moderation.remove"
		button: "[x]"
		ttl-ms: 180000
```

Notes:

- This depends on server support for signed-message deletion and may be best-effort on some forks.
- There is also an admin command path: `/bchat remove <id>`.

### Mentions

```yml
mentions:
	enabled: true
	highlight-format: "<yellow>@<name></yellow>"
	sound:
		enabled: true
		key: "minecraft:entity.experience_orb.pickup"
		volume: 1.0
		pitch: 1.2
```

### `[item]` hover item display

Players can type `[item]` to show their held item with hover text:

```yml
chat:
	item:
		enabled: true
		token: "[item]"
		permission: "bubblechat.item"
```

### Join/Quit/Death formatting + MOTD/Announcements

- Join/quit/death formats are MiniMessage templates.
- Join announcements/MOTD system supports opt-out and per-player toggles.

## Automated Responses / Rules Engine

Yes — BubbleChat has built-in automated responses via `rules.yml`.

Enable it:

```yml
rules:
	enabled: true
	file: "rules.yml"
```

Then edit `plugins/BubbleChat/rules.yml`.

Rules can:

- `reply` (auto-responder)
- `cancel` / `silent_cancel`
- `replace` text
- enforce per-player `cooldown`
- run commands as console/player
- `notify_staff` (and optionally Discord)
- `log` to `rules.log`

Variables:

- `{player}` `{uuid}` `{message}`
- `{1}..{9}` regex capture groups
- `{remaining_s}` / `{remaining_ms}` on cooldown blocks

Reload after editing rules: `/bchat reload`

## Commands

Main:

- `/bchat info`
- `/bchat reload`
- `/bchat remove <id>`

Chat:

- `/ch <global|local|staff>`
- `/local`, `/global`
- `/shout` (radius chat)
- `/chatmute [on|off]`
- `/clearchat [-s]`
- `/me <action>`

Social:

- `/msg <player> <message>`
- `/reply <message>`
- `/socialspy`

Moderation:

- `/mute <player> [duration] [reason]`
- `/unmute <player>`
- `/ignore <player>`
- `/ignorelist`

Announcements:

- `/announcements [on|off|toggle|status]`
- `/motd`
- `/toggle <join|quit|death|announcements|pm>`

## Permissions

Core:

- `bubblechat.reload` – use `/bchat reload`
- `bubblechat.format` – allow MiniMessage in player messages
- `bubblechat.placeholders` – allow `%placeholders%` expansion in player messages
- `bubblechat.item` – allow `[item]` token

Channels & Spy:

- `bubblechat.channel.staff` – access staff channel
- `bubblechat.socialspy` – SocialSpy
- `bubblechat.chat.spy` – see all radius chat
- `bubblechat.chat.shout` – allow shout override (`!`) / shout mode
- `bubblechat.chat.question` – allow question override (`?`)

Moderation:

- `bubblechat.moderation.mute` – `/mute` and `/unmute`
- `bubblechat.moderation.remove` – click-to-remove `[x]`
- `bubblechat.bypass.spam` – bypass anti-spam
- `bubblechat.bypass.filter` – bypass filters

QoL:

- `bubblechat.announcements.optout` – allow opting out of join announcements
- `bubblechat.motd` – `/motd`
- `bubblechat.clearchat` – `/clearchat`
- `bubblechat.me` – `/me`
- `bubblechat.toggle` – `/toggle`

## Metrics (bStats)

BubbleChat includes bStats (plugin id `28824`).

## Build & Tests

Requires Java 21 and Maven.

- Run tests: `mvn -q test`
- Build jar: `mvn -q package`
- Output: `target/bubblechat-1.0.1.jar`

## Troubleshooting

### “My config changes didn’t apply”

- Run `/bchat reload`
- Avoid plugin reloaders that unload/reload plugins at runtime; prefer a full server restart.

### “Why is there a ':' / why does it show [G]?”

- In **channels mode**, BubbleChat uses `chat.channel-prefixes.<channel>` + `chat.format`.
- If you see a separator you don’t want, it’s coming from `chat.format`.
