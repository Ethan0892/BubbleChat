BubbleChat is a packed, modern chat plugin for **Paper 1.21+** that aims to replace EssentialsChat + ChatControl-style setups.

Features:
- MiniMessage formatting for chat and system messages
- Two chat styles:
  - Channels: global/local/staff
  - Radius: Essentials-style proximity chat with shout/question behavior
- Private messages (/msg, /reply) + SocialSpy
- Moderation tools:
  - Mute/unmute, ignore, global chat mute
  - Clickable message removal: moderators can click an appended [x] to remove a chat line (Paper signed chat)
- Anti-spam: cooldown, duplicate window, caps limiting
- Filters: blocked words, regex replacements, link blocking
- Powerful rules engine (rules.yml): regex triggers for chat + commands
  - reply (FAQ/bot), replace, cancel/silent_cancel
  - cooldown, run commands (console/player), staff notify, logging
- Announcements system with opt-out + /motd
- QoL commands: /clearchat, /me, /toggle
- Optional integrations: PlaceholderAPI, DiscordSRV

Setup:
- Install the jar into /plugins and restart
- Configure config.yml + rules.yml
- Reload with /bchat reload

Paper signed chat note:
- The clickable [x] removal feature requires a server build that supports signed-message deletion.
