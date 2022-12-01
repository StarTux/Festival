package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaEffects;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.festival.session.Session;
import com.cavetale.mytems.util.Entities;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

public final class ArcheryAttraction extends Attraction<ArcheryAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(60 * 2);
    protected final Map<TargetMob, List<Vec3i>> targetMobs = new EnumMap<>(TargetMob.class);
    protected long secondsLeft;
    protected Vec3i respawnVector = Vec3i.ZERO;
    protected final List<Cuboid> forbiddenZones = new ArrayList<>();

    @RequiredArgsConstructor
    public enum TargetMob {
        FROG(Frog.class, "Frog", 120),
        RABBIT(Rabbit.class, "Rabbit", 100),
        CHICKEN(Chicken.class, "Chicken", 80),
        BEE(Bee.class, "Bee", 240),
        SHEEP(Sheep.class, "Sheep", 80),
        PIG(Pig.class, "Pig", 80),
        ;

        public final String key = name().toLowerCase();
        public final Class<? extends Mob> entityClass;
        public final String displayName;
        public final int cooldown;

        public static TargetMob of(String in) {
            for (TargetMob it : values()) {
                if (in.equals(it.key)) return it;
            }
            return null;
        }

        public Mob spawn(Location location) {
            return location.getWorld().spawn(location, entityClass, e -> {
                    e.setPersistent(false);
                    e.setRemoveWhenFarAway(true);
                    e.setSilent(true);
                    Entities.setTransient(e);
                    if (e instanceof Pig pig) pig.setBaby();
                    if (e instanceof Sheep sheep) sheep.setBaby();
                });
        }
    }

    public static final class TargetMobData {
        protected int cooldown;
        protected List<UUID> uuids = new ArrayList<>();
    }

    protected ArcheryAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (TargetMob targetMob : TargetMob.values()) targetMobs.put(targetMob, new ArrayList<>());
        for (Area area : allAreas) {
            if (area.name == null) continue;
            TargetMob targetMob = TargetMob.of(area.name);
            if (targetMob != null) {
                targetMobs.get(targetMob).add(area.getMin());
                continue;
            }
            switch (area.name) {
            case "forbidden":
                forbiddenZones.add(area.toCuboid());
                break;
            case "respawn":
                respawnVector = area.getMin();
                break;
            default: break;
            }
        }
        this.displayName = booth.format("Archery");
        this.description = text("Shoot the moving targets, but avoid the glowing ones.");
        for (TargetMob targetMob : TargetMob.values()) {
            this.areaNames.add(targetMob.key);
        }
        this.areaNames.add("forbidden");
        this.areaNames.add("respawn");
        for (TargetMob targetMob : TargetMob.values()) {
            List<Vec3i> ls = targetMobs.get(targetMob);
            if (ls.size() != 2) {
                debugLine(targetMob.key + " bad size: " + ls.size() + "/2");
            }
        }
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        changeState(State.PLAY);
        player.showTitle(title(empty(),
                               text("Shoot!", GOLD, ITALIC),
                               times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)));
        player.sendMessage(text("Shoot!", GOLD, ITALIC));
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
    protected void onLoad() {
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (getEntityUuids().contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    protected void onDisable() {
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    /**
     * Called by EventListener.
     */
    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if (saveTag.state != State.PLAY) return;
        event.setCancelled(true);
        Projectile projectile = event.getEntity();
        switch (projectile.getType()) {
        case ARROW: case SPECTRAL_ARROW: break;
        default: return;
        }
        projectile.remove();
        if (!(projectile.getShooter() instanceof Player player)) return;
        if (!player.getUniqueId().equals(saveTag.currentPlayer)) return;
        Entity hitEntity = event.getHitEntity();
        if (hitEntity != null) {
            final UUID uuid = hitEntity.getUniqueId();
            final TargetMobData targetMobData = findTargetMobData(uuid);
            if (targetMobData == null) return;
            hitEntity.remove();
            if (hitEntity.isGlowing()) {
                fail(player, "Do not hit glowing targets");
                saveTag.wrong += 1;
                changeState(State.IDLE);
            } else {
                confetti(hitEntity.getLocation());
                targetMobData.uuids.remove(uuid);
                progress(player);
                saveTag.score += 1;
            }
        }
    }

    private void clearEntities() {
        for (UUID uuid : getEntityUuids()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
    }

    protected State tickGame() {
        Player player = getCurrentPlayer();
        if (player == null || !isInArea(player.getLocation())) return State.IDLE;
        final long now = System.currentTimeMillis();
        final long then = saveTag.gameStarted + GAME_TIME.toMillis();
        secondsLeft = Math.max(0, (then - now - 1) / 1000L + 1L);
        if (secondsLeft <= 0L) {
            Session session = festival.sessionOf(player);
            boolean perfectRound = saveTag.missed == 0 && saveTag.wrong == 0;
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
        if (saveTag.missed >= 10) {
            fail(player, "Too many targets escaped");
            return State.IDLE;
        }
        for (TargetMob targetMob : TargetMob.values()) {
            tickTargetMob(player, targetMob);
        }
        final Location location = player.getLocation();
        for (Cuboid zone : forbiddenZones) {
            if (zone.contains(location)) {
                Vec3i vec = respawnVector.equals(Vec3i.ZERO)
                    ? npcVector
                    : respawnVector;
                Location target = vec.toCenterFloorLocation(world);
                target.setPitch(location.getPitch());
                target.setYaw(location.getYaw());
                player.teleport(target);
                player.sendMessage(text("Do not step on the shooting range", RED));
            }
        }
        return null;
    }

    private static int signum(int d) {
        if (d == 0) return 0;
        return d > 0 ? 1 : -1;
    }

    private void tickTargetMob(Player player, TargetMob targetMob) {
        List<Vec3i> vectors = targetMobs.get(targetMob);
        if (vectors.size() != 2) return;
        final Vec3i from = vectors.get(0);
        final Vec3i to = vectors.get(1);
        TargetMobData data = saveTag.targetMobs.get(targetMob.key);
        assert data != null;
        for (UUID uuid : List.copyOf(data.uuids)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity == null) {
                data.uuids.remove(uuid);
                continue;
            }
            Location mobLocation = entity.getLocation();
            if (to.contains(mobLocation)) {
                if (!entity.isGlowing()) {
                    saveTag.missed += 1;
                    player.sendActionBar(text(targetMob.displayName + " Escaped", RED));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
                }
                data.uuids.remove(uuid);
                entity.remove();
            } else if (entity instanceof Mob mob) {
                mob.getPathfinder().moveTo(to.toCenterFloorLocation(world));
            }
        }
        if (data.cooldown > 0) {
            data.cooldown -= 1;
        } else {
            data.cooldown = targetMob.cooldown + 20 * (random.nextInt(4) - random.nextInt(4));
            Mob mob = targetMob.spawn(from.toCenterFloorLocation(world));
            if (mob != null) {
                data.uuids.add(mob.getUniqueId());
                Bukkit.getMobGoals().removeAllGoals(mob);
                if (random.nextInt(5) == 0) {
                    mob.setGlowing(true);
                } else {
                    saveTag.spawned += 1;
                }
            }
        }
    }

    enum State {
        IDLE,
        PLAY {
            @Override protected void enter(ArcheryAttraction instance) {
                instance.saveTag.gameStarted = System.currentTimeMillis();
                for (TargetMob targetMob : TargetMob.values()) {
                    TargetMobData data = new TargetMobData();
                    data.cooldown = targetMob.cooldown;
                    instance.saveTag.targetMobs.put(targetMob.key, data);
                }
                instance.saveTag.score = 0;
                instance.saveTag.spawned = 0;
                instance.saveTag.missed = 0;
                instance.saveTag.wrong = 0;
            }

            @Override protected void exit(ArcheryAttraction instance) {
                instance.clearEntities();
                instance.saveTag.targetMobs.clear();
            }

            @Override protected State tick(ArcheryAttraction instance) {
                return instance.tickGame();
            }
        };

        protected void enter(ArcheryAttraction instance) { }

        protected void exit(ArcheryAttraction instance) { }

        protected State tick(ArcheryAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
        protected int score;
        protected int spawned;
        protected int missed;
        protected int wrong;
        protected Map<String, TargetMobData> targetMobs = new HashMap<>();
    }

    private List<UUID> getEntityUuids() {
        List<UUID> result = new ArrayList<>();
        for (TargetMobData data : saveTag.targetMobs.values()) {
            result.addAll(data.uuids);
        }
        return result;
    }

    private TargetMobData findTargetMobData(UUID uuid) {
        for (TargetMobData data : saveTag.targetMobs.values()) {
            if (data.uuids.contains(uuid)) return data;
        }
        return null;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft, VanillaEffects.SPEED, saveTag.missed, 10),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) GAME_TIME.toSeconds());
    }
}
