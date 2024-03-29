package com.cavetale.festival.session;

import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.util.Json;
import com.cavetale.festival.Festival;
import com.cavetale.festival.attraction.Attraction;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;

/**
 * Player save file per festival.
 */
@Getter
public final class Session {
    protected final Festival festival;
    protected final UUID uuid;
    protected final String name;
    protected final File saveFile;
    protected Tag tag;

    protected Session(final Festival festival, final UUID uuid, final String name) {
        this.festival = festival;
        this.uuid = uuid;
        this.name = name;
        this.saveFile = new File(festival.getPlayersFolder(), uuid + ".json");
    }

    protected void load() {
        this.tag = Json.load(saveFile, Tag.class, Tag::new);
    }

    public void save() {
        Json.save(saveFile, tag, true);
    }

    public static List<Session> loadAll(Festival festival) {
        List<Session> result = new ArrayList<>();
        for (File file : festival.getPlayersFolder().listFiles()) {
            String name = file.getName();
            if (!file.getName().endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            UUID uuid = UUID.fromString(name);
            Session session = new Session(festival, uuid, PlayerCache.nameForUuid(uuid));
            session.load();
            result.add(session);
        }
        return result;
    }

    public Duration getCooldown(Attraction attraction) {
        Long cd = tag.cooldowns.get(attraction.getUniqueKey());
        if (cd == null) return null;
        long now = System.currentTimeMillis();
        if (now > cd) {
            tag.cooldowns.remove(attraction.getUniqueKey());
            return null;
        }
        return Duration.ofMillis(cd - now);
    }

    public void setCooldown(Attraction attraction, Duration duration) {
        long newValue = duration.toMillis() + System.currentTimeMillis();
        Long oldValue = tag.cooldowns.get(attraction.getUniqueKey());
        if (oldValue != null) {
            newValue = Math.max(newValue, oldValue);
        }
        tag.cooldowns.put(attraction.getUniqueKey(), newValue);
    }

    public boolean isUniqueLocked(Attraction attraction) {
        return tag.uniquesGot.contains(attraction.getUniqueKey());
    }

    public void lockUnique(Attraction attraction) {
        tag.uniquesGot.add(attraction.getUniqueKey());
    }

    public int getCompletionCount(Attraction attraction) {
        return tag.completionCounts.getOrDefault(attraction.getUniqueKey(), 0);
    }

    public void addCompletion(Attraction attraction) {
        final int value = getCompletionCount(attraction);
        tag.completionCounts.put(attraction.getUniqueKey(), value + 1);
    }

    public boolean isTotallyCompleted() {
        return tag.totallyCompleted;
    }

    public void lockTotallyCompleted() {
        tag.totallyCompleted = true;
    }

    public int getPrizeWaiting(Attraction attraction) {
        Integer result = tag.prizesWaiting.get(attraction.getUniqueKey());
        return result != null ? result : 0;
    }

    public void setFirstCompletionPrizeWaiting(Attraction attraction) {
        tag.prizesWaiting.put(attraction.getUniqueKey(), 2);
    }

    public void setRegularCompletionPrizeWaiting(Attraction attraction) {
        tag.prizesWaiting.put(attraction.getUniqueKey(), 1);
    }

    public int getProgress() {
        return tag.progress;
    }

    public void setProgress(int progress) {
        tag.progress = progress;
    }

    public void clearPrizeWaiting(Attraction attraction) {
        tag.prizesWaiting.remove(attraction.getUniqueKey());
    }

    public Set<String> getCollection() {
        return tag.collection;
    }

    public void reset() {
        tag = new Tag();
    }

    @Data
    public static final class Tag {
        protected boolean totallyCompleted;
        protected final Map<String, Long> cooldowns = new HashMap<>();
        protected final Set<String> uniquesGot = new HashSet<>();
        protected final Map<String, Integer> completionCounts = new HashMap<>();
        protected final Map<String, Integer> prizesWaiting = new HashMap<>(); // 1 = regular, 2 = unique
        protected final Set<String> collection = new HashSet<>(); // Festival specific
        protected int progress; // Festival specific
    }
}
