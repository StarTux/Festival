package com.cavetale.festival;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionConfiguration;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.attraction.Attractions;
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
import java.util.TreeSet;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class FestivalAdminCommand extends AbstractCommand<FestivalPlugin> {
    private static final int ATTRACTIONS_PER_PAGE = 8;
    private static final int[] TELEPORT_OFFSETS = {0, -1, 1};

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
        final CommandArgCompleter worldNames = CommandArgCompleter.supplyStream(() -> Bukkit.getWorlds().stream().map(World::getName).sorted());
        rootNode.addChild("count").arguments("<world>")
            .description("Count attractions")
            .completers(worldNames)
            .senderCaller(this::count);
        rootNode.addChild("countcompletions").arguments("<world>")
            .description("Count total completions")
            .completers(worldNames)
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
            .completers(CommandArgCompleter.PLAYER_CACHE)
            .playerCaller(this::sessionClearCooldowns);
        CommandNode attractionNode = rootNode.addChild("attraction")
            .description("Attraction subcommands");
        attractionNode.addChild("create").arguments("<type> <name>")
            .completers(CommandArgCompleter.enumLowerList(AttractionType.class))
            .playerCaller(this::attractionCreate);
        for (final String action : List.of("setarea", "addarea", "rmarea")) {
            final CommandNode editNode = attractionNode.addChild(action)
                .playerCaller((player, args) -> editAttraction(player, action, args));
            final CommandArgCompleter keys = CommandArgCompleter.producePlayerList(player -> completeAreaKeys(player, action));
            if (action.equals("rmarea")) {
                editNode.arguments("<area> [1-based index]")
                    .completers(keys, CommandArgCompleter.producePlayerList(this::completeAreaIndices));
            } else {
                editNode.arguments("<area>").completers(keys);
            }
        }
        attractionNode.addChild("setvalue").arguments("<key> <value>")
            .completers(CommandArgCompleter.producePlayerList(player -> completeSettingKeys(player, false)),
                        CommandArgCompleter.producePlayerList(this::completeSettingValue))
            .playerCaller((player, args) -> editAttraction(player, "setvalue", args));
        attractionNode.addChild("resetvalue").arguments("<key>")
            .completers(CommandArgCompleter.producePlayerList(player -> completeSettingKeys(player, true)))
            .playerCaller((player, args) -> editAttraction(player, "resetvalue", args));
        attractionNode.addChild("debug").arguments("[name]")
            .description("Show attraction debug lines")
            .completers((context, node, arg) -> context.isPlayer() ? List.of()
                        : plugin.getAttractions().getLoadedAttractions().stream()
                        .map(Attraction::getName).filter(name -> name.contains(arg)).sorted().toList())
            .senderCaller(this::attractionDebug);
        attractionNode.addChild("info").denyTabCompletion()
            .description("Show the configuration of the attraction at your location")
            .playerCaller(this::attractionInfo);
        attractionNode.addChild("highlight").denyTabCompletion()
            .description("Highlight areas of the attraction at your location")
            .playerCaller(this::highlight);
        attractionNode.addChild("list").arguments("[page]")
            .description("List attractions")
            .completers(CommandArgCompleter.supplyList(() -> IntStream.rangeClosed(1, attractionPageCount())
                                                      .mapToObj(Integer::toString).toList()))
            .senderCaller(this::attractionList);
        attractionNode.addChild("teleport").arguments("<name>")
            .description("Teleport to an attraction")
            .completers(CommandArgCompleter.supplyStream(() -> plugin.getAttractions().getConfigurations().stream()
                                                        .map(AttractionConfiguration::getName).sorted()))
            .playerCaller(this::attractionTeleport);
    }

    private boolean attractionDebug(final CommandSender sender, final String[] args) {
        if (args.length > 1) return false;
        final List<Attraction> attractions;
        if (args.length == 1) {
            if (sender instanceof Player) throw new CommandWarn("Use /festadm attraction debug to show all debug lines.");
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

    private List<String> completeAreaKeys(final Player player, final String action) {
        final Attraction attraction = editableAttractionAt(player);
        if (attraction == null) return List.of();
        final AttractionConfiguration config = attraction.getConfiguration();
        final TreeSet<String> keys = new TreeSet<>();
        if (!action.equals("rmarea")) keys.addAll(attraction.getAreaNames());
        switch (action) {
        case "setarea" -> {
            keys.addAll(config.getAreas().keySet());
            keys.removeAll(config.getAreaLists().keySet());
            keys.add("main");
        }
        case "addarea" -> {
            keys.addAll(config.getAreaLists().keySet());
            keys.removeAll(config.getAreas().keySet());
            keys.remove("main");
        }
        case "rmarea" -> {
            keys.addAll(config.getAreas().keySet());
            keys.addAll(config.getAreaLists().keySet());
        }
        default -> throw new IllegalArgumentException(action);
        }
        return List.copyOf(keys);
    }

    private List<String> completeAreaIndices(final Player player, final String[] args) {
        final Attraction attraction = editableAttractionAt(player);
        if (attraction == null) return List.of();
        final List<Cuboid> areas = attraction.getConfiguration().getAreaLists().get(args[args.length - 2]);
        return areas != null ? IntStream.rangeClosed(1, areas.size()).mapToObj(Integer::toString).toList() : List.of();
    }

    private Attraction editableAttractionAt(final Player player) {
        try {
            final Attraction attraction = Attraction.requireAt(player.getLocation());
            return configFile(attraction.getName()).isFile() ? attraction : null;
        } catch (CommandWarn warning) {
            return null;
        }
    }

    private List<String> completeSettingKeys(final Player player, final boolean existingOnly) {
        final Attraction attraction = editableAttractionAt(player);
        if (attraction == null) return List.of();
        final TreeSet<String> keys = new TreeSet<>(attraction.getConfiguration().getSettings().keySet());
        if (!existingOnly) {
            keys.addAll(attraction.getStringKeys());
            keys.addAll(attraction.getIntKeys());
        }
        return List.copyOf(keys);
    }

    private List<String> completeSettingValue(final Player player, final String[] args) {
        final Attraction attraction = editableAttractionAt(player);
        if (attraction == null) return List.of();
        final String key = args[args.length - 2];
        final Object value = attraction.getConfiguration().getSettings().get(key);
        if (attraction.getIntKeys().contains(key)) {
            return value instanceof Number number ? List.of(Integer.toString(number.intValue())) : List.of();
        }
        return value != null ? List.of(value.toString()) : List.of();
    }

    private File configFile(final String name) {
        if (!Attractions.isValidName(name)) throw new CommandWarn("Invalid attraction name");
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
        if (args.length < 1) return false;
        final Attraction attraction = Attraction.requireAt(player.getLocation());
        final File file = configFile(attraction.getName());
        if (!file.isFile()) throw new CommandWarn("Missing attraction file");
        final AttractionConfiguration config = Json.load(file, AttractionConfiguration.class);
        if (config == null || !attraction.getName().equals(config.getName()) || !player.getWorld().getName().equals(config.getWorld())) {
            throw new CommandWarn("Attraction name or world does not match");
        }
        final String key = args[0];
        switch (action) {
        case "setarea", "addarea" -> {
            if (args.length != 1) return false;
            final Cuboid area = Cuboid.requireSelectionOf(player);
            if (key.equals("main")) {
                if (action.equals("addarea")) throw new CommandWarn("Use setarea for the main area");
                config.setMain(area);
            } else if (action.equals("setarea")) {
                if (config.getAreaLists().containsKey(key)) throw new CommandWarn("Key is an area list");
                config.getAreas().put(key, area);
            } else {
                if (config.getAreas().containsKey(key)) throw new CommandWarn("Key is a single area");
                config.getAreaLists().computeIfAbsent(key, k -> new ArrayList<>()).add(area);
            }
        }
        case "rmarea" -> {
            if (args.length == 2) {
                final int index = CommandArgCompleter.requireInt(args[1]) - 1;
                final List<Cuboid> list = config.getAreaLists().get(key);
                if (list == null || index < 0 || index >= list.size()) throw new CommandWarn("Invalid list index");
                list.remove(index);
                if (list.isEmpty()) config.getAreaLists().remove(key);
            } else if (args.length != 1) {
                return false;
            } else if (config.getAreas().remove(key) == null && config.getAreaLists().remove(key) == null) {
                throw new CommandWarn("Unknown area");
            }
        }
        case "setvalue" -> {
            if (args.length < 2) return false;
            final String value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            config.getSettings().put(key, attraction.getIntKeys().contains(key)
                                     ? CommandArgCompleter.requireInt(value) : value);
        }
        case "resetvalue" -> {
            if (args.length != 1) return false;
            config.getSettings().remove(key);
        }
        default -> throw new IllegalArgumentException(action);
        }
        plugin.getAttractions().storeConfiguration(config);
        player.sendMessage(text("Updated " + config.getName(), YELLOW));
        return true;
    }

    private boolean attractionInfo(final Player player, final String[] args) {
        if (args.length != 0) return false;
        final Attraction attraction = Attraction.requireAt(player.getLocation());
        final AttractionConfiguration config = attraction.getConfiguration();
        sendConfigValue(player, "Name", config.getName());
        sendConfigValue(player, "Type", config.getType());
        sendConfigValue(player, "World", config.getWorld());
        sendConfigValue(player, "Theme", config.getTheme());
        sendConfigValue(player, "Main area", config.getMain());

        final TreeSet<String> areaKeys = new TreeSet<>(attraction.getAreaNames());
        areaKeys.addAll(config.getAreas().keySet());
        areaKeys.addAll(config.getAreaLists().keySet());
        for (final String key : areaKeys) {
            final List<Cuboid> list = config.getAreaLists().get(key);
            if (config.getAreas().containsKey(key) || list == null || list.isEmpty()) {
                sendConfigValue(player, key, config.getAreas().get(key));
            }
            if (list == null) continue;
            for (int index = 0; index < list.size(); index += 1) {
                sendConfigValue(player, key + "[" + (index + 1) + "]", list.get(index));
            }
        }

        final TreeSet<String> settingKeys = new TreeSet<>(config.getSettings().keySet());
        settingKeys.addAll(attraction.getStringKeys());
        settingKeys.addAll(attraction.getIntKeys());
        if (!settingKeys.isEmpty()) player.sendMessage(text("Settings", GRAY));
        for (final String key : settingKeys) {
            sendConfigValue(player, key, config.getSettings().get(key));
        }
        return true;
    }

    private void sendConfigValue(final Player player, final String name, final Object value) {
        final Component content;
        if (value == null) {
            content = text("(N/A)", DARK_GRAY);
        } else if (value instanceof Cuboid area) {
            final String min = area.ax + " " + area.ay + " " + area.az;
            final String coordinates = area.getMin().equals(area.getMax())
                ? min
                : min + " to " + area.bx + " " + area.by + " " + area.bz;
            content = text(coordinates, WHITE);
        } else {
            content = text(value.toString(), WHITE);
        }
        player.sendMessage(textOfChildren(
                               text(name + ": ", GRAY),
                               content));
    }

    private boolean highlight(final Player player, final String[] args) {
        if (args.length != 0) return false;
        final Attraction attraction = Attraction.requireAt(player.getLocation());
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

    private int attractionPageCount() {
        return Math.max(1, Math.ceilDiv(plugin.getAttractions().getConfigurations().size(), ATTRACTIONS_PER_PAGE));
    }

    private boolean attractionList(final CommandSender sender, final String[] args) {
        if (args.length > 1) return false;
        final Attractions registry = plugin.getAttractions();
        final List<AttractionConfiguration> configs = registry.getConfigurations().stream()
            .sorted(Comparator.comparing(AttractionConfiguration::getName)).toList();
        if (configs.isEmpty()) {
            sender.sendMessage(text("No attractions configured.", GRAY));
            return true;
        }
        final int pages = Math.ceilDiv(configs.size(), ATTRACTIONS_PER_PAGE);
        final int page = args.length == 0 ? 1 : CommandArgCompleter.requireInt(args[0], n -> n >= 1 && n <= pages);
        sender.sendMessage(text("Attractions (" + page + "/" + pages + ")", GOLD));
        final int start = (page - 1) * ATTRACTIONS_PER_PAGE;
        for (final AttractionConfiguration config : configs.subList(start, Math.min(start + ATTRACTIONS_PER_PAGE, configs.size()))) {
            final Vec3i center = config.getMain().getCenter();
            sender.sendMessage(registry.getDisplayName(config)
                               .hoverEvent(showText(textOfChildren(
                                                        text(config.getName(), YELLOW),
                                                        newline(),
                                                        text("Type: " + config.getType()),
                                                        newline(),
                                                        text("World: " + config.getWorld()),
                                                        newline(),
                                                        text("Location: " + center.x + ", " + center.y + ", " + center.z))))
                               .clickEvent(runCommand("/festadm attraction teleport " + config.getName())));
        }
        if (pages > 1) {
            final Component previous = page > 1
                ? text("Previous", YELLOW).clickEvent(runCommand("/festadm attraction list " + (page - 1)))
                : Component.empty();
            final Component next = page < pages
                ? text("Next", YELLOW).clickEvent(runCommand("/festadm attraction list " + (page + 1)))
                : Component.empty();
            sender.sendMessage(textOfChildren(previous, page > 1 && page < pages ? text("  ") : Component.empty(), next));
        }
        return true;
    }

    private boolean attractionTeleport(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final Attractions registry = plugin.getAttractions();
        final AttractionConfiguration config = registry.getConfiguration(args[0]);
        if (config == null) throw new CommandWarn("Unknown attraction: " + args[0]);
        final Component displayName = registry.getDisplayName(config);
        final World world = Bukkit.getWorld(config.getWorld());
        if (world == null) {
            throw new CommandWarn(textOfChildren(
                                      text("World " + config.getWorld() + " is unavailable for ", RED),
                                      displayName));
        }
        final Location destination = findTeleportLocation(world, config.getMain());
        if (destination == null || !player.teleport(destination)) {
            throw new CommandWarn(textOfChildren(
                                      text("Could not teleport to ", RED),
                                      displayName));
        }
        return true;
    }

    private Location findTeleportLocation(final World world, final Cuboid area) {
        final Vec3i center = area.getCenter();
        final int minY = Math.max(area.ay, world.getMinHeight() + 1);
        final int maxY = Math.min(area.by, world.getMaxHeight() - 2);
        for (int y = minY; y <= maxY; y += 1) {
            for (int dx : TELEPORT_OFFSETS) {
                for (int dz : TELEPORT_OFFSETS) {
                    final Vec3i point = Vec3i.of(center.x + dx, y, center.z + dz);
                    if (!area.contains(point)) continue;
                    final Block feet = point.toBlock(world);
                    if (!feet.isEmpty() || !feet.getRelative(0, 1, 0).isEmpty()) continue;
                    if (!feet.getRelative(0, -1, 0).getType().isSolid()) continue;
                    final Location location = point.toCenterFloorLocation(world);
                    if (world.getWorldBorder().isInside(location)) return location;
                }
            }
        }
        return null;
    }

}
