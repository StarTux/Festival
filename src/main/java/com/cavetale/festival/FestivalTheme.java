package com.cavetale.festival;

import com.cavetale.resident.ZoneType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

public enum FestivalTheme {
    TESTING {
        @Override public Component format(String txt) {
            return text(txt, GREEN);
        }

        @Override public ZoneType getZoneType() {
            return ZoneType.SPAWN;
        }
    },
    HALLOWEEN {
        @Override public Component format(String txt) {
            TextComponent.Builder builder = text();
            for (int i = 0; i < txt.length(); i += 1) {
                builder.append(text(txt.charAt(i), i % 2 == 0 ? GOLD : RED));
            }
            return builder.build();
        }

        @Override public ZoneType getZoneType() {
            return ZoneType.HALLOWEEN;
        }
    },
    CHRISTMAS {
        @Override public Component format(String txt) {
            TextComponent.Builder builder = text();
            for (int i = 0; i < txt.length(); i += 1) {
                builder.append(text(txt.substring(i, i + 1),
                                    i % 2 == 0 ? color(0xE40010) : color(0x00B32C)));
            }
            return builder.build();
        };

        @Override public ZoneType getZoneType() {
            return ZoneType.CHRISTMAS;
        }
    },
    Valentine {
        @Override public Component format(String txt) {
            return text(txt, color(0xFF69B4));
        };

        @Override public ZoneType getZoneType() {
            return ZoneType.NONE;
        }
    },
    ;

    public abstract Component format(String txt);

    public abstract ZoneType getZoneType();
}
