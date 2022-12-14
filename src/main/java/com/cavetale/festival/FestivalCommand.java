package com.cavetale.festival;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.festival.attraction.Attraction;
import java.util.Arrays;
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
        rootNode.addChild("inv").hidden(true).denyTabCompletion()
            .description("Open inventory")
            .playerCaller(this::inv);
        rootNode.addChild("send").hidden(true).denyTabCompletion()
            .description("Send message to attraction")
            .playerCaller(this::send);
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

    protected boolean inv(Player player, String[] args) {
        if (args.length != 0) return true;
        Festival festival = plugin.getFestival(player.getWorld());
        if (festival == null) return true;
        festival.openInventory(player);
        return true;
    }

    protected boolean send(Player player, String[] args) {
        if (args.length == 0) return true;
        Attraction attraction = plugin.getAttraction(player.getWorld(), args[0]);
        if (attraction == null) return true;
        attraction.onCommand(player, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }
}
