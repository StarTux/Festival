package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.festival.session.Session;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import static java.util.Comparator.comparing;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class OpenChestAttraction extends Attraction<OpenChestAttraction.SaveTag> {
    protected static final Duration OPEN_TIME = Duration.ofSeconds(30);
    protected Set<Vec3i> chestBlockSet = new HashSet<>();
    protected int secondsLeft;

    protected OpenChestAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area cuboid : allAreas) {
            if ("chest".equals(cuboid.name)) {
                chestBlockSet.addAll(cuboid.enumerate());
            }
        }
        this.displayName = text("Chest Game", DARK_RED);
        this.description = text("Choose one of my chests and keep"
                                + " what you find inside!");
        this.areaNames.add("chest");
        if (this.chestBlockSet.isEmpty()) {
            debugLine("No chest blocks");
        }
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        placeChests(player);
        startingGun(player);
        changeState(State.OPEN);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public void onTick() {
        State newState = saveTag.state.tick(this);
        if (newState != null) changeState(newState);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (saveTag.state != State.OPEN) return;
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
        case LEFT_CLICK_BLOCK:
            break;
        default: return;
        }
        Player player = getCurrentPlayer();
        if (player == null) return;
        if (!event.getPlayer().equals(player)) return;
        Vec3i clickedBlock = Vec3i.of(event.getClickedBlock());
        for (Vec3i chestBlock : chestBlockSet) {
            if (clickedBlock.equals(chestBlock)) {
                event.setCancelled(true);
                openChest(player, event.getClickedBlock());
                return;
            }
        }
    }

    public void openChest(Player player, Block block) {
        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        confetti(player, location);
        progress(player);
        final boolean bingo = saveTag.treasureChest.equals(Vec3i.of(block));
        final Session session = festival.sessionOf(player);
        if (bingo) {
            perfect(player);
            giveReward(player, bingo);
            session.setCooldown(this, completionCooldown);
        } else {
            fail(player);
            session.setCooldown(this, (session.isUniqueLocked(this)
                                       ? completionCooldown
                                       : Duration.ofSeconds(10)));
        }
        changeState(State.IDLE);
    }

    private static BlockFace horizontalBlockFace(Vec3i vec) {
        if (Math.abs(vec.x) > Math.abs(vec.z)) {
            return vec.x > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return vec.z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    private void placeChests(Player player) {
        final Vec3i playerVector = Vec3i.of(player.getLocation());
        final List<Vec3i> chestBlockList = new ArrayList<>(chestBlockSet);
        chestBlockList.sort(comparing(Vec3i::getY)
                            .thenComparing(Vec3i::getZ)
                            .thenComparing(Vec3i::getX));
        saveTag.blockList = new ArrayList<>();
        saveTag.blockDataList = new ArrayList<>();
        for (Vec3i vec : chestBlockList) {
            final Block block = vec.toBlock(world);
            saveTag.blockList.add(vec);
            saveTag.blockDataList.add(block.getBlockData().getAsString(false));
            Chest blockData = (Chest) Material.CHEST.createBlockData();
            blockData.setFacing(horizontalBlockFace(npcVector.subtract(vec)));
            block.setBlockData(blockData);
        }
        Session session = festival.sessionOf(player);
        final int completionCount = session.getCompletionCount(this);
        int hash = player.getUniqueId().hashCode();
        hash = hash * 31 + completionCount;
        hash = hash * 31 + (npcVector != null ? npcVector.hashCode() : mainArea.hashCode());
        hash *= 31;
        Random random2 = new Random((long) hash);
        final int treasureIndex = random2.nextInt(chestBlockList.size());
        saveTag.treasureChest = chestBlockList.get(treasureIndex);
    }

    private void clearChests() {
        if (saveTag.blockList == null) return;
        if (saveTag.blockDataList == null) return;
        for (int i = 0; i < saveTag.blockList.size(); i += 1) {
            Vec3i vec = saveTag.blockList.get(i);
            String string = saveTag.blockDataList.get(i);
            BlockData blockData = Bukkit.createBlockData(string);
            Block block = vec.toBlock(world);
            vec.toBlock(world).setBlockData(blockData);
        }
        saveTag.blockList = null;
        saveTag.blockDataList = null;
    }

    protected State tickOpen() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        long now = System.currentTimeMillis();
        long timeout = saveTag.openStarted + OPEN_TIME.toMillis();
        if (now > timeout) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((timeout - now - 1L) / 1000L) + 1;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            player.sendActionBar(textOfChildren(text(Unicode.WATCH.string + seconds, GOLD),
                                                text(" Pick a chest!", WHITE)));
            for (Vec3i vec : chestBlockSet) {
                highlight(player, vec.toCenterLocation(player.getWorld()));
            }
        }
        return null;
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    enum State {
        IDLE,
        OPEN {
            @Override protected State tick(OpenChestAttraction instance) {
                return instance.tickOpen();
            }

            @Override protected void enter(OpenChestAttraction instance) {
                instance.saveTag.openStarted = System.currentTimeMillis();
            }

            @Override protected void exit(OpenChestAttraction instance) {
                instance.clearChests();
            }
        };

        protected void enter(OpenChestAttraction instance) { }

        protected void exit(OpenChestAttraction instance) { }

        protected State tick(OpenChestAttraction instance) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long openStarted;
        protected List<Vec3i> blockList;
        protected List<String> blockDataList;
        protected Vec3i treasureChest = Vec3i.ZERO;
    }
}
