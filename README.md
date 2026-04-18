# DataGuard
> Never Lose Your Progress Again.

DataGuard is a PaperMC survival plugin that automatically saves player and server data snapshots — so if your server ever rolls back, corrupts, or something just goes wrong, nothing is permanently lost.

Built after experiencing a real rollback on a survival server that wiped weeks of progress. DataGuard was made to make sure that never happens again.

---

## ✨ Features

- 📥 **Auto-save on join** — Snapshot taken the moment a player connects
- 📤 **Auto-save on quit** — Snapshot taken when a player disconnects
- ⏱️ **Auto-save every 30 minutes** — Runs silently in the background
- 🗄️ **Server-wide snapshots** — Captures all online players at once
- 🔒 **Anti-duplication** — Only admins can restore data, preventing item exploits
- ⚙️ **Fully configurable** — Interval, max snapshots, messages, and more
- 🪶 **Zero dependencies** — Just drop in the jar and go

---

## 📋 What Gets Saved

| Data | Saved |
|---|---|
| Inventory (all 36 slots) | ✅ |
| Armor (helmet, chest, legs, boots) | ✅ |
| Offhand item | ✅ |
| Ender chest | ✅ |
| XP level & progress | ✅ |
| Health & hunger | ✅ |
| Active potion effects | ✅ |
| Player location & world | ✅ |
| Game mode | ✅ |

---

## 🔧 Installation

1. Download the latest `DataGuard.jar` from [Releases](../../releases)
2. Drop it into your server's `/plugins` folder
3. Restart your server
4. Done — DataGuard starts protecting your data immediately

**Requirements:**
- PaperMC `1.21.11`
- Java `21+`

---

## 📖 Commands

### Admin / OP (`dataguard.admin`)

| Command | Description |
|---|---|
| `/data server create [label]` | Take a full server-wide snapshot |
| `/data server delete <label>` | Delete a server snapshot |
| `/data server automatic <true/false>` | Toggle server auto-saving |
| `/data player create [player] [label]` | Snapshot a specific player |
| `/data player delete <player> <label>` | Delete a player's snapshot |
| `/data load player <player> <snapshot>` | Restore a player's data |
| `/data load server <snapshot>` | Restore all online players from a snapshot |
| `/data list server` | List all server snapshots |
| `/data list player <name>` | List a player's snapshots |
| `/data reload` | Reload config.yml live |

### Members (`dataguard.player`)

| Command | Description |
|---|---|
| `/data player create [label]` | Manually save your own data |
| `/data player delete <label>` | Delete one of your own snapshots |
| `/data automatic <true/false>` | Toggle your personal auto-saving |
| `/data list player` | View your own snapshots |

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `dataguard.admin` | Full access to all commands | OP |
| `dataguard.player` | Access to member-level commands | Everyone |

---

## ⚙️ Configuration

Located at `plugins/DataGuard/config.yml` after first launch.

```yaml
# Auto-save interval in minutes
auto-save-interval: 30

# Whether automatic server-wide saving is enabled by default
server-auto-save: true

# Maximum snapshots to keep per player (0 = unlimited)
max-player-snapshots: 20

# Maximum server snapshots to keep (0 = unlimited)
max-server-snapshots: 10
```

---

## 🔒 Why Can't Members Use `/data load`?

Allowing players to restore their own data on demand would open the door to item duplication. For example, a player could drop Netherite gear on the ground, load an old snapshot to get it back in their inventory, then pick up the dropped gear — duplicating it for free. To keep servers fair and healthy, only admins can restore data.

---

## 📁 Snapshot Storage

Snapshots are stored in `plugins/DataGuard/snapshots/` organized like this:

```
snapshots/
├── players/
│   └── <uuid>/
│       └── <timestamp>/
│           ├── player_stats.properties
│           ├── inventory.dat
│           ├── armor.dat
│           ├── offhand.dat
│           ├── enderchest.dat
│           └── potions.dat
└── server/
    └── <timestamp>/
        ├── server_meta.properties
        └── <uuid>/
            └── (same as above)
```

Old snapshots are automatically pruned based on your configured limits.

---

## 📜 Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

---

## 📄 License

This project is licensed under the **MIT License** — see [LICENSE.txt](LICENSE.txt) for details.  
Free to use on any server, modify, and redistribute.

---

## 👤 Author

Made by **mctrrmlol**  
Built for PaperMC survival servers that deserve better data protection.
