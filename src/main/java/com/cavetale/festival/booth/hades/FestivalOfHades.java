package com.cavetale.festival.booth.hades;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.booth.Booth;
import com.cavetale.mytems.Mytems;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
                                    null); // gui
        }
        return festival;
    }

    public Booth makeBooth(final String name) {
        return new HadesBooth();
    }

    public void onComplete(Player player) {
    }
}