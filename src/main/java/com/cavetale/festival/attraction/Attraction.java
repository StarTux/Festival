package com.cavetale.festival.attraction;

import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.item.PlayerReceiveItemsEvent;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import com.cavetale.festival.FestivalPlugin;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.event.AttractionCompleteEvent;
import com.cavetale.festival.gui.Gui;
import com.cavetale.festival.session.Session;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import com.cavetale.resident.PluginSpawn;
import com.cavetale.resident.save.Loc;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

/**
 * Base class for all attractions.
 */
@Getter
public abstract class Attraction {
    protected final FestivalPlugin plugin;
    protected final AttractionConfiguration configuration;
    protected final AttractionType type;
    protected final World world;
    protected final String name;
    protected final File saveFile;
    protected final Cuboid mainArea;
    protected PluginSpawn mainVillager;
    protected final Random random = ThreadLocalRandom.current();
    protected Vec3i npcVector;
    protected boolean doesRequireInstrument;
    @Setter protected Duration completionCooldown = Duration.ofMinutes(10);
    protected final FestivalTheme theme;
    private final String defaultDisplayName;
    @Setter protected Component displayName;
    @Setter protected Component description = empty();
    @Setter protected ItemStack firstCompletionReward = new ItemStack(Material.DIAMOND);
    @Setter protected List<List<ItemStack>> prizePool = List.of(List.of(new ItemStack(Material.DIAMOND)));
    @Setter protected List<ItemStack> bonusPrizePool = List.of(new ItemStack(Material.DIAMOND));
    @Setter protected ItemStack entryFee = Mytems.RUBY.createItemStack();
    @Setter protected Component mapCursorCaption;
    protected final List<Component> debugLines = new ArrayList<>();
    protected final List<String> areaNames = new ArrayList<>();
    protected final List<String> stringKeys = new ArrayList<>();
    protected final List<String> intKeys = new ArrayList<>();
    @Setter protected boolean disabled = false;

    protected Attraction(final AttractionConfiguration config) {
        this(config, config.getType().getDisplayName());
    }

    protected Attraction(final AttractionConfiguration config, final String defaultDisplayName) {
        this.plugin = FestivalPlugin.getInstance();
        this.theme = config.resolveTheme(this::debugLine);
        this.defaultDisplayName = defaultDisplayName;
        this.configuration = config;
        this.type = config.getType();
        this.world = Bukkit.getWorld(config.getWorld());
        this.name = config.getName();
        this.saveFile = new File(plugin.getStateFolder(), name + ".json");
        this.mainArea = config.getMain();
        for (final Map.Entry<String, Cuboid> entry : configuration.namedAreas()) {
            final Cuboid area = entry.getValue();
            if ("npc".equals(entry.getKey())) {
                if (area.getVolume() != 1) {
                    debugLine("NPC vector bigger than 1");
                }
                if (npcVector != null) {
                    debugLine("Duplicate area: NPC");
                }
                npcVector = area.getMin();
            }
        }
        this.areaNames.add("npc");
        this.stringKeys.add("description");
        this.stringKeys.add("display_name");
    }

    public final void debugLine(String txt) {
        debugLines.add(text(txt, RED));
    }

    public final void logWarn(String msg) {
        plugin.getLogger().warning("[" + name + "] " + msg);
    }

    public final void logInfo(String msg) {
        plugin.getLogger().info("[" + name + "] " + msg);
    }

    public static Attraction requireAt(final Location location) {
        final List<Attraction> attractions = FestivalPlugin.getInstance().getAttractionsAt(location);
        if (attractions.isEmpty()) throw new CommandWarn("You are not inside an attraction.");
        if (attractions.size() > 1) throw new CommandWarn("More than one attraction overlaps this location.");
        return attractions.getFirst();
    }

    public final boolean isInArea(Location location) {
        return world.equals(location.getWorld()) && mainArea.contains(location);
    }

    public final boolean isInArea(Vec3i vector) {
        return mainArea.contains(vector);
    }

    protected abstract void loadSaveTag();

    public abstract SaveTag getSaveTag();

    protected final <T extends SaveTag> T readSaveTag(final Class<T> clazz, final Supplier<T> fallback) {
        return Json.load(saveFile, clazz, fallback);
    }

    public final void load() {
        loadSaveTag();
        onLoad();
    }

    public final void save() {
        onSave();
        final SaveTag saveTag = getSaveTag();
        if (saveTag != null) {
            Json.save(saveFile, saveTag, true);
        }
    }

