# BubbleChat — Complete Guide

A modern Paper 1.21+ chat plugin with channels/radius chat, moderation tools, private messages, a rules/automation engine, and optional integrations.

## Table of contents

- [Requirements](#requirements)
- [Install / Update](#install--update)
- [Quick start](#quick-start)
- [Features](#features)
- [Commands](#commands)
- [Permissions](#permissions)
- [Placeholders](#placeholders)
- [Rules engine (automation)](#rules-engine-automation)
- [Configuration reference](#configuration-reference)
- [FAQ / Troubleshooting](#faq--troubleshooting)

---

## Requirements

- Server: Paper/Purpur `1.21+` (plugin declares `api-version: 1.21`)
- Java: `21`

### Optional integrations (auto-detected)

- **PlaceholderAPI**: expands `%placeholders%` in formats, and optionally in player-typed messages
- **Vault**: (via PlaceholderAPI placeholders) best-effort prefix/suffix placeholders like `%vault_prefix%` and `%vault_suffix%`
- **DiscordSRV**: send rule/staff alerts to Discord
- **ChatColor2**: uses players’ `/chatcolor` colour when MiniMessage isn’t being used (and optionally as a base style even when it is)

---

## Install / Update

1. Drop the jar into `plugins/`
2. Restart the server
3. Configure:
   - `plugins/BubbleChat/config.yml`
   - `plugins/BubbleChat/rules.yml`
4. Reload configs (recommended after edits): `/bchat reload`

---

## Quick start

### Pick a chat mode

BubbleChat supports two main styles:

- **Channels mode** (default): Global/Local/Staff channels + one main legacy format line
- **Radius mode**: Proximity chat with optional shout/question behavior

#### Channels mode (recommended default)

```yml
chat:
  mode: channels
  format: '&f{DISPLAYNAME} &7 » &r{MESSAGE}'

  channel-prefixes:
    global: "&7[G]&r "
    local: "&7[L]&r "
    staff: "&c[Staff]&r "
```

#### Radius mode (Essentials-style)

```yml
chat:
  mode: radius
  radius: 100
  format: '&f{DISPLAYNAME} &7 » &r{MESSAGE}'
```

Radius mode extras:

- `!message` can act as “shout” (permission-gated)
- `?message` can act as “question” (permission-gated)
- `/shout` toggles persistent shout mode per player (permission-gated)
- `/local` and `/global` switch between local/global radius behavior

---

## Features

### Chat systems

- **Channels**: `global`, `local`, `staff`
  - Local uses `chat.local-radius-blocks`
  - Staff channel visibility requires permission
- **Radius chat**: optional proximity radius + shout/question modes
  - “Spy” permission can see local chat regardless of radius

### Formatting

- Default formatting is legacy `&` (server-dev friendly)
- Optional **MiniMessage in player messages** (permission-gated)
- Optional **PlaceholderAPI expansion inside player messages** (permission-gated)

### Moderation

- Mute/unmute (duration + optional reason)
- Ignore/unignore + ignore list
- Global chat mute
- Clear chat
- Optional click-to-remove messages (`[x]`) with a TTL, best-effort on supported servers

### Social

- Private messages `/msg` + `/reply`
- SocialSpy toggle

### Quality of life

- Mentions: `@name` highlight + sound
- `/me` roleplay action messages
- MOTD and join announcements
- Per-player toggles for join/quit/death visibility, announcements opt-out, and PM blocking
- `[item]` hover item display

### Rules engine (automation)

A built-in rules system that can match chat messages or commands with Java regex and then:

- cooldown/gate
- reply
- replace text
- cancel/silent_cancel
- run console/player commands
- notify online staff (and optionally Discord)
- log to a file

See: [Rules engine (automation)](#rules-engine-automation)

---

## Commands

Command list comes from `plugin.yml` plus observed behavior in the command handlers.

### Admin

- `/bubblechat` (alias: `/bchat`)
  - `/bchat info` — show plugin version
  - `/bchat reload` — reload config/rules
  - `/bchat remove <id>` — remove a tracked message (usually used for the clickable `[x]`)

### Channels / Chat mode

- `/ch <global|local|staff>` (alias: `/channel`) — switch channel
- `/local` (alias: `/lchat`) — switch to local channel (or local mode in radius chat)
- `/global` (alias: `/gchat`) — switch to global channel (or global mode in radius chat)
- `/shout` — toggle shout mode (radius chat)

Notes:

- In **radius mode**, `/global` requires `bubblechat.chat.shout` (it’s treated as “go global”).

### Social

- `/msg <player> <message>` (aliases: `/m`, `/tell`, `/w`, `/whisper`) — send a private message
- `/reply <message>` (alias: `/r`) — reply to last private message
- `/socialspy` (alias: `/ss`) — toggle spying on private messages (permission-gated)

### Moderation

- `/mute <player> [duration] [reason]` — mute a player
- `/unmute <player>` — unmute a player
- `/ignore <player>` — ignore/unignore a player
- `/ignorelist` — list ignored players
- `/chatmute [on|off]` — toggle global chat mute
- `/clearchat [-s]` (alias: `/cc`) — clear chat

### Utility

- `/me <action>` (alias: `/action`) — action/roleplay message
- `/motd` (alias: `/welcome`) — show the MOTD again
- `/announcements [on|off|toggle|status]` (alias: `/ann`) — manage join announcements opt-out
- `/toggle <join|quit|death|announcements|pm>` (alias: `/togg`) — toggle personal preferences

---

## Permissions

BubbleChat defines many permissions in `plugin.yml` and also checks a few additional “bypass” nodes in code.

### Wildcards

- `bubblechat.*` — grants `bubblechat.admin`
- `bubblechat.admin` — grants most admin/moderation permissions

### Common player permissions

- `bubblechat.motd` (default: true)
- `bubblechat.me` (default: true)
- `bubblechat.toggle` (default: true)
- `bubblechat.item` (default: true)
- `bubblechat.announcements.optout` (default: true)

### Formatting permissions

- `bubblechat.format` — allow MiniMessage in what players type
- `bubblechat.color` — if a player has `bubblechat.format` but not this, BubbleChat strips common color tags
- `bubblechat.placeholders` — allow `%placeholders%` typed by players to expand (if enabled in config)

### Chat channel / radius permissions

- `bubblechat.channel.staff` — access/see staff channel
- `bubblechat.chat.shout` — allow shout mode and “global” behavior in radius mode
- `bubblechat.chat.question` — allow question mode (prefix `?`)
- `bubblechat.chat.spy` — see local/radius messages regardless of distance

### Moderation / staff tools

- `bubblechat.reload` — reload config/rules
- `bubblechat.moderation.mute` — mute/unmute and global chat mute
- `bubblechat.moderation.remove` — remove messages / use clickable `[x]` (default permission; can be changed in config)
- `bubblechat.clearchat` — clear chat
- `bubblechat.socialspy` — toggle SocialSpy
- `bubblechat.alerts` — receive staff alerts from rule actions

### Anti-spam / filter bypass

- `bubblechat.bypass.spam` — bypass anti-spam checks
- `bubblechat.bypass.filter` — bypass word/regex filters

Additional bypass nodes used in code (not listed in `plugin.yml`, but supported):

- `bubblechat.bypass.mute` — talk while personally muted
- `bubblechat.bypass.chatmute` — talk while global chat mute is enabled
- `bubblechat.bypass.pmblock` — message someone who has blocked PMs

### Links

- `bubblechat.links` — bypass link blocking when `filters.block-links-without-permission` is enabled

### Command spying

- `bubblechat.spy.commands` — receive command spy output (when enabled)
- `bubblechat.spy.exempt` — exempt a player from being command-spied

---

## Placeholders

BubbleChat supports three kinds of placeholders:

1. **BubbleChat format tokens** like `{DISPLAYNAME}`
2. **MiniMessage placeholders** like `<message>`
3. **PlaceholderAPI placeholders** like `%luckperms_primary_group%`

### 1) Chat format tokens (legacy `&` formats)

In `chat.format`, `chat.channel-prefixes.*`, and radius/group/type overrides, you can use:

- `{MESSAGE}`
- `{USERNAME}`
- `{DISPLAYNAME}` / `{NICKNAME}`
- `{WORLD}` `{WORLDNAME}` `{SHORTWORLDNAME}`
- `{GROUP}` (best-effort via PlaceholderAPI: `%luckperms_primary_group%`)
- `{PREFIX}` `{SUFFIX}` (best-effort via PlaceholderAPI: `%vault_prefix%`, `%vault_suffix%`)

Notes:

- If PlaceholderAPI is installed, BubbleChat also expands any other `%placeholders%` inside formats.
- If PlaceholderAPI isn’t installed, `{PREFIX}` and `{SUFFIX}` resolve to empty.

### 2) MiniMessage placeholders

BubbleChat uses MiniMessage templates for several parts of the config, including:

- join/quit/death message formats
- MOTD/announcement messages
- rule engine reply/cancel messages

If you enable MiniMessage for radius formats (`chat.radius-format-minimessage: true`), BubbleChat converts the legacy tokens to MiniMessage tags:

- `{MESSAGE}` → `<message>`
- `{USERNAME}` → `<username>`
- `{DISPLAYNAME}` → `<displayname>`
- `{NICKNAME}` → `<nickname>`
- `{PREFIX}` → `<prefix>`
- `{SUFFIX}` → `<suffix>`
- `{GROUP}` → `<group>`
- `{WORLD}` → `<world>`
- `{WORLDNAME}` → `<worldname>`
- `{SHORTWORLDNAME}` → `<shortworldname>`

### 3) PlaceholderAPI

If PlaceholderAPI is installed:

- BubbleChat expands placeholders in formats automatically.
- BubbleChat can optionally expand `%placeholders%` in player-typed messages:

```yml
placeholders:
  enabled-in-player-messages: true
  permission: "bubblechat.placeholders"
```

#### Built-in BubbleChat PlaceholderAPI expansion

BubbleChat registers its own PlaceholderAPI identifier: `bubblechat`

Available placeholders:

- `%bubblechat_channel%` — player’s current channel (`global`, `local`, `staff`)
- `%bubblechat_muted%` — `true/false`
- `%bubblechat_muted_until%` — epoch ms (`0` if not muted)
- `%bubblechat_socialspy%` — `true/false`
- `%bubblechat_ignored_count%` — number of ignored players

---

## Rules engine (automation)

Rules live in `plugins/BubbleChat/rules.yml` and can match either:

- `scope: chat` — normal chat messages
- `scope: command` — the full command line including `/`

Each rule:

- has an `id`
- has a Java regex `match`
- contains a list of ordered `actions`

Supported action types (from the shipped template):

- `cooldown`
- `reply`
- `replace`
- `cancel`
- `silent_cancel`
- `run_command_console`
- `run_command_player`
- `notify_staff` (and optionally Discord)
- `log`

Variables available in rule messages/commands:

- `{player}` `{uuid}` `{message}`
- `{1}..{9}` regex capture groups
- `{remaining_s}` / `{remaining_ms}` (only when cooldown blocks)

Reload after editing rules: `/bchat reload`

---

## Configuration reference

This is a “what to look for” map of `config.yml` (not every line repeated).

### `chat.*`

- `chat.mode`: `channels` or `radius`
- `chat.format`: legacy `&` format line
- `chat.radius`: radius distance in blocks (radius mode)
- `chat.radius-format-minimessage`: treat radius format as MiniMessage
- `chat.local-radius-blocks`: used for local channel recipients
- `chat.channel-prefixes.*`: channel mode prefixes

Player message parsing:

- `chat.message.allow-player-minimessage`
- `chat.message.escape-without-permission`
- `chat.message.strip-legacy-section-colors`
- `chat.message.max-length`

Extras:

- `chat.item.*`: `[item]` hover support
- `chat.remove.*`: clickable `[x]` message removal
- `chat.clear.*`: `/clearchat` behavior
- `chat.me.*`: `/me` format

### `mentions.*`

- `mentions.enabled`
- `mentions.highlight-format`
- `mentions.sound.*`

### `private-messages.*`

- `private-messages.enabled`
- `private-messages.format-to-sender`
- `private-messages.format-to-target`

### `placeholders.*`

- `placeholders.enabled-in-player-messages`
- `placeholders.permission`

### `anti-spam.*`

- `anti-spam.enabled`
- `anti-spam.cooldown-ms`
- duplicate detection window + caps ratio controls

### `filters.*`

- `filters.enabled`
- `filters.blocked-words`
- `filters.regex-replacements`
- `filters.block-links-without-permission`
- `filters.link-permission`

### `rules.*`

- `rules.enabled`
- `rules.file`
- `rules.staff-alerts-enabled`

### `discordsrv.*`

- `discordsrv.enabled`
- `discordsrv.alert-channel-id`
- `discordsrv.alert-prefix`

### `spy.commands.*`

- `spy.commands.enabled`

### `announcements.*`

- `announcements.enabled`
- `announcements.opt-out.*`
- `announcements.join.motd.*` and other join actions (title/actionbar/sound/run-commands)

---

## FAQ / Troubleshooting

### “My staff channel isn’t visible / I can’t switch to staff chat.”

- Grant `bubblechat.channel.staff`.

### “Players can’t use MiniMessage tags in chat.”

- Ensure `chat.message.allow-player-minimessage: true`.
- Grant `bubblechat.format`.
- If you want them to use colors, also grant `bubblechat.color`.

### “%placeholders% typed by players aren’t expanding.”

- Install PlaceholderAPI.
- Set `placeholders.enabled-in-player-messages: true`.
- Grant `bubblechat.placeholders`.

### “Prefix/suffix/group placeholders don’t work.”

- Prefix/suffix/group is best-effort via PlaceholderAPI:
  - `%vault_prefix%` / `%vault_suffix%`
  - `%luckperms_primary_group%`
- Ensure you have PlaceholderAPI + a compatible placeholder source (e.g., Vault + permissions plugin, LuckPerms).

### “Click-to-remove `[x]` doesn’t delete messages.”

- Message deletion is best-effort and depends on Paper signed-message deletion support.
- Ensure `chat.remove.enabled: true` and the moderator has `chat.remove.permission` (default `bubblechat.moderation.remove`).

### “Global chat mute blocks too much / too little.”

- Players with `bubblechat.bypass.chatmute` can still talk.
- `moderation.mute-hides-system-messages` controls whether join/quit/death are hidden while chat mute is enabled.

### “A player can’t PM someone.”

- Target may have PMs blocked via `/toggle pm`.
- Staff can bypass with `bubblechat.bypass.pmblock`.

