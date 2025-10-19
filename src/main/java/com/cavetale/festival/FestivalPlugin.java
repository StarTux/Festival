package com.cavetale.festival;

import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.booth.MidnightEstates;
import com.cavetale.festival.booth.TestingBooth;
import com.cavetale.festival.booth.TwinGourds;
import com.cavetale.festival.booth.WintersHearth;
import com.cavetale.festival.booth.valentine.ValentineBooth;
import com.cavetale.festival.gui.Gui;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import static com.cavetale.festival.booth.hades.FestivalOfHades.festivalOfHades;
import static com.cavetale.festival.booth.halloween2024.Halloween2024.halloween2024;
import static com.cavetale.festival.booth.halloween2025.Halloween2025.halloween2025;
import static com.cavetale.festival.booth.spawn.SpawnFestival.spawnFestival;

public final class FestivalPlugin extends JavaPlugin {
    @Getter protected static FestivalPlugin instance;
    protected final FestivalCommand festivalCommand = new FestivalCommand(this);
    protected final FestivalAdminCommand festivalAdminCommand = new FestivalAdminCommand(this);
    protected final EventListener eventListener = new EventListener(this);
    @Getter protected final Map<String, Festival> festivalMap = new HashMap<>();
    @Getter protected File attractionsFolder;
    @Getter protected File playersFolder;

    @Override
    public void onEnable() {
        instance = this;
        festivalCommand.enable();
        festivalAdminCommand.enable();
        eventListener.enable();
        attractionsFolder = new File(getDataFolder(), "attractions");
        playersFolder = new File(getDataFolder(), "players");
        attractionsFolder.mkdirs();
        playersFolder.mkdirs();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 0L, 0L);
        Gui.enable(this);
        loadFestivals();
    }

    @Override
    public void onDisable() {
        clearFestivals();
    }

    public void loadFestivals() {
        loadFestival(TestingBooth.FESTIVAL);
        loadFestival(MidnightEstates.FESTIVAL);
        loadFestival(WintersHearth.FESTIVAL);
        loadFestival(ValentineBooth.FESTIVAL);
        loadFestival(TwinGourds.FESTIVAL);
        loadFestival(festivalOfHades().getFestival());
        loadFestival(spawnFestival().getFestival());
        loadFestival(halloween2024().getFestival());
        loadFestival(halloween2025().getFestival());
    }

    private void loadFestival(Festival festival) {
        if (!festival.isOnThisServer()) return;
        try {
            festival.load();
        } catch (IllegalStateException ise) {
            getLogger().warning(festival.getWorldName() + ": " + ise.getMessage());
            return;
        }
        festivalMap.put(festival.getWorldName(), festival);
    }

    public void clearFestivals() {
        for (Festival festival : festivalMap.values()) {
            festival.unload();
        }
        festivalMap.clear();
    }

    private void tick() {
        for (Festival festival : festivalMap.values()) {
            festival.tick();
        }
    }

    protected <T extends Attraction> void applyActiveAttraction(Class<T> type, Consumer<T> consumer) {
        for (Festival festival : festivalMap.values()) {
            festival.applyActiveAttraction(type, consumer);
        }
    }

    protected void applyAttraction(Location location, Consumer<Attraction<?>> consumer) {
        Festival festival = getFestival(location.getWorld());
        if (festival == null) return;
        for (Attraction attraction : festival.attractionsMap.values()) {
            if (attraction.getMainArea().contains(location)) {
                consumer.accept(attraction);
            }
        }
    }

    protected void applyAttraction(Block block, Consumer<Attraction<?>> consumer) {
        Festival festival = getFestival(block.getWorld());
        if (festival == null) return;
        for (Attraction attraction : festival.attractionsMap.values()) {
            if (attraction.getMainArea().contains(block)) {
                consumer.accept(attraction);
            }
        }
    }

    public static FestivalPlugin plugin() {
        return instance;
    }

    public Attraction<?> getAttraction(World world, String attractionName) {
        Festival festival = festivalMap.get(world.getName());
        if (festival == null) return null;
        return festival.getAttraction(attractionName);
    }

    public List<Attraction<?>> getAttractionsAt(Location location) {
        Festival festival = festivalMap.get(location.getWorld().getName());
        if (festival == null) return List.of();
        return festival.getAttractionsAt(location);
    }

    public List<Attraction<?>> getAttractionsIn(World world) {
        Festival festival = festivalMap.get(world.getName());
        if (festival == null) return List.of();
        return festival.getAttractions();
    }

    public Festival getFestival(World world) {
        return festivalMap.get(world.getName());
    }
}
