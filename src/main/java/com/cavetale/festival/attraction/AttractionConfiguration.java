package com.cavetale.festival.attraction;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.festival.FestivalTheme;
import com.cavetale.festival.FestivalThemes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Data;

@Data
public final class AttractionConfiguration {
    private String name;
    private String world;
    private AttractionType type;
    private String theme;
    private transient FestivalTheme customTheme;
    private Cuboid main;
    private Map<String, Cuboid> areas = new LinkedHashMap<>();
    private Map<String, List<Cuboid>> areaLists = new LinkedHashMap<>();
    private Map<String, Object> settings = new LinkedHashMap<>();

    /** Applied before enable, including when the world is loaded again. */
    private transient Consumer<Attraction> initializer;

    FestivalTheme resolveTheme(final Consumer<String> debugLine) {
        if (customTheme != null) return customTheme;
        if (theme == null) return FestivalThemes.DEFAULT.getTheme();
        try {
            return FestivalThemes.valueOf(theme.toUpperCase(Locale.ROOT)).getTheme();
        } catch (IllegalArgumentException e) {
            debugLine.accept("Unknown theme '" + theme + "', using DEFAULT");
            return FestivalThemes.DEFAULT.getTheme();
        }
    }

    List<Map.Entry<String, Cuboid>> namedAreas() {
        final List<Map.Entry<String, Cuboid>> result = new ArrayList<>(areas.entrySet());
        areaLists.forEach((key, list) -> list.forEach(area -> result.add(Map.entry(key, area))));
        return result;
    }
}
