package com.oreseer;

/** Per-ore user settings (stored in config/oreseer.json). */
public class OreSetting {
	public boolean enabled = true;
	public HighlightStyle style = HighlightStyle.CRYSTAL;
	public int colorIndex = 0;
	public float size = 1.0f;

	public static OreSetting defaultsFor(OreType type) {
		OreSetting s = new OreSetting();
		s.enabled = type.defaultEnabled;
		s.style = type.defaultStyle;
		s.colorIndex = type.defaultColor;
		return s;
	}
}
