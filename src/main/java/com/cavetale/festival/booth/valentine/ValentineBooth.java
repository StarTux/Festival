package com.cavetale.festival.booth.valentine;

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

public final class ValentineBooth implements Booth {
    public static final Festival FESTIVAL = new Festival("valentines2023", // worldName
                                                         "Valentine", // areasFileName
                                                         FestivalTheme.TESTING,
                                                         ValentineBooth::makeBooth,
                                                         ValentineBooth::onComplete,
                                                         null, // load
                                                         null, // unload
                                                         null) // gui
        .festivalServer();

    private static Booth makeBooth(String name) {
        return new ValentineBooth();
    }

    private static void onComplete(Player player) { }

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
        return Mytems.RUBY.createItemStack();
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return Mytems.LOVE_LETTER.createItemStack();
    }

    private static final List<List<ItemStack>> PRIZE_POOL =
        List.of(List.of(Mytems.KITTY_COIN.createItemStack(1)),
                List.of(Mytems.RUBY.createItemStack(2),
                        Mytems.RUBY.createItemStack(4),
                        Mytems.RUBY.createItemStack(8)),
                List.of(new ItemStack(Material.EMERALD),
                        new ItemStack(Material.COD),
                        new ItemStack(Material.DIAMOND),
                        new ItemStack(Material.POISONOUS_POTATO)));

    @Override
    public List<List<ItemStack>> getPrizePool() {
        return PRIZE_POOL;
    }

    private static final List<ItemStack> BONUS_PRIZE_POOL =
        List.of(Mytems.RUBY.createItemStack(1),
                Mytems.COPPER_COIN.createItemStack(1),
                Mytems.SILVER_COIN.createItemStack(1),
                new ItemStack(Material.DIAMOND),
                new ItemStack(Material.EMERALD),
                new ItemStack(Material.COPPER_INGOT),
                new ItemStack(Material.GOLD_INGOT),
                new ItemStack(Material.IRON_INGOT),
                new ItemStack(Material.LAPIS_LAZULI));

    @Override
    public List<ItemStack> getBonusPrizePool() {
        return BONUS_PRIZE_POOL;
    }
}
