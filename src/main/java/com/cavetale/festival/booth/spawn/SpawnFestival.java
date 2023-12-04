package com.cavetale.festival.booth.spawn;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.booth.Booth;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * The festival at spawn.
 */
public final class SpawnFestival {
    private static SpawnFestival instance;
    private Festival festival;

    private SpawnFestival() { }

    public static SpawnFestival spawnFestival() {
        if (instance == null) instance = new SpawnFestival();
        return instance;
    }

    public Festival getFestival() {
        if (festival == null) {
            festival = new Festival("spawn", // worldName
                                    "SpawnFestival", // areasFileName
                                    FestivalTheme.CHRISTMAS,
                                    this::makeBooth,
                                    this::onComplete,
                                    null, // load
                                    null, // unload
                                    null) // gui
                .hubServer();
        }
        return festival;
    }

    public Booth makeBooth(final String name) {
        return new SpawnBooth();
    }

    public void onComplete(Player player) { }
}
