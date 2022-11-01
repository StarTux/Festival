package com.cavetale.festival;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.booth.Booth;
import com.cavetale.festival.booth.DefaultBooth;
import com.cavetale.festival.session.Session;
import com.cavetale.festival.session.Sessions;
import com.cavetale.resident.PluginSpawn;
import com.cavetale.resident.ZoneType;
import com.cavetale.resident.save.Loc;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import static com.cavetale.festival.FestivalPlugin.plugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter @RequiredArgsConstructor
public final class Festival {
    protected static final String TOTAL_COMPLETION = "TotalCompletion";
    // ctor
    protected final String worldName;
    protected final String areasFileName;
    protected final ZoneType zoneType;
    protected final Function<String, Booth> boothFunction;
    protected final Consumer<Player> totalCompletionHandler;
    // data
    protected final Map<String, Attraction> attractionsMap = new HashMap<>();
    protected final Sessions sessions = new Sessions(this);
    protected PluginSpawn totalCompletionVillager;
    protected File saveFolder;
    protected File playersFolder;

    public void clear() {
        for (Attraction attraction : attractionsMap.values()) {
            try {
                attraction.save();
                attraction.disable();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        attractionsMap.clear();
        if (totalCompletionVillager != null) {
            totalCompletionVillager.unregister();
            totalCompletionVillager = null;
        }
        sessions.clear();
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    private Booth getBooth(String name) {
        return boothFunction.apply(name);
    }

    private void logWarn(String msg) {
        plugin().getLogger().warning("[" + worldName + "] " + msg);
    }

    private void logInfo(String msg) {
        plugin().getLogger().info("[" + worldName + "] " + msg);
    }

    public void load() {
        World world = getWorld();
        if (world == null) {
            logWarn("World not found: " + worldName);
            return;
        }
        saveFolder = new File(plugin().getAttractionsFolder(), worldName);
        saveFolder.mkdirs();
        playersFolder = new File(saveFolder, "players");
        playersFolder.mkdirs();
        AreasFile areasFile = AreasFile.load(world, areasFileName);
        if (areasFile == null) throw new IllegalStateException("Areas file not found: " + worldName + "/" + areasFileName);
        for (Map.Entry<String, List<Area>> entry : areasFile.areas.entrySet()) {
            String name = entry.getKey();
            if (name.equals(TOTAL_COMPLETION)) {
                Location location = entry.getValue().get(0).min.toCenterFloorLocation(world);
                this.totalCompletionVillager = PluginSpawn.register(plugin(), zoneType, Loc.of(location));
                this.totalCompletionVillager.setOnPlayerClick(this::clickTotalCompletionVillager);
                this.totalCompletionVillager.setOnMobSpawning(mob -> mob.setCollidable(false));
                continue;
            }
            final Booth booth = getBooth(name);
            if (booth == null) {
                logWarn(name + ": No Booth found!");
            }
            List<Area> areaList = entry.getValue();
            Attraction attraction = Attraction.of(this, name, areaList, booth != null ? booth : new DefaultBooth());
            if (attraction == null) {
                logWarn(name + ": No Attraction!");
            }
            try {
                attraction.enable();
                attraction.load();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            attractionsMap.put(name, attraction);
        }
        Map<AttractionType, Integer> counts = new EnumMap<>(AttractionType.class);
        for (AttractionType type : AttractionType.values()) counts.put(type, 0);
        for (Attraction attraction : attractionsMap.values()) {
            AttractionType type = AttractionType.of(attraction);
            counts.put(type, counts.get(type) + 1);
        }
        List<AttractionType> rankings = new ArrayList<>(List.of(AttractionType.values()));
        Collections.sort(rankings, (a, b) -> Integer.compare(counts.get(a), counts.get(b)));
        for (AttractionType type : rankings) {
            logInfo(counts.get(type) + " " + type);
        }
        logInfo(attractionsMap.size() + " Total");
    }

    public void tick() {
        for (Attraction attraction : new ArrayList<>(attractionsMap.values())) {
            if (attraction.isPlaying()) {
                try {
                    attraction.tick();
                } catch (Exception e) {
                    plugin().getLogger().log(Level.SEVERE, attraction.getName(), e);
                }
            }
        }
    }

    public <T extends Attraction> void applyActiveAttraction(Class<T> type, Consumer<T> consumer) {
        for (Attraction attraction : attractionsMap.values()) {
            if (attraction.isPlaying() && type.isInstance(attraction)) {
                consumer.accept(type.cast(attraction));
            }
        }
    }

    public void clickTotalCompletionVillager(Player player) {
        Session session = sessionOf(player);
        if (session.isTotallyCompleted()) {
            player.sendMessage(text("You completed everything. Congratulations!", GOLD));
            return;
        }
        final int total = attractionsMap.size();
        int locked = 0;
        for (Attraction attraction : attractionsMap.values()) {
            if (session.isUniqueLocked(attraction)) {
                locked += 1;
            }
        }
        if (locked >= total) {
            session.lockTotallyCompleted();
            session.save();
            Attraction.perfect(player, true);
            totalCompletionHandler.accept(player);
        } else {
            player.sendMessage(text("You completed " + locked + "/" + total + " games."
                                    + " Please return when you completed everything!"
                                    + " Use your Magic Map to locate more games.",
                                    GOLD));
        }
    }

    public Attraction getAttraction(String name) {
        return attractionsMap.get(name);
    }

    public List<Attraction> getAttractions() {
        return List.copyOf(attractionsMap.values());
    }

    public Attraction getAttraction(Location location) {
        for (Attraction attraction : attractionsMap.values()) {
            if (attraction.isInArea(location)) return attraction;
        }
        return null;
    }

    public Session sessionOf(Player player) {
        return sessions.of(player);
    }

    public Session sessionOf(PlayerCache player) {
        return sessions.of(player);
    }
}
