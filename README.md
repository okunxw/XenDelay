# XenDelay

**XenDelay** is a flexible and beautiful Minecraft plugin for managing artificial player lag with an intuitive admin GUI.  
Easily apply or remove "lag" to players, unlag everyone at once, switch languages, and reload configuration on the fly — all without server restarts!

---

## ✨ Features

- **Modern GUI menu** for easy lag management
- **One-click** lag or unlag for any player
- **Bulk unlag** all players at once
- **Instant config and menu reload** (no server restart needed)
- **Fully customizable** menu and messages via YAML
- **Multi-language support** (RU, EN, FR — and more via messages)
- **Action cooldowns** to prevent admin spam
- **Colorful hex chat formatting** in all texts
- **Pagination** — supports 100+ online players in the GUI
- **Permissions support** for safe usage by staff
- **Crash command & menu action** — force-crash a player's client by entity, sign, or payload packets (admin fun & moderation!)

---

## 🚀 Installation

1. [Download the latest XenDelay JAR from GitHub Releases](https://github.com/okunxw/XenDelay/releases)
2. Place `XenDelay-<version>.jar` into your server's `/plugins/` folder
3. Restart your server or use `/reload`

---

## 🕹️ Usage

- Open the lag management GUI:
  ```
  /xendelay gui
  ```
- Click player heads to apply/remove lag
- Use the bottom menu for:
  - 🟥 **Unlag All** — remove lag from everyone
  - 🟨 **Reload** — instantly reload configs
  - 🟦 **Close** — close the GUI
- Use **SHIFT+LMB/SHIFT+RMB/MIDDLE** on a player in the GUI to crash their client (see below)

- Or use commands:
  ```
  /xendelay lag <player>
  /xendelay unlag <player>
  /xendelay unlagall
  /xendelay reload
  /xendelay language <code>
  /xendelay crash <player> <entity|sign|payload>
  ```

### 🧨 Crash Command & GUI Action

- Crash a player's game with:
  ```
  /xendelay crash <player> <entity|sign|payload>
  ```
  - `entity` — spawns a special entity with a long name to force a client crash (most versions)
  - `sign` — places a sign with a very long string (works on 1.16+ clients)
  - `payload` — sends a heavy/invalid packet payload (most effective, but may not work on all versions/clients)
- In the GUI, use:
  - **SHIFT+LMB** on a player: crash via entity
  - **SHIFT+RMB** on a player: crash via sign
  - **MIDDLE CLICK** on a player: crash via payload packet

> ⚠️ **Warning:** Not all crash methods may work on every client or Minecraft version.  
> The `payload` method is currently the most effective and works on most modern clients, but is not guaranteed.  
> Crash features are for moderation, pranking, or testing only. Use responsibly!

---

## ⚙️ Configuration

XenDelay is fully customizable through YAML files.  
Below is a sample `menu.yml` with a beautiful gradient title and fully arranged layout:

```yaml name=menu.yml
menu:
  title: "&#F45454X&#EE545Ae&#E85460n&#E25466D&#DC546Ee&#D65473l&#CA547Fy &#C55485C&#BF548Bo&#B95491n&#B35497t&#AD549Er&#A754A4o&#A154AAl &#9B54B0P&#9554B6a&#8F54BCn&#8954C2e&#8354C8l"
  size: 54
  items:
    players:
      slot_start: 10
      slot_end: 44
      material: PLAYER_HEAD
      display: "%player_colored%"
      lore:
        - ""
        - "&#bfcfd2Status: %status%"
        - ""
        - "&#fdc886&lLMB &7→ &#b6ffe6Remove lag"
        - "&#fdc886&lRMB &7→ &#f75c47Apply lag"
        - "&#fdc886&lSHIFT+RMB &7→ &#f75c47Crash with signs"
        - "&#fdc886&lSHIFT+LMB &7→ &#f75c47Crash with entity"
        - "&#fdc886&lMIDDLE CLICK &7→ &#f75c47Crash with payload packets"
        - ""
        - "&#8d99ae■ &7Player control"
        - ""
      actions:
        left: "toggle_unlag"
        right: "toggle_lag"
        shift_left: "crash_entity"
        shift_right: "crash_sign"
        middle: "crash_payload"
    unlagall:
      slot: 47
      material: BARRIER
      display: "&#f75c47&l■ Remove lag from all"
      lore:
        - ""
        - "&#e86c7aRemove lag from all players"
        - ""
        - "&#ffcfa6&l! &#f0e68c&lWARNING: &#f5c378&lBe careful"
        - ""
      actions:
        left: "unlag_all"
    reload:
      slot: 49
      material: COMMAND_BLOCK
      display: "&#f2d857&l■ Reload"
      lore:
        - ""
        - "&#a1eaf7Reload config and menu"
        - "&#d2c6f5(Changes apply immediately)"
        - ""
      actions:
        left: "reload_config"
    close:
      slot: 51
      material: BARRIER
      display: "&#a2a2a2&l■ Close"
      lore:
        - ""
        - "&#d8d8d8Click to exit"
        - ""
      actions:
        left: "close"
    page_prev:
      slot: 0
      material: ARROW
      display: "&#f6f6f6← Previous page"
      lore:
        - ""
        - "&#bfcfd2Scroll back"
        - ""
      actions:
        left: "page_prev"
    page_info:
      slot: 4
      material: PAPER
      display: "&#b1bdd4Page %page% of %total%"
      lore:
        - ""
        - "&#bfcfd2Current page"
        - ""
    page_next:
      slot: 8
      material: ARROW
      display: "&#f6f6f6→ Next page"
      lore:
        - ""
        - "&#bfcfd2Scroll forward"
        - ""
      actions:
        left: "page_next"
```

- **Menu layout:**  
  Customize `menu.yml` for GUI size, button placement, materials, and texts.
- **Messages & languages:**  
  All texts are in `messages_<lang>.yml` (e.g. `messages_en.yml`).  
  Add/translate files for more languages!

---

## 📝 Permissions

| Permission              | Description                       |
|-------------------------|-----------------------------------|
| `xendelay.use`          | Access to the GUI and commands    |
| `xendelay.lag`          | Apply lag to players              |
| `xendelay.unlag`        | Remove lag from players           |
| `xendelay.reload`       | Reload config and menu            |
| `xendelay.unlagall`     | Unlag all players                 |
| `xendelay.crash`        | Crash a player's client           |

---

## 🌍 Language Support

- 🇷🇺 Russian (`messages_ru.yml`)
- 🇬🇧 English (`messages_en.yml`)
- 🇫🇷 French (`messages_fr.yml`)
- Want to help translate? PRs welcome!

---

## ❓ FAQ

**Q:** What versions are supported?  
**A:** Minecraft 1.16+ (Paper/Spigot), Java 8+.

**Q:** Can I customize menu colors and items?  
**A:** Yes, fully! Edit `menu.yml` and use hex color codes.

**Q:** How do I add a new language?  
**A:** Copy `messages_en.yml`, rename to `messages_<code>.yml`, and translate.

**Q:** Does this impact server performance?  
**A:** No, all actions are event-driven and optimized.

**Q:** What does the `/xendelay crash` command do?  
**A:** It forces the target player's client to crash using one of three methods (entity, sign, or payload). Not all methods are guaranteed to work on all Minecraft versions or clients.

---

## 🐞 Feedback & Support

- [GitHub Issues](https://github.com/okunxw/XenDelay/issues) — for bugs and suggestions
- Telegram: _t.me/okunivaxx_

---

## 📜 License

[MIT](LICENSE)

---

_Made with ❤️ by [okunivaxx](https://github.com/okunxw)_