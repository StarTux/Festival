package com.cavetale.festival.attraction;

import java.time.Duration;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

/** An attraction whose active run belongs to one player. */
public abstract class SinglePlayerAttraction extends Attraction {
    protected SinglePlayerAttraction(final AttractionConfiguration config) {
        super(config);
    }

    protected SinglePlayerAttraction(final AttractionConfiguration config, final String defaultDisplayName) {
        super(config, defaultDisplayName);
    }

    @Override
    public abstract SaveTag getSaveTag();

    public final Player getCurrentPlayer() {
        if (getSaveTag().currentPlayer == null || !isPlaying()) {
            return null;
        }
        final Player player = Bukkit.getPlayer(getSaveTag().currentPlayer);
        if (player == null) return null;
        if (!isInArea(player.getLocation())) {
            fail(player, "You left the game area");
            plugin.getSessions().of(player).setCooldown(this, Duration.ofMinutes(1));
            stop();
            return null;
        }
        return player;
    }

    @Override
    public final boolean isParticipant(final Player player) {
        return isPlaying() && player.getUniqueId().equals(getSaveTag().currentPlayer);
    }

    @Override
    public final void onPlayerQuit(final PlayerQuitEvent event) {
        if (isPlaying() && event.getPlayer().equals(getCurrentPlayer())) {
            plugin.getSessions().of(event.getPlayer()).setCooldown(this, Duration.ofMinutes(1));
            stop();
        }
    }

    @Override
    protected final boolean canJoin(final Player player) {
        if (!isPlaying()) return true;
        final Player somebody = getCurrentPlayer();
        if (player.equals(somebody)) return false; // fail silently
        final Component somebodyName = somebody != null
            ? somebody.displayName()
            : text("Somebody");
        final Component message = join(noSeparators(),
                                 text("Please wait: "),
                                 somebodyName,
                                 text(" is playing this right now"))
            .color(RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    protected abstract static class SaveTag extends Attraction.SaveTag {
        protected UUID currentPlayer;
    }
}
