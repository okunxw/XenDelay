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

- Or use commands:
  ```
  /xendelay lag <player>
  /xendelay unlag <player>
  /xendelay unlagall
  /xendelay reload
  /xendelay language <code>
  ```

---

## ⚙️ Configuration

XenDelay is fully customizable through YAML files.  
Below is a sample `menu.yml` with a beautiful gradient title and fully arranged layout:

```yaml name=menu.yml
menu:
  title: "&#F45454C&#EE545Ao&#E85460n&#E25466t&#DC546Dr&#D65473o&#CA547Fl &#C55485P&#BF548Ba&#B95491n&#B35497e&#AD549El"
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
        - ""
        - "&#8d99ae■ &7Player management"
        - ""
      actions:
        left: "toggle_unlag"
        right: "toggle_lag"
    unlagall:
      slot: 47
      material: BARRIER
      display: "&#f75c47&l■ Remove lag from all"
      lore:
        - ""
        - "&#e86c7aRemove lag from all players"
        - ""
        - "&#ffcfa6&l! &#f0e68c&lWARNING: &#f5c378&lUse with caution"
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
        - "&#d2c6f5(Changes apply instantly)"
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
        - "&#bfcfd2Go to previous page"
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
        - "&#bfcfd2Go to next page"
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

---

## 🐞 Feedback & Support

- [GitHub Issues](https://github.com/okunxw/XenDelay/issues) — for bugs and suggestions
- Telegram: _t.me/okunivaxx_

---

## 📜 License

[MIT](LICENSE)

---

_Made with ❤️ by [okunivaxx](https://github.com/okunxw)_