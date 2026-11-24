package com.cavetale.festival.attraction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.map.MapCursor;

@RequiredArgsConstructor
public enum AttractionType {
    DUMMY("DUM", "Dummy Attraction", DummyAttraction.class, DummyAttraction::new),
    REPEAT_MELODY("MEL", "Play the Melody", RepeatMelodyAttraction.class, RepeatMelodyAttraction::new),
    SHOOT_TARGET("SHT", "Bull's Eye!", ShootTargetAttraction.class, ShootTargetAttraction::new),
    FIND_SPIDERS("SPI", "Spider Hunt", FindSpidersAttraction.class, FindSpidersAttraction::new),
    OPEN_CHEST("CHE", "Chest Game", OpenChestAttraction.class, OpenChestAttraction::new),
    FIND_BLOCKS("FBL", "Hidden Blocks", FindBlocksAttraction.class, FindBlocksAttraction::new),
    RACE("RAC", "Race Me", RaceAttraction.class, RaceAttraction::new),
    MUSIC_HERO("MHE", "Music Hero", MusicHeroAttraction.class, MusicHeroAttraction::new),
    POSTER("POS", "Picture Puzzle", PosterAttraction.class, PosterAttraction::new),
    SNOWBALL_FIGHT("SNF", "Snowball Fight", SnowballFightAttraction.class, SnowballFightAttraction::new),
    ZOMBIE_FIGHT("ZMB", "Zombie Defense", ZombieFightAttraction.class, ZombieFightAttraction::new),
    MEMORY("MEM", "Memory Game", MemoryAttraction.class, MemoryAttraction::new),
    FIND_GHOSTS("GHO", "Ghostbusters", FindGhostsAttraction.class, FindGhostsAttraction::new),
    CARVE_PUMPKIN("PUM", "Pumpkin Carving", CarvePumpkinAttraction.class, CarvePumpkinAttraction::new),
    PET_PILE("PET", "Pet Pile", PetPileAttraction.class, PetPileAttraction::new),
    ARCHERY("ARC", "Archery", ArcheryAttraction.class, ArcheryAttraction::new),
    SHOOT_SNOWMEN("SSM", "Snow Golem Attack", ShootSnowmenAttraction.class, ShootSnowmenAttraction::new),
    TRADE_CHAIN("TRC", "Trade Chain", TradeChainAttraction.class, TradeChainAttraction::new),
    PARKOUR("PRK", "Parkour", ParkourAttraction.class, ParkourAttraction::new),
    TIC_TAC_TOE("TTT", "Tic Tac Toe", TicTacToeAttraction.class, TicTacToeAttraction::new),
    GUESS_NUMBER("GNU", "Guess the Number", GuessNumberAttraction.class, GuessNumberAttraction::new),
    TNT_SWEEP("TNT", "TNT Sweep", TNTSweepAttraction.class, TNTSweepAttraction::new),
    FISH("FIS", "Candy Fishing", FishAttraction.class, FishAttraction::new),
    ;

    public final String shortcut;
    @Getter private final String displayName;
    public final Class<? extends Attraction> type;
    private final AttractionConstructor ctor;

    public static AttractionType forName(String name) {
        for (AttractionType it : AttractionType.values()) {
            if (name.toUpperCase().equals(it.name())) return it;
        }
        return DUMMY;
    }

    public static AttractionType of(Attraction attraction) {
        for (AttractionType it : AttractionType.values()) {
            if (it.type.isInstance(attraction)) return it;
        }
        return null;
    }

    @FunctionalInterface
    public interface AttractionConstructor {
        Attraction make(AttractionConfiguration config);
    }

    public Attraction make(AttractionConfiguration config) {
        return ctor.make(config);
    }

    public MapCursor.Type getMapCursorIcon() {
        return switch (this) {
        case TRADE_CHAIN -> MapCursor.Type.TARGET_X;
        default -> MapCursor.Type.RED_X;
        };
    }
}
