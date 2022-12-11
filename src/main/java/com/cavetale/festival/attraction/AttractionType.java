package com.cavetale.festival.attraction;

import lombok.RequiredArgsConstructor;
import org.bukkit.map.MapCursor;

@RequiredArgsConstructor
public enum AttractionType {
    DUMMY("DUM", DummyAttraction.class, DummyAttraction::new),
    REPEAT_MELODY("MEL", RepeatMelodyAttraction.class, RepeatMelodyAttraction::new),
    SHOOT_TARGET("SHT", ShootTargetAttraction.class, ShootTargetAttraction::new),
    FIND_SPIDERS("SPI", FindSpidersAttraction.class, FindSpidersAttraction::new),
    OPEN_CHEST("CHE", OpenChestAttraction.class, OpenChestAttraction::new),
    FIND_BLOCKS("FBL", FindBlocksAttraction.class, FindBlocksAttraction::new),
    RACE("RAC", RaceAttraction.class, RaceAttraction::new),
    MUSIC_HERO("MHE", MusicHeroAttraction.class, MusicHeroAttraction::new),
    POSTER("POS", PosterAttraction.class, PosterAttraction::new),
    SNOWBALL_FIGHT("SNF", SnowballFightAttraction.class, SnowballFightAttraction::new),
    ZOMBIE_FIGHT("ZMB", ZombieFightAttraction.class, ZombieFightAttraction::new),
    MEMORY("MEM", MemoryAttraction.class, MemoryAttraction::new),
    FIND_GHOSTS("GHO", FindGhostsAttraction.class, FindGhostsAttraction::new),
    CARVE_PUMPKIN("PUM", CarvePumpkinAttraction.class, CarvePumpkinAttraction::new),
    PET_PILE("PET", PetPileAttraction.class, PetPileAttraction::new),
    ARCHERY("ARC", ArcheryAttraction.class, ArcheryAttraction::new),
    SHOOT_SNOWMEN("SSM", ShootSnowmenAttraction.class, ShootSnowmenAttraction::new),
    TRADE_CHAIN("TRC", TradeChainAttraction.class, TradeChainAttraction::new),
    PARKOUR("PRK", ParkourAttraction.class, ParkourAttraction::new),
    TIC_TAC_TOE("TTT", TicTacToeAttraction.class, TicTacToeAttraction::new),
    ;

    public final String shortcut;
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
        case TRADE_CHAIN -> MapCursor.Type.WHITE_CROSS;
        default -> MapCursor.Type.RED_X;
        };
    }
}
