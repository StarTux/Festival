package com.cavetale.festival.booth.spawn;

import com.cavetale.festival.Festival;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.attraction.Music;
import com.cavetale.festival.booth.Booth;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.music.Melody;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class SpawnBooth implements Booth {
    @Override
    public AttractionType getType() {
        return null;
    }

    @Override
    public Component format(String txt) {
        return text(txt, GREEN);
    }

    @Override
    public Festival getFestival() {
        return SpawnFestival.spawnFestival().getFestival();
    }

    @Override
    public Melody getFailMelody() {
        return Music.GRINCH.melody;
    }

    @Override
    public Melody getSuccessMelody() {
        return Music.DECK_THE_HALLS.melody;
    }

    @Override
    public ItemStack getEntryFee() {
        return Mytems.RUBY.createItemStack();
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return new ItemStack(Mytems.RUBY.createItemStack(3));
    }

    @Override
    public List<List<ItemStack>> getPrizePool() {
        return List.of(List.of(Mytems.RUBY.createItemStack(2)));
    }

    @Override
    public List<ItemStack> getBonusPrizePool() {
        return List.of(new ItemStack(Material.DIAMOND));
    }

    @Override
    public void onFirstCompletion(Attraction attraction, Player player) {
        onCompletion(attraction, player);
    }

    @Override
    public void onRegularCompletion(Attraction attraction, Player player) {
        onCompletion(attraction, player);
    }

    private void onCompletion(Attraction attraction, Player player) {
        switch (attraction.getName()) {
        case "SnowballFight":
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "adventadmin unlock " + player.getName() + " 4");
            break;
        case "MerryChristmas":
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "adventadmin unlock " + player.getName() + " 8");
            break;
        default: break;
        }
    }
}
