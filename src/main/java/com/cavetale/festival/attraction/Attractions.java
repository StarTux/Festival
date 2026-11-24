package com.cavetale.festival.attraction;

import com.cavetale.core.util.Json;
import com.cavetale.festival.FestivalPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

@RequiredArgsConstructor
public final class Attractions {
    private final FestivalPlugin plugin;
    private final Map<String, AttractionConfiguration> configurations = new HashMap<>();
    private final Map<String, Attraction> attractions = new HashMap<>();
    private final Set<String> fileNames = new HashSet<>();
    @Getter private File attractionsFolder;
    @Getter private File stateFolder;
    private BukkitTask tickTask;

    public void enable() {
        attractionsFolder = new File(plugin.getDataFolder(), "attractions");
        stateFolder = new File(plugin.getDataFolder(), "state");
        attractionsFolder.mkdirs();
        stateFolder.mkdirs();
        load();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void disable() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (final String name : List.copyOf(configurations.keySet())) unregister(name);
    }

    public void reload() {
        for (final String name : List.copyOf(fileNames)) unregister(name);
        load();
    }

    public void load() {
        final File[] files = attractionsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) throw new IllegalStateException("Cannot list " + attractionsFolder);
        for (final File file : files) {
            try {
                final AttractionConfiguration config = Json.load(file, AttractionConfiguration.class);
                if (!file.getName().equals(config.getName() + ".json")) throw new IllegalArgumentException("Name must match filename");
                register(config);
                fileNames.add(config.getName());
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Loading " + file, e);
            }
        }
    }

    public void register(final AttractionConfiguration config) {
        final String name = config.getName();
        if (!isValidName(name) || config.getType() == null
            || config.getMain() == null || config.getWorld() == null) throw new IllegalArgumentException("Invalid attraction configuration");
        if (configurations.containsKey(name)) throw new IllegalArgumentException("Duplicate attraction " + name);
        if (Bukkit.getWorld(config.getWorld()) != null) activate(config);
        configurations.put(name, config);
    }

    private void activate(final AttractionConfiguration config) {
        final Attraction attraction = config.getType().make(config);
        try {
            if (config.getInitializer() != null) config.getInitializer().accept(attraction);
            attraction.enable();
            attraction.load();
        } catch (RuntimeException e) {
            attraction.disable();
            throw e;
        }
        attractions.put(config.getName(), attraction);
    }

    private void deactivate(final String name) {
        final Attraction attraction = attractions.remove(name);
        if (attraction == null) return;
        try {
            attraction.save();
        } finally {
            attraction.disable();
        }
    }

    public void unregister(final String name) {
        deactivate(name);
        configurations.remove(name);
        fileNames.remove(name);
    }

    public void storeConfiguration(final AttractionConfiguration config) {
        final String name = config.getName();
        final AttractionConfiguration previous = configurations.get(name);
        if (previous != null && !fileNames.contains(name)) throw new IllegalArgumentException("Client owns " + name);
        unregister(name);
        try {
            register(config);
            Json.save(new File(attractionsFolder, name + ".json"), config, true);
            fileNames.add(name);
        } catch (RuntimeException e) {
            unregister(name);
            if (previous != null) {
                register(previous);
                fileNames.add(name);
            }
            throw e;
        }
    }

    public static boolean isValidName(final String name) {
        return name != null && name.matches("[A-Za-z0-9_.:-]+") && !name.equals(".") && !name.equals("..");
    }

    public List<AttractionConfiguration> getConfigurations() {
        return List.copyOf(configurations.values());
    }

    public AttractionConfiguration getConfiguration(final String name) {
        return configurations.get(name);
    }

    public Component getDisplayName(final AttractionConfiguration config) {
        final Attraction attraction = getAttraction(config.getName());
        if (attraction != null) return attraction.getDisplayName();
        final Component configuredName = config.readTextSetting("display_name");
        return configuredName != null
            ? configuredName
            : config.resolveTheme(message -> { }).format(config.getType().getDisplayName());
    }

    public boolean hasConfiguration(final String name) {
        return configurations.containsKey(name);
    }

    public void loadWorld(final World world) {
        for (final AttractionConfiguration config : configurations.values()) {
            if (!config.getWorld().equals(world.getName()) || attractions.containsKey(config.getName())) continue;
            try {
                activate(config);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, config.getName(), e);
            }
        }
    }

    public void unloadWorld(final World world) {
        for (final Attraction attraction : getAttractionsIn(world)) deactivate(attraction.getName());
    }

    private void tick() {
        for (final Attraction attraction : List.copyOf(attractions.values())) {
            if (!attraction.isPlaying()) continue;
            try {
                attraction.tick();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, attraction.getName(), e);
            }
        }
    }

    public <T extends Attraction> void applyActiveAttraction(final Class<T> type, final Consumer<T> consumer) {
        for (final Attraction attraction : attractions.values()) {
            if (attraction.isPlaying() && type.isInstance(attraction)) consumer.accept(type.cast(attraction));
        }
    }

    public void applyAttraction(final Location location, final Consumer<Attraction> consumer) {
        getAttractionsAt(location).forEach(consumer);
    }

    public void applyAttraction(final Block block, final Consumer<Attraction> consumer) {
        applyAttraction(block.getLocation(), consumer);
    }

    public List<Attraction> getLoadedAttractions() {
        return List.copyOf(attractions.values());
    }

    public Attraction getAttraction(final String name) {
        return attractions.get(name);
    }

    public Attraction getAttraction(final World world, final String name) {
        final Attraction attraction = attractions.get(name);
        return attraction != null && attraction.getWorld().equals(world) ? attraction : null;
    }

    public List<Attraction> getAttractionsAt(final Location location) {
        return attractions.values().stream().filter(a -> a.isInArea(location)).toList();
    }

    public List<Attraction> getAttractionsIn(final World world) {
        return attractions.values().stream().filter(a -> a.getWorld().equals(world)).toList();
    }
}
