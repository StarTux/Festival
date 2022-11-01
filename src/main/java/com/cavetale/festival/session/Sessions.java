package com.cavetale.festival.session;

import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.festival.Festival;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class Sessions {
    protected final Festival festival;
    protected final Map<UUID, Session> sessionsMap = new HashMap<>();

    public void clear() {
        for (Session session : sessionsMap.values()) {
            session.save();
        }
        sessionsMap.clear();
    }

    public void clear(UUID uuid) {
        Session session = sessionsMap.remove(uuid);
        if (session != null) session.save();
    }

    public Session of(Player player) {
        return sessionsMap.computeIfAbsent(player.getUniqueId(), u -> {
                Session newSession = new Session(festival, player.getUniqueId(), player.getName());
                newSession.load();
                return newSession;
            });
    }

    public Session of(PlayerCache player) {
        return sessionsMap.computeIfAbsent(player.uuid, u -> {
                Session newSession = new Session(festival, player.uuid, player.name);
                newSession.load();
                return newSession;
            });
    }
}
