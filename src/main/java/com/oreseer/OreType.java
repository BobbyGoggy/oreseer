package com.oreseer;

import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

public enum OreType {
	ANCIENT_DEBRIS("Ancient Debris", s -> s.isOf(Blocks.ANCIENT_DEBRIS), true, HighlightStyle.RIPPLE, 1),
	DIAMOND("Diamond", s -> s.isIn(BlockTags.DIAMOND_ORES), true, HighlightStyle.CRYSTAL, 3),
	EMERALD("Emerald", s -> s.isIn(BlockTags.EMERALD_ORES), true, HighlightStyle.ORBIT, 4),
	GOLD("Gold", s -> s.isIn(BlockTags.GOLD_ORES), true, HighlightStyle.AURORA, 5),
	REDSTONE("Redstone", s -> s.isIn(BlockTags.REDSTONE_ORES), true, HighlightStyle.HEARTBEAT, 2),
	LAPIS("Lapis", s -> s.isIn(BlockTags.LAPIS_ORES), true, HighlightStyle.PRISM, 8),
	QUARTZ("Nether Quartz", s -> s.isOf(Blocks.NETHER_QUARTZ_ORE), true, HighlightStyle.GLITCH, 7),
	IRON("Iron", s -> s.isIn(BlockTags.IRON_ORES), false, HighlightStyle.PRISM, 9),
	COPPER("Copper", s -> s.isIn(BlockTags.COPPER_ORES), false, HighlightStyle.AURORA, 1),
	COAL("Coal", s -> s.isIn(BlockTags.COAL_ORES), false, HighlightStyle.GLITCH, 7);

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

	/** Returns the ore type of a block state, or null if it is not an ore we track. */
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
