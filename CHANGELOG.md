# Changelog

All notable changes to DataGuard will be documented in this file.

---

## [1.0.0] — April 18, 2026

### 🎉 Initial Release

#### Added
- Automatic player snapshot on join
- Automatic player snapshot on quit
- Automatic server-wide snapshot every 30 minutes (configurable interval)
- Per-player auto-save toggle via `/data automatic <true/false>`
- Server-wide auto-save toggle via `/data server automatic <true/false>`
- Player snapshot saves: inventory, armor, offhand, ender chest, XP, health, hunger, potion effects, location, and game mode
- Server snapshot captures all online players at once with server metadata (world time, player count)
- Manual snapshot creation for players and admins
- Manual snapshot deletion for players (own) and admins (any)
- Admin-only snapshot loading for both player and server snapshots
- Snapshot listing for players (own) and admins (any player or server)
- Full tab completion on all subcommands including snapshot names
- Automatic snapshot pruning based on configurable max limits
- `dataguard.admin` permission for OP-level commands
- `dataguard.player` permission for member-level commands
- Anti-duplication protection — `/data load` restricted to admins only
- Zero external dependencies
- Fully configurable `config.yml` with custom messages, prefix, interval, and snapshot limits
- Live config reload via `/data reload`
- PaperMC 1.21.11 support
- Java 21 support

---

*Upcoming in future versions:*
- GUI-based snapshot browser
- Per-world snapshot support
- Discord webhook notifications on auto-save
- Offline player snapshot loading
- Snapshot size display in list command
