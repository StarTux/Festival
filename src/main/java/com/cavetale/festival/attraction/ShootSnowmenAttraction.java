package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.festival.session.Session;
import com.cavetale.mytems.util.Entities;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;
import static org.bukkit.attribute.Attribute.*;

public final class ShootSnowmenAttraction extends Attraction<ShootSnowmenAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(60 * 2);
    protected final Set<Vec3i> snowmanBlocks = new HashSet<>();
    protected long secondsLeft;
    protected int mobsPerWave = 9;

    protected ShootSnowmenAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if (area.name == null) continue;
            switch (area.name) {
            case "snowman":
                snowmanBlocks.addAll(area.enumerate());
                break;
            default: break;
            }
        }
        this.displayName = booth.format("Snow Golem Attack");
        this.description = text("Hit the the evil snowmen with snowballs, but spare the friendly ones.");
        this.areaNames.add("snowman");
        if (snowmanBlocks.isEmpty()) {
            debugLine("No snowman blocks");
        }
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        changeState(State.PLAY);
        player.showTitle(title(empty(),
                               text("Go!", GREEN, ITALIC),
                               times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)));
        player.sendMessage(text("Go!", GREEN, ITALIC));
        startingGun(player);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void onTick() {
        State newState = saveTag.state.tick(this);
        if (newState != null) {
            changeState(newState);
        }
    }

    @Override
    protected void onLoad() { }

    @Override
    protected void onDisable() { }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    protected State tickGame() {
        Player player = getCurrentPlayer();
        if (player == null || !isInArea(player.getLocation())) return State.IDLE;
        final long now = System.currentTimeMillis();
        final long then = saveTag.gameStarted + GAME_TIME.toMillis();
        secondsLeft = Math.max(0, (then - now - 1) / 1000L + 1L);
        if (secondsLeft <= 0L) {
            timeout(player);
            return State.IDLE;
        }
        if (saveTag.score >= saveTag.total) {
            Session session = festival.sessionOf(player);
            boolean perfectRound = saveTag.score >= saveTag.total;
            if (perfectRound) {
                perfect(player);
                prepareReward(player, true);
                session.setCooldown(this, completionCooldown);
            } else {
                victory(player);
                prepareReward(player, false);
                session.setCooldown(this, session.isUniqueLocked(this)
                                    ? completionCooldown
                                    : Duration.ofSeconds(30));
            }
            return State.IDLE;
        }
        int alive = 0;
        for (UUID uuid : List.copyOf(saveTag.snowmen)) {
            if (!(Bukkit.getEntity(uuid) instanceof Snowman snowman)) {
                saveTag.snowmen.remove(uuid);
                continue;
            }
            if (!snowman.isDerp()) {
                alive += 1;
            }
        }
        if (alive == 0) {
            clearMobs();
            spawnMobs();
            progress(player);
            player.sendActionBar(booth.format("Way to go!"));
        }
        return null;
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        event.setCancelled(true);
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        snowball.remove();
        if (!(snowball.getShooter() instanceof Player player)) return;
        if (!isCurrentPlayer(player)) return;
        if (!(event.getHitEntity() instanceof Snowman snowman)) return;
        if (!saveTag.snowmen.contains(snowman.getUniqueId())) return;
        if (snowman.isDerp()) {
            fail(player);
            changeState(State.IDLE);
            player.sendMessage(text("You have to spare the friendly snowmen", RED));
        } else {
            confetti(snowman.getEyeLocation());
            snowman.remove();
            saveTag.score += 1;
            progress(player);
        }
    }

    enum State {
        IDLE,
        PLAY {
            @Override protected void enter(ShootSnowmenAttraction instance) {
                instance.saveTag.gameStarted = System.currentTimeMillis();
                instance.saveTag.score = 0;
                instance.saveTag.total = 54;
                instance.spawnMobs();
            }

            @Override protected void exit(ShootSnowmenAttraction instance) {
                instance.clearMobs();
            }

            @Override protected State tick(ShootSnowmenAttraction instance) {
                return instance.tickGame();
            }
        };

        protected void enter(ShootSnowmenAttraction instance) { }

        protected void exit(ShootSnowmenAttraction instance) { }

        protected State tick(ShootSnowmenAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
        protected int score;
        protected int total;
        protected List<UUID> snowmen = new ArrayList<>();
    }

    private void spawnMobs() {
        List<Block> blocks = new ArrayList<>();
        for (Vec3i vec : snowmanBlocks) {
            Block block = vec.toBlock(world);
            if (block.isEmpty() && block.getRelative(0, 1, 0).isEmpty()) {
                blocks.add(block);
            }
        }
        Collections.shuffle(blocks, random);
        final int total = mobsPerWave;
        for (int i = 0; i < total; i += 1) {
            Block block = blocks.get(i % blocks.size());
            final boolean derp = i < total / 3;
            Snowman snowman = world.spawn(block.getLocation().add(0.5, 0.0, 0.5), Snowman.class, s -> {
                    Entities.setTransient(s);
                    s.getAttribute(GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
                    s.setDerp(derp);
                });
            if (snowman != null) {
                saveTag.snowmen.add(snowman.getUniqueId());
            }
        }
        for (int i = 0; i < total; i += Material.SNOWBALL.getMaxStackSize()) {
            ItemStack item = new ItemStack(Material.SNOWBALL, Math.min(total, total - i));
            getCurrentPlayer().getInventory().addItem(item);
        }
    }

    private void clearMobs() {
        for (UUID uuid : saveTag.snowmen) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        saveTag.snowmen.clear();
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft, VanillaItems.SNOWBALL.component, saveTag.score, saveTag.total),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) GAME_TIME.toSeconds());
    }
}
