package com.cavetale.festival.booth;

import com.cavetale.festival.Festival;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.attraction.FindSpidersAttraction;
import com.cavetale.festival.attraction.Music;
import com.cavetale.festival.attraction.MusicHeroAttraction;
import com.cavetale.festival.attraction.RepeatMelodyAttraction;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * Static information for Attractions.
 */
@Getter
public enum MidnightEstates implements Booth {
    // Music Hero 8
    MOUNTAIN_HERO(AttractionType.MUSIC_HERO,
                  null, Component.text("Thanks for visiting me up here."
                                       + " Want to learn a new melody?"),
                  Mytems.HALLOWEEN_TOKEN_2,
                  a -> ((MusicHeroAttraction) a).setMusic(Music.ODE_TO_JOY)),
    GLASS_PALADIN_HERO(AttractionType.MUSIC_HERO,
                       null, Component.text("Nothing like a nice tune to"
                                            + " brighten your spirits."
                                            + " Care to learn it?"),
                       Mytems.HALLOWEEN_TOKEN_2,
                       a -> ((MusicHeroAttraction) a).setMusic(Music.FOX_DU_HAST_DIE_GANS_GESTOHLEN)),
    MOTH_DEN_MUSIC_HERO(AttractionType.MUSIC_HERO,
                        null, Component.text("I always play this melody for our esteemed guests!"
                                             + " Care to learn it?"),
                        Mytems.HALLOWEEN_TOKEN_2,
                        a -> ((MusicHeroAttraction) a).setMusic(Music.HANSCHEN_KLEIN)),
    LOVE_CRYME_MUSIC(AttractionType.MUSIC_HERO,
                     null, null,
                     Mytems.HALLOWEEN_TOKEN_2,
                     a -> ((MusicHeroAttraction) a).setMusic(Music.EINE_KLEINE_NACHTMUSIK)),
    FLARE_DANCER_MUSIC(AttractionType.MUSIC_HERO,
                       null, null,
                       Mytems.HALLOWEEN_TOKEN_2,
                       a -> ((MusicHeroAttraction) a).setMusic(Music.FRERE_JACQUES)),
    OWO_TREE_MUSIC(AttractionType.MUSIC_HERO,
                   null, null,
                   Mytems.HALLOWEEN_TOKEN_2,
                   a -> ((MusicHeroAttraction) a).setMusic(Music.HAPPY_BIRTHDAY)),
    BRENPETER_MUSIC_HERO(AttractionType.MUSIC_HERO,
                         null, null,
                         Mytems.HALLOWEEN_TOKEN_2,
                         a -> ((MusicHeroAttraction) a).setMusic(Music.ALLE_MEINE_ENTCHEN)),
    ADIS_MUSIC(AttractionType.MUSIC_HERO,
               null, null,
               Mytems.HALLOWEEN_TOKEN_2,
               a -> {
                   ((MusicHeroAttraction) a).setMusic(Music.KOROBENIKI);
               }),

