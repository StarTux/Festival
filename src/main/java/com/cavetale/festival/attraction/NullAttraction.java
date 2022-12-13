package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.festival.session.Session;
import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

/**
 * Dummy attraction for the purpose of copy and paste.
 */
public final class NullAttraction extends Attraction<NullAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(60 * 3);
    protected long secondsLeft;

    protected NullAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if (area.name == null) continue;
        }
        this.displayName = booth.format("Null");
        this.description = text("Null");
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
        final long now = System.currentTimeMillis();
        final long then = saveTag.gameStarted + GAME_TIME.toMillis();
        secondsLeft = Math.max(0, (then - now - 1) / 1000L + 1L);
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
        if (player == null) return State.IDLE;
        if (secondsLeft <= 0L) {
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
        return null;
    }

    enum State {
        IDLE,
        PLAY {
            @Override protected void enter(NullAttraction instance) {
                instance.saveTag.gameStarted = System.currentTimeMillis();
                instance.saveTag.score = 0;
                instance.saveTag.total = 0;
            }

            @Override protected void exit(NullAttraction instance) { }

            @Override protected State tick(NullAttraction instance) {
                return instance.tickGame();
            }
        };

        protected void enter(NullAttraction instance) { }

        protected void exit(NullAttraction instance) { }

        protected State tick(NullAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
        protected int score;
        protected int total;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft, VanillaItems.DIAMOND.component, saveTag.score, saveTag.total),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) GAME_TIME.toSeconds());
    }
}
