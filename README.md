# RivalSMP Plugin

Starter implementation for Paper 1.21.1 / Java 21.

## Build
1. Install JDK 21 and Maven 3.9+.
2. Run `mvn clean package`.
3. Copy `target/RivalSMP-1.0.0.jar` into your server's `plugins` folder.
4. Restart the server.

Commands:
- `/rivalsmp help`
- `/rivalsmp list`
- `/rivalsmp give <player> <weapon>`
- `/rivalsmp reload`\n- `/rivalsmp announce <message>` — broadcasts a moving violet-to-cyan gradient in chat and an animated title/subtitle on every online player's screen.

Admin permission: `rivalsmp.admin`

## Current implementation status
Implemented in this source package:
- Plugin bootstrap, command registration, tab completion and YAML config.
- Custom weapon identity stored with PersistentDataContainer.
- Admin weapon give command for all seven weapon IDs.
- Blood Sword Soul Feast hit healing.
- Configurable DoomsHammer hit counter and Thunderfall-style lightning/damage trigger.
- Basic Lifesteal max-health gain/loss on player kills/deaths.\n- Animated gradient announcements to chat and player screens.

Not yet implemented in this starter:
- Custom ritual structures and persistent ritual progress.
- Recipe editor / custom crafting GUI.
- Ally management and friendly-fire protection.
- Blood Sword Heartbreaker target max-health effect and cooldown.
- Full Scythe, KillerKnife, AutoBow, CrossBow and BanHammer abilities.
- SQLite persistence, revive-book flow, resource-pack auto-hosting and GUI menus.

This is deliberately labelled a starter build, not a finished production release. Test on a backup server before using it publicly. The plugin API dependency currently targets Paper 1.21.1; compatibility with 1.21.1–26.1 has NOT been verified. Later Minecraft versions may need a separate build/runtime target.
