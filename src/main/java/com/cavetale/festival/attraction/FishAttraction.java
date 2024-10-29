package com.cavetale.festival.attraction;

import com.cavetale.core.font.VanillaItems;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class FishAttraction extends Attraction<FishAttraction.SaveTag> {
    protected static final Duration PLAY_TIME = Duration.ofSeconds(120);
    protected int secondsLeft;
    private int requiredScore = 3;

    protected FishAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        this.displayName = booth.format("Candy Fishing");
        this.description = text("Can you catch " + requiredScore + " Halloween Candies out of the water in time?");
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        startingGun(player);
        changeState(State.PLAY);
        saveTag.progress = 0;
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public void onTick() {
        if (saveTag.state == State.IDLE) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        State newState = saveTag.state.tick(this, player);
        if (newState != null) changeState(newState);
    }

    protected State tickPlay(Player player) {
        long now = System.currentTimeMillis();
        long timeout = saveTag.playStarted + PLAY_TIME.toMillis();
        if (now > timeout) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((timeout - now - 1) / 1000L) + 1;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            final List<Component> progress = new ArrayList<>();
            for (int i = 0; i < requiredScore; i += 1) {
                if (i < saveTag.progress) {
                    progress.add(Mytems.ORANGE_CANDY.asComponent());
                } else {
                    progress.add(Mytems.ORANGE_CANDY.getColoredComponent(BLACK));
                }
            }
            player.sendActionBar(textOfChildren(VanillaItems.CLOCK,
                                                text(seconds + " ", GOLD),
                                                join(noSeparators(), progress)));
        }
        return null;
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    public void onPlayerFish(PlayerFishEvent event) {
        final Player player = event.getPlayer();
        if (!isPlaying() || !player.equals(getCurrentPlayer())) {
            return;
        }
        switch (event.getState()) {
        case CAUGHT_FISH: break;
        default: return;
        }
        if (random.nextInt(3) == 0) {
            if (event.getCaught() instanceof Item item) {
                final List<Mytems> prizePool = List.of(Mytems.CANDY_CORN,
                                                 Mytems.CHOCOLATE_BAR,
                                                 Mytems.LOLLIPOP,
                                                 Mytems.ORANGE_CANDY);
                final Mytems prize = prizePool.get(random.nextInt(prizePool.size()));
                item.setItemStack(prize.createItemStack());
            }
            saveTag.progress += 1;
            if (saveTag.progress >= requiredScore) {
                victory(player);
                prepareReward(player, true);
                festival.sessionOf(player).setCooldown(this, completionCooldown);
                changeState(State.IDLE);
            } else {
                progress(player);
            }
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, SoundCategory.MASTER, 1f, 0.5f);
        }
    }

    enum State {
        IDLE,
        PLAY {
            @Override protected State tick(FishAttraction instance, Player player) {
                return instance.tickPlay(player);
            }

            @Override protected void enter(FishAttraction instance) {
                instance.saveTag.playStarted = System.currentTimeMillis();
            }

            @Override protected void exit(FishAttraction instance) {
            }
        };

        protected void enter(FishAttraction instance) { }

        protected void exit(FishAttraction instance) { }

        protected State tick(FishAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long playStarted;
        protected int progress;
    }
}
