package com.cavetale.festival.booth.halloween2024;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.booth.Booth;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Halloween2024 {
    private static Halloween2024 instance;
    private Festival festival;

    private Halloween2024() { }

    public static Halloween2024 halloween2024() {
        if (instance == null) instance = new Halloween2024();
        return instance;
    }

    public Festival getFestival() {
        if (festival == null) {
            festival = new Festival("halloween2024", // worldName
                                    "Halloween", // areasFileName
                                    FestivalTheme.HALLOWEEN,
                                    this::makeBooth,
                                    this::onComplete,
                                    null, // load
                                    null, // unload
                                    null) // gui
                .creativeServer()
                .festivalServer();
        }
        return festival;
    }

    public Booth makeBooth(final String name) {
        return new Halloween2024Booth();
    }

    public void onComplete(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kite member HalloweenComplete2024 " + player.getName());
    }
}
