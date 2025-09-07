# SoloSkies Bug Fixes

- Added explicit `InventoryHolder` checks and null guards for menu clicks to prevent NPEs.
- Temporary time/weather tasks now cancelled on plugin disable and on new overrides.
- Messages now resolved through language files; avoids missing-string NPEs and supports i18n.
- Warm-up movement checks operate on block changes only, reducing event spam.
- Action bar and chat messaging use MiniMessage components, preventing `NoClassDefFoundError` on Spigot.