    public final void enable() {
        if (displayName == null) {
            final Component configuredName = configuration.readTextSetting("display_name");
            displayName = configuredName != null ? configuredName : theme.format(defaultDisplayName);
        }
        final Component configuredDescription = configuration.readTextSetting("description");
        if (configuredDescription != null) description = configuredDescription;
        if (disabled) return;
        onEnable();
        if (npcVector == null) {
            debugLine("Area missing: NPC");
        } else {
            Location location = npcVector.toCenterFloorLocation(world);
            mainVillager = PluginSpawn.register(plugin, theme.getZoneType(), Loc.of(location));
            mainVillager.setOnPlayerClick(this::clickMainVillager);
            mainVillager.setOnMobSpawning(mob -> {
                    mob.setCollidable(false);
                });
        }
    }

    public final void disable() {
        if (mainVillager != null) {
            mainVillager.unregister();
            mainVillager = null;
        }
        onDisable();
    }

    public final void tick() {
        onTick();
    }

    protected final void startingGun(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);
    }

    protected final void timeout(Player player) {
        player.showTitle(Title.title(text("Timeout", DARK_RED),
                                     text("Try Again", DARK_RED)));
        theme.getFailMelody().play(plugin, player);
        plugin.getSessions().of(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void fail(Player player) {
        player.showTitle(Title.title(text("Wrong", DARK_RED),
                                     text("Try Again", DARK_RED)));
        theme.getFailMelody().play(plugin, player);
        plugin.getSessions().of(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void fail(Player player, String msg) {
        player.showTitle(Title.title(empty(), text(msg, DARK_RED)));
        player.sendMessage(text(msg, RED));
        theme.getFailMelody().play(plugin, player);
        plugin.getSessions().of(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void progress(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.1f, 2.0f);
    }

    protected final void victory(Player player) {
        Component message = theme.format("Complete");
        player.showTitle(Title.title(message, theme.format("Good Job!")));
        player.sendMessage(message);
        theme.getSuccessMelody().play(plugin, player);
    }

    protected final void perfect(Player player, boolean withMusic) {
        Component message = theme.format("PERFECT!");
        player.showTitle(Title.title(message, empty()));
        player.sendMessage(message);
        if (withMusic) {
            theme.getSuccessMelody().play(plugin, player);
        }
    }

    protected final void perfect(Player player) {
        perfect(player, true);
    }

    protected final void countdown(Player player, int seconds) {
        player.sendActionBar(theme.format("" + seconds));
        List<Note.Tone> tones = List.of(Note.Tone.D, Note.Tone.A, Note.Tone.G);
        if ((int) seconds <= tones.size()) {
            player.playNote(player.getLocation(), Instrument.PLING, Note.natural(0, tones.get((int) seconds - 1)));
        }
    }

    protected final Component makeProgressComponent(int seconds, ComponentLike prefix, int has, int max) {
        return join(noSeparators(),
                    theme.format(Unicode.WATCH.string + seconds),
                    space(), prefix, theme.format(has + "/" + max));
    }

    protected final Component makeProgressComponent(int seconds) {
        return theme.format(Unicode.WATCH.string + seconds);
    }

    /**
     * Override me to tell if this attraction is currently playing!
     */
    public abstract boolean isPlaying();

    /** Whether the player is participating in an active game at this attraction. */
    public abstract boolean isParticipant(Player player);

    protected abstract void onTick();

    protected void onEnable() { }

    protected void onDisable() { }

    protected void onLoad() { }

    protected void onSave() { }

    public void onEntityDamage(EntityDamageEvent event) { }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) { }

    public void onEntityCombust(EntityCombustEvent event) { }

    public void onPlayerInteract(PlayerInteractEvent event) { }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) { }

    public void onProjectileHit(ProjectileHitEvent event) { }

    public void onPlayerTeleport(PlayerTeleportEvent event) { }

    public void onEntityToggleGlide(EntityToggleGlideEvent event) { }

    public void onPlayerBlockAbility(PlayerBlockAbilityQuery event) { }

    public void onEntityPathfind(EntityPathfindEvent event) { }

    public void onEntityTarget(EntityTargetEvent event) { }

    public void onPlayerQuit(final PlayerQuitEvent event) { }

    protected final ItemStack getRegularCompletionReward(Player player) {
        List<List<ItemStack>> list = getPrizePool();
        List<ItemStack> list2 = list.get(random.nextInt(list.size()));
        ItemStack item = list2.get(random.nextInt(list2.size()));
        return item.clone();
    }

    /**
     * Give unique prize in a chest and update the session.
     */
    protected final void giveFirstCompletionReward(Player player) {
        Session session = plugin.getSessions().of(player);
        session.clearPrizeWaiting(this);
        session.lockUnique(this);
        session.save();
        List<ItemStack> bonus = getBonusPrizePool();
        giveInGui(player, getFirstCompletionReward().clone(),
                  bonus.get(random.nextInt(bonus.size())).clone(),
                  bonus.get(random.nextInt(bonus.size())).clone(),
                  bonus.get(random.nextInt(bonus.size())).clone(),
                  bonus.get(random.nextInt(bonus.size())).clone());
    }

    /**
     * Give regular prize in a chest and update the session.
     */
    protected final void giveRegularCompletionReward(Player player) {
        Session session = plugin.getSessions().of(player);
        session.clearPrizeWaiting(this);
        session.save();
        List<ItemStack> bonus = getBonusPrizePool();
        giveInGui(player, getRegularCompletionReward(player),
                  bonus.get(random.nextInt(bonus.size())).clone(),
                  bonus.get(random.nextInt(bonus.size())).clone(),
                  bonus.get(random.nextInt(bonus.size())).clone(),
                  bonus.get(random.nextInt(bonus.size())).clone());
    }

    protected final void giveReward(final Player player, final boolean appliesForFirstCompletion) {
        complete(player, appliesForFirstCompletion, true);
    }

    protected final void prepareReward(final Player player, final boolean appliesForFirstCompletion) {
        complete(player, appliesForFirstCompletion, false);
    }

    private void complete(final Player player, final boolean appliesForFirstCompletion, final boolean giveImmediately) {
        final Session session = plugin.getSessions().of(player);
        final boolean uniqueLocked = session.isUniqueLocked(this);
        // Older sessions can have a unique reward recorded without a completion count.
        final boolean firstCompletion = session.getCompletionCount(this) == 0 && !uniqueLocked
            && session.getPrizeWaiting(this) == 0;
        final boolean firstReward = appliesForFirstCompletion && !uniqueLocked;
        session.addCompletion(this);
        if (giveImmediately) {
            if (firstReward) {
                giveFirstCompletionReward(player);
            } else {
                giveRegularCompletionReward(player);
            }
        } else {
            if (firstReward) {
                session.setFirstCompletionPrizeWaiting(this);
                session.lockUnique(this);
            } else {
                session.setRegularCompletionPrizeWaiting(this);
            }
            session.save();
        }
        new AttractionCompleteEvent(player, this, firstCompletion, firstReward).callEvent();
    }

    protected final void giveInGui(Player player, ItemStack prize, ItemStack... extras) {
        final int size = 27;
        Component title = GuiOverlay.BLANK.builder(size, DARK_RED).title(displayName).build();
        Gui gui = new Gui();
        gui.size(size);
        gui.setItem(13, prize);
        int[] indexes = {
            22, 4, 12, 14,
        };
        for (int i = 0; i < extras.length; i += 1) {
            if (i >= indexes.length) {
                throw new IllegalStateException("Index exceeded: " + i + "/" + indexes.length);
            }
            gui.setItem(indexes[i], extras[i]);
        }
        gui.setEditable(true);
        gui.title(title);
        gui.onClose(evt -> {
                PlayerReceiveItemsEvent.receiveInventory(player, gui.getInventory());
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            });
        gui.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    protected final boolean checkCooldown(Player player) {
        if (player.hasPermission("festival.nocooldown")) return true;
        Session session = plugin.getSessions().of(player);
        Duration cooldown = session.getCooldown(this);
        if (cooldown != null) {
            long minutes = cooldown.toMinutes();
            long seconds = cooldown.toSeconds() % 60L;
            Component message = text("Give others a chance and wait "
                                               + minutes + "m "
                                               + seconds + "s",
                                               RED);
            player.sendActionBar(message);
            return false;
        }
        return true;
    }

    /** Checked when offering entry and again before charging the player. */
    protected boolean canJoin(final Player player) {
        return !isParticipant(player);
    }

    protected final boolean checkInstrument(Player player) {
        for (ItemStack itemStack : player.getInventory()) {
            Mytems mytems = Mytems.forItem(itemStack);
            if (mytems != null && mytems.category == MytemsCategory.MUSIC) {
                return true;
            }
        }
        Component message = join(noSeparators(),
                                 text("You don't have a "),
                                 Mytems.ANGELIC_HARP.component,
                                 text("musical instrument!"))
            .color(RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    protected final boolean takeEntryFee(Player player) {
        for (ItemStack itemStack : player.getInventory()) {
            if (entryFee.isSimilar(itemStack)) {
                itemStack.subtract(1);
                return true;
            }
        }
        Component message = join(
            noSeparators(),
            text("You don't have "),
            ItemKinds.chatDescription(getEntryFee()),
            text("!")
        ).color(RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    /**
     * Admit the player after the entry checks. An attraction can start a run or join an existing one.
     */
    protected abstract void start(Player player);

    protected abstract void stop();

    /**
     * Player clickes the main villager.
     */
    protected void clickMainVillager(Player player) {
        if (!checkPermission(player)) return;
        if (checkPrizeWaiting(player)) return;
        Session session = plugin.getSessions().of(player);
        if (!checkCooldown(player)) return;
        if (!canJoin(player)) return;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                Component page = join(noSeparators(),
                                      displayName,
                                      newline(),
                                      description,
                                      (doesRequireInstrument
                                       ? join(noSeparators(),
                                              newline(),
                                              Mytems.ANGELIC_HARP.component,
                                              text("Musical Instrument Required", RED))
                                       : empty()),
                                      newline(),
                                      (!session.isUniqueLocked(this)
                                       ? text(Unicode.CHECKBOX.character + " Not yet finished", DARK_GRAY)
                                       : text(Unicode.CHECKED_CHECKBOX.character + " Finished", BLUE)),
                                      newline(),
                                      newline(),
                                      text("Play game for "),
                                      newline(),
                                      DefaultFont.bookmarked(color(0x888888), ItemKinds.chatDescription(getEntryFee())),
                                      text("?"),
                                      newline(),
                                      (DefaultFont.START_BUTTON.component
                                       .clickEvent(ClickEvent.runCommand("/fest yes"))
                                       .hoverEvent(HoverEvent.showText(text("Play this Game", GREEN)))),
                                      space(),
                                      (DefaultFont.CANCEL_BUTTON.component
                                       .clickEvent(ClickEvent.runCommand("/fest no"))
                                       .hoverEvent(HoverEvent.showText(text("Goodbye!", RED)))));
                meta.setAuthor("Cavetale");
                meta.title(text("Festival"));
                meta.pages(List.of(page));
            });
        player.openBook(book);
    }

    protected final boolean checkPermission(Player player) {
        final boolean creative = NetworkServer.current() == NetworkServer.CREATIVE;
        if (creative && !player.hasPermission("festival.testing")) {
            player.sendMessage(text("You don't have permission", RED));
            return false;
        }
        if (!player.hasPermission("festival.festival")) {
            player.sendMessage(text("You don't have permission", RED));
            return false;
        }
        return true;
    }

    protected final boolean checkPrizeWaiting(Player player) {
        Session session = plugin.getSessions().of(player);
        int prizeWaiting = session.getPrizeWaiting(this);
        if (prizeWaiting > 0) {
            session.clearPrizeWaiting(this);
            session.save();
            if (prizeWaiting == 2) {
                giveFirstCompletionReward(player);
                return true;
            } else {
                giveRegularCompletionReward(player);
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Player clicks "Yes" in the main villager dialogue.
     */
    public void onClickYes(Player player) {
        final boolean creative = NetworkServer.current() == NetworkServer.CREATIVE;
        if (creative && !player.hasPermission("festival.testing")) {
            player.sendMessage(text("You don't have permission", RED));
            return;
        }
        if (!checkCooldown(player)) return;
        if (!canJoin(player)) return;
        if (doesRequireInstrument && !checkInstrument(player)) return;
        if (!takeEntryFee(player)) return;
        if (creative) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        start(player);
    }

    public void onPluginPlayer(PluginPlayerEvent event) { }

    protected abstract static class SaveTag { }

    protected final void subtitle(Player player, Component component) {
        player.showTitle(Title.title(empty(), component));
    }

    protected final void confetti(Player player, Location location) {
        player.spawnParticle(Particle.ENTITY_EFFECT, location, 16, 0.25, 0.25, 0.25, 1.0,
                             Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
    }

    protected final void confetti(Location location) {
        location.getWorld().spawnParticle(Particle.ENTITY_EFFECT, location, 16, 0.25, 0.25, 0.25, 1.0,
                                          Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
    }

    public final List<Player> getPlayersIn(Cuboid area) {
        List<Player> result = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (area.contains(player.getLocation())) {
                result.add(player);
            }
        }
        return result;
    }

    public final String getUniqueKey() {
        return name;
    }

    /**
     * Print to the hud.
     */
    public void onPlayerHud(PlayerHudEvent event) { }


    /** Command via /fest send <args...> from inside this attraction. */
    public void onCommand(Player player, String[] args) { }
}
