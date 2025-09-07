# SoloSkies Upgrade Notes

## 0.2.0-SNAPSHOT
- Built against Paper API **1.20.6** and Adventure MiniMessage 4.14.0.
- Requires Java 21 (Paper 1.20.6 minimum). Source remains Java 17 compatible.
- All messages now pulled from `lang/messages_<code>.yml` via new `Msg` helper.
- Added warm-up and cooldown handling through `ApplyService` with bypass perms:
  - `soloskies.bypass.warmup`
  - `soloskies.bypass.cooldown`
- Temporary overrides and GUI actions now send action-bar pulses and respect cooldown/warm-up.
- MiniMessage library is shaded into the final jar; no external dependency required.
- plugin.yml `api-version` updated to `1.20`.

### Migration
- Replace old jars with the new build; ensure server runs Java 21+.
- Existing configuration and player data files remain compatible.
