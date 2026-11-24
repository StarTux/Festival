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
        rootNode.addChild("inv").hidden(true).denyTabCompletion()
            .description("Open inventory")
            .playerCaller(this::inv);
        rootNode.addChild("send").hidden(true).denyTabCompletion()
            .description("Send message to attraction")
            .playerCaller(this::send);
    }

    protected boolean yes(Player player, String[] args) {
        if (args.length != 0) return false;
        Attraction.requireAt(player.getLocation()).onClickYes(player);
        return true;
    }

    protected boolean no(Player player, String[] args) {
        return true;
    }

    protected boolean inv(Player player, String[] args) {
        if (args.length != 0) return true;
        plugin.showCollection(player);
        return true;
    }

    protected boolean send(Player player, String[] args) {
        Attraction.requireAt(player.getLocation()).onCommand(player, args);
        return true;
    }
}
