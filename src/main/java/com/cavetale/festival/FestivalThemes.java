package com.cavetale.festival;

import com.cavetale.festival.attraction.Music;
import com.cavetale.resident.ZoneType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

@Getter
@RequiredArgsConstructor
public enum FestivalThemes {
    DEFAULT(FestivalTheme.builder().build()),
    TESTING(FestivalTheme.builder()
            .formatter(value -> text(value, GREEN))
            .build()),
    HALLOWEEN(FestivalTheme.builder()
              .formatter(value -> alternating(value, GOLD, RED))
              .zoneType(ZoneType.HALLOWEEN)
              .build()),
    CHRISTMAS(FestivalTheme.builder()
              .formatter(value -> alternating(value, color(0xE40010), color(0x00B32C)))
              .zoneType(ZoneType.CHRISTMAS)
              .successMelody(Music.DECK_THE_HALLS.melody)
              .failMelody(Music.GRINCH.melody)
              .build()),
    VALENTINE(FestivalTheme.builder()
              .formatter(value -> text(value, color(0xFF69B4)))
              .zoneType(ZoneType.NONE)
              .build());

    private final FestivalTheme theme;

    private static Component alternating(final String value, final TextColor first, final TextColor second) {
        final TextComponent.Builder result = text();
        boolean useFirst = true;
        for (int i = 0; i < value.length();) {
            final int codePoint = value.codePointAt(i);
            result.append(text(Character.toString(codePoint), useFirst ? first : second));
            useFirst = !useFirst;
            i += Character.charCount(codePoint);
        }
        return result.build();
    }
}
