
package com.oreseer;

import java.util.function.Predicate;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum OreType {
    ANCIENT_DEBRIS("Ancient Debris", s -> s.is(Blocks.ANCIENT_DEBRIS), true, HighlightStyle.RIPPLE, 1),
    DIAMOND("Diamond", s -> s.is(BlockItemTags.DIAMOND_ORES.block()), true, HighlightStyle.CRYSTAL, 3),
    EMERALD("Emerald", s -> s.is(BlockItemTags.EMERALD_ORES.block()), true, HighlightStyle.ORBIT, 4),
    GOLD("Gold", s -> s.is(BlockItemTags.GOLD_ORES.block()), true, HighlightStyle.AURORA, 5),
    REDSTONE("Redstone", s -> s.is(BlockItemTags.REDSTONE_ORES.block()), true, HighlightStyle.HEARTBEAT, 2),
    LAPIS("Lapis", s -> s.is(BlockItemTags.LAPIS_ORES.block()), true, HighlightStyle.PRISM, 8),
    QUARTZ("Nether Quartz", s -> s.is(Blocks.NETHER_QUARTZ_ORE), true, HighlightStyle.GLITCH, 7),
    IRON("Iron", s -> s.is(BlockItemTags.IRON_ORES.block()), false, HighlightStyle.PRISM, 9),
    COPPER("Copper", s -> s.is(BlockItemTags.COPPER_ORES.block()), false, HighlightStyle.AURORA, 1),
    COAL("Coal", s -> s.is(BlockItemTags.COAL_ORES.block()), false, HighlightStyle.GLITCH, 7);

    public static final OreType[] ALL = values();

    public final String label;
    private final Predicate<BlockState> matcher;
    public final boolean defaultEnabled;
    public final HighlightStyle defaultStyle;
    public final int defaultColor;

    OreType(String label, Predicate<BlockState> matcher, boolean defaultEnabled, HighlightStyle defaultStyle, int defaultColor) {
        this.label = label;
        this.matcher = matcher;
        this.defaultEnabled = defaultEnabled;
        this.defaultStyle = defaultStyle;
        this.defaultColor = defaultColor;
    }

    public boolean matches(BlockState state) {
        return matcher.test(state);
    }

    public static OreType of(BlockState state) {
        if (state.isAir()) {
            return null;
        }
        for (OreType t : ALL) {
            if (t.matches(state)) {
                return t;
            }
        }
        return null;
    }
}