    // Repeat Melody 12
    COFFEE_SONG(AttractionType.REPEAT_MELODY,
                null, null,
                null, a -> ((RepeatMelodyAttraction) a).set(Instrument.PIANO, 1)),
    DRACO_WOLF_SONG(AttractionType.REPEAT_MELODY,
                    null, null,
                    null, a -> ((RepeatMelodyAttraction) a).set(Instrument.BELL, 1)),
    HAWK_MELODY(AttractionType.REPEAT_MELODY,
                null, null,
                null, a -> ((RepeatMelodyAttraction) a).set(Instrument.FLUTE, 1)),
    ARCANTOS_MELODY(AttractionType.REPEAT_MELODY,
                    null, null,
                    Mytems.HALLOWEEN_TOKEN_2,
                    a -> ((RepeatMelodyAttraction) a).set(Instrument.CHIME, 1)),
    CRITTER_MELODY(AttractionType.REPEAT_MELODY,
                   null, null,
                   null, a -> ((RepeatMelodyAttraction) a).set(Instrument.XYLOPHONE, 1)),
    FAST_PUPPY_MELODY(AttractionType.REPEAT_MELODY,
                      null, null,
                      null, a -> ((RepeatMelodyAttraction) a).set(Instrument.IRON_XYLOPHONE, 1)),
    YYONGI_MELODY(AttractionType.REPEAT_MELODY,
                  null, null,
                  null, a -> ((RepeatMelodyAttraction) a).set(Instrument.COW_BELL, 1)),
    ABYSMAL_VOID_MELODY(AttractionType.REPEAT_MELODY,
                        null, null,
                        null, a -> ((RepeatMelodyAttraction) a).set(Instrument.BIT, 1)),
    CEDRIC_MELODY(AttractionType.REPEAT_MELODY,
                  null, null,
                  null, a -> ((RepeatMelodyAttraction) a).set(Instrument.BANJO, 1)),
    AIRONT_MELODY(AttractionType.REPEAT_MELODY,
                  null, null,
                  null, a -> ((RepeatMelodyAttraction) a).set(Instrument.PLING, 1)),
    ROXY_CAT_MELODY(AttractionType.REPEAT_MELODY,
                    null, null,
                    null, a -> ((RepeatMelodyAttraction) a).set(Instrument.BASS_GUITAR, 1)),
    PILLOW_MELODY(AttractionType.REPEAT_MELODY,
                  null, null,
                  Mytems.HALLOWEEN_TOKEN_2,
                  a -> ((RepeatMelodyAttraction) a).set(Instrument.GUITAR, 1)),

    // Shoot Target
    SHOOTING_PYRAMID(AttractionType.SHOOT_TARGET,
                     Component.text("Pyramid Shooting Gallery", DARK_RED),
                     Component.text("Targets will appear on each level of this pyramid."
                                    + " Use the Televator to hit them all with an arrow."
                                    + " Don't forget about the ghasts!"),
                     Mytems.HALLOWEEN_TOKEN_2,
                     null),
    VERANDA_TARGETS(AttractionType.SHOOT_TARGET,
                    null, Component.text("Go on my backyard veranda and shoot"
                                         + " all the targets with a bow and arrow!"),
                    Mytems.HALLOWEEN_TOKEN_2,
                    null),
    VIXEN_SHOOTING(AttractionType.SHOOT_TARGET,
                   null, null,
                   null, null),
    CANADIAN_JELLY_SHOOTING(AttractionType.SHOOT_TARGET,
                            null, null,
                            null, null),
    LIVV_SHOOTING(AttractionType.SHOOT_TARGET,
                  null, Component.text("Shoot the targets that appear around"
                                       + " the circle over there"
                                       + " with a bow and arrow!"),
                  null, null),
    KOONTZY_SHOOTING(AttractionType.SHOOT_TARGET,
                     null, Component.text("Shoot the targets that appear in the frontyard"
                                          + " with a bow and arrow!"),
                     Mytems.HALLOWEEN_TOKEN_2, null),
    KITSUNE_SHOOTING(AttractionType.SHOOT_TARGET,
                     null, Component.text("Ghasts and target blocks spawn in these"
                                          + " halls. Shoot them all with your"
                                          + " bow and arrow!"),
                     null, null),
    RAT_CRYPT_SHOOTING(AttractionType.SHOOT_TARGET,
                       null, null,
                       null, null),
    CRYSTAL_DEAR_WITCH_MANOR_SHOOTING(AttractionType.SHOOT_TARGET,
                                      null, null,
                                      null, null),
    CALCULA_CLOWN_SHOOTING(AttractionType.SHOOT_TARGET,
                           null, null,
                           null, null),
    CORGI_HURRICANE_SHOOTING(AttractionType.SHOOT_TARGET,
                             null, null,
                             Mytems.HALLOWEEN_TOKEN_2,
                             null),
    GROOVE_SOUL_GRAVEYARD_SHOOTING(AttractionType.SHOOT_TARGET,
                                   null, null,
                                   null, null),

