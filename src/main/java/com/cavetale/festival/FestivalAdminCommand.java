package com.cavetale.festival;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.util.Json;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionConfiguration;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.attraction.Music;
import com.cavetale.festival.session.Session;
import com.cavetale.mytems.item.axis.CuboidOutline;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
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
            .senderCaller(this::count);
        rootNode.addChild("countcompletions").arguments("<world>")
            .description("Count total completions")
            .senderCaller(this::countCompletions);
        CommandNode sessionNode = rootNode.addChild("session")
            .description("Session subcommands");
        sessionNode.addChild("reset").arguments("<player>")
            .description("Reset player session")
            .completers(CommandArgCompleter.PLAYER_CACHE)
            .playerCaller(this::sessionReset);
        sessionNode.addChild("complete").arguments("<player>")
            .description("Complete player session")
            .completers(CommandArgCompleter.PLAYER_CACHE)
            .playerCaller(this::sessionComplete);
        sessionNode.addChild("clearcooldowns").arguments("<player>")
            .description("Clear player cooldowns")
            .playerCaller(this::sessionClearCooldowns);
        CommandNode attractionNode = rootNode.addChild("attraction")
            .description("Attraction subcommands");
        attractionNode.addChild("create").arguments("<type> <name>")
            .completers(CommandArgCompleter.enumLowerList(AttractionType.class))
            .playerCaller(this::attractionCreate);
        for (final String action : List.of("set", "add", "remove", "setvalue", "resetvalue")) {
            attractionNode.addChild(action).arguments(switch (action) {
                case "remove" -> "<name> <area> [1-based index]";
                case "setvalue" -> "<name> <key> <value>";
                default -> "<name> <key>";
            }).playerCaller((player, args) -> editAttraction(player, action, args));
        }
        attractionNode.addChild("debug").arguments("[name]")
            .description("Show attraction debug lines")
            .completers(CommandArgCompleter.supplyList(() -> plugin.getAttractions().getLoadedAttractions().stream()
                                                      .map(Attraction::getName).sorted().toList()))
            .senderCaller(this::attractionDebug);
        attractionNode.addChild("highlight").arguments("<name>").playerCaller(this::highlight);
    }

    private boolean attractionDebug(final CommandSender sender, final String[] args) {
        if (args.length > 1) return false;
        final List<Attraction> attractions;
        if (args.length == 1) {
            final Attraction attraction = plugin.getAttraction(args[0]);
            if (attraction == null) throw new CommandWarn("Unknown loaded attraction: " + args[0]);
            attractions = List.of(attraction);
        } else {
            attractions = plugin.getAttractions().getLoadedAttractions().stream()
                .sorted(Comparator.comparing(Attraction::getName)).toList();
        }
        boolean found = false;
        for (final Attraction attraction : attractions) {
            if (attraction.getDebugLines().isEmpty()) continue;
            sender.sendMessage(text(attraction.getName(), YELLOW));
            attraction.getDebugLines().forEach(sender::sendMessage);
            found = true;
        }
        if (!found) sender.sendMessage(text("No attraction debug lines.", GRAY));
        return true;
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.getAttractions().reload();
        sender.sendMessage(text("Attractions reloaded", YELLOW));
        return true;
    }

    protected boolean music(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
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
        final World world = Bukkit.getWorld(args[0]);
        if (world == null) throw new CommandWarn("World not found: " + args[0]);
        Map<AttractionType, Integer> counts = new EnumMap<>(AttractionType.class);
        for (AttractionType type : AttractionType.values()) counts.put(type, 0);
        for (Attraction attraction : plugin.getAttractionsIn(world)) {
            AttractionType type = AttractionType.of(attraction);
            counts.put(type, counts.get(type) + 1);
        }
        List<AttractionType> rankings = new ArrayList<>(List.of(AttractionType.values()));
        Collections.sort(rankings, (a, b) -> Integer.compare(counts.get(a), counts.get(b)));
        for (AttractionType type : rankings) {
            sender.sendMessage(counts.get(type) + " " + type);
        }
        sender.sendMessage(plugin.getAttractionsIn(world).size() + " Total");
        return true;
    }

    protected boolean countCompletions(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final World world = Bukkit.getWorld(args[0]);
        if (world == null) throw new CommandWarn("World not found: " + args[0]);
        final int total = plugin.getAttractionsIn(world).size();
        List<Session> completedSessions = new ArrayList<>();
        for (Session session : Session.loadAll(plugin)) {
            if (total > 0 && plugin.getAttractionsIn(world).stream().allMatch(session::isUniqueLocked)) {
                completedSessions.add(session);
            }
        }
        for (Session session : completedSessions) {
            sender.sendMessage(text(session.getUuid() + " " + session.getName()));
        }
        sender.sendMessage(text("Total " + completedSessions.size() + " total completions", YELLOW));
        return true;
    }

    private boolean sessionReset(Player player, String[] args) {
        if (args.length != 1) return false;
        final PlayerCache target = PlayerCache.require(args[0]);
        final Session session = plugin.getSessions().of(target);
        session.reset();
        session.save();
        plugin.getSessions().clear(target.uuid);
        player.sendMessage(text("Session reset: " + target.getName(), YELLOW));
        return true;
    }

    private boolean sessionComplete(Player player, String[] args) {
        if (args.length != 1) return false;
        final PlayerCache target = PlayerCache.require(args[0]);
        final Session session = plugin.getSessions().of(target);
        for (Attraction attraction : plugin.getAttractionsIn(player.getWorld())) {
            session.lockUnique(attraction);
        }
        session.save();
        player.sendMessage(text("Session completed: " + target.getName(), YELLOW));
        return true;
    }

    private boolean sessionClearCooldowns(Player player, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        Session session = plugin.getSessions().of(target);
        session.getTag().getCooldowns().clear();
        session.save();
        player.sendMessage(text("Cooldowns cleared: " + target.getName(), YELLOW));
        return true;
    }

    private File configFile(final String name) {
        if (!name.matches("[A-Za-z0-9_-]+")) throw new CommandWarn("Invalid attraction name");
        return new File(plugin.getAttractionsFolder(), name + ".json");
    }

    private boolean attractionCreate(final Player player, final String[] args) {
        if (args.length != 2) return false;
        final File file = configFile(args[1]);
        if (file.exists() || plugin.getAttractions().hasConfiguration(args[1])) throw new CommandWarn("Name already in use");
        final AttractionConfiguration config = new AttractionConfiguration();
        try {
            config.setType(AttractionType.valueOf(args[0].toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new CommandWarn("Unknown attraction type");
        }
        config.setName(args[1]);
        config.setWorld(player.getWorld().getName());
        config.setMain(Cuboid.requireSelectionOf(player));
        plugin.getAttractions().storeConfiguration(config);
        player.sendMessage(text("Created " + config.getName(), YELLOW));
        return true;
    }

    private boolean editAttraction(final Player player, final String action, final String[] args) {
        if (args.length < 2) return false;
        final File file = configFile(args[0]);
        if (!file.isFile()) throw new CommandWarn("Missing attraction file");
        final AttractionConfiguration config = Json.load(file, AttractionConfiguration.class);
        if (!args[0].equals(config.getName()) || !player.getWorld().getName().equals(config.getWorld())) {
            throw new CommandWarn("Attraction name or world does not match");
        }
        final String key = args[1];
        switch (action) {
        case "set", "add" -> {
            if (args.length != 2) return false;
            final Cuboid area = Cuboid.requireSelectionOf(player);
            if (key.equals("main")) {
                if (action.equals("add")) throw new CommandWarn("Use set for the main area");
                config.setMain(area);
            } else if (action.equals("set")) {
                if (config.getAreaLists().containsKey(key)) throw new CommandWarn("Key is an area list");
                config.getAreas().put(key, area);
            } else {
                if (config.getAreas().containsKey(key)) throw new CommandWarn("Key is a single area");
                config.getAreaLists().computeIfAbsent(key, k -> new ArrayList<>()).add(area);
            }
        }
        case "remove" -> {
            if (args.length == 3) {
                final int index = CommandArgCompleter.requireInt(args[2]) - 1;
                final List<Cuboid> list = config.getAreaLists().get(key);
                if (list == null || index < 0 || index >= list.size()) throw new CommandWarn("Invalid list index");
                list.remove(index);
                if (list.isEmpty()) config.getAreaLists().remove(key);
            } else if (args.length != 2) {
                return false;
            } else if (config.getAreas().remove(key) == null && config.getAreaLists().remove(key) == null) {
                throw new CommandWarn("Unknown area");
            }
        }
        case "setvalue" -> {
            if (args.length < 3) return false;
            final String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            final Attraction attraction = plugin.getAttraction(args[0]);
            config.getSettings().put(key, attraction != null && attraction.getIntKeys().contains(key)
                                     ? CommandArgCompleter.requireInt(value) : value);
        }
        case "resetvalue" -> {
            if (args.length != 2) return false;
            config.getSettings().remove(key);
        }
        default -> throw new IllegalArgumentException(action);
        }
        plugin.getAttractions().storeConfiguration(config);
        player.sendMessage(text("Updated " + config.getName(), YELLOW));
        return true;
    }

    private boolean highlight(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final Attraction attraction = plugin.getAttraction(player.getWorld(), args[0]);
        if (attraction == null) throw new CommandWarn("Unknown attraction");
        final AttractionConfiguration config = attraction.getConfiguration();
        final List<Cuboid> areas = new ArrayList<>(config.getAreas().values());
        areas.add(config.getMain());
        config.getAreaLists().values().forEach(areas::addAll);
        for (final Cuboid area : areas) {
            final CuboidOutline outline = new CuboidOutline(player.getWorld(), area);
            outline.showOnlyTo(player);
            outline.spawn();
            outline.removeLater(100L);
        }
        return true;
    }
}
