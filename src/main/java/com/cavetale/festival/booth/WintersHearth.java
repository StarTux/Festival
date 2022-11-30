package com.cavetale.festival.booth;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.mytems.Mytems;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.festival.FestivalPlugin.plugin;

/**
 * Christmas 2022.
 */
public final class WintersHearth implements Booth {
    public static final WintersHearth INSTANCE = new WintersHearth();
    public static final Festival FESTIVAL = new Festival("christmas2022",
                                                         "Xmas",
                                                         FestivalTheme.CHRISTMAS,
                                                         s -> INSTANCE,
                                                         WintersHearth::onComplete,
                                                         WintersHearth::onLoad,
                                                         WintersHearth::onUnload);
    private int day;
    private static int currentDay;

    @RequiredArgsConstructor
    public enum XmasPresent {
        PAINTBRUSH(Mytems.RED_PAINTBRUSH,
                   "Paintbrush",
                   "My house needs a new coat of paint. I asked Santa for a brush for Christmas."),
        BROOM(Mytems.WITCH_BROOM,
              "Broom",
              "This place needs a good clean. Too bad my last broom broke on me."),
        DICE(Mytems.DICE,
             "Dice",
             "I can never decide how to sort my chests. If only I had an item to choose for me..."),
        WARM_SOCKS(Mytems.SANTA_BOOTS,
                   "Warm Socks",
                   "I wish I could go out visit my parents, but my feet always get cold."),
        DIAMOND_RING(Mytems.WEDDING_RING,
                     "Diamond Ring",
                     "I can't believe I lost my diamond ring. What am I supposed to do now?"),
        ARMCHAIR(Mytems.RED_ARMCHAIR,
                 "Comfy Armchair",
                 "I hope Santa brings me something comfortable to sit in and relax."),
        KNITTED_HAT(Mytems.KNITTED_BEANIE,
                    "Knitted Hat",
                    "The cold outside hurts my ears. Have you seen my hat?"),
        CHRISTMAS_BALL(Mytems.BLUE_CHRISTMAS_BALL, // 8
                       "Christmas Ball",
                       "I'm decorating my Christmas tree, but something's missing..."),
        SNOW_SHOVEL(Mytems.SNOW_SHOVEL, // 9
                    "Snow Shovel",
                    "I desperately need to clear my front yard of all this new snow!"),
        PRESENT(Mytems.CHRISTMAS_TOKEN, // 10
                "Wrapped Present",
                "I'm so late on my Christmas shopping this year!"
                + " I'm missing a wrapped present for my sister."),
        SANTAS_LIST(Mytems.MAGIC_MAP, // 11
                    "Santa's List",
                    "Can you keep a secret? I'll be Santa this year,"
                    + " but I lost my list somewhere..."),
        ONION(Mytems.ORANGE_ONION, // 12
              "Festive Onion",
              "I'm making my delicious Christmas stew but ran out of onions!"),
        DODO_EGG(Mytems.EASTER_EGG, // 13
                 "Dodo Egg",
                 "For my next cake, I need the egg from a dodo."
                 + " Unfortunately, not many dodos have been around here lately."),
        SCARY_PUMPKIN(Mytems.KINGS_PUMPKIN, // 14
                      "Scary Pumpkin",
                      "I want to make an even better snowman,"
                      + " but my last pumpkin with a face got lost around Halloween."),
        CHOCOLATE(Mytems.CHOCOLATE_BAR, // 15
                  "Chocolate",
                  "Chocolate's all sold out. I'm having a craving!"),
        PALETTE(Mytems.PAINT_PALETTE, // 16
                "Paint Palette",
                "How am I going to finish this masterpiece without my paint palette?"),
        GLOBE(Mytems.EARTH, // 17
              "Globe",
              "I need a present for a friend who is obsessed with geography."
              + " She's so hard to buy presents for..."),
        TRAFFIC_LIGHT(Mytems.TRAFFIC_LIGHT, // 18
                      "Traffic Lights",
                      "My kid has a toy car, firetruck, miniature street corner..."
                      + " what else could I give them for Christmas?"),
        GOBLET(Mytems.GOLDEN_CUP, // 19
               "Goblet",
               "I'm looking for a nice decorative present for my friend"
               + " that makes him feel like a winner."),
        SHADES(Mytems.BLACK_SUNGLASSES, // 20
               "Shades",
               "I can't go out. The snow makes me go snowblind."
               + " It's a real pickle."),
        CLAMSHELL(Mytems.CLAMSHELL, // 21
                  "Clamshell",
                  "What else can I use to decorate my fancy aquarium?"),
        LOLLY(Mytems.LOLLIPOP, // 22
              "Lolly",
              "Someone stole my lollipop!"),
        TOY_SWORD(Mytems.CAPTAINS_CUTLASS, // 23
                  "Toy Sword",
                  "My boy loves those pirate movies. What would be the perfect present?"),
        CAT(Mytems.PIC_CAT, // 24
            "Tuxedo Cat",
            "I want to gift my children a new pet. What would be best?"),
        STAR(Mytems.STAR, // 25
             "Christmas Star",
             "My Christmas tree is missing something... at the top."
             + " got any ideas by chance?");

        public final Mytems mytems;
        public final String itemName;
        public final String request;
        private ItemStack itemStack;

        public ItemStack makeItemStack() {
            if (itemStack == null) {
                itemStack = mytems.createIcon(List.of(FestivalTheme.CHRISTMAS.format(itemName)));
                itemStack.editMeta(meta -> {
                        meta.addItemFlags(ItemFlag.values());
                        for (Enchantment ench : Enchantment.values()) {
                            meta.removeEnchant(ench);
                        }
                    });
            }
            return itemStack.clone();
        }
    }

    private static void onLoad() {
        final int year = 2022;
        final YearMonth month = YearMonth.of(year, Month.DECEMBER);
        if (NetworkServer.current() == NetworkServer.CREATIVE) {
            currentDay = 25;
        } else {
            LocalDate now = LocalDate.now();
            if (now.getYear() != year) {
                currentDay = 0;
            } else {
                for (int i = 25; i > 0; i -= 1) {
                    LocalDate date = month.atDay(i);
                    if (now.isAfter(date) || now.isEqual(date)) {
                        currentDay = i;
                        break;
                    }
                }
            }
        }
        FESTIVAL.logInfo("CurrentDay = " + currentDay);
    }

    private static void onUnload() {
    }

    @Override
    public AttractionType getType() {
        return null;
    }

    @Override
    public void onEnable(Attraction<?> attraction) {
        attraction.getIntKeys().add("day");
        if (attraction.getFirstArea().getRaw() != null
            && attraction.getFirstArea().getRaw().get("day") instanceof Number number) {
            this.day = number.intValue();
        }
        if (day < 1 || day > 25) {
            attraction.debugLine("Invalid day: " + day);
        }
        if (currentDay < day) {
            attraction.setDisabled(true);
        }
    }

    @Override
    public void onDisable(Attraction<?> attraction) {
        day = 0;
    }

    @Override
    public Festival getFestival() {
        return FESTIVAL;
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return Mytems.CHRISTMAS_TOKEN.createItemStack();
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
                new ItemStack(Material.DIAMOND));

    @Override
    public List<ItemStack> getBonusPrizePool() {
        return BONUS_PRIZE_POOL;
    }

    private static void onComplete(Player player) {
        final String cmd = "kite member ChristmasComplete2022 " + player.getName();
        plugin().getLogger().info("[WintersHearth] Executing command: " + cmd);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}