    // Find Spiders
    SPIDER_MANSION(AttractionType.FIND_SPIDERS,
                   null, null,
                   null, null),
    BLACKOUT_SPIDER_HOUSE(AttractionType.FIND_SPIDERS,
                          null, null,
                          null, null),
    COOKY_SPIDER_HUNT(AttractionType.FIND_SPIDERS,
                      null, null,
                      null, null),
    PEARLESQUE_SPIDERS(AttractionType.FIND_SPIDERS,
                       null, null,
                       Mytems.HALLOWEEN_TOKEN_2,
                       null),
    ADIS_SPIDER_HOUSE(AttractionType.FIND_SPIDERS,
                      null, null,
                      Mytems.HALLOWEEN_TOKEN_2,
                      a -> {
                          ((FindSpidersAttraction) a).setSearchTime(Duration.ofSeconds(80));
                      }),
    NOOOMYZ_SPIDER_HOUSE(AttractionType.FIND_SPIDERS,
                         null, null,
                         null, null),
    DMS_SPIDER_MANSION(AttractionType.FIND_SPIDERS,
                       null, null,
                       null, null),
    FOIVI_SPIDER_GARDEN(AttractionType.FIND_SPIDERS,
                        Component.text("Spider Garden", DARK_RED),
                        Component.text("Help, my home and garden are infested with spiders!"
                                       + " They come out one by one and make noise."
                                       + " Please find them all."),
                        Mytems.HALLOWEEN_TOKEN_2,
                        null),
    TEKKERSMON_SPIDER_HOUSE(AttractionType.FIND_SPIDERS,
                            null, null,
                            null, null),
    SPIRITUS_SPIDER_COTTAGE(AttractionType.FIND_SPIDERS,
                            null, null,
                            Mytems.HALLOWEEN_TOKEN_2,
                            null),
    BRIGHTER_GIANT_SPIDER(AttractionType.FIND_SPIDERS,
                          Component.text("Giant Spider", DARK_RED),
                          Component.text("I knew moving into a giant spider was a bad idea."
                                         + " Turns out this place is spider infested."
                                         + " Can you find them all?"),
                          null, null),
    PAPAMACI_SPIDERS(AttractionType.FIND_SPIDERS,
                     null, null,
                     null, null),

    // Find Blocks
    GHOST_TOWER(AttractionType.FIND_BLOCKS,
                null, null,
                Mytems.HALLOWEEN_TOKEN_2,
                null),
    HIDDEN_BLOCKS_TIX(AttractionType.FIND_BLOCKS,
                      null, null,
                      null, null),
    ARNOLD_HAUNTED_BLOCKS(AttractionType.FIND_BLOCKS,
                          null, null,
                          null, null),
    PYRO_GHOST_BLOCKS(AttractionType.FIND_BLOCKS,
                      null, null,
                      null, null),
    PUP_GHOST_BLOCKS(AttractionType.FIND_BLOCKS,
                     null, null,
                     null, null),
    LILLYPADDS_ORPHANAGE(AttractionType.FIND_BLOCKS,
                         null, Component.text("The orphanage is haunted!"
                                              + " a ghost keeps placing and rearranging blocks."
                                              + " Can you find them all?"),
                         null, null),
    NOT_NOT_ROB_GHOST_BLOCKS(AttractionType.FIND_BLOCKS,
                             null, null,
                             null, null),
    ADIS_GHOST_BLOCKS(AttractionType.FIND_BLOCKS,
                      null, null,
                      null, null),

