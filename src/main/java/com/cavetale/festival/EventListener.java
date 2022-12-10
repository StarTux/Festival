package com.cavetale.festival;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.festival.attraction.Attraction;
import com.cavetale.festival.attraction.MusicHeroAttraction;
import com.cavetale.festival.session.Session;
import com.cavetale.magicmap.event.MagicMapCursorEvent;
import com.cavetale.magicmap.util.Cursors;
import com.cavetale.mytems.event.music.PlayerBeatEvent;
import com.cavetale.mytems.event.music.PlayerCloseMusicalInstrumentEvent;
import com.cavetale.mytems.event.music.PlayerMelodyCompleteEvent;
import com.cavetale.mytems.event.music.PlayerOpenMusicalInstrumentEvent;
import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
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
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.map.MapCursor;

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
        for (Attraction attraction : plugin.getAttractions(player.getWorld())) {
            attraction.onPlayerQuit(event);
        }
        for (Festival festival : plugin.festivalMap.values()) {
            festival.sessions.clear(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        for (Festival festival : plugin.festivalMap.values()) {
            festival.sessions.clear(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPluginPlayer(PluginPlayerEvent event) {
        Player player = event.getPlayer();
        for (Attraction attraction : plugin.getAttractions(player.getWorld())) {
            if (!attraction.isInArea(player.getLocation())) continue;
            attraction.onPluginPlayer(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onProjectileHit(ProjectileHitEvent event) {
        Projectile entity = event.getEntity();
        for (Attraction attraction : plugin.getAttractions(entity.getWorld())) {
            if (!attraction.isInArea(entity.getLocation())) continue;
            attraction.onProjectileHit(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        Attraction attraction = plugin.getAttraction(location);
        if (attraction != null && attraction.isInArea(location)) {
            attraction.onEntityDamage(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        for (Attraction attraction : plugin.getAttractions(entity.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onEntityDamageByEntity(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityCombust(EntityCombustEvent event) {
        Attraction attraction = plugin.getAttraction(event.getEntity().getLocation());
        if (attraction == null) return;
        attraction.onEntityCombust(event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityBlockForm(EntityBlockFormEvent event) {
        Attraction attraction = plugin.getAttraction(event.getEntity().getLocation());
        if (attraction != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Location location = entity.getLocation();
        for (Attraction attraction : plugin.getAttractions(entity.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onPlayerInteractEntity(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        Location location = event.getClickedBlock().getLocation();
        for (Attraction attraction : plugin.getAttractions(location.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onPlayerInteract(event);
        }
    }

    @EventHandler
    private void onPlayerOpenMusicalInstrument(PlayerOpenMusicalInstrumentEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerOpenMusicalInstrument(event));
    }

    @EventHandler
    private void onPlayerCloseMusicalInstrument(PlayerCloseMusicalInstrumentEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerCloseMusicalInstrument(event));
    }

    @EventHandler
    private void onPlayerBeat(PlayerBeatEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerBeat(event));
    }

    @EventHandler
    private void onPlayerMelodyComplete(PlayerMelodyCompleteEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerMelodyComplete(event));
    }

    @EventHandler
    private void onMagicMapCursor(MagicMapCursorEvent event) {
        Festival festival = plugin.getFestival(event.getPlayer().getWorld());
        if (festival == null) return;
        Session session = festival.sessionOf(event.getPlayer());
        for (Attraction attraction : festival.getAttractions()) {
            if (attraction.isDisabled()) continue;
            Vec3i vec = attraction.getNpcVector();
            if (vec == null) continue;
            if (vec.x < event.getMinX() || vec.x > event.getMaxX()) continue;
            if (vec.z < event.getMinZ() || vec.z > event.getMaxZ()) continue;
            boolean completed = session.isUniqueLocked(attraction);
            boolean pickedUp = session.getPrizeWaiting(attraction) != 2;
            MapCursor.Type cursorType;
            if (completed && pickedUp) {
                cursorType = MapCursor.Type.MANSION;
            } else if (completed && !pickedUp) {
                cursorType = MapCursor.Type.RED_MARKER;
            } else {
                cursorType = attraction.getType().getMapCursorIcon();
            }
            MapCursor mapCursor = Cursors.make(cursorType,
                                               vec.x - event.getMinX(),
                                               vec.z - event.getMinZ(),
                                               8);
            Component caption = attraction.getBooth().getMapCursorCaption();
            if (caption != null) mapCursor.caption(caption);
            event.getCursors().addCursor(mapCursor);
        }
    }

    @EventHandler
    private void onThrownEggHatch(ThrownEggHatchEvent event) {
        Festival festival = plugin.getFestival(event.getEgg().getWorld());
        if (festival != null) {
            event.setHatching(false);
        }
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        Attraction<?> attraction = plugin.getAttraction(event.getPlayer().getLocation());
        if (attraction == null) return;
        if (attraction.isPlaying()) {
            attraction.onPlayerHud(event);
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && !attraction.getDebugLines().isEmpty()) {
            event.sidebar(PlayerHudPriority.LOW, attraction.getDebugLines());
        }
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        Festival festival = plugin.getFestival(event.getWorld());
        if (festival != null) festival.load();
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        Festival festival = plugin.getFestival(event.getWorld());
        if (festival != null) festival.unload();
    }

    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        plugin.applyAttraction(event.getTo(), a -> a.onPlayerTeleport(event));
    }

    @EventHandler
    private void onEntityToggleGlide(EntityToggleGlideEvent event) {
        plugin.applyAttraction(event.getEntity().getLocation(), a -> a.onEntityToggleGlide(event));
    }

    @EventHandler
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery event) {
        plugin.applyAttraction(event.getBlock(), a -> a.onPlayerBlockAbility(event));
    }
}
