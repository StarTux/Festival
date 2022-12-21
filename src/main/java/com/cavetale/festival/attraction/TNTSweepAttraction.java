package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.festival.gui.Gui;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.util.Items;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.subscript;
import static com.cavetale.core.font.Unicode.superscript;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

/**
 * Dummy attraction for the purpose of copy and paste.
 */
public final class TNTSweepAttraction extends Attraction<TNTSweepAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(60 * 5);
    protected long secondsLeft;
    protected int width = 9;
    protected int height = 6;
    private static final int EMPTY = -1;
    private static final int TNT = -2;
    private static final int BOOM = -3;
    private static final int MARKED_EMPTY = -4;
    private static final int MARKED_TNT = -5;

    protected TNTSweepAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if (area.name == null) continue;
        }
        this.displayName = booth.format("TNT Sweep");
        this.description = text("Reveal slots but avoid the hidden TNT. Numbers indicate how much TNT is adjacent.");
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        changeState(State.PLAY);
        startingGun(player);
        makeBoard();
        openGui(player);
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

    protected State tickEnd() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        long time = System.currentTimeMillis() - saveTag.endStarted;
        if (time > 10_000) {
            return State.IDLE;
        }
        Gui gui = Gui.of(player);
        if (gui == null || gui.getPrivateData() != this) {
            return State.IDLE;
        }
        return null;
    }

    enum State {
        IDLE {
            @Override protected void enter(TNTSweepAttraction instance) {
                instance.closeAll();
            }
        },
        PLAY {
            @Override protected void enter(TNTSweepAttraction instance) {
                instance.saveTag.gameStarted = System.currentTimeMillis();
                instance.saveTag.score = 0;
                instance.saveTag.total = 0;
            }

            @Override protected void exit(TNTSweepAttraction instance) { }

            @Override protected State tick(TNTSweepAttraction instance) {
                return instance.tickGame();
            }
        },
        LOSE {
            @Override protected void enter(TNTSweepAttraction instance) {
                instance.saveTag.endStarted = System.currentTimeMillis();
            }

            @Override protected State tick(TNTSweepAttraction instance) {
                return instance.tickEnd();
            }
        },
        WIN {
            @Override protected void enter(TNTSweepAttraction instance) {
                instance.saveTag.endStarted = System.currentTimeMillis();
            }

            @Override protected State tick(TNTSweepAttraction instance) {
                return instance.tickEnd();
            }
        };

        protected void enter(TNTSweepAttraction instance) { }

        protected void exit(TNTSweepAttraction instance) { }

        protected State tick(TNTSweepAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
        protected long endStarted;
        protected int score;
        protected int total;
        protected List<Integer> board = new ArrayList<>();
    }

    @Override
    protected void clickMainVillager(Player player) {
        Player currentPlayer = getCurrentPlayer();
        if (saveTag.state == State.PLAY) {
            assert currentPlayer != null;
            openGui(player);
            if (!isCurrentPlayer(player)) {
                player.sendMessage(textOfChildren(text("Spectating ", GREEN), currentPlayer.displayName()));
            }
        } else {
            super.clickMainVillager(player);
        }
    }

    private void openAll() {
        for (Player player : world.getPlayers()) {
            if (isCurrentPlayer(player)) {
                openGui(player);
            } else {
                Gui gui = Gui.of(player);
                if (gui != null && gui.getPrivateData() == this) {
                    openGui(player);
                }
            }
        }
    }

    private void closeAll() {
        for (Player player : world.getPlayers()) {
            Gui gui = Gui.of(player);
            if (gui != null && gui.getPrivateData() == this) {
                player.closeInventory();
            }
        }
    }

    private void openGui(Player player) {
        final int size = width * height;
        Gui gui = new Gui().size(size);
        final TextColor bg;
        final Component title;
        if (saveTag.state == State.LOSE) {
            bg = RED;
            title = textOfChildren(Mytems.SURPRISED, text(" You Lost", DARK_RED, BOLD));
        } else if (saveTag.state == State.WIN) {
            bg = GREEN;
            title = textOfChildren(Mytems.COOL, text(" You Win ", GOLD, BOLD));
        } else {
            bg = GRAY;
            title = textOfChildren(Mytems.SMILE,
                                   text(" TNT Sweep ", RED),
                                   text(superscript(saveTag.score) + "/" + subscript(saveTag.total)));
        }
        GuiOverlay.Builder overlay = GuiOverlay.BLANK.builder(size, bg).title(title);
        for (int cy = 0; cy < height; cy += 1) {
            for (int cx = 0; cx < width; cx += 1) {
                final int cell = saveTag.board.get(cx + cy * width);
                final ItemStack icon;
                if (cell == 0) {
                    icon = Mytems.INVISIBLE_ITEM.createIcon(List.of(text("Empty", GRAY)));
                } else if (cell > 0) {
                    icon = Glyph.toGlyph((char) ('0' + cell)).mytems.createIcon(List.of(textOfChildren(text(cell, BLUE),
                                                                                                       VanillaItems.TNT,
                                                                                                       text(" adjacent", BLUE))));
                } else if (cell == EMPTY) {
                    icon = Mytems.CHECKBOX.createIcon(List.of(textOfChildren(Mytems.MOUSE_LEFT, text(" Reveal", GRAY)),
                                                              textOfChildren(Mytems.MOUSE_RIGHT, text(" Mark", GRAY))));
                } else if (cell == TNT) {
                    if (saveTag.state == State.LOSE) {
                        icon = Items.text(new ItemStack(Material.TNT),
                                          List.of(text("Boom! You lost.", RED)));
                    } else {
                        icon = Mytems.CHECKBOX.createIcon(List.of(textOfChildren(Mytems.MOUSE_LEFT, text(" Reveal", GRAY)),
                                                                  textOfChildren(Mytems.MOUSE_RIGHT, text(" Mark", GRAY))));
                    }
                } else if (cell == BOOM) {
                    icon = Items.text(new ItemStack(Material.TNT),
                                      List.of(text("Boom! You lost.", RED)));
                } else if (cell == MARKED_EMPTY || cell == MARKED_TNT) {
                    icon = Mytems.CROSSED_CHECKBOX.createIcon(List.of(textOfChildren(Mytems.MOUSE_RIGHT, text(" Unmark", GRAY))));
                } else {
                    icon = Mytems.QUESTION_MARK.createIcon(List.of(text(cell, DARK_RED)));
                }
                final int x = cx;
                final int y = cy;
                gui.setItem(x, y, icon, click -> {
                        if (!isCurrentPlayer(player) || saveTag.state != State.PLAY) {
                            return;
                        }
                        if (click.isLeftClick()) {
                            if (reveal(player, x, y)) {
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                                if (saveTag.state == State.PLAY && saveTag.score == saveTag.total) {
                                    perfect(player);
                                    prepareReward(player, true);
                                    festival.sessionOf(player).setCooldown(this, completionCooldown);
                                    changeState(State.WIN);
                                    openAll();
                                }
                            }
                        } else if (click.isRightClick()) {
                            int newCell = cell;
                            if (cell == EMPTY) {
                                newCell = MARKED_EMPTY;
                            } else if (cell == TNT) {
                                newCell = MARKED_TNT;
                            } else if (cell == MARKED_EMPTY) {
                                newCell = EMPTY;
                            } else if (cell == MARKED_TNT) {
                                newCell = TNT;
                            } else {
                                return;
                            }
                            if (cell != newCell) {
                                saveTag.board.set(x + y * width, newCell);
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                                openAll();
                            }
                        }
                    });
            }
        }
        gui.title(overlay.build());
        gui.setPrivateData(this);
        gui.open(player);
    }

    private void makeBoard() {
        saveTag.board.clear();
        final int size = width * height;
        final int bombs = size / 8;
        saveTag.total = size - bombs;
        saveTag.score = 0;
        for (int i = 0; i < width * height; i += 1) {
            saveTag.board.add(i < bombs ? TNT : EMPTY);
        }
        Collections.shuffle(saveTag.board, random);
    }

    private boolean reveal(Player player, int x, int y) {
        int cell = saveTag.board.get(x + y * width);
        if (cell == TNT && saveTag.score == 0) {
            cell = -1;
            do {
                Collections.shuffle(saveTag.board, random);
            } while (saveTag.board.get(x + y * width) != cell);
        }
        if (cell == EMPTY) {
            revealRec(x, y);
            openAll();
            return true;
        } else if (cell == TNT) {
            saveTag.board.set(x + y * width, BOOM);
            fail(player);
            changeState(State.LOSE);
            openAll();
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1.0f, 1.0f);
            return false;
        } else {
            return false;
        }
    }

    private void revealRec(int x, int y) {
        if (x < 0 || x >= width) return;
        if (y < 0 || y >= height) return;
        int cell = saveTag.board.get(x + y * width);
        if (cell != EMPTY) return;
        cell = 0;
        final int ax = Math.max(0, x - 1);
        final int bx = Math.min(width - 1, x + 1);
        final int ay = Math.max(0, y - 1);
        final int by = Math.min(height - 1, y + 1);
        for (int cy = ay; cy <= by; cy += 1) {
            for (int cx = ax; cx <= bx; cx += 1) {
                int cell2 = saveTag.board.get(cx + cy * width);
                if (cell2 <= TNT) {
                    cell += 1;
                }
            }
        }
        saveTag.board.set(x + y * width, cell);
        saveTag.score += 1;
        if (cell != 0) return;
        revealRec(x - 1, y);
        revealRec(x + 1, y);
        revealRec(x, y - 1);
        revealRec(x, y + 1);
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft, VanillaItems.TNT.component, saveTag.score, saveTag.total),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) GAME_TIME.toSeconds());
    }
}