    // Open Chest
    DRAGON_TOWER(AttractionType.OPEN_CHEST,
                 null, null,
                 Mytems.HALLOWEEN_TOKEN_2,
                 null),
    CHESTS_ON_HILLS(AttractionType.OPEN_CHEST,
                    null, null,
                    null, null),
    TOAST_CHESTS(AttractionType.OPEN_CHEST,
                 null, null,
                 null, null),
    HENDRIKS_CHESTS(AttractionType.OPEN_CHEST,
                    null, null,
                    null, null),
    BRINDLE_MANSION_CHESTS(AttractionType.OPEN_CHEST,
                           null, null,
                           null, null),
    GICHYU_CHESTS(AttractionType.OPEN_CHEST,
                  null, null,
                  null, null),
    POIVON_CHESTS(AttractionType.OPEN_CHEST,
                  null, null,
                  null, null),
    HYRULE_CHESTS(AttractionType.OPEN_CHEST,
                  null, null,
                  null, null),
    COOLJEFF_CHESTS(AttractionType.OPEN_CHEST,
                    null, null,
                    null, null),
    RINTAMAKI_CHESTS(AttractionType.OPEN_CHEST,
                     null, null,
                     null, null),

    // Race
    POOL_RACE(AttractionType.RACE,
              null, Component.text("Race me, once aroune the house,"
                                   + " counter clockwise!"
                                   + " We meet back here, haha!"),
              null, null),
    MELANTHIA_GRAVEYARD_RACE(AttractionType.RACE,
                             Component.text("Graveyard Race", DARK_RED),
                             Component.text("Let's race once around the graveyard,"
                                            + " counter clockwise,"
                                            + " then back here!"),
                             null, null),
    TECHNOLOGY_TENT_RACE(AttractionType.RACE,
                         null, Component.text("Race me once around the tent,"
                                              + " counter clockwise!"),
                         null, null),
    LORD_SHEEP_CHURCH_RACE(AttractionType.RACE,
                           null, Component.text("A race round the church,"
                                                + " counter clockwise?"
                                                + " Nobody's faster than me!"),
                           null, null);

    public static final Festival FESTIVAL = new Festival("midnight_estates",
                                                         "Halloween",
                                                         FestivalTheme.HALLOWEEN,
                                                         MidnightEstates::forName,
                                                         MidnightEstates::onTotalCompletion,
                                                         null, null, null).festivalServer();

    private final String name; // Corresponds with area.name
    private final AttractionType type;
    private final Component displayName;
    private final Component description;
    private final Mytems reward;
    private final Consumer<Attraction> consumer;

    MidnightEstates(final AttractionType type,
          final Component displayName,
          final Component description,
          final Mytems reward,
          final Consumer<Attraction> consumer) {
        this.name = Stream.of(name().split("_"))
            .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase())
            .collect(Collectors.joining(""));
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.reward = reward;
        this.consumer = consumer;
    }

    public static MidnightEstates forName(String n) {
        for (MidnightEstates booth : MidnightEstates.values()) {
            if (n.equals(booth.name)) return booth;
        }
        return null;
    }

    @Override
    public Component format(String txt) {
        return text(txt, GOLD);
    }

    @Override
    public void onEnable(Attraction<?> attraction) {
        if (consumer != null) {
            consumer.accept(attraction);
        }
    }

    @Override
    public Festival getFestival() {
        return FESTIVAL;
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return Mytems.HALLOWEEN_TOKEN.createItemStack();
    }

    private static final List<List<ItemStack>> PRIZE_POOL =
    List.of(List.of(Mytems.CANDY_CORN.createItemStack(),
                    Mytems.CHOCOLATE_BAR.createItemStack(),
                    Mytems.LOLLIPOP.createItemStack(),
                    Mytems.ORANGE_CANDY.createItemStack()),
            List.of(new ItemStack(Material.DIAMOND, 2),
                    new ItemStack(Material.DIAMOND, 4),
                    new ItemStack(Material.DIAMOND, 8),
                    new ItemStack(Material.DIAMOND, 16),
                    new ItemStack(Material.DIAMOND, 32),
                    new ItemStack(Material.DIAMOND, 64)),
            List.of(new ItemStack(Material.EMERALD),
                    new ItemStack(Material.COD),
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
