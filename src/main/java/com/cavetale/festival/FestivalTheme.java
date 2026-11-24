package com.cavetale.festival;

import com.cavetale.festival.attraction.Music;
import com.cavetale.mytems.item.music.Melody;
import com.cavetale.resident.ZoneType;
import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

@Getter
@Builder(toBuilder = true)
public final class FestivalTheme {
    @NonNull @Builder.Default private final Melody successMelody = Music.TREASURE.melody;
    @NonNull @Builder.Default private final Melody failMelody = Music.DECKED_OUT.melody;
    @NonNull @Builder.Default private final Function<String, Component> formatter = Component::text;
    @NonNull @Builder.Default private final ZoneType zoneType = ZoneType.SPAWN;

    public Component format(final String value) {
        return formatter.apply(value);
    }
}
