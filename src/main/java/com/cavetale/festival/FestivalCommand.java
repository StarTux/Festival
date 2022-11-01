package com.cavetale.festival;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.festival.attraction.Attraction;
import org.bukkit.entity.Player;

public final class FestivalCommand extends AbstractCommand<FestivalPlugin> {
    protected FestivalCommand(final FestivalPlugin plugin) {
        super(plugin, "festival");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("yes").hidden(true).denyTabCompletion()
            .description("Say yes")
            .playerCaller(this::yes);
        rootNode.addChild("no").hidden(true).denyTabCompletion()
            .description("Say no")
            .playerCaller(this::no);
    }

    protected boolean yes(Player player, String[] args) {
        if (args.length != 1) return true;
        Attraction attraction = plugin.getAttraction(player.getWorld(), args[0]);
        if (attraction == null) return true;
        attraction.onClickYes(player);
        return true;
    }

    protected boolean no(Player player, String[] args) {
        return true;
    }
}
