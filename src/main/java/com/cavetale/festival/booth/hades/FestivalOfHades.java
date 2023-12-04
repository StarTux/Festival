package com.cavetale.festival.booth.hades;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.booth.Booth;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * Festival of Hades 2023.
 */
public final class FestivalOfHades {
    private static FestivalOfHades instance;
    private Festival festival;

    private FestivalOfHades() { }

    public static FestivalOfHades festivalOfHades() {
        if (instance == null) instance = new FestivalOfHades();
        return instance;
    }

    public Festival getFestival() {
        if (festival == null) {
            festival = new Festival("halloweencavebuild2023", // worldName
                                    "Halloween", // areasFileName
                                    FestivalTheme.HALLOWEEN,
                                    this::makeBooth,
                                    this::onComplete,
                                    null, // load
                                    null, // unload
                                    null) // gui
                .festivalServer();
        }
        return festival;
    }

    public Booth makeBooth(final String name) {
        return new HadesBooth();
    }

    public void onComplete(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kite member HalloweenComplete2023 " + player.getName());
    }
}
