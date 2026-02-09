# Changelog

All notable changes to this project will be documented in this file.

The format is based on *Keep a Changelog*, and this project follows semantic versioning.

## [1.0.3] - 2026-02-09
### Added
- Admin toggle for command spying: `/commandspy` (alias: `/cmdspy`).

### Changed
- Command spy now respects a per-player toggle (permission still required).
- Reduced method complexity in `ChatService` and `RuleEngine` via helper extraction.

## [1.0.2] - 2026-01-13
### Added
- ChatColor2 compatibility: apply player chat colour (and `&#RRGGBB`) when BubbleChat is rendering messages.
- New config toggles under `chat.chatcolor2.*` to control ChatColor2 integration.
- Unit tests for `TextSanitizer` hex/code handling.

## [1.0.1] - 2026-01-13
### Changed
- Made `rules.yml` substantially easier to understand for non-regex users (plain-English guide, safer YAML tips, clearer structure).
- Improved FAQ auto-responses (Discord / land claiming / trust) with cleaner formatting and more actionable buttons.
- Clarified `config.yml` comments for rules and regex replacements.

## [1.0.0] - 2026-01-13
### Added
- Initial release.
