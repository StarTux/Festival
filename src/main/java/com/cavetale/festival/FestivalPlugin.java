package com.cavetale.festival;

import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.booth.TestingBooth;
import com.cavetale.festival.booth.WintersHearth;
import com.cavetale.festival.gui.Gui;
import java.io.File;
import java.util.ArrayList;
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
        loadFestival(WintersHearth.FESTIVAL);
    }

    private void loadFestival(Festival festival) {
        festivalMap.put(festival.getWorldName(), festival);
        festival.load();
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

    public Attraction<?> getAttraction(Location location) {
        Festival festival = festivalMap.get(location.getWorld().getName());
        if (festival == null) return null;
        return festival.getAttraction(location);
    }

    public List<Attraction<?>> getAttractions(World world) {
        Festival festival = festivalMap.get(world.getName());
        if (festival == null) return List.of();
        return festival.getAttractions();
    }

    public List<Attraction<?>> getAllAttractions() {
        List<Attraction<?>> result = new ArrayList<>();
        for (Festival festival : festivalMap.values()) {
            result.addAll(festival.getAttractions());
        }
        return result;
    }

    public Festival getFestival(World world) {
        return festivalMap.get(world.getName());
    }
}
