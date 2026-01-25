# BubbleChat

BubbleChat is a modern, config-first chat plugin for **Paper 1.21+** designed to replace the “stack of chat plugins” approach.
It combines EssentialsChat-style proximity chat, ChatControl-style automation, and modern Adventure/MiniMessage formatting.

## Highlights
- **MiniMessage formatting** for chat + system messages
- **Chat modes**: `channels` (global/local/staff) or `radius` (Essentials-style proximity)
- **Private messages** `/msg`, `/reply` + **SocialSpy**
- **Moderation**: mute/unmute, ignore, global chat mute, clickable **message remove [x]** (Paper signed chat)
- **Anti-spam**: cooldown, duplicate detection, caps limiter
- **Filters**: blocked words, regex replacements, link blocking w/ permission
- **Rules engine (rules.yml)**: regex triggers for chat/commands with actions:
  - reply (bots/FAQ), replace, cancel/silent_cancel
  - cooldown gates
  - run commands as console/player
  - notify staff + log
- **Join announcements** with opt-out (`/announcements`) + `/motd`
- **QoL**: `/clearchat`, `/me`, `/toggle` for preferences
- Optional integrations: **PlaceholderAPI**, **DiscordSRV**

## Quick Start
1) Drop the jar into `plugins/` and restart
2) Edit `config.yml` and `rules.yml`
3) Reload with `/bchat reload`

## Permissions (common)
- `bubblechat.format`, `bubblechat.color`
- `bubblechat.moderation.mute`, `bubblechat.moderation.remove`
- `bubblechat.socialspy`
- `bubblechat.bypass.spam`, `bubblechat.bypass.filter`

## Notes
- The clickable `[x]` message removal relies on Paper’s signed chat deletion support.
