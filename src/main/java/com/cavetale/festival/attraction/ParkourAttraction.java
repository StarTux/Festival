package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.festival.session.Session;
import java.time.Duration;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

public final class ParkourAttraction extends Attraction<ParkourAttraction.SaveTag> {
    protected Duration gameTime = Duration.ofSeconds(60);
    protected long secondsLeft;
    protected Cuboid goal = Cuboid.ZERO;

    protected ParkourAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if (area.name == null) continue;
            switch (area.name) {
                case "goal":
                    if (!goal.equals(Cuboid.ZERO)) {
                        debugLine("Multiple goal areas");
                    }
                    goal = area.toCuboid();
                    break;
            default: break;
            }
        }
        this.displayName = booth.format("Parkour");
        this.description = text("Can you get to the goal in time?");
        this.areaNames.add("goal");
        if (goal.equals(Cuboid.ZERO)) {
            debugLine("No goal area");
        }
        this.intKeys.add("time");
    }

    @Override
    protected void onEnable() {
        Map<String, Object> raw = getFirstArea().getRaw() != null
            ? getFirstArea().getRaw()
            : Map.of();
        if (raw.get("time") instanceof Number number) {
            this.gameTime = Duration.ofSeconds(number.intValue());
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

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!isCurrentPlayer(player)) return;
        fail(player, "No teleporting");
        changeState(State.IDLE);
    }

    @Override
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isCurrentPlayer(player)) return;
        if (event.isGliding()) {
            fail(player, "No flying");
            changeState(State.IDLE);
        }
    }

    @Override
    public void onPlayerBlockAbility(PlayerBlockAbilityQuery event) {
        if (!isCurrentPlayer(event.getPlayer())) return;
        switch (event.getAction()) {
        case FLY:
            event.setCancelled(true);
            break;
        default: break;
        }
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    protected State tickGame() {
        Player player = getCurrentPlayer();
        if (player == null || !isInArea(player.getLocation())) return State.IDLE;
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            fail(player, "No flying");
            return State.IDLE;
        }
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            fail(player, "No Levitation");
            return State.IDLE;
        }
        final long now = System.currentTimeMillis();
        final long then = saveTag.gameStarted + gameTime.toMillis();
        secondsLeft = Math.max(0, (then - now - 1) / 1000L + 1L);
        if (goal.contains(player.getLocation())) {
            Session session = festival.sessionOf(player);
            victory(player);
            prepareReward(player, true);
            session.setCooldown(this, completionCooldown);
            Duration time = Duration.ofMillis(now - saveTag.gameStarted);
            player.sendMessage(booth.format(String.format("You completed in %02d:%02d.%03d",
                                                          time.toMinutes(),
                                                          time.toSeconds() % 60L,
                                                          time.toMillis() % 1000L)));
            return State.IDLE;
        }
        if (secondsLeft <= 0L) {
            timeout(player);
            return State.IDLE;
        }
        return null;
    }

    enum State {
        IDLE,
        PLAY {
            @Override protected void enter(ParkourAttraction instance) {
                instance.saveTag.gameStarted = System.currentTimeMillis();
            }

            @Override protected void exit(ParkourAttraction instance) { }

            @Override protected State tick(ParkourAttraction instance) {
                return instance.tickGame();
            }
        };

        protected void enter(ParkourAttraction instance) { }

        protected void exit(ParkourAttraction instance) { }

        protected State tick(ParkourAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) gameTime.toSeconds());
    }
}
