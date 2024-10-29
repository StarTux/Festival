package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static com.cavetale.core.font.Unicode.subscript;
import static com.cavetale.core.font.Unicode.superscript;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

/**
 * Dummy attraction for the purpose of copy and paste.
 */
public final class GuessNumberAttraction extends Attraction<GuessNumberAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(70);
    protected final int maxAttempts = 7;
    protected long secondsLeft;

    protected GuessNumberAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if (area.name == null) continue;
        }
        this.displayName = booth.format("Guess the Number");
        this.description = text("I will think of a number between 1 and 100. You may guess 7 times.");
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        changeState(State.PLAY);
        startingGun(player);
        openBook(player);
        player.sendMessage(textOfChildren(newline(),
                                          Mytems.ARROW_RIGHT, text(" Click here to make a guess", GREEN),
                                          newline())
                           .hoverEvent(showText(text("Open the guessing selection")))
                           .clickEvent(runCommand("/fest send " + name)));
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
            timeout(player);
            return State.IDLE;
        }
        return null;
    }

    @Override
    public void onCommand(Player player, String[] args) {
        if (!isCurrentPlayer(player)) return;
        if (args.length == 0) {
            openBook(player);
            return;
        } else if (args.length != 1) {
            return;
        }
        final int guess;
        try {
            guess(player, Integer.parseInt(args[0]));
        } catch (NumberFormatException nfe) {
            return;
        }
    }

    private String getGuessMessage() {
        if (saveTag.guess == 0) return "Guess the number";
        return saveTag.guess < saveTag.number
            ? "" + saveTag.guess + " is too low"
            : "" + saveTag.guess + " is too high";
    }

    private void guess(Player player, int guess) {
        if (guess < 1 || guess > 100) return;
        if (guess == saveTag.number) {
            perfect(player);
            player.sendMessage(booth.format("Correct! The number is " + saveTag.number));
            prepareReward(player, true);
            festival.sessionOf(player).setCooldown(this, completionCooldown);
            changeState(State.IDLE);
            return;
        }
        saveTag.guess = guess;
        player.sendMessage(booth.format(getGuessMessage()));
        saveTag.guesses.add(guess);
        saveTag.attempts += 1;
        if (saveTag.attempts >= 7) {
            fail(player, "The number was " + saveTag.number);
            changeState(State.IDLE);
        } else {
            openBook(player);
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, SoundCategory.MASTER, 1f, 0.5f);
        }
    }

    private void openBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                if (!(m instanceof BookMeta meta)) return;
                meta.author(text("Cavetale"));
                meta.title(text("Festival"));
                List<Component> lines = new ArrayList<>();
                lines.add(booth.format(superscript(saveTag.attempts + 1) + "/" + subscript(maxAttempts)
                                       + " " + getGuessMessage()));
                List<Component> numbers = new ArrayList<>();
                for (int i = 1; i <= 100; i += 1) {
                    if (saveTag.guesses.contains(i)) {
                        boolean low = i < saveTag.number;
                        numbers.add(text(subscript(i), low ? BLACK : DARK_RED)
                                    .hoverEvent(showText(text("" + i + " was too " + (low ? "low" : "high"), low ? BLUE : RED))));
                    } else {
                        numbers.add(text(subscript(i), BLUE)
                                    .hoverEvent(showText(text("Guess " + i, BLUE)))
                                    .clickEvent(runCommand("/fest send " + name + " " + i)));
                    }
                }
                lines.add(join(separator(space()), numbers));
                meta.pages(join(separator(newline()), lines));
            });
        player.closeInventory();
        player.openBook(book);
    }

    enum State {
        IDLE,
        PLAY {
            @Override protected void enter(GuessNumberAttraction instance) {
                instance.saveTag.gameStarted = System.currentTimeMillis();
                instance.saveTag.number = 1 + instance.random.nextInt(100);
                instance.saveTag.guess = 0;
                instance.saveTag.attempts = 0;
                instance.saveTag.guesses.clear();
            }

            @Override protected void exit(GuessNumberAttraction instance) { }

            @Override protected State tick(GuessNumberAttraction instance) {
                return instance.tickGame();
            }
        };

        protected void enter(GuessNumberAttraction instance) { }

        protected void exit(GuessNumberAttraction instance) { }

        protected State tick(GuessNumberAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
        protected int number;
        protected int guess;
        protected int attempts;
        protected List<Integer> guesses = new ArrayList<>();
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft, Mytems.QUESTION_MARK.component, saveTag.attempts, maxAttempts),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) GAME_TIME.toSeconds());
    }
}
