package com.cavetale.festival;

import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionConfiguration;
import com.cavetale.festival.attraction.Attractions;
import com.cavetale.festival.gui.Gui;
import com.cavetale.festival.session.Sessions;
import java.io.File;
import java.util.List;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FestivalPlugin extends JavaPlugin {
    @Getter protected static FestivalPlugin instance;
    @Getter private final Attractions attractions = new Attractions(this);
    @Getter private final Sessions sessions = new Sessions(this);
    @Getter private File playersFolder;

    @Override
    public void onEnable() {
        instance = this;
        playersFolder = new File(getDataFolder(), "players");
        playersFolder.mkdirs();
        new FestivalCommand(this).enable();
        new FestivalAdminCommand(this).enable();
        new EventListener(this).enable();
        Gui.enable(this);
        attractions.enable();
    }

    @Override
    public void onDisable() {
        attractions.disable();
        sessions.clear();
    }

    public void register(final AttractionConfiguration config) {
        attractions.register(config);
    }

    public void unregister(final String name) {
        attractions.unregister(name);
    }

    public File getAttractionsFolder() {
        return attractions.getAttractionsFolder();
    }

    public File getStateFolder() {
        return attractions.getStateFolder();
    }

    public Attraction getAttraction(final String name) {
        return attractions.getAttraction(name);
    }

    public Attraction getAttraction(final World world, final String name) {
        return attractions.getAttraction(world, name);
    }

    public List<Attraction> getAttractionsAt(final Location location) {
        return attractions.getAttractionsAt(location);
    }

    public List<Attraction> getAttractionsIn(final World world) {
        return attractions.getAttractionsIn(world);
    }

    public void showCollection(final Player player) {
        player.sendMessage("Collection: " + String.join(", ", sessions.of(player).getCollection()));
    }
}
