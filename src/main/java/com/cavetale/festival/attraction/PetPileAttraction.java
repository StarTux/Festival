package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.util.Text;
import com.destroystokyo.paper.entity.ai.GoalType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import static net.kyori.adventure.text.Component.text;

public final class PetPileAttraction extends Attraction<PetPileAttraction.SaveTag> {
    private int realPetCount = 7;
    private int fakePetCount = 21;
    protected final Set<Vec3i> petBlocks = new HashSet<>();
    protected Vec3i showBlock;
    protected PetSpawner petSpawner;
    protected Duration playTime = Duration.ofSeconds(60);
    protected int secondsLeft;

    protected PetPileAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        this.displayName = booth.format("Pet Pile");
        this.description = text("Can you help me sort out my pets?");
        for (Area area : allAreas) {
            if ("pet".equals(area.name)) {
                petBlocks.addAll(area.enumerate());
            } else if ("show".equals(area.name)) {
                showBlock = area.min;
            }
        }
        this.areaNames.add("pet");
        this.areaNames.add("show");
        if (petBlocks.isEmpty()) debugLine("No pet blocks");
        if (showBlock == null) debugLine("No show block");
        this.intKeys.add("realPetCount");
        this.intKeys.add("fakePetCount");
        this.intKeys.add("time");
        setCats();
    }

    @Override
    public void onEnable() {
        Map<String, Object> raw = getFirstArea().getRaw() != null
            ? getFirstArea().getRaw()
            : Map.of();
        if (raw.get("realPetCount") instanceof Number number) {
            this.realPetCount = number.intValue();
        }
        if (raw.get("fakePetCount") instanceof Number number) {
            this.fakePetCount = number.intValue();
        }
        if (raw.get("time") instanceof Number number) {
            this.playTime = Duration.ofSeconds(number.intValue());
        }
    }

    public void setCats() {
        petSpawner = new CatSpawner();
    }

    public void setPets(int real, int fake) {
        this.realPetCount = real;
        this.fakePetCount = fake;
    }

    @Override
    public void start(Player player) {
        if (petSpawner == null) throw new IllegalStateException("petSpawner=null");
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

    private List<Block> makePetBlockList() {
        List<Block> petBlockList = new ArrayList<>();
        for (Vec3i v : petBlocks) {
            petBlockList.add(v.toBlock(world));
        }
        petBlockList.removeIf(block -> {
                if (block.getLightFromBlocks() == 0 || block.isLiquid()) return true;
                final Block below = block.getRelative(0, -1, 0);
                if (below.isEmpty() || below.isLiquid() || below.getBlockData() instanceof Waterlogged) return true;
                if (Tag.FENCES.isTagged(below.getType()) || Tag.WALLS.isTagged(below.getType())) return true;
                if (block.isEmpty()) return false;
                Material mat = block.getType();
                if (Tag.WOOL_CARPETS.isTagged(mat)) return false;
                if (Tag.CROPS.isTagged(mat)) return false;
                switch (mat) {
                case COBWEB:
                case GRASS:
                case PUMPKIN_STEM:
                case MELON_STEM:
                case CARROTS:
                case POTATOES:
                case WHEAT:
                case BEETROOTS:
                case SNOW:
                    return false;
                default: return true;
                }
            });
        return petBlockList;
    }

    protected State tickPlay(Player player) {
        long playedTime = System.currentTimeMillis() - saveTag.playStarted;
        long timeLeft = playTime.toMillis() - playedTime;
        if (timeLeft < 0) {
            timeout(player);
            return State.IDLE;
        }
        secondsLeft = (int) ((timeLeft - 1) / 1000L) + 1;
        List<Block> petBlockList = makePetBlockList();
        for (UUID uuid : saveTag.pets.keySet()) {
            if (!(Bukkit.getEntity(uuid) instanceof Mob mob)) continue;
            Block block = mob.getLocation().getBlock();
            if (block.isLiquid() || block.getBlockData() instanceof Waterlogged w && w.isWaterlogged()) {
                Block target = petBlockList.get(random.nextInt(petBlockList.size()));
                mob.teleport(target.getLocation().add(0.5, 0.0, 0.5));
                continue;
            }
            var path = mob.getPathfinder().getCurrentPath();
            if (path != null && path.getFinalPoint().distanceSquared(mob.getLocation()) > 3.0) {
                continue;
            }
            Block target = petBlockList.get(random.nextInt(petBlockList.size()));
            mob.getPathfinder().moveTo(target.getLocation().add(0.5, 0.0, 0.5));
        }
        return null;
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    protected void rollPets() {
        saveTag.totalRealPets = 0;
        saveTag.petsFound = 0;
        List<Block> petBlockList = makePetBlockList();
        Collections.shuffle(petBlockList);
        petSpawner.roll();
        saveTag.realName = petSpawner.realName();
        saveTag.pets = new HashMap<>();
        int blockListIndex = 0;
        for (int i = 0; i < realPetCount + fakePetCount; i += 1) {
            boolean real = i < realPetCount;
            if (blockListIndex >= petBlockList.size()) blockListIndex = 0;
            Block block = petBlockList.get(blockListIndex++);
            Location location = block.getLocation().add(0.5, 0.0, 0.5);
            location.setYaw(random.nextFloat() * 360f);
            Entity entity = petSpawner.spawn(location, real, false);
            saveTag.pets.put(entity.getUniqueId(), real);
            if (real) saveTag.totalRealPets += 1;
        }
        if (showBlock != null) {
            Entity entity = petSpawner.spawn(showBlock.toCenterFloorLocation(world), true, true);
            saveTag.showEntity = entity.getUniqueId();
        }
    }

    protected void clearPets() {
        if (saveTag.pets != null) {
            for (UUID uuid : saveTag.pets.keySet()) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null) entity.remove();
            }
            saveTag.pets = null;
        }
        if (saveTag.showEntity != null) {
            Entity entity = Bukkit.getEntity(saveTag.showEntity);
            if (entity != null) entity.remove();
            saveTag.showEntity = null;
        }
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            onPlayerUseEntity(player, event.getEntity());
        }
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        onPlayerUseEntity(event.getPlayer(), event.getRightClicked());
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isPlaying()) return;
        if (saveTag.pets == null) return;
        if (saveTag.pets.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    void onPlayerUseEntity(Player player, Entity entity) {
        if (!isPlaying()) return;
        if (!player.equals(getCurrentPlayer())) return;
        if (saveTag.pets == null) return;
        Boolean real = saveTag.pets.get(entity.getUniqueId());
        if (real == null) return;
        if (real) {
            progress(player);
            confetti(player, entity.getLocation());
            saveTag.pets.remove(entity.getUniqueId());
            entity.remove();
            saveTag.petsFound += 1;
            if (saveTag.petsFound >= saveTag.totalRealPets) {
                prepareReward(player, true);
                perfect(player, true);
                festival.sessionOf(player).setCooldown(this, completionCooldown);
                stop();
            }
        } else {
            player.sendActionBar(booth.format("You picked the wrong pet!"));
            fail(player);
            stop();
        }
    }

    enum State {
        IDLE {
            @Override protected void enter(PetPileAttraction instance) {
                instance.saveTag.currentPlayer = null;
                instance.clearPets();
            }
        },
        PLAY {
            @Override protected void enter(PetPileAttraction instance) {
                instance.saveTag.playStarted = System.currentTimeMillis();
                instance.rollPets();
            }
            @Override protected State tick(PetPileAttraction instance, Player player) {
                return instance.tickPlay(player);
            }
        };

        protected void enter(PetPileAttraction instance) { }

        protected void exit(PetPileAttraction instance) { }

        protected State tick(PetPileAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long playStarted;
        protected Map<UUID, Boolean> pets;
        protected UUID showEntity;
        protected int petsFound;
        protected int totalRealPets;
        protected String realName;
    }

    public interface PetSpawner {
        void roll();
        Entity spawn(Location location, boolean real, boolean forShow);
        String realName();
    }

    public final class CatSpawner implements PetSpawner {
        private Cat.Type realType;
        private List<Cat.Type> fakeTypes;
        private int fakeIndex;

        @Override public void roll() {
            List<Cat.Type> types = new ArrayList<>(List.of(Cat.Type.values()));
            Collections.shuffle(types, random);
            realType = types.remove(types.size() - 1);
            fakeTypes = List.copyOf(types);
            fakeIndex = 0;
        }

        @Override public Cat spawn(Location location, boolean real, boolean forShow) {
            if (fakeIndex >= fakeTypes.size()) fakeIndex = 0;
            Cat result = location.getWorld().spawn(location, Cat.class, cat -> {
                    cat.setPersistent(false);
                    cat.setCatType(real ? realType : fakeTypes.get(fakeIndex++));
                    cat.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.15);
                    if (!forShow && random.nextBoolean()) {
                        cat.setBaby();
                    }
                    if (forShow) {
                        cat.setTamed(true);
                        cat.setSitting(true);
                        cat.setSilent(true);
                        cat.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
                        cat.setCollidable(false);
                        cat.customName(booth.format(realName()));
                    }
                    Bukkit.getMobGoals().removeAllGoals(cat);
                });
            Bukkit.getMobGoals().removeAllGoals(result, GoalType.TARGET);
            return result;
        }

        @Override public String realName() {
            switch (realType) {
            case BLACK: return "Tuxedo Cat";
            case ALL_BLACK: return "Black Cat";
            default: return Text.toCamelCase(realType) + " Cat";
            }
        }
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent(secondsLeft, text(saveTag.realName + " "), saveTag.petsFound, saveTag.totalRealPets),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) playTime.toSeconds());
    }
}
