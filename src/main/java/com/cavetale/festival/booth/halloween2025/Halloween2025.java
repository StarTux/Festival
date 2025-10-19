package com.cavetale.festival.booth.halloween2025;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.booth.Booth;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Halloween2025 {
    private static Halloween2025 instance;
    private Festival festival;

    private Halloween2025() { }

    public static Halloween2025 halloween2025() {
        if (instance == null) instance = new Halloween2025();
        return instance;
    }

    public Festival getFestival() {
        if (festival == null) {
            festival = new Festival("halloween2025", // worldName
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
        return new Halloween2025Booth();
    }

    public void onComplete(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kite member HalloweenComplete2025 " + player.getName());
    }
}
