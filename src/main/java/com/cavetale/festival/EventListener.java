package com.cavetale.festival;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.FishAttraction;
import com.cavetale.festival.attraction.MusicHeroAttraction;
import com.cavetale.festival.session.Session;
import com.cavetale.magicmap.event.MagicMapCursorEvent;
import com.cavetale.mytems.event.music.PlayerBeatEvent;
import com.cavetale.mytems.event.music.PlayerCloseMusicalInstrumentEvent;
import com.cavetale.mytems.event.music.PlayerMelodyCompleteEvent;
import com.cavetale.mytems.event.music.PlayerOpenMusicalInstrumentEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.map.MapCursor;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final FestivalPlugin plugin;
    protected boolean enabled = true;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Attraction attraction : plugin.getAttractionsIn(player.getWorld())) {
            attraction.onPlayerQuit(event);
        }
        plugin.getSessions().clear(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPluginPlayer(PluginPlayerEvent event) {
        Player player = event.getPlayer();
        for (Attraction attraction : plugin.getAttractionsIn(player.getWorld())) {
            if (!attraction.isInArea(player.getLocation())) continue;
            attraction.onPluginPlayer(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onProjectileHit(ProjectileHitEvent event) {
        Projectile entity = event.getEntity();
        for (Attraction attraction : plugin.getAttractionsIn(entity.getWorld())) {
            if (!attraction.isInArea(entity.getLocation())) continue;
            attraction.onProjectileHit(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        for (Attraction attraction : plugin.getAttractionsAt(location)) {
            attraction.onEntityDamage(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        for (Attraction attraction : plugin.getAttractionsIn(entity.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onEntityDamageByEntity(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityCombust(EntityCombustEvent event) {
        for (Attraction attraction : plugin.getAttractionsAt(event.getEntity().getLocation())) {
            attraction.onEntityCombust(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityBlockForm(EntityBlockFormEvent event) {
        for (Attraction attraction : plugin.getAttractionsAt(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Location location = entity.getLocation();
        for (Attraction attraction : plugin.getAttractionsIn(entity.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onPlayerInteractEntity(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        Location location = event.getClickedBlock().getLocation();
        for (Attraction attraction : plugin.getAttractionsIn(location.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onPlayerInteract(event);
        }
    }

    @EventHandler
    private void onPlayerOpenMusicalInstrument(PlayerOpenMusicalInstrumentEvent event) {
        plugin.getAttractions().applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerOpenMusicalInstrument(event));
    }

    @EventHandler
    private void onPlayerCloseMusicalInstrument(PlayerCloseMusicalInstrumentEvent event) {
        plugin.getAttractions().applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerCloseMusicalInstrument(event));
    }

    @EventHandler
    private void onPlayerBeat(PlayerBeatEvent event) {
        plugin.getAttractions().applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerBeat(event));
    }

    @EventHandler
    private void onPlayerMelodyComplete(PlayerMelodyCompleteEvent event) {
        plugin.getAttractions().applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerMelodyComplete(event));
    }

    @EventHandler
    private void onMagicMapCursor(MagicMapCursorEvent event) {
        Session session = plugin.getSessions().of(event.getPlayer());
        for (Attraction attraction : plugin.getAttractionsIn(event.getPlayer().getWorld())) {
            if (attraction.isDisabled()) continue;
            Vec3i vec = attraction.getNpcVector();
            if (vec == null) continue;
            if (!event.contains(vec.x, vec.z)) continue;
            final boolean completed = session.isUniqueLocked(attraction);
            final boolean pickedUp = session.getPrizeWaiting(attraction) != 2;
            final MapCursor.Type cursorType;
            if (completed && pickedUp) {
                cursorType = MapCursor.Type.MANSION;
            } else if (completed && !pickedUp) {
                cursorType = MapCursor.Type.RED_MARKER;
            } else {
                cursorType = attraction.getType().getMapCursorIcon();
            }
            final Component caption = attraction.getMapCursorCaption();
            event.addCursor(cursorType, vec.toLocation(event.getPlayer().getWorld()), caption);
        }
    }

    @EventHandler
    private void onThrownEggHatch(ThrownEggHatchEvent event) {
        if (!plugin.getAttractionsAt(event.getEgg().getLocation()).isEmpty()) {
            event.setHatching(false);
        }
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        for (Attraction attraction : plugin.getAttractionsAt(event.getPlayer().getLocation())) {
            if (attraction.isParticipant(event.getPlayer())) {
                attraction.onPlayerHud(event);
            }
            if (
                event.getPlayer().getGameMode() == GameMode.CREATIVE
                && !attraction.getDebugLines().isEmpty()
                && event.getPlayer().hasPermission("festival.testing")
            ) {
                final List<Component> lines = new ArrayList<>();
                lines.add(text(attraction.getName(), DARK_RED));
                lines.addAll(attraction.getDebugLines());
                event.sidebar(PlayerHudPriority.LOW, lines);
            }
        }
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        plugin.getAttractions().loadWorld(event.getWorld());
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        plugin.getAttractions().unloadWorld(event.getWorld());
    }

    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        plugin.getAttractions().applyAttraction(event.getTo(), a -> a.onPlayerTeleport(event));
    }

    @EventHandler
    private void onEntityToggleGlide(EntityToggleGlideEvent event) {
        plugin.getAttractions().applyAttraction(event.getEntity().getLocation(), a -> a.onEntityToggleGlide(event));
    }

    @EventHandler
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery event) {
        plugin.getAttractions().applyAttraction(event.getBlock(), a -> a.onPlayerBlockAbility(event));
    }

    @EventHandler
    private void onEntityPathfind(EntityPathfindEvent event) {
        plugin.getAttractions().applyAttraction(event.getEntity().getLocation(), a -> a.onEntityPathfind(event));
    }

    @EventHandler
    private void onEntityTarget(EntityTargetEvent event) {
        plugin.getAttractions().applyAttraction(event.getEntity().getLocation(), a -> a.onEntityTarget(event));
    }


    @EventHandler(priority = EventPriority.LOW)
    private void onPlayerFish(PlayerFishEvent event) {
        plugin.getAttractions().applyAttraction(event.getPlayer().getLocation(), a -> {
                if (a instanceof FishAttraction fa) {
                    fa.onPlayerFish(event);
                }
            });
    }
}
