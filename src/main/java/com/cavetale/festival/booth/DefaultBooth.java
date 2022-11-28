package com.cavetale.festival.booth;

import com.cavetale.festival.Festival;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.mytems.Mytems;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;

public final class DefaultBooth implements Booth {
    @Override
    public AttractionType getType() {
        return null;
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    @Override
    public Component getDescription() {
        return null;
    }

    @Override
    public Mytems getReward() {
        return null;
    }

    @Override
    public void apply(Attraction attraction) { }

    @Override
    public Component format(String txt) {
        return text(txt);
    }

    @Override
    public Festival getFestival() {
        return null;
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return new ItemStack(Material.DIAMOND);
    }

    @Override
    public List<List<ItemStack>> getPrizePool() {
        return List.of(List.of(new ItemStack(Material.DIAMOND)));
    }

    @Override
    public List<ItemStack> getBonusPrizePool() {
        return List.of(new ItemStack(Material.DIAMOND));
    }
}
