package com.cavetale.festival.booth;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.mytems.Mytems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Halloween 2022.
 */
public final class TwinGourds implements Booth {
    public static final TwinGourds INSTANCE = new TwinGourds();
    public static final Festival FESTIVAL = new Festival("twin_gourds",
                                                         "Halloween",
                                                         FestivalTheme.HALLOWEEN,
                                                         s -> INSTANCE,
                                                         TwinGourds::onTotalCompletion,
                                                         null, null);

    @Override
    public AttractionType getType() {
        return null;
    }

    @Override
    public Festival getFestival() {
        return FESTIVAL;
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return Mytems.HALLOWEEN_TOKEN_2.createItemStack();
    }

    private static final List<List<ItemStack>> PRIZE_POOL =
        List.of(List.of(Mytems.CANDY_CORN.createItemStack(),
                        Mytems.CHOCOLATE_BAR.createItemStack(),
                        Mytems.LOLLIPOP.createItemStack(),
                        Mytems.ORANGE_CANDY.createItemStack()),
                List.of(Mytems.RUBY.createItemStack(1),
                        Mytems.RUBY.createItemStack(3),
                        Mytems.RUBY.createItemStack(5),
                        Mytems.RUBY.createItemStack(7)),
                List.of(new ItemStack(Material.EMERALD),
                        new ItemStack(Material.COD),
                        new ItemStack(Material.DIAMOND),
                        new ItemStack(Material.POISONOUS_POTATO)));

    @Override
    public List<List<ItemStack>> getPrizePool() {
        return PRIZE_POOL;
    }

    @Override
    public List<ItemStack> getBonusPrizePool() {
        return List.of(getEntryFee(),
                       new ItemStack(Material.DIAMOND),
                       Mytems.CANDY_CORN.createItemStack(),
                       Mytems.CHOCOLATE_BAR.createItemStack(),
                       Mytems.LOLLIPOP.createItemStack(),
                       Mytems.ORANGE_CANDY.createItemStack());
    }

    private static void onTotalCompletion(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kite member HalloweenComplete2022 " + player.getName());
    }
}
