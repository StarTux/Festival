package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.festival.session.Session;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

/**
 * Dummy attraction for the purpose of copy and paste.
 */
public final class TicTacToeAttraction extends Attraction<TicTacToeAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(60);
    protected long secondsLeft;
    protected Cuboid boardArea;

    protected TicTacToeAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if (area.name == null) continue;
            switch (area.name) {
            case "board": this.boardArea = area.toCuboid();
            default: break;
            }
        }
        this.displayName = booth.format("Tic Tac Toe");
        this.description = text("We take turns to place blocks. First to place 3 in a row wins the game.");
        if (boardArea == null) {
            debugLine("Missing board");
        } else if (boardArea.getVolume() != 9) {
            debugLine("Invalid board size: " + boardArea.getVolume() + "/" + 9);
        }
        this.areaNames.add("board");
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        changeState(random.nextBoolean() ? State.PLAYER_TURN : State.COMPUTER_TURN);
        player.showTitle(title(empty(),
                               text("Go!", GREEN, ITALIC),
                               times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)));
        startingGun(player);
        getBoard().draw();
        saveTag.gameStarted = System.currentTimeMillis();
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

    private boolean updateTimer(Player player) {
        final long now = System.currentTimeMillis();
        final long then = saveTag.gameStarted + GAME_TIME.toMillis();
        secondsLeft = Math.max(0, (then - now - 1) / 1000L + 1L);
        if (secondsLeft > 0) return true;
        timeout(player);
        return false;
    }

    protected State tickPlayerTurn() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        if (!updateTimer(player)) return State.IDLE;
        Session session = festival.sessionOf(player);
        return null;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (saveTag.state != State.PLAYER_TURN) return;
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
        case LEFT_CLICK_BLOCK:
            break;
        default: return;
        }
        Player player = getCurrentPlayer();
        if (player == null) return;
        if (!event.getPlayer().equals(player)) return;
        final Vec3i vector = Vec3i.of(event.getClickedBlock());
        Board board = getBoard();
        int index = List.of(board.blocks).indexOf(vector);
        if (index < 0) return;
        if (board.cells[index] != 0) return;
        board.cells[index] = 1;
        board.draw();
        if (board.getWinner() == 1) {
            perfect(player);
            prepareReward(player, true);
            festival.sessionOf(player).setCooldown(this, completionCooldown);
            changeState(State.WIN);
        } else {
            changeState(State.COMPUTER_TURN);
        }
        Location location = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.5, 0.5);
        player.playSound(location, Sound.BLOCK_STONE_PLACE, SoundCategory.MASTER, 1.0f, 1.0f);
        confetti(location);
    }

    protected State tickComputerTurn() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        if (!updateTimer(player)) return State.IDLE;
        if (saveTag.aiTicks > 0) {
            saveTag.aiTicks -= 1;
            return null;
        } else {
            Board board = getBoard();
            int i = board.getNextAiMove();
            if (i < 0) {
                fail(player, "It's a draw!");
                return State.DRAW;
            }
            board.cells[i] = 2;
            board.draw();
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_PLACE, SoundCategory.MASTER, 1.0f, 1.0f);
            if (board.getWinner() == 2) {
                fail(player, "Looks like I won");
                return State.LOSE;
            } else if (board.isFull()) {
                fail(player, "It's a draw!");
                return State.DRAW;
            } else {
                return State.PLAYER_TURN;
            }
        }
    }

    protected State tickEnd() {
        if (saveTag.endTicks > 0) {
            saveTag.endTicks -= 1;
            return null;
        } else {
            return State.IDLE;
        }
    }

    enum State {
        IDLE {
            @Override protected void enter(TicTacToeAttraction instance) {
                instance.clearBoard();
            }
        },
        PLAYER_TURN {
            @Override protected State tick(TicTacToeAttraction instance) {
                return instance.tickPlayerTurn();
            }
        },
        COMPUTER_TURN {
            @Override protected void enter(TicTacToeAttraction instance) {
                instance.saveTag.aiTicks = 40;
            }

            @Override protected State tick(TicTacToeAttraction instance) {
                return instance.tickComputerTurn();
            }
        },
        WIN {
            @Override protected void enter(TicTacToeAttraction instance) {
                instance.saveTag.endTicks = 100;
            }

            @Override protected State tick(TicTacToeAttraction instance) {
                return instance.tickEnd();
            }
        },
        LOSE {
            @Override protected void enter(TicTacToeAttraction instance) {
                instance.saveTag.endTicks = 100;
            }

            @Override protected State tick(TicTacToeAttraction instance) {
                return instance.tickEnd();
            }
        },
        DRAW {
            @Override protected void enter(TicTacToeAttraction instance) {
                instance.saveTag.endTicks = 100;
            }

            @Override protected State tick(TicTacToeAttraction instance) {
                return instance.tickEnd();
            }
        };

        protected void enter(TicTacToeAttraction instance) { }

        protected void exit(TicTacToeAttraction instance) { }

        protected State tick(TicTacToeAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
        protected int aiTicks;
        protected int endTicks;
    }

    final class Board {
        private static final int[][] ROWS = {
            {0, 1, 2},
            {3, 4, 5},
            {6, 7, 8},
            {0, 3, 6},
            {1, 4, 7},
            {2, 5, 8},
            {0, 4, 8},
            {2, 4, 6},
        };
        private int[] cells = new int[9];
        private Vec3i[] blocks = new Vec3i[9];

        public int get(int row, int col) {
            return cells[row + col * 3];
        }

        public int getWinner() {
            OUTER: for (int[] row : ROWS) {
                int blue = 0;
                int red = 0;
                for (int i : row) {
                    switch (cells[i]) {
                    case 1:
                        if (red > 0) continue OUTER;
                        blue += 1;
                        break;
                    case 2:
                        if (blue > 0) continue OUTER;
                        red += 1;
                        break;
                    default: continue OUTER;
                    }
                }
                if (blue == 3) return 1;
                if (red == 3) return 2;
            }
            return 0;
        }

        public int getNextAiMove() {
            int rescue = -1;
            for (int[] row : ROWS) {
                int blue = 0;
                int red = 0;
                int lastEmpty = 0;
                for (int i : row) {
                    switch (cells[i]) {
                    case 1: blue += 1; break;
                    case 2: red += 1; break;
                    default: lastEmpty = i;
                    }
                }
                if (red == 2 && blue == 0) {
                    return lastEmpty;
                } else if (blue == 2 && red == 0) {
                    rescue = lastEmpty;
                }
            }
            if (rescue >= 0) return rescue;
            int result = -1;
            int chance = 1;
            for (int i = 1; i < 9; i += 1) {
                if (cells[i] == 0) {
                    if (random.nextInt(chance) == 0) {
                        result = i;
                    }
                    chance += 1;
                }
            }
            return result;
        }

        public void draw() {
            for (int i = 0; i < 9; i += 1) {
                Block block = blocks[i].toBlock(world);
                switch (cells[i]) {
                case 1: block.setType(Material.LAPIS_BLOCK, false); break;
                case 2: block.setType(Material.REDSTONE_BLOCK, false); break;
                default: block.setType(Material.SMOOTH_STONE, false); break;
                }
            }
        }

        public boolean isFull() {
            for (int i : cells) {
                if (i == 0) return false;
            }
            return true;
        }

        @Override public String toString() {
            List<Integer> ls = new ArrayList<>();
            for (int i : cells) ls.add(i);
            return ls + "|" + List.of(blocks);
        }
    }

    private Board getBoard() {
        Board result = new Board();
        int i = 0;
        for (Vec3i vec : boardArea.enumerate()) {
            result.blocks[i] = vec;
            result.cells[i] = switch (vec.toBlock(world).getType()) {
            case LAPIS_BLOCK -> 1;
            case REDSTONE_BLOCK -> 2;
            default -> 0;
            };
            i += 1;
        }
        return result;
    }

    private void clearBoard() {
        for (Vec3i vec : boardArea.enumerate()) {
            vec.toBlock(world).setType(Material.AIR);
        }
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        Component text = switch (saveTag.state) {
        case PLAYER_TURN -> booth.format(Unicode.WATCH.string + secondsLeft + " Your turn");
        case COMPUTER_TURN -> booth.format(Unicode.WATCH.string + secondsLeft + " My turn...");
        case WIN -> text("You win!", GREEN);
        case LOSE -> text("I win!", DARK_RED);
        case DRAW -> text("Nobody wins!", DARK_RED);
        default -> empty();
        };
        event.bossbar(PlayerHudPriority.HIGHEST,
                      text,
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) GAME_TIME.toSeconds());
    }
}
