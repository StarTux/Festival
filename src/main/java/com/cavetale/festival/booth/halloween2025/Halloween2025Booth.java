package com.cavetale.festival.booth.halloween2025;

import com.cavetale.festival.Festival;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.AttractionType;
import com.cavetale.festival.attraction.FindBlocksAttraction;
import com.cavetale.festival.attraction.Music;
import com.cavetale.festival.booth.Booth;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.music.Melody;
import com.cavetale.resident.ZoneType;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

public final class Halloween2025Booth implements Booth {
    @Override
    public AttractionType getType() {
        return null; // default
    }

    /**
     * Called when the attraction has finished loading and will
     * enable.  This is an opportunity to set some values, or set
     * disabled to true.
     */
    @Override
    public void onEnable(Attraction<?> attraction) {
        if (attraction instanceof FindBlocksAttraction findBlocks) {
            findBlocks.logInfo("Setting Block Data Function of " + findBlocks.getName());
            findBlocks.setBlockDataFunction(
                block -> {
                    if (!block.getType().name().contains("COPPER")) return null;
                    int exposure = 0;
                    for (BlockFace face : FindBlocksAttraction.HORIZONTAL_FACES) {
                        final Block nbor = block.getRelative(face);
                        if (nbor.isEmpty() && nbor.getLightFromSky() > 0) {
                            exposure += 1;
                        }
                    }
                    if (exposure < 1) {
                        return null;
                    }
                    final String newName = switch (ThreadLocalRandom.current().nextInt(3)) {
                        case 0 -> "EXPOSED_" + block.getType().name();
                        case 1 -> "WEATHERED_" + block.getType().name();
                        default -> "OXIDIZED_" + block.getType().name();
                    };
                    final Material mat;
                    try {
                        mat = Material.valueOf(newName);
                    } catch (IllegalArgumentException iae) {
                        return null;
                    }
                    final BlockData blockData = mat.createBlockData();
                    block.getBlockData().copyTo(blockData);
                    return blockData;
                }
            );
        }
    }

    @Override
    public void onDisable(Attraction<?> attraction) { }

    @Override
    public Component format(String txt) {
        return getFestival().getTheme().format(txt);
    }

    @Override
    public Festival getFestival() {
        return Halloween2025.halloween2025().getFestival();
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return Mytems.HALLOWEEN_TOKEN_2.createItemStack();
    }

    private static final List<List<ItemStack>> PRIZE_POOL = List.of(List.of(Mytems.CANDY_CORN.createItemStack(),
                                                                            Mytems.CHOCOLATE_BAR.createItemStack(),
                                                                            Mytems.LOLLIPOP.createItemStack(),
                                                                            Mytems.ORANGE_CANDY.createItemStack()),
                                                                    List.of(Mytems.RUBY.createItemStack(2),
                                                                            Mytems.RUBY.createItemStack(4),
                                                                            Mytems.RUBY.createItemStack(8),
                                                                            Mytems.RUBY.createItemStack(16)),
                                                                    List.of(new ItemStack(Material.DIAMOND, 2),
                                                                            new ItemStack(Material.DIAMOND, 4),
                                                                            new ItemStack(Material.DIAMOND, 8),
                                                                            new ItemStack(Material.EMERALD, 16)));

    private static final List<ItemStack> BONUS_PRIZE_POOL = List.of(Mytems.RUBY.createItemStack(),
                                                                    new ItemStack(Material.DIAMOND),
                                                                    Mytems.CANDY_CORN.createItemStack(),
                                                                    Mytems.CHOCOLATE_BAR.createItemStack(),
                                                                    Mytems.LOLLIPOP.createItemStack(),
                                                                    Mytems.ORANGE_CANDY.createItemStack());


    @Override
    public List<List<ItemStack>> getPrizePool() {
        return PRIZE_POOL;
    }

    /**
     * 4 items will be picked at random to be added as decoration
     * around the main prize.
     */
    @Override
    public List<ItemStack> getBonusPrizePool() {
        return BONUS_PRIZE_POOL;
    }

    @Override
    public ItemStack getEntryFee() {
        return Mytems.RUBY.createItemStack();
    }

    @Override
    public ZoneType getZoneType() {
        return getFestival().getTheme().getZoneType();
    }

    @Override
    public Melody getFailMelody() {
        return Music.DECKED_OUT.melody;
    }

    @Override
    public Melody getSuccessMelody() {
        return Music.TREASURE.melody;
    }

    @Override
    public Component getMapCursorCaption() {
        return null; // default
    }
}
