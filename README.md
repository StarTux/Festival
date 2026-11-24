# Festival

Festival plugin.

## Standalone attractions

Use a cuboid selection in the attraction's world for each of the first three commands.
For example, select the entire game area, then the NPC block, then each chest in turn.

    /festadm attraction create open_chest treasure
    /festadm attraction setarea npc
    /festadm attraction addarea chest
    /festadm attraction rmarea chest 1

Stand inside the attraction to edit or highlight it. Use `setarea main` to replace the main region. List indices start at 1.
Omit the index to remove a named area or its entire list. Use
`highlight` under the same subcommand to see the saved regions.
Use `/festadm attraction debug` in chat or the server console to show debug
lines for all loaded attractions. The console can also pass an attraction name.

Use `/festadm attraction list [page]` to browse attractions. Hover over a name
for its type, world, and approximate coordinates. Click it or run
`/festadm attraction teleport <name>` to teleport.

Each definition lives in `plugins/Festival/attractions/<name>.json`. Its fields
are `name`, `type`, `world`, `main`, `areas`, `areaLists`, `settings`, and the optional `theme`.
Use the enum name for the type in JSON (for example, `OPEN_CHEST`). Each cuboid
has inclusive block coordinates `ax`, `ay`, `az`, `bx`, `by`, and `bz`.
Set `"theme": "halloween"` to select a preset. Names are case-insensitive:
`DEFAULT`, `TESTING`, `HALLOWEEN`, `CHRISTMAS`, and `VALENTINE`. Omitting the
field uses `DEFAULT`. Client plugins can call `config.setCustomTheme(theme)`
before registration to override the preset with a theme built using
`FestivalTheme.builder()` or a preset's `getTheme().toBuilder()`.

After editing a file manually, run `/festadm reload`.

Client plugins can supply an `AttractionConfiguration` to
`FestivalPlugin.getInstance().register(config)` and unregister it by name.
Set its optional initializer to customize the attraction before each activation.
Names are unique across worlds and configuration sources and can include dots
and colons, such as `winter:spawn.parkour`. Player progress is
stored in `players/<uuid>.json` and attraction runtime state in `state/<name>.json`.
