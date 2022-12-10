package com.cavetale.festival.attraction;

import lombok.RequiredArgsConstructor;
import org.bukkit.map.MapCursor;

@RequiredArgsConstructor
public enum AttractionType {
    DUMMY(DummyAttraction.class, DummyAttraction::new),
    REPEAT_MELODY(RepeatMelodyAttraction.class, RepeatMelodyAttraction::new),
    SHOOT_TARGET(ShootTargetAttraction.class, ShootTargetAttraction::new),
    FIND_SPIDERS(FindSpidersAttraction.class, FindSpidersAttraction::new),
    OPEN_CHEST(OpenChestAttraction.class, OpenChestAttraction::new),
    FIND_BLOCKS(FindBlocksAttraction.class, FindBlocksAttraction::new),
    RACE(RaceAttraction.class, RaceAttraction::new),
    MUSIC_HERO(MusicHeroAttraction.class, MusicHeroAttraction::new),
    POSTER(PosterAttraction.class, PosterAttraction::new),
    SNOWBALL_FIGHT(SnowballFightAttraction.class, SnowballFightAttraction::new),
    ZOMBIE_FIGHT(ZombieFightAttraction.class, ZombieFightAttraction::new),
    MEMORY(MemoryAttraction.class, MemoryAttraction::new),
    FIND_GHOSTS(FindGhostsAttraction.class, FindGhostsAttraction::new),
    CARVE_PUMPKIN(CarvePumpkinAttraction.class, CarvePumpkinAttraction::new),
    PET_PILE(PetPileAttraction.class, PetPileAttraction::new),
    ARCHERY(ArcheryAttraction.class, ArcheryAttraction::new),
    SHOOT_SNOWMEN(ShootSnowmenAttraction.class, ShootSnowmenAttraction::new),
    TRADE_CHAIN(TradeChainAttraction.class, TradeChainAttraction::new),
    PARKOUR(ParkourAttraction.class, ParkourAttraction::new),
    ;

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
