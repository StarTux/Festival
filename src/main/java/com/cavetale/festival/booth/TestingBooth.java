package com.cavetale.festival.booth;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.attraction.AttractionType;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class TestingBooth implements Booth {
    public static final TestingBooth INSTANCE = new TestingBooth();
    public static final Festival FESTIVAL = new Festival("festival_test",
                                                         "Testing",
                                                         FestivalTheme.TESTING,
                                                         s -> INSTANCE,
                                                         TestingBooth::onComplete,
                                                         null, null, null).betaServer();

    @Override
    public AttractionType getType() {
        return null;
    }

    @Override
    public Festival getFestival() {
        return FESTIVAL;
    }

    @Override
    public ItemStack getEntryFee() {
        return new ItemStack(Material.DIAMOND);
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return new ItemStack(Material.DIAMOND);
    }

    private static final List<List<ItemStack>> PRIZE_POOL =
        List.of(List.of(new ItemStack(Material.DIAMOND)));

    @Override
    public List<List<ItemStack>> getPrizePool() {
        return PRIZE_POOL;
    }

    private static final List<ItemStack> BONUS_PRIZE_POOL =
        List.of(new ItemStack(Material.EMERALD));

    @Override
    public List<ItemStack> getBonusPrizePool() {
        return BONUS_PRIZE_POOL;
    }

    private static void onComplete(Player player) {
    }
}
