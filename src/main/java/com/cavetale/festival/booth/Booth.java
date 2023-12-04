package com.cavetale.festival.booth;

import com.cavetale.festival.Festival;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.attraction.Music;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.music.Melody;
import com.cavetale.resident.ZoneType;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A "booth" is the static, plugin provided part of the Attraction
 * Configuration for one individual attraction.
 */
public interface Booth {
    AttractionType getType();

    /**
     * Called when the attraction has finished loading and will
     * enable.  This is an opportunity to set some values, or set
     * disabled to true.
     */
    default void onEnable(Attraction<?> attraction) { }

    default void onDisable(Attraction<?> attraction) { }

    default Component format(String txt) {
        return getFestival().getTheme().format(txt);
    }

    Festival getFestival();

    ItemStack getFirstCompletionReward();

    List<List<ItemStack>> getPrizePool();

    /**
     * 4 items will be picked at random to be added as decoration
     * around the main prize.
     */
    List<ItemStack> getBonusPrizePool();

    default ItemStack getEntryFee() {
        return Mytems.RUBY.createItemStack();
    }

    default ZoneType getZoneType() {
        return getFestival().getTheme().getZoneType();
    }

    default Melody getFailMelody() {
        return Music.DECKED_OUT.melody;
    }

    default Melody getSuccessMelody() {
        return Music.TREASURE.melody;
    }

    default Component getMapCursorCaption() {
        return null;
    }

    default void onFirstCompletion(Attraction attraction, Player player) { }

    default void onRegularCompletion(Attraction attraction, Player player) { }
}
