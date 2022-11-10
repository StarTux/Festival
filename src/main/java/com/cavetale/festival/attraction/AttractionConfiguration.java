package com.cavetale.festival.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.festival.Festival;
import com.cavetale.festival.booth.Booth;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AttractionConfiguration {
    protected final Festival festival;
    protected final AttractionType type;
    protected final String name;
    protected final List<Area> areaList;
    protected final Booth booth;
}
