package com.cavetale.festival;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.attraction.Music;
import com.cavetale.festival.session.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class FestivalAdminCommand extends AbstractCommand<FestivalPlugin> {
    protected FestivalAdminCommand(final FestivalPlugin plugin) {
        super(plugin, "festivaladmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload areas")
            .senderCaller(this::reload);
        rootNode.addChild("music").arguments("<melody> [player]")
            .description("Play music")
            .completers(CommandArgCompleter.enumLowerList(Music.class),
                        CommandArgCompleter.NULL)
            .senderCaller(this::music);
        rootNode.addChild("count").arguments("<world>")
            .description("Count attractions")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.festivalMap.keySet())))
            .senderCaller(this::count);
        CommandNode sessionNode = rootNode.addChild("session")
            .description("Session subcommands");
        sessionNode.addChild("reset").arguments("<player>")
            .description("Reset player session")
            .completers(CommandArgCompleter.PLAYER_CACHE)
            .playerCaller(this::sessionReset);
        CommandNode attractionNode = rootNode.addChild("attraction")
            .description("Attraction subcommands");
        attractionNode.addChild("create").arguments("<type> <name>")
            .completers(CommandArgCompleter.enumLowerList(AttractionType.class),
                        CommandArgCompleter.supplyList(() -> {
                                List<String> result = new ArrayList<>();
                                for (Festival festival : plugin.getFestivalMap().values()) {
                                    for (Attraction attraction : festival.getAttractionsMap().values()) {
                                        result.add(attraction.getName());
                                    }
                                }
                                return result;
                            }))
            .description("Create attraction")
            .playerCaller(this::attractionCreate);
        attractionNode.addChild("set").arguments("<type>")
            .completers(CommandArgCompleter.enumLowerList(AttractionType.class))
            .description("Set attraction of area")
            .playerCaller(this::attractionSet);
        attractionNode.addChild("addarea").arguments("<area>")
            .completers(this::completeAreaNames)
            .description("Add attraction area")
            .playerCaller(this::attractionAddArea);
        attractionNode.addChild("removearea").arguments("<area>")
            .completers(this::completePresentAreaNames)
            .description("remove attraction area")
            .playerCaller(this::attractionRemoveArea);
        attractionNode.addChild("setvalue").arguments("<key> <value>")
            .completers(this::completeAttractionKeys)
            .description("Set an attraction value")
            .playerCaller(this::attractionSetValue);
        attractionNode.addChild("resetvalue").arguments("<key>")
            .completers(this::completeExistingAttractionKeys)
            .description("Reset an attraction value")
            .playerCaller(this::attractionResetValue);
    }

    private int requireInt(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new CommandWarn("Number expected: " + arg);
        }
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.clearFestivals();
        plugin.loadFestivals();
        sender.sendMessage(text("Players and attractions reloaded", YELLOW));
        return true;
    }

    protected boolean music(CommandSender sender, String[] args) {
        if (args.length > 3) return false;
        Music music;
        Player target;
        try {
            music = Music.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Music not found: " + args[0]);
        }
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                throw new CommandWarn("Player not found: " + args[1]);
            }
        } else {
            if (!(sender instanceof Player)) {
                throw new CommandWarn("[fam:music] Player expected!");
            }
            target = (Player) sender;
        }
        music.melody.play(plugin, target);
        sender.sendMessage(text("Playing " + music + " to " + target.getName(), YELLOW));
        return true;
    }

    protected boolean count(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Festival festival = plugin.festivalMap.get(args[0]);
        if (festival == null) throw new CommandWarn("World not found: " + args[0]);
        Map<AttractionType, Integer> counts = new EnumMap<>(AttractionType.class);
        for (AttractionType type : AttractionType.values()) counts.put(type, 0);
        for (Attraction attraction : festival.getAttractionsMap().values()) {
            AttractionType type = AttractionType.of(attraction);
            counts.put(type, counts.get(type) + 1);
        }
        List<AttractionType> rankings = new ArrayList<>(List.of(AttractionType.values()));
        Collections.sort(rankings, (a, b) -> Integer.compare(counts.get(a), counts.get(b)));
        for (AttractionType type : rankings) {
            sender.sendMessage(counts.get(type) + " " + type);
        }
        sender.sendMessage(festival.getAttractionsMap().size() + " Total");
        return true;
    }

    private boolean sessionReset(Player player, String[] args) {
        if (args.length != 1) return false;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) throw new CommandWarn("No festival here!");
        PlayerCache target = PlayerCache.require(args[0]);
        Session session = festival.sessions.of(target);
        session.reset();
        session.save();
        festival.sessions.clear(target.uuid);
        player.sendMessage(text("Session reset: " + target.getName(), YELLOW));
        return true;
    }

    private boolean attractionCreate(Player player, String[] args) {
        if (args.length != 2) return false;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) throw new CommandWarn("There is no festival here!");
        Cuboid cuboid = Cuboid.requireSelectionOf(player);
        for (Attraction attraction : festival.getAttractionsMap().values()) {
            if (attraction.getMainArea().overlaps(cuboid)) {
                throw new CommandWarn("Selection overlaps with attraction " + attraction.getName());
            }
        }
        AttractionType type = CommandArgCompleter.requireEnum(AttractionType.class, args[0]);
        String name = args[1];
        if (festival.getAttractionsMap().containsKey(name)) {
            throw new CommandWarn("Area already exists: " + name);
        }
        List<Area> areaList = new ArrayList<>();
        areaList.add(new Area(cuboid.getMin(), cuboid.getMax(), type.name().toLowerCase(), null));
        AreasFile areasFile = festival.loadAreasFile();
        areasFile.getAreas().put(name, areaList);
        areasFile.save(festival.getWorld(), festival.getAreasFileName());
        plugin.clearFestivals();
        plugin.loadFestivals();
        player.sendMessage(text("Attraction created: " + type + ", " + name + ", " + cuboid, GREEN));
        return true;
    }

    private boolean attractionSet(Player player, String[] args) {
        if (args.length != 1) return false;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) throw new CommandWarn("There is no festival here!");
        String areaName = null;
        List<Area> areaList = null;
        AreasFile areasFile = festival.loadAreasFile();
        final Vec3i playerVector = Vec3i.of(player.getLocation());
        for (String name : areasFile.getAreas().keySet()) {
            List<Area> list = areasFile.getAreas().get(name);
            if (list.isEmpty()) continue;
            if (list.get(0).contains(playerVector)) {
                areaName = name;
                areaList = list;
                break;
            }
        }
        if (areaName == null || areaList == null) {
            throw new CommandWarn("There is no area list here: " + playerVector);
        }
        AttractionType type = CommandArgCompleter.requireEnum(AttractionType.class, args[0]);
        Area mainArea = areaList.get(0);
        String oldType = mainArea.getName();
        areaList.set(0, mainArea.withName(type.name().toLowerCase()));
        areasFile.save(festival.getWorld(), festival.getAreasFileName());
        plugin.clearFestivals();
        plugin.loadFestivals();
        player.sendMessage(text("Attraction set: " + oldType + " => " + type + " in area " + areaName, GREEN));
        return true;
    }

    private List<String> completeAreaNames(CommandContext context, CommandNode node, String arg) {
        if (!context.isPlayer()) return List.of();
        Attraction<?> attraction = plugin.getAttraction(context.player.getLocation());
        if (attraction == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String name : attraction.getAreaNames()) {
            if (name.contains(arg.toLowerCase())) result.add(name);
        }
        return result;
    }

    private List<String> completePresentAreaNames(CommandContext context, CommandNode node, String arg) {
        if (!context.isPlayer()) return List.of();
        Attraction<?> attraction = plugin.getAttraction(context.player.getLocation());
        if (attraction == null) return List.of();
        List<String> result = new ArrayList<>();
        boolean first = false;
        for (var area : attraction.getAllAreas()) {
            if (first) {
                first = true;
                continue;
            }
            String name = area.getName();
            if (area == null) continue;
            if (name.contains(arg.toLowerCase())) result.add(name);
        }
        return result;
    }

    private boolean attractionAddArea(Player player, String[] args) {
        if (args.length != 1) return false;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) throw new CommandWarn("There is no festival here!");
        Cuboid cuboid = Cuboid.requireSelectionOf(player);
        Attraction<?> attraction = festival.getAttraction(cuboid.getMin());
        if (attraction == null) {
            throw new CommandWarn("There is no attraction here");
        }
        if (!attraction.getMainArea().contains(cuboid)) {
            throw new CommandWarn("Selection is not contained in main area: " + attraction.getName());
        }
        final String name = args[0];
        if (!attraction.getAreaNames().contains(name)) {
            throw new CommandWarn("Attraction does not require area named " + name + " (" + attraction.getType() + ")");
        }
        AreasFile areasFile = festival.loadAreasFile();
        List<Area> areaList = areasFile.getAreas().get(attraction.getName());
        assert areaList != null;
        Area area = new Area(cuboid.getMin(), cuboid.getMax(), name, null);
        areaList.add(area);
        areasFile.save(festival.getWorld(), festival.getAreasFileName());
        plugin.clearFestivals();
        plugin.loadFestivals();
        player.sendMessage(text("Area added: " + area, GREEN));
        area.highlight(player.getWorld(), loc -> player.spawnParticle(Particle.REDSTONE, loc,
                                                                      1, 0.0, 0.0, 0.0, 0.0,
                                                                      new Particle.DustOptions(Color.GREEN, 1.0f)));
        return true;
    }

    private boolean attractionRemoveArea(Player player, String[] args) {
        if (args.length != 1) return false;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) throw new CommandWarn("There is no festival here!");
        Cuboid cuboid = Cuboid.requireSelectionOf(player);
        Attraction<?> attraction = festival.getAttraction(cuboid.getMin());
        if (attraction == null) {
            throw new CommandWarn("There is no attraction here");
        }
        if (!attraction.getMainArea().contains(cuboid)) {
            throw new CommandWarn("Selection is not contained in main area: " + attraction.getName());
        }
        final String name = args[0];
        AreasFile areasFile = festival.loadAreasFile();
        List<Area> areaList = areasFile.getAreas().get(attraction.getName());
        assert areaList != null;
        Area removedArea = null;
        for (int i = areaList.size() - 1; i >= 0; i -= 1) {
            Area area = areaList.get(i);
            if (name.equals(area.getName())) {
                removedArea = areaList.remove(i);
                break;
            }
        }
        if (removedArea == null) throw new CommandWarn("Area not found: " + name);
        areasFile.save(festival.getWorld(), festival.getAreasFileName());
        plugin.clearFestivals();
        plugin.loadFestivals();
        player.sendMessage(text("Area removed: " + removedArea, YELLOW));
        removedArea.highlight(player.getWorld(), loc -> player.spawnParticle(Particle.REDSTONE,
                                                                             loc, 1, 0.0, 0.0, 0.0, 0.0,
                                                                             new Particle.DustOptions(Color.RED, 1.0f)));
        return true;
    }

    private List<String> completeAttractionKeys(CommandContext context, CommandNode node, String arg) {
        if (!context.isPlayer()) return List.of();
        Attraction<?> attraction = plugin.getAttraction(context.player.getLocation());
        if (attraction == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String key : attraction.getStringKeys()) {
            if (key.contains(arg.toLowerCase())) result.add(key);
        }
        for (String key : attraction.getIntKeys()) {
            if (key.contains(arg.toLowerCase())) result.add(key);
        }
        return result;
    }

    private boolean attractionSetValue(Player player, String[] args) {
        if (args.length < 1) return false;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) throw new CommandWarn("There is no festival here!");
        Attraction<?> attraction = festival.getAttraction(player.getLocation());
        if (attraction == null) throw new CommandWarn("There is no attraction here");
        String key = args[0];
        if (args.length < 2) {
            Object value = attraction.getFirstArea().getRaw().get(key);
            player.sendMessage(textOfChildren(text("Value of ", AQUA),
                                              text(key, YELLOW),
                                              text(" is ", AQUA),
                                              text("" + value, YELLOW)));
            return true;
        }
        String value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        AreasFile areasFile = festival.loadAreasFile();
        List<Area> areas = areasFile.find(attraction.getName());
        Area area = areas.get(0);
        if (area.getRaw() == null) {
            area = area.withRaw(new HashMap<>());
            areas.set(0, area);
        }
        if (attraction.getStringKeys().contains(key)) {
            area.getRaw().put(key, value);
            areasFile.save(festival.getWorld(), festival.getAreasFileName());
            player.sendMessage(textOfChildren(text("Value of ", AQUA),
                                              text(key, YELLOW),
                                              text(" set to ", AQUA),
                                              text(value, YELLOW)));
        } else if (attraction.getIntKeys().contains(key)) {
            final int intValue = CommandArgCompleter.requireInt(value);
            area.getRaw().put(key, intValue);
            areasFile.save(festival.getWorld(), festival.getAreasFileName());
            player.sendMessage(textOfChildren(text("Value of ", AQUA),
                                              text(key, YELLOW),
                                              text(" set to ", AQUA),
                                              text(intValue, YELLOW)));
        } else {
            throw new CommandWarn("Invalid key: " + key);
        }
        plugin.clearFestivals();
        plugin.loadFestivals();
        return true;
    }

    private List<String> completeExistingAttractionKeys(CommandContext context, CommandNode node, String arg) {
        if (!context.isPlayer()) return List.of();
        Attraction<?> attraction = plugin.getAttraction(context.player.getLocation());
        if (attraction == null) return List.of();
        Area area = attraction.getFirstArea();
        if (area.getRaw() == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String key : area.getRaw().keySet()) {
            if (key.contains(arg.toLowerCase())) result.add(key);
        }
        return result;
    }

    private boolean attractionResetValue(Player player, String[] args) {
        if (args.length != 1) return false;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) throw new CommandWarn("There is no festival here!");
        Attraction<?> attraction = festival.getAttraction(player.getLocation());
        if (attraction == null) throw new CommandWarn("There is no attraction here");
        String key = args[0];
        AreasFile areasFile = festival.loadAreasFile();
        List<Area> areas = areasFile.find(attraction.getName());
        Area area = areas.get(0);
        if (area.getRaw() == null) {
            throw new CommandWarn("There are no values set");
        }
        final Object value = area.getRaw().remove(key);
        if (value == null) {
            throw new CommandWarn("Does not set " + key);
        }
        areasFile.save(festival.getWorld(), festival.getAreasFileName());
        player.sendMessage(textOfChildren(text("Value of ", YELLOW),
                                          text(key, GRAY),
                                          text(" was reset: ", YELLOW),
                                          text("" + value, RED)));
        plugin.clearFestivals();
        plugin.loadFestivals();
        return true;
    }
}
