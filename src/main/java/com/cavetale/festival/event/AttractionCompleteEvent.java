package com.cavetale.festival.event;

import com.cavetale.festival.attraction.Attraction;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/** Fired once when a player's successful attraction run is recorded. */
@Getter
public final class AttractionCompleteEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Attraction attraction;
    /** Whether this is the player's first recorded win at this attraction. */
    private final boolean firstCompletion;
    /** Whether this win earned the attraction's one-time reward. */
    private final boolean firstCompletionReward;

    public AttractionCompleteEvent(final Player player, final Attraction attraction,
                                   final boolean firstCompletion, final boolean firstCompletionReward) {
        super(player);
        this.attraction = attraction;
        this.firstCompletion = firstCompletion;
        this.firstCompletionReward = firstCompletionReward;
    }

    public String getAttractionName() {
        return attraction.getName();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
