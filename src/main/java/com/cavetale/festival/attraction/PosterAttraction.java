package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.poster.PosterPlugin;
import com.cavetale.poster.save.Poster;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import static net.kyori.adventure.text.Component.text;

public final class PosterAttraction extends Attraction<PosterAttraction.SaveTag> {
    private Poster poster; // set by booth!
    private Vec3i posterBlock;
    private Vec3i posterFace;
    private BlockFace face;
    private BlockFace px;
    private BlockFace py;
    private Duration playTime = Duration.ofMinutes(5);
    private long secondsLeft;

    protected PosterAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        this.displayName = booth.format("Picture Puzzle");
        this.description = text("Can you unscramble my painting?");
        Vec3i posterX = null;
        Vec3i posterY = null;
        for (Area area : allAreas) {
            if (area.name == null) continue;
            switch (area.name) {
            case "block":
                posterBlock = area.min;
                break;
            case "face":
                posterFace = area.min;
                break;
            case "px":
                posterX = area.min;
                break;
            case "py":
                posterY = area.min;
                break;
            default:
                break;
            }
        }
        if (posterBlock == null || posterFace == null) {
            debugLine("Poster block or face missing");
        } else {
            face = vec2face(posterFace, posterBlock);
        }
        if (posterX != null && posterY != null) {
            px = vec2face(posterX, posterBlock);
            py = vec2face(posterY, posterBlock);
        }
        this.areaNames.add("block");
        this.areaNames.add("face");
        this.areaNames.add("px");
        this.areaNames.add("py");
        this.intKeys.add("time");
        this.stringKeys.add("poster");
    }

    @Override
    protected void onEnable() {
        Map<String, Object> raw = getFirstArea().getRaw() != null
            ? getFirstArea().getRaw()
            : Map.of();
        if (raw.get("time") instanceof Number number) {
            this.playTime = Duration.ofSeconds(number.intValue());
        }
        if (raw.get("poster") instanceof String posterName) {
            setPoster(posterName);
        } else {
            debugLine("Missing value: poster");
        }
    }

    private static BlockFace vec2face(Vec3i head, Vec3i foot) {
        if (head.x > foot.x) {
            return BlockFace.EAST;
        } else if (head.x < foot.x) {
            return BlockFace.WEST;
        } else if (head.z > foot.z) {
            return BlockFace.SOUTH;
        } else if (head.z < foot.z) {
            return BlockFace.NORTH;
        } else if (head.y > foot.y) {
            return BlockFace.UP;
        } else if (head.y < foot.z) {
            return BlockFace.DOWN;
        } else {
            return BlockFace.SELF;
        }
    }

    @Override
    public void start(Player player) {
        if (poster == null) throw new IllegalStateException("poster=null");
        saveTag.currentPlayer = player.getUniqueId();
        startingGun(player);
        changeState(State.PLAY);
    }

    @Override
    public void stop() {
        changeState(State.IDLE);
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void onTick() {
        if (saveTag.state == State.IDLE) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        State newState = saveTag.state.tick(this, player);
        if (newState != null) changeState(newState);
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    protected void rollPoster() {
        saveTag.frames = new ArrayList<>();
        if (poster == null) throw new IllegalStateException("poster=null");
        int nextIndex = 0;
        for (int mapId : poster.getMapIds()) {
            Frame frame = new Frame();
            frame.mapId = mapId;
            frame.mapIndex = nextIndex++;
            saveTag.frames.add(frame);
        }
        saveTag.frames.get(saveTag.frames.size() - 1).mapId = -1;
        final int width = poster.getWidth();
        final int height = poster.getHeight();
        int x = width - 1;
        int y = height - 1;
        for (int i = 0; i < 100; i += 1) {
            int chance = 1;
            int nx = x;
            int ny = y;
            if (x > 0 && random.nextInt(chance++) == 0) {
                nx = x - 1;
                ny = y;
            }
            if (x < width - 1 && random.nextInt(chance++) == 0) {
                nx = x + 1;
                ny = y;
            }
            if (y > 0 && random.nextInt(chance++) == 0) {
                nx = x;
                ny = y - 1;
            }
            if (y < height - 1 && random.nextInt(chance++) == 0) {
                nx = x;
                ny = y + 1;
            }
            int emptyIndex = x + width * y;
            int otherIndex = nx + width * ny;
            Frame emptyFrame = saveTag.frames.get(emptyIndex);
            Frame otherFrame = saveTag.frames.get(otherIndex);
            saveTag.frames.set(otherIndex, emptyFrame);
            saveTag.frames.set(emptyIndex, otherFrame);
            x = nx;
            y = ny;
        }
    }

    protected void spawnPoster(int index) {
        Frame frame = saveTag.frames.get(index);
        if (frame.mapId <= 0) return;
        if (frame.uuid != null && Bukkit.getEntity(frame.uuid) != null) return;
        int x = index % poster.getWidth();
        int y = index / poster.getWidth();
        Vec3i vec = getPosterBlock(x, y);
        Location location = vec.toCenterLocation(world);
        PosterPlugin posterPlugin = (PosterPlugin) Bukkit.getPluginManager().getPlugin("Poster");
        GlowItemFrame entity = location.getWorld().spawn(location, GlowItemFrame.class, e -> {
                e.setPersistent(false);
                e.setFixed(true);
                e.setItem(posterPlugin.createPosterMapItem(frame.mapId));
                e.setFacingDirection(face);
            });
        if (entity != null) {
            frame.uuid = entity.getUniqueId();
        }
    }

    protected Vec3i getPosterBlock(int x, int y) {
        if (px != null && py != null) {
            return posterBlock
                .add(px.getModX() * x,
                     px.getModY() * x,
                     px.getModZ() * x)
                .add(py.getModX() * y,
                     py.getModY() * y,
                     py.getModZ() * y);
        }
        return switch (face) {
        case NORTH -> posterBlock.add(-x, -y, 0);
        case SOUTH -> posterBlock.add(x, -y, 0);
        case EAST -> posterBlock.add(0, -y, -x);
        case WEST -> posterBlock.add(0, -y, x);
        case UP -> posterBlock.add(x, 0, y);
        default -> posterBlock;
        };
    }

    protected void spawnAllPosters() {
        for (int i = 0; i < saveTag.frames.size(); i += 1) {
            spawnPoster(i);
        }
    }

    protected void despawnPoster(int index) {
        Frame frame = saveTag.frames.get(index);
        if (frame.uuid == null) return;
        Entity entity = Bukkit.getEntity(frame.uuid);
        if (entity instanceof GlowItemFrame glowItemFrame) {
            glowItemFrame.remove();
        }
        frame.uuid = null;
    }

    protected void despawnAllPosters() {
        if (saveTag.frames == null) return;
        for (int i = 0; i < saveTag.frames.size(); i += 1) {
            despawnPoster(i);
        }
    }

    protected Frame frameOfEntity(UUID uuid) {
        if (saveTag.frames == null) return null;
        for (Frame frame : saveTag.frames) {
            if (uuid.equals(frame.uuid)) return frame;
        }
        return null;
    }

    protected Frame findEmptyFrame() {
        for (Frame frame : saveTag.frames) {
            if (frame.mapId < 0) return frame;
        }
        throw new IllegalStateException("Empty frame not found!");
    }

    protected static class Frame {
        protected int mapId;
        protected int mapIndex; // index within poster
        protected UUID uuid;
    }

    protected State tickPlay(Player player) {
        Duration timeSpent = Duration.ofMillis(System.currentTimeMillis() - saveTag.playStarted);
        Duration timeLeft = playTime.minus(timeSpent);
        if (timeLeft.isNegative()) {
            timeout(player);
            return State.IDLE;
        }
        secondsLeft = (timeLeft.toMillis() - 1) / 1000L + 1L;
        return null;
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        onPlayerUseEntity(event.getPlayer(), event.getRightClicked());
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            onPlayerUseEntity(player, event.getEntity());
        }
    }

    protected void onPlayerUseEntity(Player player, Entity used) {
        if (saveTag.state != State.PLAY) return;
        if (!player.equals(getCurrentPlayer())) return;
        Frame frame = frameOfEntity(used.getUniqueId());
        if (frame == null) return;
        onClickFrame(player, frame);
    }

    protected void onClickFrame(Player player, Frame frame) {
        int frameIndex = saveTag.frames.indexOf(frame);
        int frameX = frameIndex % poster.getWidth();
        int frameY = frameIndex / poster.getWidth();
        Frame emptyFrame = findEmptyFrame();
        int emptyIndex = saveTag.frames.indexOf(emptyFrame);
        int emptyX = emptyIndex % poster.getWidth();
        int emptyY = emptyIndex / poster.getHeight();
        boolean validX = (Math.abs(frameX - emptyX) == 1) && (Math.abs(frameY - emptyY) == 0);
        boolean validY = (Math.abs(frameX - emptyX) == 0) && (Math.abs(frameY - emptyY) == 1);
        boolean valid = validX ^ validY;
        if (!valid) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
            return;
        }
        Vec3i vec = getPosterBlock(emptyX, emptyY);
        Entity entity = Bukkit.getEntity(frame.uuid);
        Location location = vec.toCenterFloorLocation(world);
        location.setDirection(entity.getLocation().getDirection());
        entity.teleport(location);
        // swap
        saveTag.frames.set(emptyIndex, frame);
        saveTag.frames.set(frameIndex, emptyFrame);
        player.playSound(entity.getLocation(), Sound.ENTITY_ITEM_FRAME_ROTATE_ITEM, SoundCategory.MASTER, 0.5f, 1.0f);
        for (int i = 0; i < saveTag.frames.size(); i += 1) {
            if (i != saveTag.frames.get(i).mapIndex) return;
        }
        perfect(player, false);
        prepareReward(player, true);
        changeState(State.WIN);
    }

    enum State {
        IDLE {
            @Override protected void enter(PosterAttraction instance) {
                instance.despawnAllPosters();
                instance.saveTag.frames = null;
            }
        },
        PLAY {
            @Override protected void enter(PosterAttraction instance) {
                instance.saveTag.playStarted = System.currentTimeMillis();
                instance.rollPoster();
                instance.spawnAllPosters();
            }

            @Override protected State tick(PosterAttraction instance, Player player) {
                return instance.tickPlay(player);
            }
        },
        WIN {
            @Override protected void enter(PosterAttraction instance) {
                instance.saveTag.winStarted = System.currentTimeMillis();
            }

            @Override protected State tick(PosterAttraction instance, Player player) {
                if (System.currentTimeMillis() - instance.saveTag.winStarted > 5000L) {
                    return State.IDLE;
                }
                return null;
            }
        };

        protected void enter(PosterAttraction instance) { }

        protected void exit(PosterAttraction instance) { }

        protected State tick(PosterAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected List<Frame> frames;
        protected long playStarted;
        protected long winStarted;
    }

    public void setPoster(String name) {
        PosterPlugin posterPlugin = (PosterPlugin) Bukkit.getPluginManager().getPlugin("Poster");
        this.poster = posterPlugin.findPosterNamed(name);
        if (poster == null) debugLine("Poster not found: " + name);
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) playTime.toSeconds());
    }
}
